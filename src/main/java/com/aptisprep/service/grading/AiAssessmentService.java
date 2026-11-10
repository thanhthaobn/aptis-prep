package com.aptisprep.service.grading;

import com.aptisprep.entity.*;
import com.aptisprep.mvc.auth.entity.User;
import com.aptisprep.mvc.auth.repository.UserRepository;
import com.aptisprep.mvc.common.config.AiProperties;
import com.aptisprep.mvc.common.enums.*;
import com.aptisprep.mvc.common.enums.CefrLevel;
import com.aptisprep.mvc.common.event.AiAssessmentCompletedEvent;
import com.aptisprep.mvc.common.exception.AccessDeniedException;
import com.aptisprep.mvc.common.exception.AiGradingException;
import com.aptisprep.mvc.common.exception.BusinessRuleException;
import com.aptisprep.mvc.common.exception.ResourceNotFoundException;
import com.aptisprep.mvc.exam.service.AttemptOutboxService;
import com.aptisprep.mvc.storage.service.StorageService;
import com.aptisprep.repository.*;
import com.aptisprep.service.contract.AiAssessmentContract;
import com.aptisprep.service.contract.GradingTextLimits;
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
public class AiAssessmentService implements AiAssessmentContract {
    private static final Logger log = LoggerFactory.getLogger(AiAssessmentService.class);
    private static final Set<String> SKILLS = Set.of("WRITING", "SPEAKING");
    private static final Set<String> LEVELS = Set.copyOf(CefrLevel.codes());
    private final AiAssessmentJobRepository aiAssessmentJobRepository;
    private final AiAssessmentRepository aiAssessmentRepository;
    private final UserRepository userRepository;
    private final WritingGradingService writing;
    private final SpeakingGradingService speaking;
    private final StorageService storage;
    private final TransactionTemplate transactions;
    private final ApplicationEventPublisher events;
    private final AttemptOutboxService outbox;
    private final AiProperties properties;

    public AiAssessmentService(
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
    public AssessmentView request(AssessmentRequest request) {
        validateRequest(request);
        try {
            return transactions.execute(status -> createOrReadAssessment(request));
        } catch (DataIntegrityViolationException duplicate) {
            return transactions.execute(status -> readExistingAssessment(request));
        }
    }

    @Override
    public AssessmentView process(String assessmentKey) {
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

        // Exam jobs contain locked audio paths, grouped with the same labels as their prompts.
        // Generate the transcript after claiming the job so replay and retry use that same audio.
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
    public AssessmentView retry(String assessmentKey, Integer requesterId, boolean admin) {
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
    public AssessmentView get(String assessmentKey, Integer requesterId, boolean admin) {
        AiAssessmentJob job = job(assessmentKey);
        authorize(job, requesterId, admin);
        return viewFor(job);
    }

    @Override
    public AssessmentView getForGrading(String assessmentKey) {
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
            // All AI outbox writers hold the same job lock, including a missing-row backfill.
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
            // A provider error requires an authorized retry, not an accidental second process call.
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

    private AssessmentView succeed(JobSnapshot claim, GradingResult result) {
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
        // Lesson feedback is not an exam result, even when numeric IDs happen to match.
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
                // claim() atomically reclaims the lease. There is no committed PENDING gap.
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
                        // A provider/database failure can happen after the claim has
                        // committed. Release that lease as well; otherwise the job is
                        // left in PROCESSING until the stale-lease timeout and restart
                        // recovery cannot make progress.
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

    private AssessmentView fail(JobSnapshot claim, String error) {
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

    private AssessmentView viewFor(AiAssessmentJob job) {
        AiAssessment assessment =
                aiAssessmentRepository
                        .findFirstByJobIdAndAnswerVersionOrderByIdDesc(
                                job.getId(), job.getAnswerVersion())
                        .orElse(null);
        if (assessment == null)
            return new AssessmentView(
                    null,
                    job.getAssessmentKey(),
                    job.getAnswerId(),
                    job.getAnswerVersion(),
                    job.getState().name(),
                    null,
                    null,
                    null,
                    job.getTranscript(),
                    job.getRubricVersion(),
                    job.getPolicyVersion(),
                    null,
                    null,
                    job.getLastError(),
                    instant(job.getCreatedAt()),
                    null);
        return new AssessmentView(
                assessment.getId(),
                assessment.getAssessmentKey(),
                assessment.getAnswerId(),
                assessment.getAnswerVersion(),
                assessment.getState().name(),
                assessment.getScore(),
                assessment.getEstimatedCefr(),
                assessment.getFeedback(),
                assessment.getTranscript(),
                assessment.getRubricVersion(),
                assessment.getPolicyVersion(),
                assessment.getProvider(),
                assessment.getModel(),
                assessment.getErrorMessage(),
                instant(assessment.getCreatedAt()),
                instant(assessment.getCompletedAt()));
    }

    private void validateRequest(AssessmentRequest request) {
        if (request == null
                || blank(request.assessmentKey())
                || request.assessmentKey().length() > 255
                || request.userId() == null
                || request.answerVersion() == null
                || request.answerVersion() < 1)
            throw new BusinessRuleException("Yêu cầu đánh giá không hợp lệ");
        boolean exam = "EXAM_ANSWER".equals(request.sourceType());
        boolean lesson = "LESSON_SUBMISSION".equals(request.sourceType());
        if ((!exam && !lesson)
                || request.sourceId() == null
                || request.sourceId() < 1
                || (exam && (request.attemptId() == null || request.answerId() == null))
                || (lesson && (request.attemptId() != null || request.answerId() != null)))
            throw new BusinessRuleException("Nguồn bài đánh giá không hợp lệ");
        if (blank(request.taskContent())
                || request.taskContent().length() > GradingTextLimits.MAX_AI_TASK_CHARS
                || blank(request.lockedContent())
                || request.lockedContent().length() > GradingTextLimits.MAX_AI_SUBMISSION_CHARS
                || request.taskContent().length() + request.lockedContent().length()
                        > GradingTextLimits.MAX_AI_PAYLOAD_CHARS) {
            throw new BusinessRuleException("Nội dung bài đã khóa vượt giới hạn ngữ cảnh chấm AI");
        }
        if (blank(request.rubricVersion())
                || blank(request.policyVersion())
                || request.maxScore() == null
                || request.maxScore().compareTo(BigDecimal.ZERO) <= 0
                || request.maxScore().compareTo(BigDecimal.valueOf(50)) > 0)
            throw new BusinessRuleException(
                    "Thiếu phiên bản rubric/policy hoặc thang điểm không hợp lệ");
        normalizeSkill(request.skill());
        normalizeTask(request.taskType());
    }

    private boolean sameRequest(AiAssessmentJob job, AssessmentRequest request) {
        return Objects.equals(job.getUserId(), request.userId())
                && Objects.equals(job.getSourceType(), request.sourceType())
                && Objects.equals(job.getSourceId(), request.sourceId())
                // Legacy lesson jobs used submission IDs in these exam-only columns.
                && ("LESSON_SUBMISSION".equals(request.sourceType())
                        || (Objects.equals(job.getAttemptId(), request.attemptId())
                                && Objects.equals(job.getAnswerId(), request.answerId())))
                && Objects.equals(job.getAnswerVersion(), request.answerVersion())
                && Objects.equals(job.getSkill(), normalizeSkill(request.skill()))
                && Objects.equals(job.getTaskType(), normalizeTask(request.taskType()))
                && Objects.equals(job.getTaskContent(), request.taskContent().trim())
                && Objects.equals(job.getLockedContent(), request.lockedContent().trim())
                && Objects.equals(job.getRubricVersion(), request.rubricVersion().trim())
                && Objects.equals(job.getPolicyVersion(), request.policyVersion().trim())
                && job.getMaxScore() != null
                && job.getMaxScore().compareTo(request.maxScore()) == 0
                // The exam recording is the input. A previously generated transcript can differ.
                && (("EXAM_ANSWER".equals(job.getSourceType())
                                && "SPEAKING".equals(job.getTaskType()))
                        || Objects.equals(job.getTranscript(), blankToNull(request.transcript())));
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
            AssessmentView view,
            AiAssessmentCompletedEvent replayEvent) {}

    private record FinishSnapshot(
            AssessmentView view, AiAssessmentCompletedEvent event, boolean applied) {}

    private record FailureSnapshot(
            AssessmentView view, AiAssessmentCompletedEvent event, boolean applied) {}

    private AssessmentView createOrReadAssessment(AssessmentRequest request) {
        Optional<AiAssessmentJob> existing =
                aiAssessmentJobRepository.findByAssessmentKeyForUpdate(request.assessmentKey());
        if (existing.isPresent()) return matchingAssessment(existing.get(), request);
        User user = activeUser(request.userId());
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

    private AiAssessmentJob newPendingJob(AssessmentRequest request, Integer userId) {
        AiAssessmentJob job = new AiAssessmentJob();
        job.setSourceType(request.sourceType());
        job.setSourceId(request.sourceId());
        job.setAssessmentKey(request.assessmentKey());
        job.setUserId(userId);
        job.setAttemptId(request.attemptId());
        job.setAnswerId(request.answerId());
        job.setAnswerVersion(request.answerVersion());
        job.setSkill(normalizeSkill(request.skill()));
        job.setTaskType(normalizeTask(request.taskType()));
        job.setTaskContent(request.taskContent().trim());
        job.setLockedContent(request.lockedContent().trim());
        job.setRubricVersion(request.rubricVersion().trim());
        job.setPolicyVersion(request.policyVersion().trim());
        job.setMaxScore(request.maxScore());
        job.setTranscript(blankToNull(request.transcript()));
        job.setState(AiJobState.PENDING);
        job.setRetryCount(0);
        return job;
    }

    private AssessmentView readExistingAssessment(AssessmentRequest request) {
        AiAssessmentJob job =
                aiAssessmentJobRepository
                        .findByAssessmentKeyForUpdate(request.assessmentKey())
                        .orElseThrow(
                                () ->
                                        new BusinessRuleException(
                                                "Không thể tạo tác vụ đánh giá lúc này"));
        return matchingAssessment(job, request);
    }

    private AssessmentView matchingAssessment(AiAssessmentJob job, AssessmentRequest request) {
        if (!sameRequest(job, request))
            throw new BusinessRuleException(
                    "assessmentKey đã được dùng cho một phiên bản bài khác");
        return viewFor(job);
    }
}
