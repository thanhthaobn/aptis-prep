package com.aptisprep.mvc.service.grading.impl;

import com.aptisprep.mvc.config.AiProperties;
import com.aptisprep.mvc.dto.AiAssessmentDTO;
import com.aptisprep.mvc.dto.AiAssessmentJobDTO;
import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import com.aptisprep.mvc.enums.CefrLevel;
import com.aptisprep.mvc.event.AiAssessmentCompletedEvent;
import com.aptisprep.mvc.exception.AccessDeniedException;
import com.aptisprep.mvc.exception.AiGradingException;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.exception.ResourceNotFoundException;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.service.exam.AttemptOutboxService;
import com.aptisprep.mvc.service.grading.*;
import com.aptisprep.mvc.service.grading.AiAssessmentService;
import com.aptisprep.mvc.service.storage.StorageService;
import com.aptisprep.mvc.support.contract.GradingTextLimits;
import com.aptisprep.mvc.support.grading.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AiAssessmentServiceImpl implements AiAssessmentService {
    private Logger log = LoggerFactory.getLogger(AiAssessmentService.class);
    private Set<String> SKILLS = Set.of("WRITING", "SPEAKING");
    private Set<String> LEVELS = Set.copyOf(CefrLevel.codes());
    private AiAssessmentJobRepository aiAssessmentJobRepository;
    private AiAssessmentRepository aiAssessmentRepository;
    private UserRepository userRepository;
    private WritingGradingService writing;
    private SpeakingGradingService speaking;
    private StorageService storage;
    private TransactionTemplate transactions;
    private ApplicationEventPublisher events;
    private AttemptOutboxService outbox;
    private AiProperties properties;

    public AiAssessmentServiceImpl(
            AiAssessmentJobRepository aiAssessmentJobRepository,
            AiAssessmentRepository aiAssessmentRepository,
            UserRepository userRepository,
            WritingGradingService writing,
            SpeakingGradingService speaking,
            StorageService storage,
            PlatformTransactionManager transactionManager,
            ApplicationEventPublisher events,
            AttemptOutboxService outbox,
            AiProperties properties) {
        this.aiAssessmentJobRepository = aiAssessmentJobRepository;
        this.aiAssessmentRepository = aiAssessmentRepository;
        this.userRepository = userRepository;
        this.writing = writing;
        this.speaking = speaking;
        this.storage = storage;
        this.transactions = new TransactionTemplate(transactionManager);
        this.events = events;
        this.outbox = outbox;
        this.properties = properties;
    }

    @Override
    public AiAssessmentDTO request(AiAssessmentJobDTO request) {
        validateRequest(request);
        try {
            return transactions.execute(status -> createOrReadAssessment(request));
        } catch (DataIntegrityViolationException duplicate) {
            return transactions.execute(status -> readExistingAssessment(request));
        }
    }

    @Override
    public AiAssessmentDTO process(String assessmentKey) {
        JobSnapshot claim = transactions.execute(status -> claim(assessmentKey));
        if (claim.view() != null) {
            if (claim.replayEvent() != null) {
                publishAfterCommit(claim.replayEvent());
            }
            return claim.view();
        }
        GradingResult result;
        try {
            result =
                    switch (claim.taskType()) {
                        case "WRITING" -> writing.grade(claim.taskContent(), claim.lockedContent());
                        case "SPEAKING" ->
                                speaking.gradeTranscript(claim.taskContent(), transcriptFor(claim));
                        default ->
                                throw new AiGradingException("Dạng bài tự luận không được hỗ trợ.");
                    };
            result = normalizePracticeScore(result, claim.maxScore());
            validateResult(result, claim.maxScore());
        } catch (AiGradingException exception) {
            return fail(claim, cleanError(exception.getMessage()));
        } catch (RuntimeException exception) {
            return fail(claim, "Chưa thể chấm bài lúc này. Vui lòng thử lại sau.");
        }
        return succeed(claim, result);
    }

    private String transcriptFor(JobSnapshot claim) {
        if (!blank(claim.transcript())) return claim.transcript();
        if (!claim.examAnswer())
            throw new AiGradingException("Chưa có transcript để chấm Speaking.");

        // Job của đề thi chứa đường dẫn audio đã khóa, được nhóm theo đúng nhãn của prompt.
        // Tạo transcript sau khi nhận job để các lần replay/retry vẫn dùng đúng audio đó.
        List<String> transcripts = new ArrayList<>();
        for (String item : claim.lockedContent().split("\n\n|\n--- ITEM ---\n")) {
            int lineBreak = item.startsWith("Mục ") ? item.indexOf('\n') : -1;
            String path = item.substring(lineBreak + 1).trim();
            if (path.equals("[Chưa trả lời]")) continue;
            String transcript = speaking.transcribe(storage.localPath(path));
            if (blank(transcript))
                throw new AiGradingException("Chưa có transcript để chấm Speaking.");
            transcripts.add(item.substring(0, lineBreak + 1) + transcript);
        }
        if (transcripts.isEmpty())
            throw new AiGradingException("Chưa có transcript để chấm Speaking.");
        return String.join("\n\n", transcripts);
    }

    @Override
    public AiAssessmentDTO retry(String assessmentKey, Integer requesterId, boolean admin) {
        transactions.executeWithoutResult(
                status -> {
                    AiAssessmentJob job = jobForUpdate(assessmentKey);
                    authorize(job, requesterId, admin);
                    if (job.getState() != AiJobState.ERROR)
                        throw new BusinessRuleException("Chỉ tác vụ lỗi mới được thử lại");
                    if (job.getRetryCount() >= AiProcessingPolicy.MAX_ATTEMPTS)
                        throw new BusinessRuleException("Tác vụ đã hết số lần thử lại");
                    job.setState(AiJobState.PENDING);
                    job.setProcessingToken(null);
                    job.setLastError(null);
                    job.setNextRetryAt(null);
                    aiAssessmentJobRepository.save(job);
                    aiAssessmentRepository
                            .findFirstByJobIdAndAnswerVersionOrderByIdDesc(
                                    job.getId(), job.getAnswerVersion())
                            .ifPresent(
                                    value -> {
                                        value.setState(AiAssessmentState.PENDING);
                                        value.setErrorMessage(null);
                                        aiAssessmentRepository.save(value);
                                    });
                });
        return process(assessmentKey);
    }

    @Override
    public AiAssessmentDTO get(String assessmentKey, Integer requesterId, boolean admin) {
        AiAssessmentJob job = job(assessmentKey);
        authorize(job, requesterId, admin);
        return viewFor(job);
    }

    @Override
    public AiAssessmentDTO getForGrading(String assessmentKey) {
        return viewFor(job(assessmentKey));
    }

    private JobSnapshot claim(String key) {
        AiAssessmentJob job =
                aiAssessmentJobRepository
                        .findByAssessmentKeyForUpdate(key)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy tác vụ đánh giá"));
        if (job.getState() == AiJobState.COMPLETED) {
            AiAssessmentCompletedEvent event = completedEvent(job);
            // Mọi luồng ghi AI outbox đều giữ cùng khóa job, kể cả luồng bổ sung row bị thiếu.
            if (event != null) outbox.ensureAiAssessmentCompleted(event);
            return new JobSnapshot(
                    key,
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    viewFor(job),
                    event);
        }
        if (job.getState() == AiJobState.ERROR) {
            // Lỗi từ provider phải đi qua retry được cấp quyền, không được vô tình gọi process lần
            // hai.
            return new JobSnapshot(
                    key,
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    viewFor(job),
                    null);
        }
        if (job.getState() == AiJobState.PROCESSING && !isStale(job)) {
            return new JobSnapshot(
                    key,
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    viewFor(job),
                    null);
        }
        if (job.getRetryCount() >= AiProcessingPolicy.MAX_ATTEMPTS) return exhausted(job);
        String processingToken = UUID.randomUUID().toString();
        job.setState(AiJobState.PROCESSING);
        job.setProcessingToken(processingToken);
        job.setNextRetryAt(null);
        job.setRetryCount(job.getRetryCount() + 1);
        aiAssessmentJobRepository.save(job);
        return new JobSnapshot(
                key,
                "EXAM_ANSWER".equals(job.getSourceType()),
                job.getTaskType(),
                job.getTaskContent(),
                job.getLockedContent(),
                job.getTranscript(),
                job.getMaxScore(),
                job.getUserId(),
                job.getAttemptId(),
                job.getAnswerId(),
                job.getAnswerVersion(),
                processingToken,
                null,
                null);
    }

    private AiAssessmentDTO succeed(JobSnapshot claim, GradingResult result) {
        FinishSnapshot finish =
                transactions.execute(
                        status -> {
                            AiAssessmentJob job = jobForUpdate(claim.assessmentKey());
                            if (job.getState() != AiJobState.PROCESSING
                                    || !Objects.equals(
                                            job.getProcessingToken(), claim.processingToken()))
                                return new FinishSnapshot(viewFor(job), null, false);
                            job.setState(AiJobState.COMPLETED);
                            job.setProcessingToken(null);
                            job.setLastError(null);
                            job.setNextRetryAt(null);
                            aiAssessmentJobRepository.save(job);
                            AiAssessment assessment =
                                    aiAssessmentRepository
                                            .findFirstByJobIdAndAnswerVersionOrderByIdDesc(
                                                    job.getId(), job.getAnswerVersion())
                                            .orElseThrow();
                            assessment.setState(AiAssessmentState.COMPLETED);
                            assessment.setScore(result.estimatedScore());
                            assessment.setEstimatedCefr(result.estimatedCefr());
                            assessment.setFeedback(result.feedback());
                            assessment.setTranscript(
                                    result.transcript() == null
                                            ? job.getTranscript()
                                            : result.transcript());
                            assessment.setProvider(result.provider());
                            assessment.setModel(result.model());
                            assessment.setErrorMessage(null);
                            assessment.setCompletedAt(LocalDateTime.now());
                            aiAssessmentRepository.save(assessment);
                            AiAssessmentCompletedEvent event = completedEvent(job);
                            if (event != null) outbox.enqueueAiAssessmentCompleted(event);
                            return new FinishSnapshot(viewFor(job), event, true);
                        });
        if (finish.applied() && finish.event() != null) publishAfterCommit(finish.event());
        return finish.view();
    }

    private void publishAfterCommit(AiAssessmentCompletedEvent event) {
        // Feedback của bài học không phải kết quả đề thi, kể cả khi hai ID tình cờ giống nhau.
        boolean examSource =
                Boolean.TRUE.equals(
                        transactions.execute(
                                status ->
                                        aiAssessmentJobRepository
                                                .findByAssessmentKey(event.assessmentKey())
                                                .map(
                                                        job ->
                                                                "EXAM_ANSWER"
                                                                        .equals(
                                                                                job
                                                                                        .getSourceType()))
                                                .orElse(false)));
        if (!examSource) return;
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            events.publishEvent(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        events.publishEvent(event);
                    }
                });
    }

    @Scheduled(fixedDelayString = "${app.ai.recovery-ms:30000}")
    public void recoverAiJobs() {
        LocalDateTime now = LocalDateTime.now();
        PageRequest page = PageRequest.of(0, AiProcessingPolicy.RECOVERY_BATCH_SIZE);
        List<String> recoverable =
                properties == null || properties.isEnabled()
                        ? aiAssessmentJobRepository.findRecoverableKeys(
                                AiJobState.PENDING,
                                AiJobState.PROCESSING,
                                now,
                                now.minusMinutes(AiProcessingPolicy.LEASE_MINUTES),
                                page)
                        : List.<String>of();
        for (String key : recoverable) {
            try {
                // claim() nhận lại lease một cách nguyên tử. Không có khoảng PENDING đã commit.
                process(key);
            } catch (RuntimeException exception) {
                deferPending(key);
            }
        }
        for (String key : aiAssessmentJobRepository.findCompletedWithoutCallback(page)) {
            try {
                transactions.executeWithoutResult(
                        status -> {
                            AiAssessmentJob job = jobForUpdate(key);
                            if (job.getState() != AiJobState.COMPLETED) return;
                            AiAssessmentCompletedEvent event = completedEvent(job);
                            if (event != null) outbox.ensureAiAssessmentCompleted(event);
                        });
            } catch (RuntimeException exception) {
                log.warn("AI callback recovery will retry {}", key);
            }
        }
    }

    private void deferPending(String key) {
        try {
            transactions.executeWithoutResult(
                    status -> {
                        AiAssessmentJob job = jobForUpdate(key);
                        // Lỗi provider/database có thể xảy ra sau khi claim đã commit.
                        // Cũng phải nhả lease đó; nếu không job sẽ bị kẹt ở PROCESSING
                        // cho đến khi hết thời gian stale-lease và luồng khôi phục sau restart
                        // không thể tiếp tục.
                        if (job.getState() == AiJobState.PENDING
                                || job.getState() == AiJobState.PROCESSING) {
                            job.setState(AiJobState.PENDING);
                            job.setProcessingToken(null);
                            job.setNextRetryAt(
                                    LocalDateTime.now()
                                            .plusSeconds(AiProcessingPolicy.CLAIM_RETRY_SECONDS));
                            aiAssessmentJobRepository.save(job);
                        }
                    });
        } catch (RuntimeException unavailable) {
            log.warn("AI recovery will retry {} when the database is available", key);
        }
    }

    private JobSnapshot exhausted(AiAssessmentJob job) {
        String message = "Tác vụ đã hết số lần xử lý sau khi lượt trước bị gián đoạn";
        job.setState(AiJobState.ERROR);
        job.setProcessingToken(null);
        job.setNextRetryAt(null);
        job.setLastError(message);
        aiAssessmentJobRepository.save(job);
        aiAssessmentRepository
                .findFirstByJobIdAndAnswerVersionOrderByIdDesc(job.getId(), job.getAnswerVersion())
                .ifPresent(
                        value -> {
                            value.setState(AiAssessmentState.ERROR);
                            value.setScore(null);
                            value.setEstimatedCefr(null);
                            value.setFeedback(null);
                            value.setCompletedAt(null);
                            value.setErrorMessage(message);
                            aiAssessmentRepository.save(value);
                        });
        AiAssessmentCompletedEvent event =
                "EXAM_ANSWER".equals(job.getSourceType())
                                && job.getAttemptId() != null
                                && job.getAnswerId() != null
                        ? new AiAssessmentCompletedEvent(
                                job.getAssessmentKey(),
                                job.getUserId(),
                                job.getAttemptId(),
                                job.getAnswerId(),
                                "ERROR",
                                message,
                                job.getAnswerVersion())
                        : null;
        if (event != null) outbox.enqueueAiAssessmentCompleted(event);
        return new JobSnapshot(
                job.getAssessmentKey(),
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                viewFor(job),
                event);
    }

    private boolean isStale(AiAssessmentJob job) {
        return job.getUpdatedAt() == null
                || job.getUpdatedAt()
                        .isBefore(
                                LocalDateTime.now().minusMinutes(AiProcessingPolicy.LEASE_MINUTES));
    }

    private AiAssessmentCompletedEvent completedEvent(AiAssessmentJob job) {
        if (!"EXAM_ANSWER".equals(job.getSourceType())
                || job.getAttemptId() == null
                || job.getAnswerId() == null) return null;
        return aiAssessmentRepository
                .findFirstByJobIdAndAnswerVersionOrderByIdDesc(job.getId(), job.getAnswerVersion())
                .filter(
                        value ->
                                value.getState() == AiAssessmentState.COMPLETED
                                        && value.getScore() != null)
                .map(
                        value ->
                                new AiAssessmentCompletedEvent(
                                        job.getAssessmentKey(),
                                        job.getUserId(),
                                        job.getAttemptId(),
                                        job.getAnswerId(),
                                        "COMPLETED",
                                        null,
                                        job.getAnswerVersion()))
                .orElse(null);
    }

    private AiAssessmentDTO fail(JobSnapshot claim, String error) {
        FailureSnapshot failure =
                transactions.execute(
                        status -> {
                            AiAssessmentJob job = jobForUpdate(claim.assessmentKey());
                            if (job.getState() != AiJobState.PROCESSING
                                    || !Objects.equals(
                                            job.getProcessingToken(), claim.processingToken()))
                                return new FailureSnapshot(viewFor(job), null, false);
                            job.setState(AiJobState.ERROR);
                            job.setProcessingToken(null);
                            job.setNextRetryAt(null);
                            job.setLastError(error);
                            aiAssessmentJobRepository.save(job);
                            AiAssessment assessment =
                                    aiAssessmentRepository
                                            .findFirstByJobIdAndAnswerVersionOrderByIdDesc(
                                                    job.getId(), job.getAnswerVersion())
                                            .orElseThrow();
                            assessment.setState(AiAssessmentState.ERROR);
                            assessment.setScore(null);
                            assessment.setEstimatedCefr(null);
                            assessment.setFeedback(null);
                            assessment.setErrorMessage(error);
                            assessment.setCompletedAt(null);
                            aiAssessmentRepository.save(assessment);
                            AiAssessmentCompletedEvent event =
                                    new AiAssessmentCompletedEvent(
                                            job.getAssessmentKey(),
                                            job.getUserId(),
                                            job.getAttemptId(),
                                            job.getAnswerId(),
                                            "ERROR",
                                            error,
                                            job.getAnswerVersion());
                            if ("EXAM_ANSWER".equals(job.getSourceType()))
                                outbox.enqueueAiAssessmentCompleted(event);
                            return new FailureSnapshot(viewFor(job), event, true);
                        });
        if (failure.applied() && failure.event() != null) publishAfterCommit(failure.event());
        return failure.view();
    }

    private AiAssessmentDTO viewFor(AiAssessmentJob job) {
        AiAssessment assessment =
                aiAssessmentRepository
                        .findFirstByJobIdAndAnswerVersionOrderByIdDesc(
                                job.getId(), job.getAnswerVersion())
                        .orElse(null);
        if (assessment == null) {
            AiAssessmentState state =
                    job.getState() == AiJobState.COMPLETED
                            ? AiAssessmentState.COMPLETED
                            : job.getState() == AiJobState.ERROR
                                    ? AiAssessmentState.ERROR
                                    : AiAssessmentState.PENDING;
            return new AiAssessmentDTO(
                    null,
                    job,
                    job.getAssessmentKey(),
                    job.getAnswerId(),
                    job.getAnswerVersion(),
                    state,
                    null,
                    null,
                    null,
                    job.getTranscript(),
                    job.getRubricVersion(),
                    job.getPolicyVersion(),
                    null,
                    null,
                    job.getLastError(),
                    job.getCreatedAt(),
                    null);
        }
        return AiAssessmentDTO.convertEntityToDto(assessment);
    }

    private void validateRequest(AiAssessmentJobDTO request) {
        if (request == null
                || blank(request.getAssessmentKey())
                || request.getAssessmentKey().length() > 255
                || request.getUserId() == null
                || request.getAnswerVersion() == null
                || request.getAnswerVersion() < 1)
            throw new BusinessRuleException("Yêu cầu đánh giá không hợp lệ");
        boolean exam = "EXAM_ANSWER".equals(request.getSourceType());
        boolean lesson = "LESSON_SUBMISSION".equals(request.getSourceType());
        if ((!exam && !lesson)
                || request.getSourceId() == null
                || request.getSourceId() < 1
                || (exam && (request.getAttemptId() == null || request.getAnswerId() == null))
                || (lesson && (request.getAttemptId() != null || request.getAnswerId() != null)))
            throw new BusinessRuleException("Nguồn bài đánh giá không hợp lệ");
        if (blank(request.getTaskContent())
                || request.getTaskContent().length() > GradingTextLimits.MAX_AI_TASK_CHARS
                || blank(request.getLockedContent())
                || request.getLockedContent().length() > GradingTextLimits.MAX_AI_SUBMISSION_CHARS
                || request.getTaskContent().length() + request.getLockedContent().length()
                        > GradingTextLimits.MAX_AI_PAYLOAD_CHARS) {
            throw new BusinessRuleException("Nội dung bài đã khóa vượt giới hạn ngữ cảnh chấm AI");
        }
        if (blank(request.getRubricVersion())
                || blank(request.getPolicyVersion())
                || request.getMaxScore() == null
                || request.getMaxScore().compareTo(BigDecimal.ZERO) <= 0
                || request.getMaxScore().compareTo(new BigDecimal("9999.99")) > 0)
            throw new BusinessRuleException(
                    "Thiếu phiên bản rubric/policy hoặc thang điểm không hợp lệ");
        normalizeSkill(request.getSkill());
        normalizeTask(request.getTaskType());
    }

    /** AI evaluates on a stable 0-50 internal scale; Practice mode stores the result on the
     * administrator-defined point range of the Writing/Speaking task. */
    private GradingResult normalizePracticeScore(GradingResult result, BigDecimal max) {
        if (result == null || result.estimatedScore() == null || max == null) return result;
        BigDecimal normalized =
                result.estimatedScore()
                        .multiply(max)
                        .divide(BigDecimal.valueOf(50), 8, java.math.RoundingMode.HALF_UP)
                        .setScale(2, java.math.RoundingMode.HALF_UP);
        return new GradingResult(
                normalized,
                result.estimatedCefr(),
                result.feedback(),
                result.provider(),
                result.model(),
                result.transcript());
    }

    private boolean sameRequest(AiAssessmentJob job, AiAssessmentJobDTO request) {
        return Objects.equals(job.getUserId(), request.getUserId())
                && Objects.equals(job.getSourceType(), request.getSourceType())
                && Objects.equals(job.getSourceId(), request.getSourceId())
                // Job bài học cũ từng dùng submission ID trong các cột chỉ dành cho đề thi này.
                && ("LESSON_SUBMISSION".equals(request.getSourceType())
                        || (Objects.equals(job.getAttemptId(), request.getAttemptId())
                                && Objects.equals(job.getAnswerId(), request.getAnswerId())))
                && Objects.equals(job.getAnswerVersion(), request.getAnswerVersion())
                && Objects.equals(job.getSkill(), normalizeSkill(request.getSkill()))
                && Objects.equals(job.getTaskType(), normalizeTask(request.getTaskType()))
                && Objects.equals(job.getTaskContent(), request.getTaskContent().trim())
                && Objects.equals(job.getLockedContent(), request.getLockedContent().trim())
                && Objects.equals(job.getRubricVersion(), request.getRubricVersion().trim())
                && Objects.equals(job.getPolicyVersion(), request.getPolicyVersion().trim())
                && job.getMaxScore() != null
                && job.getMaxScore().compareTo(request.getMaxScore()) == 0
                // Bản ghi âm của đề thi mới là input. Transcript đã tạo trước đó có thể khác.
                && (("EXAM_ANSWER".equals(job.getSourceType())
                                && "SPEAKING".equals(job.getTaskType()))
                        || Objects.equals(
                                job.getTranscript(), blankToNull(request.getTranscript())));
    }

    private User activeUser(Integer id) {
        User user =
                userRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (user.getRole() != Role.STUDENT || user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("Tài khoản không còn hoạt động");
        }
        return user;
    }

    private AiAssessmentJob job(String key) {
        return aiAssessmentJobRepository
                .findByAssessmentKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tác vụ đánh giá"));
    }

    private AiAssessmentJob jobForUpdate(String key) {
        return aiAssessmentJobRepository
                .findByAssessmentKeyForUpdate(key)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tác vụ đánh giá"));
    }

    private void authorize(AiAssessmentJob job, Integer requesterId, boolean admin) {
        User requester =
                userRepository
                        .findById(requesterId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền xem đánh giá này"));
        if (requester.getStatus() != UserStatus.ACTIVE
                || (admin && requester.getRole() != Role.ADMIN)
                || (!admin
                        && (requester.getRole() != Role.STUDENT
                                || !Objects.equals(job.getUserId(), requesterId)))) {
            throw new AccessDeniedException("Bạn không có quyền xem đánh giá này");
        }
    }

    private String normalizeSkill(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!SKILLS.contains(normalized))
            throw new BusinessRuleException("Kỹ năng AI không hợp lệ");
        return normalized;
    }

    private String normalizeTask(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("WRITING") && !normalized.equals("SPEAKING"))
            throw new BusinessRuleException("Dạng bài AI không hợp lệ");
        return normalized;
    }

    private void validateResult(GradingResult result, BigDecimal max) {
        if (result == null
                || result.estimatedScore() == null
                || result.estimatedScore().compareTo(BigDecimal.ZERO) < 0
                || result.estimatedScore().compareTo(max) > 0
                || result.estimatedCefr() == null
                || !LEVELS.contains(result.estimatedCefr())
                || blank(result.feedback())
                || result.feedback().length() > 12000
                || blank(result.provider())
                || result.provider().length() > 50
                || blank(result.model())
                || result.model().length() > 100) {
            throw new AiGradingException(
                    "Chưa nhận được kết quả chấm hợp lệ. Vui lòng thử lại sau.");
        }
    }

    private String cleanError(String message) {
        return message == null || message.isBlank() || message.length() > 1000
                ? "Chưa thể chấm bài lúc này. Vui lòng thử lại sau."
                : message;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String blankToNull(String value) {
        return blank(value) ? null : value.trim();
    }

    private Instant instant(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private record JobSnapshot(
            String assessmentKey,
            boolean examAnswer,
            String taskType,
            String taskContent,
            String lockedContent,
            String transcript,
            BigDecimal maxScore,
            Integer userId,
            Integer attemptId,
            Integer answerId,
            Integer answerVersion,
            String processingToken,
            AiAssessmentDTO view,
            AiAssessmentCompletedEvent replayEvent) {}

    private record FinishSnapshot(
            AiAssessmentDTO view, AiAssessmentCompletedEvent event, boolean applied) {}

    private record FailureSnapshot(
            AiAssessmentDTO view, AiAssessmentCompletedEvent event, boolean applied) {}

    private AiAssessmentDTO createOrReadAssessment(AiAssessmentJobDTO request) {
        Optional<AiAssessmentJob> existing =
                aiAssessmentJobRepository.findByAssessmentKeyForUpdate(request.getAssessmentKey());
        if (existing.isPresent()) return matchingAssessment(existing.get(), request);
        User user = activeUser(request.getUserId());
        AiAssessmentJob job = newPendingJob(request, user.getId());
        aiAssessmentJobRepository.save(job);
        AiAssessment assessment = new AiAssessment();
        assessment.setJob(job);
        assessment.setAssessmentKey(job.getAssessmentKey());
        assessment.setAnswerId(job.getAnswerId());
        assessment.setAnswerVersion(job.getAnswerVersion());
        assessment.setState(AiAssessmentState.PENDING);
        assessment.setRubricVersion(job.getRubricVersion());
        assessment.setPolicyVersion(job.getPolicyVersion());
        aiAssessmentRepository.save(assessment);
        return viewFor(job);
    }

    private AiAssessmentJob newPendingJob(AiAssessmentJobDTO request, Integer userId) {
        AiAssessmentJob job = new AiAssessmentJob();
        job.setSourceType(request.getSourceType());
        job.setSourceId(request.getSourceId());
        job.setAssessmentKey(request.getAssessmentKey());
        job.setUserId(userId);
        job.setAttemptId(request.getAttemptId());
        job.setAnswerId(request.getAnswerId());
        job.setAnswerVersion(request.getAnswerVersion());
        job.setSkill(normalizeSkill(request.getSkill()));
        job.setTaskType(normalizeTask(request.getTaskType()));
        job.setTaskContent(request.getTaskContent().trim());
        job.setLockedContent(request.getLockedContent().trim());
        job.setRubricVersion(request.getRubricVersion().trim());
        job.setPolicyVersion(request.getPolicyVersion().trim());
        job.setMaxScore(request.getMaxScore());
        job.setTranscript(blankToNull(request.getTranscript()));
        job.setState(AiJobState.PENDING);
        job.setRetryCount(0);
        return job;
    }

    private AiAssessmentDTO readExistingAssessment(AiAssessmentJobDTO request) {
        AiAssessmentJob job =
                aiAssessmentJobRepository
                        .findByAssessmentKeyForUpdate(request.getAssessmentKey())
                        .orElseThrow(
                                () ->
                                        new BusinessRuleException(
                                                "Không thể tạo tác vụ đánh giá lúc này"));
        return matchingAssessment(job, request);
    }

    private AiAssessmentDTO matchingAssessment(AiAssessmentJob job, AiAssessmentJobDTO request) {
        if (!sameRequest(job, request))
            throw new BusinessRuleException(
                    "assessmentKey đã được dùng cho một phiên bản bài khác");
        return viewFor(job);
    }
}
