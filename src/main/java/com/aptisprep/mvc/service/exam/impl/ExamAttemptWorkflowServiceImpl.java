package com.aptisprep.mvc.service.exam.impl;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.Role;
import com.aptisprep.mvc.enums.UserStatus;
import com.aptisprep.mvc.event.AttemptSubmittedEvent;
import com.aptisprep.mvc.exception.AccessDeniedException;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.exception.ResourceNotFoundException;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.service.exam.*;
import com.aptisprep.mvc.service.exam.ExamAttemptService;
import com.aptisprep.mvc.service.grading.AttemptFinalizationService;
import com.aptisprep.mvc.service.grading.ObjectiveGradingService;
import com.aptisprep.mvc.service.scoring.ScoringPolicyService;
import com.aptisprep.mvc.service.storage.StorageService;
import com.aptisprep.mvc.support.contract.*;
import com.aptisprep.mvc.support.contract.GradingContextFormatter;
import com.aptisprep.mvc.support.contract.IntegrationModels.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Application service điều phối toàn bộ vòng đời lượt làm bài.
 *
 * <p>Một attempt không chỉ là một row trong {@code exam_attempts}: nó còn sở hữu snapshot đề đã
 * khóa, đáp án, thời gian section, quyền truy cập media và trạng thái chấm. Vì vậy các method
 * public kiểm tra quyền/trạng thái trước khi ủy quyền cho các service chuyên trách.
 */
abstract class ExamAttemptWorkflowServiceImpl implements ExamAttemptService {
    private Pattern CHECKPOINT_REQUEST_KEY = Pattern.compile("checkpoint:(\\d+):(\\d+):after:\\d+");
    private Set<String> ACTIVE_STATES = Set.of("IN_PROGRESS", "SUBMITTED", "WAITING_GRADING");
    private UserRepository userRepository;
    private MockExamRepository mockExamRepository;
    private MockExamQuestionRepository mockExamQuestionRepository;
    private QuestionMediaRepository questionMediaRepository;
    private UserMediaActivityRepository userMediaActivityRepository;
    private ExamSnapshotService examSnapshots;
    private ExamGradingPayloadService gradingPayloads;
    private ExamAttemptQueryService attemptQueries;
    private ExamAttemptRepository examAttemptRepository;
    private UserAnswerRepository userAnswerRepository;
    private UserAnswerDetailRepository userAnswerDetailRepository;
    private ExamSkillResultRepository examSkillResultRepository;
    private ScoringPolicyService scoring;
    private StorageService storage;
    private AttemptOutboxService outbox;
    private AttemptFinalizationService finalization;
    private ObjectiveGradingService objectiveGrading;
    private ApplicationEventPublisher events;
    private ExamAnswerService answerWriter;
    private ExamReadinessService startValidator;
    private CheckpointStartService checkpointValidator;

    ExamAttemptWorkflowServiceImpl(
            UserRepository userRepository,
            MockExamRepository mockExamRepository,
            MockExamQuestionRepository mockExamQuestionRepository,
            QuestionMediaRepository questionMediaRepository,
            UserMediaActivityRepository userMediaActivityRepository,
            ExamSnapshotService examSnapshots,
            ExamGradingPayloadService gradingPayloads,
            ExamAttemptRepository examAttemptRepository,
            UserAnswerRepository userAnswerRepository,
            UserAnswerDetailRepository userAnswerDetailRepository,
            ExamSkillResultRepository examSkillResultRepository,
            ScoringPolicyService scoring,
            StorageService storage,
            AttemptOutboxService outbox,
            AttemptFinalizationService finalization,
            ObjectiveGradingService objectiveGrading,
            ApplicationEventPublisher events,
            ExamAnswerService answerWriter,
            ExamReadinessService startValidator,
            CheckpointStartService checkpointValidator,
            ExamAttemptQueryService attemptQueries) {
        this.userRepository = userRepository;
        this.mockExamRepository = mockExamRepository;
        this.mockExamQuestionRepository = mockExamQuestionRepository;
        this.questionMediaRepository = questionMediaRepository;
        this.userMediaActivityRepository = userMediaActivityRepository;
        this.examSnapshots = examSnapshots;
        this.gradingPayloads = gradingPayloads;
        this.examAttemptRepository = examAttemptRepository;
        this.userAnswerRepository = userAnswerRepository;
        this.userAnswerDetailRepository = userAnswerDetailRepository;
        this.examSkillResultRepository = examSkillResultRepository;
        this.scoring = scoring;
        this.storage = storage;
        this.outbox = outbox;
        this.finalization = finalization;
        this.objectiveGrading = objectiveGrading;
        this.events = events;
        this.answerWriter = answerWriter;
        this.startValidator = startValidator;
        this.checkpointValidator = checkpointValidator;
        this.attemptQueries = attemptQueries;
    }

    /** Bắt đầu hoặc resume idempotent một lượt thi và tạo snapshot bất biến của đề. */
    @Override
    @Transactional
    public AttemptHandle start(Integer userId, Integer examId, String requestKey) {
        return startInternal(userId, examId, requestKey, null, false, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer latestResumableAttemptId(Integer userId, Integer examId) {
        return attemptQueries.latestResumableAttemptId(userId, examId);
    }

    @Override
    @Transactional
    public AttemptHandle startPlacement(Integer userId, Integer examId, String requestKey) {
        return startInternal(userId, examId, requestKey, null, true, false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasInProgressPlacement(Integer userId, String skill) {
        return attemptQueries.hasInProgressPlacement(userId, skill);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasInProgressPlacement(Integer userId) {
        return attemptQueries.hasInProgressPlacement(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPlacementAttempt(Integer userId) {
        return attemptQueries.hasPlacementAttempt(userId);
    }

    @Override
    @Transactional
    public AttemptHandle startCheckpoint(
            Integer userId, Integer examId, Integer pathId, String requestKey) {
        if (pathId == null) throw new BusinessRuleException("Checkpoint cần ngữ cảnh lộ trình học");
        return startInternal(userId, examId, requestKey, pathId, false, false);
    }

    @Override
    @Transactional
    public AttemptHandle startCheckpointRetake(
            Integer userId, Integer examId, Integer pathId, String requestKey) {
        if (pathId == null) throw new BusinessRuleException("Checkpoint cần ngữ cảnh lộ trình học");
        return startInternal(userId, examId, requestKey, pathId, false, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Integer> checkpointOriginPathId(Integer userId, Integer attemptId) {
        return attemptQueries.checkpointOriginPathId(userId, attemptId);
    }

    private AttemptHandle startInternal(
            Integer userId,
            Integer examId,
            String requestKey,
            Integer pathId,
            boolean placement,
            boolean checkpointRetake) {
        // Khóa row user trong toàn bộ transaction. Khóa ở JVM là chưa đủ:
        // request khác có thể chạy vào sau khi synchronized đầu tiên kết thúc nhưng trước
        // khi transaction đầu tiên commit row attempt.
        activeUserForUpdate(userId);
        MockExam exam = publishedExam(examId);
        if (pathId != null) {
            if (!"CHECKPOINT".equals(exam.getExamType()))
                throw new BusinessRuleException("Đề không phải Checkpoint");
            if (checkpointRetake) checkpointValidator.validateForRetake(userId, exam, pathId);
            else checkpointValidator.validate(userId, exam, pathId);
        } else if ("CHECKPOINT".equals(exam.getExamType())) {
            throw new BusinessRuleException("Checkpoint chỉ được bắt đầu từ lộ trình học");
        }
        if (placement != "PLACEMENT".equals(exam.getExamType()))
            throw new BusinessRuleException("Placement chỉ được bắt đầu từ trang kiểm tra đầu vào");
        List<MockExamQuestion> links =
                mockExamQuestionRepository.findByMockExamIdOrderByPosition(examId);
        if (links.isEmpty()) throw new BusinessRuleException("Đề thi chưa có câu hỏi");
        String key = normalizeRequestKey(requestKey, userId, examId);
        Optional<ExamAttempt> byKey = examAttemptRepository.findByUserIdAndRequestKey(userId, key);
        if (byKey.isPresent()) {
            if (!Objects.equals(byKey.get().getMockExamId(), examId)) {
                throw new BusinessRuleException("Request key đã được dùng cho một đề thi khác");
            }
            return handle(byKey.get(), examFor(byKey.get()));
        }
        if (exam.isDemoOnly()) {
            throw new BusinessRuleException(
                    "Đề demo chỉ dành cho kiểm thử, không thể bắt đầu lượt thi mới");
        }
        startValidator.validate(exam, links);
        if ("PLACEMENT".equals(exam.getExamType())) {
            // Placement là một bài đánh giá tổng hợp cho mỗi học viên. Resume lượt thi
            // chưa hoàn tất, không phụ thuộc skill cũ đang lưu trên đề.
            List<ExamAttempt> placementAttempts =
                    examAttemptRepository.findPlacementAttempts(userId);
            Optional<ExamAttempt> active =
                    placementAttempts.stream()
                            .filter(attempt -> ACTIVE_STATES.contains(attempt.getStatus()))
                            .findFirst();
            if (active.isPresent()) {
                MockExam activeExam = examFor(active.get());
                if (!expireIfNeeded(active.get(), activeExam))
                    return handle(active.get(), activeExam);
            }
        }
        ExamAttempt attempt = new ExamAttempt();
        attempt.setUserId(userId);
        attempt.setMockExamId(examId);
        attempt.setRequestKey(key);
        attempt.setStatus("IN_PROGRESS");
        attempt.setGradingStatus("NOT_STARTED");
        attempt.setAnswerVersion(1);
        attempt.setScoringSchemeId(exam.getScoringSchemeId());
        attempt.setPolicyVersion(
                exam.getScoringSchemeId() == null
                        ? "unscaled-v1"
                        : "scoring-scheme:"
                                + exam.getScoringSchemeId()
                                + ":"
                                + scoring.policyVersion(exam.getScoringSchemeId()));
        attempt.setRubricVersion(GradingContextFormatter.DEFAULT_RUBRIC_VERSION);
        attempt = examAttemptRepository.save(attempt);
        examSnapshots.createSnapshot(attempt, exam, links);
        Integer newAttemptId = attempt.getId();
        for (MockExamQuestion link : links) {
            UserAnswer answer = new UserAnswer();
            answer.setAttemptId(newAttemptId);
            answer.setQuestionId(link.getQuestionId());
            answer.setStatus("NOT_STARTED");
            userAnswerRepository.save(answer);
            questionMediaRepository.findByQuestionIdOrderByPosition(link.getQuestionId()).stream()
                    .filter(media -> "AUDIO".equals(media.getMediaType()))
                    .forEach(
                            media -> {
                                UserMediaActivity activity = new UserMediaActivity();
                                activity.setAttemptId(newAttemptId);
                                activity.setMediaId(media.getId());
                                userMediaActivityRepository.save(activity);
                            });
        }
        return handle(attempt, exam);
    }

    @Transactional
    public void saveSpeakingAudio(
            Integer userId,
            Integer attemptId,
            Integer questionId,
            Integer itemId,
            MultipartFile audioFile) {
        answerWriter.saveSpeakingAudio(
                editableAttempt(userId, attemptId), questionId, itemId, audioFile);
    }

    @Transactional
    public MediaPlayResult playMedia(Integer userId, Integer attemptId, Integer mediaId) {
        ExamAttempt attempt =
                examAttemptRepository
                        .findByIdAndUserIdForUpdate(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền với lượt thi này"));
        if (!"IN_PROGRESS".equals(attempt.getStatus()))
            throw new BusinessRuleException("Lượt thi đã khóa");
        MockExam exam = examFor(attempt);
        if (expireIfNeeded(attempt, exam))
            throw new BusinessRuleException("Lượt thi đã hết thời gian");
        ExamMediaSnapshot lockedMedia = examSnapshots.lockedMediaEntity(attemptId, mediaId);
        if (lockedMedia == null
                || !"AUDIO".equals(lockedMedia.getMediaType())
                || !userAnswerRepository
                        .findByAttemptIdAndQuestionId(
                                attemptId, lockedMedia.getQuestion().getSourceQuestionId())
                        .isPresent()) throw new AccessDeniedException("Media không thuộc lượt thi");
        ensureMediaWindow(attempt);
        examSnapshots.requireOpenSection(
                attempt,
                examSnapshots.lockedQuestionEntity(
                        attemptId, lockedMedia.getQuestion().getSourceQuestionId()));
        startValidator.validateAudio(lockedMedia.getMediaUrl());
        int limit = Math.min(2, Objects.requireNonNullElse(lockedMedia.getMaxPlayCount(), 2));
        UserMediaActivity activity =
                userMediaActivityRepository
                        .findByAttemptIdAndMediaIdForUpdate(attemptId, mediaId)
                        .orElseThrow(
                                () ->
                                        new BusinessRuleException(
                                                "Không thể khởi tạo bộ đếm lượt nghe"));
        if (activity.getPlayCount() >= limit)
            throw new BusinessRuleException("Bạn đã sử dụng hết số lượt nghe cho đoạn audio này");
        String contentToken = UUID.randomUUID().toString();
        activity.setPlayCount(activity.getPlayCount() + 1);
        activity.setLastPlayedAt(LocalDateTime.now());
        activity.setPlayTokenHash(hashToken(contentToken));
        activity.setPlayTokenExpiresAt(LocalDateTime.now().plusMinutes(1));
        userMediaActivityRepository.save(activity);
        return new MediaPlayResult(mediaId, activity.getPlayCount(), limit, contentToken);
    }

    @Transactional
    public MediaResource mediaContent(
            Integer userId, Integer attemptId, Integer mediaId, String contentToken) {
        ExamAttempt attempt =
                examAttemptRepository
                        .findByIdAndUserId(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền xem media này"));
        MockExam exam = examFor(attempt);
        if (expireIfNeeded(attempt, exam))
            throw new BusinessRuleException("Lượt thi đã hết thời gian");
        ExamMediaSnapshot lockedMedia = examSnapshots.lockedMediaEntity(attemptId, mediaId);
        if (lockedMedia == null
                || !userAnswerRepository
                        .findByAttemptIdAndQuestionId(
                                attemptId, lockedMedia.getQuestion().getSourceQuestionId())
                        .isPresent()) throw new AccessDeniedException("Media không thuộc lượt thi");
        ensureMediaWindow(attempt);
        examSnapshots.requireOpenSection(
                attempt,
                examSnapshots.lockedQuestionEntity(
                        attemptId, lockedMedia.getQuestion().getSourceQuestionId()));
        if ("AUDIO".equals(lockedMedia.getMediaType())) {
            UserMediaActivity activity =
                    userMediaActivityRepository
                            .findByAttemptIdAndMediaIdForUpdate(attemptId, mediaId)
                            .orElseThrow(
                                    () ->
                                            new AccessDeniedException(
                                                    "Media chưa được cấp lượt phát"));
            if (contentToken == null
                    || contentToken.isBlank()
                    || !Objects.equals(activity.getPlayTokenHash(), hashToken(contentToken))
                    || activity.getPlayTokenExpiresAt() == null
                    || activity.getPlayTokenExpiresAt().isBefore(LocalDateTime.now())) {
                throw new AccessDeniedException("Token phát audio không hợp lệ hoặc đã hết hạn");
            }
            // Giữ token ngắn hạn hợp lệ cho metadata và các request range của trình duyệt.
            // Trình duyệt có thể request cùng audio nhiều lần khi dựng player native,
            // nên vô hiệu hóa token sau request đầu tiên sẽ làm player bị kẹt ở 0:00 / 0:00.
        }
        return loadLockedMedia(lockedMedia);
    }

    private MediaResource loadLockedMedia(ExamMediaSnapshot lockedMedia) {
        String mediaUrl = lockedMedia.getMediaUrl();
        String contentType =
                Objects.requireNonNullElse(
                        lockedMedia.getContentType(), "application/octet-stream");
        Resource resource;
        try {
            resource = storage.load(mediaUrl);
        } catch (BusinessRuleException exception) {
            if (!"IMAGE".equals(lockedMedia.getMediaType())) throw exception;
            resource = new ClassPathResource("static/images/speaking-community.svg");
            contentType = "image/svg+xml";
        }
        return new MediaResource(resource, contentType);
    }

    @Transactional(readOnly = true)
    public MediaResource speakingAudio(Integer userId, Integer attemptId, Integer answerId) {
        ExamAttempt attempt =
                examAttemptRepository
                        .findByIdAndUserId(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền xem audio này"));
        if ("CANCELLED".equals(attempt.getStatus()))
            throw new ResourceNotFoundException("Lượt làm dở đã được xóa.");
        UserAnswer answer =
                userAnswerRepository
                        .findByIdAndAttemptId(answerId, attemptId)
                        .orElseThrow(() -> new AccessDeniedException("Audio không thuộc lượt thi"));
        if (answer.getAudioUrl() == null)
            throw new ResourceNotFoundException("Chưa có file ghi âm");
        return new MediaResource(storage.load(answer.getAudioUrl()), "audio/*");
    }

    @Transactional
    // Ghi đáp án đi qua service riêng để form submit và endpoint autosave dùng chung validation.
    public void saveAnswer(
            Integer userId,
            Integer attemptId,
            Integer questionId,
            Integer itemId,
            Integer selectedOptionId,
            Integer userPosition,
            String textAnswer) {
        answerWriter.saveAnswer(
                editableAttempt(userId, attemptId),
                questionId,
                itemId,
                selectedOptionId,
                userPosition,
                textAnswer);
    }

    /**
     * Khóa version đáp án, chấm câu objective, sau đó finalize ngay hoặc phát event để chấm
     * Writing/Speaking.
     */
    @Override
    @Transactional
    public void submit(Integer userId, Integer attemptId, String requestKey) {
        ExamAttempt attempt =
                examAttemptRepository
                        .findByIdAndUserIdForUpdate(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền với lượt thi này"));
        if ("COMPLETED".equals(attempt.getStatus()) || attempt.getSubmittedAt() != null) return;
        // Lượt hết giờ vẫn có thể nộp nếu tất cả đáp án đã được lưu.
        if (!"IN_PROGRESS".equals(attempt.getStatus())
                && !"WAITING_GRADING".equals(attempt.getStatus())
                && !("EXPIRED".equals(attempt.getStatus()) && attempt.getSubmittedAt() == null)) {
            throw new BusinessRuleException("Lượt thi không thể nộp ở trạng thái hiện tại");
        }
        requireCompleteAnswers(attemptId, examSnapshots.lockedQuestionEntities(attemptId));
        MockExam exam = examFor(attempt);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = examDeadline(attempt, exam);
        boolean expired = end != null && end.isBefore(now);
        refreshSnapshotAnswerVersion(attempt);
        attempt.setScoringSchemeId(
                attempt.getScoringSchemeId() == null
                        ? exam.getScoringSchemeId()
                        : attempt.getScoringSchemeId());
        attempt.setPolicyVersion(
                attempt.getPolicyVersion() == null
                        ? "scoring-scheme:" + exam.getScoringSchemeId() + ":v1"
                        : attempt.getPolicyVersion());
        attempt.setRubricVersion(
                attempt.getRubricVersion() == null
                        ? GradingContextFormatter.DEFAULT_RUBRIC_VERSION
                        : attempt.getRubricVersion());
        attempt.setGradingRequestKey(
                "attempt:" + attemptId + ":grading:v" + attempt.getAnswerVersion());
        validateWritingWordLimits(attempt.getId());
        LockedAttempt draftLocked = gradingPayloads.build(attempt);
        gradingPayloads.validate(draftLocked);
        attempt.setSubmittedAt(now);
        attempt.setGradingStatus("GRADING");
        attempt.setStatus(expired ? "EXPIRED" : "SUBMITTED");
        LockedAttempt locked = gradingPayloads.build(attempt);
        objectiveGrading.gradeObjective(locked, exam);
        boolean waiting =
                examSkillResultRepository.findByAttemptIdOrderBySkillAsc(attempt.getId()).stream()
                        .anyMatch(x -> !"COMPLETED".equals(x.getGradingStatus()));
        if (waiting) {
            attempt.setStatus(expired ? "EXPIRED" : "WAITING_GRADING");
            attempt.setGradingStatus("WAITING");
        }
        boolean completed = !waiting && finalization.finalizeIfReady(attempt, exam);
        examAttemptRepository.save(attempt);
        if (completed) {
            finalization.publishCompleted(attempt, exam);
        } else if ("WAITING_GRADING".equals(attempt.getStatus())
                || ("EXPIRED".equals(attempt.getStatus())
                        && "WAITING".equals(attempt.getGradingStatus()))) {
            AttemptSubmittedEvent event =
                    new AttemptSubmittedEvent(
                            userId, attemptId, exam.getId(), exam.getExamType(), locked);
            outbox.enqueueSubmitted(event);
            events.publishEvent(event);
        }
    }

    @Transactional
    public void discard(Integer userId, Integer attemptId) {
        activeUserForUpdate(userId);
        ExamAttempt attempt =
                examAttemptRepository
                        .findByIdAndUserIdForUpdate(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền với lượt thi này"));
        if ("CANCELLED".equals(attempt.getStatus())) return;
        boolean terminal =
                "ERROR".equals(attempt.getStatus())
                        || "EXPIRED".equals(attempt.getStatus())
                        || "COMPLETED".equals(attempt.getStatus());
        boolean cleanDraft =
                "IN_PROGRESS".equals(attempt.getStatus())
                        && attempt.getSubmittedAt() == null
                        && attempt.getFinalScaleScore() == null
                        && attempt.getEstimatedOverallCefr() == null
                        && !examSkillResultRepository.existsByAttemptId(attemptId);
        if (!terminal && !cleanDraft)
            throw new BusinessRuleException(
                    "Chỉ có thể loại bỏ lượt thi đang làm, đã hết thời gian hoặc đã hoàn thành.");
        attempt.setStatus("CANCELLED");
        attempt.setGradingStatus("CANCELLED");
        // Checkpoint mới có thể dùng lại start key của attempt đã bị loại bỏ.
        attempt.setRequestKey("discarded-attempt:" + attemptId);
    }

    @Override
    @Transactional
    public AttemptResult result(Integer userId, Integer attemptId) {
        return attemptQueries.result(userId, attemptId);
    }

    @Override
    @Transactional(readOnly = true)
    public HistoryPage history(Integer userId, int page, int size, HistoryFilter filter) {
        return attemptQueries.history(userId, page, size, filter);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminHistoryPage adminHistory(
            String adminEmail, int page, int size, AdminHistoryFilter filter) {
        return attemptQueries.adminHistory(adminEmail, page, size, filter);
    }

    @Override
    @Transactional(readOnly = true)
    public AttemptResult adminResult(String adminEmail, Integer attemptId) {
        return attemptQueries.adminResult(adminEmail, attemptId);
    }

    @Override
    public void retryFailedGrading(Integer userId, Integer attemptId, String requestKey) {
        finalization.retryFailedGrading(userId, attemptId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canRegradeEmpty(Integer userId, Integer attemptId) {
        activeUser(userId);
        ExamAttempt attempt =
                examAttemptRepository
                        .findByIdAndUserId(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền với lượt thi này"));
        return canRegradeEmpty(attempt);
    }

    private boolean canRegradeEmpty(ExamAttempt attempt) {
        if (!"ERROR".equals(attempt.getStatus())) return false;
        List<ExamSkillResult> results =
                examSkillResultRepository.findByAttemptIdOrderBySkillAsc(attempt.getId());
        boolean emptyError = false;
        for (ExamSkillResult result : results) {
            if (!"ERROR".equals(result.getGradingStatus())) continue;
            if (!"AI_EMPTY".equals(result.getGradingMethod())
                    && !"AI_DISABLED".equals(result.getGradingMethod())) return false;
            emptyError = true;
        }
        if (!emptyError) return false;
        LockedAttempt locked;
        try {
            locked = gradingPayloads.build(attempt);
        } catch (BusinessRuleException | ResourceNotFoundException unavailableSnapshot) {
            return false;
        }
        boolean hasSubjective = false;
        for (LockedAnswer answer : locked.answers()) {
            if (!Set.of("WRITING_TEXT", "SPEAKING_AUDIO").contains(answer.questionType())) continue;
            hasSubjective = true;
            if (GradingContextFormatter.hasSubmission(answer)) return false;
        }
        return hasSubjective;
    }

    @Override
    @Transactional
    public void regradeEmpty(Integer userId, Integer attemptId) {
        activeUser(userId);
        ExamAttempt attempt =
                examAttemptRepository
                        .findByIdAndUserIdForUpdate(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền với lượt thi này"));
        if ("COMPLETED".equals(attempt.getStatus())) return;
        if (!canRegradeEmpty(attempt)) {
            throw new BusinessRuleException(
                    "Chỉ tính lại phần bỏ trống của lượt thi bị AI_EMPTY hoặc AI_DISABLED; lỗi AI của bài có nội dung không được đổi thành 0.");
        }
        MockExam exam = examFor(attempt);
        LockedAttempt locked = gradingPayloads.build(attempt);
        objectiveGrading.gradeObjective(locked, exam);
        if (!finalization.finalizeIfReady(attempt, exam)) {
            throw new BusinessRuleException(
                    "Bộ chấm điểm chưa thể tổng hợp kết quả. Dữ liệu cũ được giữ nguyên.");
        }
        finalization.publishCompleted(attempt, exam);
    }

    @Transactional
    public void advanceSection(Integer userId, Integer attemptId, int sectionEndMinute) {
        activeUser(userId);
        ExamAttempt attempt = editableAttempt(userId, attemptId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime clockStart = examSnapshots.sectionClockStart(attempt);
        LocalDateTime sectionEnd = clockStart.plusMinutes(sectionEndMinute);
        List<ExamQuestionSnapshot> questions = examSnapshots.lockedQuestionEntities(attemptId);
        List<ExamSnapshotService.SectionWindow> windows =
                questions.stream()
                        .filter(question -> question.getSectionEndMinute() != null)
                        .map(
                                question ->
                                        new ExamSnapshotService.SectionWindow(
                                                question.getSectionStartMinute(),
                                                question.getSectionEndMinute()))
                        .filter(streamValue -> Objects.nonNull(streamValue))
                        .toList();
        boolean current =
                windows.stream()
                        .anyMatch(
                                window ->
                                        window.end() == sectionEndMinute
                                                && !now.isBefore(
                                                        clockStart.plusMinutes(window.start()))
                                                && !now.isAfter(
                                                        clockStart.plusMinutes(window.end())));
        boolean hasNext = windows.stream().anyMatch(window -> window.start() == sectionEndMinute);
        if (current && hasNext) {
            requireCompleteAnswers(
                    attemptId,
                    questions.stream()
                            .filter(
                                    question ->
                                            Objects.equals(
                                                    question.getSectionEndMinute(),
                                                    sectionEndMinute))
                            .toList());
            Duration remaining =
                    now.isBefore(sectionEnd) ? Duration.between(now, sectionEnd) : Duration.ZERO;
            examSnapshots.advanceSectionClock(attemptId, remaining);
        }
    }

    @Transactional
    public AttemptReview review(Integer userId, Integer attemptId) {
        ExamAttempt attempt = reviewableAttempt(userId, attemptId);
        return buildReview(attempt);
    }

    @Transactional(readOnly = true)
    public AttemptReview adminReview(String adminEmail, Integer attemptId) {
        User admin =
                userRepository
                        .findByEmailIgnoreCase(adminEmail)
                        .orElseThrow(() -> new AccessDeniedException("Không có quyền quản trị"));
        if (admin.getRole() != Role.ADMIN || admin.getStatus() != UserStatus.ACTIVE)
            throw new AccessDeniedException("Không có quyền quản trị");
        ExamAttempt attempt =
                examAttemptRepository
                        .findById(attemptId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy lượt thi"));
        if (attempt.getSubmittedAt() == null && !"COMPLETED".equals(attempt.getStatus()))
            return new AttemptReview("Lượt thi chưa nộp", List.of());
        return buildReview(attempt);
    }

    @Transactional
    public void adminDiscard(String adminEmail, Integer attemptId) {
        User admin =
                userRepository
                        .findByEmailIgnoreCase(adminEmail)
                        .orElseThrow(() -> new AccessDeniedException("Không có quyền quản trị"));
        if (admin.getRole() != Role.ADMIN || admin.getStatus() != UserStatus.ACTIVE)
            throw new AccessDeniedException("Không có quyền quản trị");
        ExamAttempt attempt =
                examAttemptRepository
                        .findById(attemptId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy lượt thi"));
        if ("CANCELLED".equals(attempt.getStatus())) return;
        attempt.setStatus("CANCELLED");
        attempt.setGradingStatus("CANCELLED");
        attempt.setRequestKey("admin-discarded-attempt:" + attemptId);
    }

    private AttemptReview buildReview(ExamAttempt attempt) {
        Integer attemptId = attempt.getId();
        MockExam current = null;
        if (examSnapshots.frozenExam(attemptId).isEmpty()) {
            current =
                    mockExamRepository
                            .findByIdForUpdate(attempt.getMockExamId())
                            .orElseThrow(
                                    () -> new ResourceNotFoundException("Không tìm thấy đề thi"));
            if (examSnapshots.frozenExam(attemptId).isEmpty())
                try {
                    examSnapshots.preserveLegacyAttempt(
                            attempt,
                            current,
                            mockExamQuestionRepository.findByMockExamIdOrderByPosition(
                                    current.getId()));
                } catch (BusinessRuleException ignored) {
                    return new AttemptReview(current.getTitle(), List.of());
                }
        }
        List<QuestionPage> questions = new ArrayList<>();
        int number = 1;
        for (ExamQuestionSnapshot question : examSnapshots.lockedQuestionEntities(attemptId)) {
            var details = answerDetails(attemptId, question.getSourceQuestionId());
            var page = toQuestionPage(question, details, number);
            questions.add(page);
            number += page.items().size();
        }
        String title =
                current == null ? examSnapshots.require(attemptId).getTitle() : current.getTitle();
        return new AttemptReview(title, List.copyOf(questions));
    }

    @Transactional(readOnly = true)
    public MediaResource reviewMedia(Integer userId, Integer attemptId, Integer mediaId) {
        reviewableAttempt(userId, attemptId);
        ExamMediaSnapshot media = examSnapshots.lockedMediaEntity(attemptId, mediaId);
        if (media == null) throw new ResourceNotFoundException("Không tìm thấy tệp của câu hỏi");
        return loadLockedMedia(media);
    }

    @Transactional(readOnly = true)
    public MediaResource reviewRecording(
            Integer userId, Integer attemptId, Integer questionId, Integer itemId) {
        reviewableAttempt(userId, attemptId);
        var detail = answerDetails(attemptId, questionId).get(itemId);
        if (detail == null || detail.getAudioPath() == null)
            throw new ResourceNotFoundException("Chưa có file ghi âm");
        return new MediaResource(storage.load(detail.getAudioPath()), "audio/*");
    }

    private ExamAttempt reviewableAttempt(Integer userId, Integer attemptId) {
        activeUser(userId);
        ExamAttempt attempt =
                examAttemptRepository
                        .findByIdAndUserId(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền với lượt thi này"));
        if (attempt.getSubmittedAt() == null && !"COMPLETED".equals(attempt.getStatus()))
            throw new BusinessRuleException("Chỉ có thể xem lại sau khi nộp bài");
        return attempt;
    }

    @Transactional
    public AttemptPage page(Integer userId, Integer attemptId) {
        activeUser(userId);
        ExamAttempt attempt =
                examAttemptRepository
                        .findByIdAndUserIdForUpdate(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền với lượt thi này"));
        if ("CANCELLED".equals(attempt.getStatus()))
            throw new ResourceNotFoundException("Lượt làm dở đã được xóa.");
        MockExam exam = examFor(attempt);
        expireIfNeeded(attempt, exam);
        List<QuestionPage> questionPages = new ArrayList<>();
        List<SectionPage> sections = new ArrayList<>();
        int nextItemNumber = 1;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime examEnd = examDeadline(attempt, exam);
        Instant deadline = instant(examEnd);
        Integer nextSectionAfterMinute = null;
        String sectionSkill = null;
        LocalDateTime clockStart = examSnapshots.sectionClockStart(attempt);
        var lockedQuestions = examSnapshots.lockedQuestionEntities(attemptId);
        Map<ExamSnapshotService.SectionWindow, List<ExamQuestionSnapshot>> timedSections =
                new TreeMap<>(Comparator.comparingInt(streamValue -> streamValue.start()));
        for (ExamQuestionSnapshot question : lockedQuestions) {
            var section =
                    question.getSectionEndMinute() == null
                            ? null
                            : new ExamSnapshotService.SectionWindow(
                                    question.getSectionStartMinute(),
                                    question.getSectionEndMinute());
            if (section != null)
                timedSections.computeIfAbsent(section, ignored -> new ArrayList<>()).add(question);
        }
        for (var entry : timedSections.entrySet()) {
            var window = entry.getKey();
            var questions = entry.getValue();
            boolean open =
                    "IN_PROGRESS".equals(attempt.getStatus()) && window.contains(clockStart, now);
            String state =
                    open
                            ? "OPEN"
                            : (now.isBefore(clockStart.plusMinutes(window.start()))
                                    ? "UPCOMING"
                                    : "COMPLETED");
            int items = questions.stream().mapToInt(question -> question.getItems().size()).sum();
            sections.add(
                    new SectionPage(
                            sections.size(),
                            questions.getFirst().getSkill(),
                            window.end() - window.start(),
                            items,
                            state,
                            open));
        }
        for (ExamQuestionSnapshot question : lockedQuestions) {
            var window =
                    question.getSectionEndMinute() == null
                            ? null
                            : new ExamSnapshotService.SectionWindow(
                                    question.getSectionStartMinute(),
                                    question.getSectionEndMinute());
            if (window != null) {
                if (!window.contains(clockStart, now)) continue;
                LocalDateTime sectionEnd = clockStart.plusMinutes(window.end());
                deadline = instant(sectionEnd);
                if (sectionEnd.isBefore(examEnd)) nextSectionAfterMinute = window.end();
                sectionSkill = question.getSkill();
            }
            var details = answerDetails(attemptId, question.getSourceQuestionId());
            QuestionPage questionPage = toQuestionPage(question, details, nextItemNumber);
            questionPages.add(questionPage);
            nextItemNumber += questionPage.items().size();
        }
        return new AttemptPage(
                attempt.getId(),
                exam.getTitle(),
                exam.getExamType(),
                deadline == null
                        ? null
                        : Math.max(
                                0,
                                (Duration.between(instant(now), deadline).toMillis() + 999) / 1000),
                List.copyOf(questionPages),
                deadline,
                nextSectionAfterMinute,
                sectionSkill,
                List.copyOf(sections));
    }

    private void refreshSnapshotAnswerVersion(ExamAttempt attempt) {
        Integer attemptId = attempt.getId();
        int version =
                userAnswerRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId).stream()
                        .mapToInt(streamValue -> streamValue.getAnswerVersion())
                        .max()
                        .orElse(1);
        attempt.setAnswerVersion(version);
        examSnapshots.updateAnswerVersion(attemptId, version);
    }

    private ExamAttempt editableAttempt(Integer userId, Integer attemptId) {
        ExamAttempt attempt =
                examAttemptRepository
                        .findByIdAndUserIdForUpdate(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền với lượt thi này"));
        if (!"IN_PROGRESS".equals(attempt.getStatus()))
            throw new BusinessRuleException("Lượt thi đã khóa");
        MockExam exam = examFor(attempt);
        if (expireIfNeeded(attempt, exam)) {
            throw new BusinessRuleException("Lượt thi đã hết thời gian");
        }
        return attempt;
    }

    private void ensureMediaWindow(ExamAttempt attempt) {
        if (!"IN_PROGRESS".equals(attempt.getStatus()))
            throw new BusinessRuleException("Lượt thi đã khóa");
        MockExam exam = examFor(attempt);
        if (expireIfNeeded(attempt, exam)) {
            throw new BusinessRuleException("Lượt thi đã hết thời gian");
        }
    }

    private boolean expireIfNeeded(ExamAttempt attempt, MockExam exam) {
        if (!"IN_PROGRESS".equals(attempt.getStatus())) return false;
        LocalDateTime end = examDeadline(attempt, exam);
        if (end == null || !end.isBefore(LocalDateTime.now())) return false;
        attempt.setStatus("EXPIRED");
        attempt.setGradingStatus("EXPIRED");
        examAttemptRepository.save(attempt);
        return true;
    }

    private MockExam publishedExam(Integer id) {
        return mockExamRepository
                .findByIdForUpdate(id)
                .filter(x -> "PUBLISHED".equals(x.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Đề thi chưa được công bố"));
    }

    private MockExam examFor(ExamAttempt attempt) {
        return examSnapshots
                .frozenExam(attempt.getId())
                .orElseGet(
                        () ->
                                mockExamRepository
                                        .findById(attempt.getMockExamId())
                                        .orElseThrow(
                                                () ->
                                                        new ResourceNotFoundException(
                                                                "Không tìm thấy đề thi")));
    }

    private User activeUser(Integer id) {
        User user =
                userRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (user.getRole() != Role.STUDENT || user.getStatus() != UserStatus.ACTIVE)
            throw new AccessDeniedException("Tài khoản không còn hoạt động");
        return user;
    }

    private User activeUserForUpdate(Integer id) {
        User user =
                userRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (user.getRole() != Role.STUDENT || user.getStatus() != UserStatus.ACTIVE)
            throw new AccessDeniedException("Tài khoản không còn hoạt động");
        return user;
    }

    private AttemptHandle handle(ExamAttempt attempt, MockExam exam) {
        return new AttemptHandle(
                attempt.getId(),
                attempt.getStatus(),
                instant(
                        Objects.requireNonNullElseGet(
                                examDeadline(attempt, exam), () -> attempt.getStartedAt())));
    }

    private LocalDateTime examDeadline(ExamAttempt attempt, MockExam exam) {
        Integer minutes = examSnapshots.durationMinutes(attempt.getId(), exam.getDurationMinutes());
        return minutes == null || attempt.getStartedAt() == null
                ? null
                : examSnapshots.sectionClockStart(attempt).plusMinutes(minutes);
    }

    private Instant instant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }

    private String normalizeRequestKey(String value, Integer userId, Integer examId) {
        return value == null || value.isBlank() ? "attempt:" + userId + ":" + examId : value.trim();
    }

    private String normalizeSkill(String value) {
        return Objects.toString(value, "")
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace(" & ", "_")
                .replace(' ', '_');
    }

    private String hashToken(String value) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private Map<Integer, UserAnswerDetail> answerDetails(Integer attemptId, Integer questionId) {
        UserAnswer answer =
                userAnswerRepository
                        .findByAttemptIdAndQuestionId(attemptId, questionId)
                        .orElse(null);
        if (answer == null) return Map.of();
        Map<Integer, UserAnswerDetail> detailByItem = new HashMap<>();
        for (UserAnswerDetail detail :
                userAnswerDetailRepository.findByUserAnswerIdOrderByQuestionItemIdAsc(
                        answer.getId())) {
            detailByItem.put(detail.getQuestionItemId(), detail);
        }
        return detailByItem;
    }

    private void requireCompleteAnswers(Integer attemptId, List<ExamQuestionSnapshot> questions) {
        if (questions.isEmpty())
            throw new BusinessRuleException("Lượt thi không có câu hỏi để nộp");
        for (ExamQuestionSnapshot question : questions) {
            Map<Integer, UserAnswerDetail> details =
                    answerDetails(attemptId, question.getSourceQuestionId());
            for (ExamItemSnapshot item : question.getItems()) {
                UserAnswerDetail answer = details.get(item.getSourceItemId());
                if (answer == null || !hasAnswer(question.getQuestionType(), answer))
                    throw new BusinessRuleException(
                            "Bài làm còn câu chưa trả lời. Hãy hoàn thành tất cả câu hỏi trước khi tiếp tục.");
            }
        }
    }

    private boolean hasAnswer(String type, UserAnswerDetail answer) {
        return switch (type) {
            case "ORDERING" -> answer.getUserPosition() != null;
            case "WRITING_TEXT" ->
                    answer.getAnswerText() != null && !answer.getAnswerText().isBlank();
            case "SPEAKING_AUDIO" ->
                    answer.getAudioPath() != null && !answer.getAudioPath().isBlank();
            default -> answer.getSelectedOptionId() != null;
        };
    }

    private void validateWritingWordLimits(Integer attemptId) {
        ExamAttemptSnapshot snapshot = examSnapshots.require(attemptId);
        for (ExamQuestionSnapshot question : snapshot.getQuestions()) {
            if (!"WRITING_TEXT".equals(question.getQuestionType())) continue;
            UserAnswer answer =
                    userAnswerRepository
                            .findByAttemptIdAndQuestionId(attemptId, question.getSourceQuestionId())
                            .orElse(null);
            Map<Integer, UserAnswerDetail> details =
                    answerDetails(attemptId, question.getSourceQuestionId());
            for (ExamItemSnapshot item : question.getItems()) {
                String text =
                        answer == null || details.get(item.getSourceItemId()) == null
                                ? ""
                                : Objects.requireNonNullElse(
                                        details.get(item.getSourceItemId()).getAnswerText(), "");
                int words = text.isBlank() ? 0 : text.trim().split("\\s+").length;
                if (item.getMaxWordCount() != null && words > item.getMaxWordCount())
                    throw new BusinessRuleException(
                            "Bài viết ở mục "
                                    + item.getPosition()
                                    + " không được vượt quá "
                                    + item.getMaxWordCount()
                                    + " từ");
            }
        }
    }

    private QuestionPage toQuestionPage(
            ExamQuestionSnapshot question,
            Map<Integer, UserAnswerDetail> detailByItem,
            int firstNumber) {
        List<ItemPage> itemPages = new ArrayList<>();
        for (ExamItemSnapshot item : question.getItems()) {
            itemPages.add(
                    toItemPage(
                            item,
                            detailByItem.get(item.getSourceItemId()),
                            firstNumber + itemPages.size()));
        }
        List<MediaPage> mediaPages = new ArrayList<>();
        for (ExamMediaSnapshot media : question.getMedia()) {
            mediaPages.add(
                    new MediaPage(
                            media.getSourceMediaId(),
                            media.getMediaType(),
                            media.getOriginalFilename(),
                            media.getContentType()));
        }
        return new QuestionPage(
                question.getSourceQuestionId(),
                question.getContent(),
                question.getInstruction(),
                question.getReadingPassage(),
                question.getSkill(),
                question.getQuestionType(),
                question.getPosition(),
                itemPages,
                mediaPages);
    }

    private ItemPage toItemPage(ExamItemSnapshot item, UserAnswerDetail answer, int number) {
        List<OptionPage> options = new ArrayList<>();
        for (ExamOptionSnapshot option : item.getOptions()) {
            options.add(
                    new OptionPage(
                            option.getSourceOptionId(),
                            option.getOptionCode(),
                            option.getContent(),
                            Boolean.TRUE.equals(option.getCorrect())));
        }
        Integer responseSeconds = item.getResponseSeconds();
        return new ItemPage(
                item.getSourceItemId(),
                number,
                item.getContent(),
                responseSeconds,
                options,
                answer == null ? null : answer.getSelectedOptionId(),
                answer == null ? null : answer.getUserPosition(),
                item.getCorrectPosition(),
                answer == null ? null : answer.getAnswerText(),
                answer != null && answer.getAudioPath() != null,
                item.getMinWordCount(),
                item.getMaxWordCount());
    }
}
