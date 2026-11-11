package com.aptisprep.service.exam.impl;

import static com.aptisprep.service.grading.impl.GradingContextFormatter.DEFAULT_RUBRIC_VERSION;

import com.aptisprep.entity.*;
import com.aptisprep.entity.ExamSkillResultEntity;
import com.aptisprep.entity.User;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.event.AttemptSubmittedEvent;
import com.aptisprep.exception.AccessDeniedException;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.presentation.*;
import com.aptisprep.presentation.form.*;
import com.aptisprep.repository.*;
import com.aptisprep.service.account.UserService;
import com.aptisprep.service.admin.ExamCatalogService;
import com.aptisprep.service.exam.AttemptOutboxService;
import com.aptisprep.service.exam.ExamAttemptService;
import com.aptisprep.service.exam.ExamAttemptService.*;
import com.aptisprep.service.grading.AttemptFinalizationService;
import com.aptisprep.service.grading.GradingResultQueryService;
import com.aptisprep.service.grading.ObjectiveGradingService;
import com.aptisprep.service.grading.impl.GradingContextFormatter;
import com.aptisprep.service.scoring.ScoringPolicyService;
import com.aptisprep.service.storage.StorageService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExamAttemptServiceImpl implements ExamAttemptService {
    private static final Pattern CHECKPOINT_REQUEST_KEY =
            Pattern.compile("checkpoint:(\\d+):(\\d+):after:\\d+");
    private static final Set<String> ACTIVE_STATES =
            Set.of("IN_PROGRESS", "SUBMITTED", "WAITING_GRADING");
    private final UserService userService;
    private final ExamCatalogService examCatalogService;
    private final UserMediaActivityRepository userMediaActivityRepository;
    private final ExamSnapshotFactory examSnapshots;
    private final ExamGradingPayloadFactory gradingPayloads;
    private final AttemptResultReader resultReader;
    private final ExamAttemptRepository examAttemptRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final UserAnswerDetailRepository userAnswerDetailRepository;
    private final GradingResultQueryService gradingResultQueryService;
    private final ScoringPolicyService scoring;
    private final StorageService storage;
    private final AttemptOutboxService outbox;
    private final AttemptFinalizationService finalization;
    private final ObjectiveGradingService objectiveGrading;
    private final ApplicationEventPublisher events;
    private final ExamAnswerWriter answerWriter;
    private final ExamStartValidator startValidator;
    private final CheckpointStartValidator checkpointValidator;

    public ExamAttemptServiceImpl(
            UserService userService,
            ExamCatalogService examCatalogService,
            UserMediaActivityRepository userMediaActivityRepository,
            ExamSnapshotFactory examSnapshots,
            ExamGradingPayloadFactory gradingPayloads,
            AttemptResultReader resultReader,
            ExamAttemptRepository examAttemptRepository,
            UserAnswerRepository userAnswerRepository,
            UserAnswerDetailRepository userAnswerDetailRepository,
            GradingResultQueryService gradingResultQueryService,
            ScoringPolicyService scoring,
            StorageService storage,
            AttemptOutboxService outbox,
            AttemptFinalizationService finalization,
            ObjectiveGradingService objectiveGrading,
            ApplicationEventPublisher events,
            ExamAnswerWriter answerWriter,
            ExamStartValidator startValidator,
            CheckpointStartValidator checkpointValidator) {
        this.userService = userService;
        this.examCatalogService = examCatalogService;
        this.userMediaActivityRepository = userMediaActivityRepository;
        this.examSnapshots = examSnapshots;
        this.gradingPayloads = gradingPayloads;
        this.resultReader = resultReader;
        this.examAttemptRepository = examAttemptRepository;
        this.userAnswerRepository = userAnswerRepository;
        this.userAnswerDetailRepository = userAnswerDetailRepository;
        this.gradingResultQueryService = gradingResultQueryService;
        this.scoring = scoring;
        this.storage = storage;
        this.outbox = outbox;
        this.finalization = finalization;
        this.objectiveGrading = objectiveGrading;
        this.events = events;
        this.answerWriter = answerWriter;
        this.startValidator = startValidator;
        this.checkpointValidator = checkpointValidator;
    }

    @Override
    @Transactional
    public AttemptHandleDTO start(Integer userId, Integer examId, String requestKey) {
        return startInternal(userId, examId, requestKey, null, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer latestResumableAttemptId(Integer userId, Integer examId) {
        activeUser(userId);
        LocalDateTime now = LocalDateTime.now();
        return examAttemptRepository
                .findByUserIdAndMockExamIdAndStatusAndSubmittedAtIsNullOrderByIdDesc(
                        userId, examId, "IN_PROGRESS")
                .stream()
                .filter(
                        attempt -> {
                            LocalDateTime deadline = examDeadline(attempt, examFor(attempt));
                            return deadline == null || !deadline.isBefore(now);
                        })
                .map(ExamAttemptEntity::getId)
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional
    public AttemptHandleDTO startPlacement(Integer userId, Integer examId, String requestKey) {
        return startInternal(userId, examId, requestKey, null, true);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasInProgressPlacement(Integer userId, String skill) {
        return examAttemptRepository
                .findActivePlacement(userId, skill, Set.of("IN_PROGRESS"))
                .isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasInProgressPlacement(Integer userId) {
        activeUser(userId);
        return examAttemptRepository.findPlacementAttempts(userId).stream()
                .anyMatch(attempt -> ACTIVE_STATES.contains(attempt.getStatus()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPlacementAttempt(Integer userId) {
        activeUser(userId);
        return !examAttemptRepository.findPlacementAttempts(userId).isEmpty();
    }

    @Override
    @Transactional
    public AttemptHandleDTO startCheckpoint(
            Integer userId, Integer examId, Integer pathId, String requestKey) {
        if (pathId == null) throw new BusinessRuleException("Checkpoint cần ngữ cảnh lộ trình học");
        return startInternal(userId, examId, requestKey, pathId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Integer> checkpointOriginPathId(Integer userId, Integer attemptId) {
        activeUser(userId);
        return examAttemptRepository
                .findByIdAndUserId(attemptId, userId)
                .map(ExamAttemptEntity::getRequestKey)
                .flatMap(
                        key -> {
                            var match = CHECKPOINT_REQUEST_KEY.matcher(key);
                            return match.matches() && userId.toString().equals(match.group(1))
                                    ? Optional.of(Integer.valueOf(match.group(2)))
                                    : Optional.empty();
                        });
    }

    private AttemptHandleDTO startInternal(
            Integer userId, Integer examId, String requestKey, Integer pathId, boolean placement) {
        // Lock the user row for the whole transaction. A JVM lock is not enough here:
        // another request can enter after the first synchronized block exits but before
        // the first transaction has committed its attempt row.
        activeUserForUpdate(userId);
        MockExamEntity exam = publishedExam(examId);
        if (pathId != null) {
            if (!"CHECKPOINT".equals(exam.getExamType()))
                throw new BusinessRuleException("Đề không phải Checkpoint");
            checkpointValidator.validate(userId, exam, pathId);
        } else if ("CHECKPOINT".equals(exam.getExamType())) {
            throw new BusinessRuleException("Checkpoint chỉ được bắt đầu từ lộ trình học");
        }
        if (placement != "PLACEMENT".equals(exam.getExamType()))
            throw new BusinessRuleException("Placement chỉ được bắt đầu từ trang kiểm tra đầu vào");
        List<MockExamQuestionEntity> links = examCatalogService.findExamQuestions(examId);
        if (links.isEmpty()) throw new BusinessRuleException("Đề thi chưa có câu hỏi");
        String key = normalizeRequestKey(requestKey, userId, examId);
        Optional<ExamAttemptEntity> byKey =
                examAttemptRepository.findByUserIdAndRequestKey(userId, key);
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
            // Placement is one combined assessment per learner.  Resume the one
            // unfinished attempt, regardless of the legacy skill stored on its exam.
            List<ExamAttemptEntity> placementAttempts =
                    examAttemptRepository.findPlacementAttempts(userId);
            Optional<ExamAttemptEntity> active =
                    placementAttempts.stream()
                            .filter(attempt -> ACTIVE_STATES.contains(attempt.getStatus()))
                            .findFirst();
            if (active.isPresent()) {
                MockExamEntity activeExam = examFor(active.get());
                if (!expireIfNeeded(active.get(), activeExam))
                    return handle(active.get(), activeExam);
            }
        }
        ExamAttemptEntity attempt = new ExamAttemptEntity();
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
        attempt.setRubricVersion(DEFAULT_RUBRIC_VERSION);
        attempt = examAttemptRepository.save(attempt);
        examSnapshots.createSnapshot(attempt, exam, links);
        Integer newAttemptId = attempt.getId();
        for (MockExamQuestionEntity link : links) {
            UserAnswerEntity answer = new UserAnswerEntity();
            answer.setAttemptId(newAttemptId);
            answer.setQuestionId(link.getQuestionId());
            answer.setStatus("NOT_STARTED");
            userAnswerRepository.save(answer);
            examCatalogService.findQuestionMedia(link.getQuestionId()).stream()
                    .filter(media -> "AUDIO".equals(media.getMediaType()))
                    .forEach(
                            media -> {
                                UserMediaActivityEntity activity = new UserMediaActivityEntity();
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
        ExamAttemptEntity attempt =
                examAttemptRepository
                        .findByIdAndUserIdForUpdate(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền với lượt thi này"));
        if (!"IN_PROGRESS".equals(attempt.getStatus()))
            throw new BusinessRuleException("Lượt thi đã khóa");
        MockExamEntity exam = examFor(attempt);
        if (expireIfNeeded(attempt, exam))
            throw new BusinessRuleException("Lượt thi đã hết thời gian");
        ExamMediaSnapshotEntity lockedMedia = examSnapshots.lockedMediaEntity(attemptId, mediaId);
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
        boolean browserSpeech =
                "LISTENING".equalsIgnoreCase(lockedMedia.getQuestion().getSkill())
                        && startValidator.canUseBrowserSpeech(
                                lockedMedia.getMediaUrl(), lockedMedia.getTranscript());
        if (!browserSpeech) startValidator.validateAudio(lockedMedia.getMediaUrl());
        int limit = Math.min(2, Objects.requireNonNullElse(lockedMedia.getMaxPlayCount(), 2));
        UserMediaActivityEntity activity =
                userMediaActivityRepository
                        .findByAttemptIdAndMediaIdForUpdate(attemptId, mediaId)
                        .orElseThrow(
                                () ->
                                        new BusinessRuleException(
                                                "Không thể khởi tạo bộ đếm lượt nghe"));
        if (activity.getPlayCount() >= limit)
            throw new BusinessRuleException("Bạn đã sử dụng hết số lượt nghe cho đoạn audio này");
        String contentToken = browserSpeech ? null : UUID.randomUUID().toString();
        activity.setPlayCount(activity.getPlayCount() + 1);
        activity.setLastPlayedAt(LocalDateTime.now());
        activity.setPlayTokenHash(contentToken == null ? null : hashToken(contentToken));
        activity.setPlayTokenExpiresAt(
                contentToken == null ? null : LocalDateTime.now().plusMinutes(1));
        userMediaActivityRepository.save(activity);
        return new MediaPlayResult(
                mediaId,
                activity.getPlayCount(),
                limit,
                contentToken,
                browserSpeech,
                browserSpeech ? lockedMedia.getTranscript() : null);
    }

    @Transactional
    public MediaResource mediaContent(
            Integer userId, Integer attemptId, Integer mediaId, String contentToken) {
        ExamAttemptEntity attempt =
                examAttemptRepository
                        .findByIdAndUserId(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền xem media này"));
        MockExamEntity exam = examFor(attempt);
        if (expireIfNeeded(attempt, exam))
            throw new BusinessRuleException("Lượt thi đã hết thời gian");
        ExamMediaSnapshotEntity lockedMedia = examSnapshots.lockedMediaEntity(attemptId, mediaId);
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
            UserMediaActivityEntity activity =
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
            // Keep the short-lived token valid for the browser's metadata and
            // range requests. A browser can request the same audio more than
            // once while building the native player, so invalidating it after
            // the first request leaves the player stuck at 0:00 / 0:00.
        }
        return loadLockedMedia(lockedMedia);
    }

    private MediaResource loadLockedMedia(ExamMediaSnapshotEntity lockedMedia) {
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
        ExamAttemptEntity attempt =
                examAttemptRepository
                        .findByIdAndUserId(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền xem audio này"));
        if ("CANCELLED".equals(attempt.getStatus()))
            throw new ResourceNotFoundException("Lượt làm dở đã được xóa.");
        UserAnswerEntity answer =
                userAnswerRepository
                        .findByIdAndAttemptId(answerId, attemptId)
                        .orElseThrow(() -> new AccessDeniedException("Audio không thuộc lượt thi"));
        if (answer.getAudioUrl() == null)
            throw new ResourceNotFoundException("Chưa có file ghi âm");
        return new MediaResource(storage.load(answer.getAudioUrl()), "audio/*");
    }

    @Transactional
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

    @Override
    @Transactional
    public void submit(Integer userId, Integer attemptId, String requestKey) {
        ExamAttemptEntity attempt =
                examAttemptRepository
                        .findByIdAndUserIdForUpdate(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền với lượt thi này"));
        if ("COMPLETED".equals(attempt.getStatus()) || attempt.getSubmittedAt() != null) return;
        // A deadline check may have marked an unsubmitted attempt EXPIRED. It
        // still needs one final server-side submit so objective/AI grading can
        // finish and the learner can read the result.
        if (!"IN_PROGRESS".equals(attempt.getStatus())
                && !"WAITING_GRADING".equals(attempt.getStatus())
                && !("EXPIRED".equals(attempt.getStatus()) && attempt.getSubmittedAt() == null)) {
            throw new BusinessRuleException("Lượt thi không thể nộp ở trạng thái hiện tại");
        }
        MockExamEntity exam = examFor(attempt);
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
                        ? DEFAULT_RUBRIC_VERSION
                        : attempt.getRubricVersion());
        attempt.setGradingRequestKey(
                "attempt:" + attemptId + ":grading:v" + attempt.getAnswerVersion());
        validateWritingWordLimits(attempt.getId());
        LockedAttemptDTO draftLocked = gradingPayloads.build(attempt);
        gradingPayloads.validate(draftLocked);
        attempt.setSubmittedAt(now);
        attempt.setGradingStatus("GRADING");
        attempt.setStatus(expired ? "EXPIRED" : "SUBMITTED");
        LockedAttemptDTO locked = gradingPayloads.build(attempt);
        objectiveGrading.gradeObjective(locked, exam);
        boolean waiting =
                gradingResultQueryService.findSkillResultsByAttemptId(attempt.getId()).stream()
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
        ExamAttemptEntity attempt =
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
                        && !gradingResultQueryService.existsByAttemptId(attemptId);
        if (!terminal && !cleanDraft)
            throw new BusinessRuleException(
                    "Chỉ có thể loại bỏ lượt thi đang làm, đã hết thời gian hoặc đã hoàn thành.");
        attempt.setStatus("CANCELLED");
        attempt.setGradingStatus("CANCELLED");
        // A new checkpoint may reuse the start key of the discarded attempt.
        attempt.setRequestKey("discarded-attempt:" + attemptId);
    }

    @Override
    @Transactional
    public AttemptResultDTO result(Integer userId, Integer attemptId) {
        activeUser(userId);
        ExamAttemptEntity attempt =
                examAttemptRepository
                        .findByIdAndUserIdForUpdate(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền xem kết quả này"));
        if ("CANCELLED".equals(attempt.getStatus()))
            throw new ResourceNotFoundException("Lượt làm dở đã được xóa.");
        MockExamEntity exam = examFor(attempt);
        expireIfNeeded(attempt, exam);
        return resultReader.view(attempt, exam);
    }

    @Override
    @Transactional(readOnly = true)
    public HistoryPageDTO history(Integer userId, int page, int size, HistoryFilterDTO filter) {
        return resultReader.history(userId, page, size, filter);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminHistoryPageDTO adminHistory(
            String adminEmail, int page, int size, AdminHistoryFilterDTO filter) {
        return resultReader.adminHistory(adminEmail, page, size, filter);
    }

    @Override
    @Transactional(readOnly = true)
    public AttemptResultDTO adminResult(String adminEmail, Integer attemptId) {
        return resultReader.adminResult(adminEmail, attemptId);
    }

    @Override
    public void retryFailedGrading(Integer userId, Integer attemptId, String requestKey) {
        finalization.retryFailedGrading(userId, attemptId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canRegradeEmpty(Integer userId, Integer attemptId) {
        activeUser(userId);
        ExamAttemptEntity attempt =
                examAttemptRepository
                        .findByIdAndUserId(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền với lượt thi này"));
        return canRegradeEmpty(attempt);
    }

    private boolean canRegradeEmpty(ExamAttemptEntity attempt) {
        if (!"ERROR".equals(attempt.getStatus())) return false;
        List<ExamSkillResultEntity> results =
                gradingResultQueryService.findSkillResultsByAttemptId(attempt.getId());
        boolean emptyError = false;
        for (ExamSkillResultEntity result : results) {
            if (!"ERROR".equals(result.getGradingStatus())) continue;
            if (!"AI_EMPTY".equals(result.getGradingMethod())
                    && !"AI_DISABLED".equals(result.getGradingMethod())) return false;
            emptyError = true;
        }
        if (!emptyError) return false;
        LockedAttemptDTO locked;
        try {
            locked = gradingPayloads.build(attempt);
        } catch (BusinessRuleException | ResourceNotFoundException unavailableSnapshot) {
            return false;
        }
        boolean hasSubjective = false;
        for (LockedAnswerDTO answer : locked.answers()) {
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
        ExamAttemptEntity attempt =
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
        MockExamEntity exam = examFor(attempt);
        LockedAttemptDTO locked = gradingPayloads.build(attempt);
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
        ExamAttemptEntity attempt = editableAttempt(userId, attemptId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime clockStart = examSnapshots.sectionClockStart(attempt);
        LocalDateTime sectionEnd = clockStart.plusMinutes(sectionEndMinute);
        List<ExamSnapshotFactory.SectionWindow> windows =
                examSnapshots.lockedQuestionEntities(attemptId).stream()
                        .filter(question -> question.getSectionEndMinute() != null)
                        .map(
                                question ->
                                        new ExamSnapshotFactory.SectionWindow(
                                                question.getSectionStartMinute(),
                                                question.getSectionEndMinute()))
                        .filter(Objects::nonNull)
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
            Duration remaining =
                    now.isBefore(sectionEnd) ? Duration.between(now, sectionEnd) : Duration.ZERO;
            examSnapshots.advanceSectionClock(attemptId, remaining);
        }
    }

    @Transactional
    @Override
    public boolean sectionDeadlineReached(Integer userId, Integer attemptId, int sectionEndMinute) {
        activeUser(userId);
        ExamAttemptEntity attempt = editableAttempt(userId, attemptId);
        boolean sectionExists =
                examSnapshots.lockedQuestionEntities(attemptId).stream()
                        .anyMatch(
                                question ->
                                        Objects.equals(
                                                question.getSectionEndMinute(), sectionEndMinute));
        return sectionExists
                && !LocalDateTime.now()
                        .isBefore(
                                examSnapshots
                                        .sectionClockStart(attempt)
                                        .plusMinutes(sectionEndMinute));
    }

    @Transactional
    @Override
    public boolean examDeadlineReached(Integer userId, Integer attemptId) {
        activeUser(userId);
        ExamAttemptEntity attempt =
                examAttemptRepository
                        .findByIdAndUserIdForUpdate(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền với lượt thi này"));
        LocalDateTime deadline = examDeadline(attempt, examFor(attempt));
        return deadline != null && !LocalDateTime.now().isBefore(deadline);
    }

    @Transactional
    public AttemptReview review(Integer userId, Integer attemptId) {
        ExamAttemptEntity attempt = reviewableAttempt(userId, attemptId);
        return buildReview(attempt);
    }

    @Transactional(readOnly = true)
    public AttemptReview adminReview(String adminEmail, Integer attemptId) {
        User admin =
                userService
                        .findByEmailIgnoreCase(adminEmail)
                        .orElseThrow(() -> new AccessDeniedException("Không có quyền quản trị"));
        if (admin.getRole() != Role.ADMIN || admin.getStatus() != UserStatus.ACTIVE)
            throw new AccessDeniedException("Không có quyền quản trị");
        ExamAttemptEntity attempt =
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
                userService
                        .findByEmailIgnoreCase(adminEmail)
                        .orElseThrow(() -> new AccessDeniedException("Không có quyền quản trị"));
        if (admin.getRole() != Role.ADMIN || admin.getStatus() != UserStatus.ACTIVE)
            throw new AccessDeniedException("Không có quyền quản trị");
        ExamAttemptEntity attempt =
                examAttemptRepository
                        .findById(attemptId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy lượt thi"));
        if ("CANCELLED".equals(attempt.getStatus())) return;
        attempt.setStatus("CANCELLED");
        attempt.setGradingStatus("CANCELLED");
        attempt.setRequestKey("admin-discarded-attempt:" + attemptId);
    }

    private AttemptReview buildReview(ExamAttemptEntity attempt) {
        Integer attemptId = attempt.getId();
        MockExamEntity current = null;
        if (examSnapshots.frozenExam(attemptId).isEmpty()) {
            current =
                    examCatalogService
                            .findMockExamByIdForUpdate(attempt.getMockExamId())
                            .orElseThrow(
                                    () -> new ResourceNotFoundException("Không tìm thấy đề thi"));
            if (examSnapshots.frozenExam(attemptId).isEmpty())
                try {
                    examSnapshots.preserveLegacyAttempt(
                            attempt,
                            current,
                            examCatalogService.findExamQuestions(current.getId()));
                } catch (BusinessRuleException ignored) {
                    return new AttemptReview(current.getTitle(), List.of());
                }
        }
        List<QuestionPage> questions = new ArrayList<>();
        int number = 1;
        for (ExamQuestionSnapshotEntity question :
                examSnapshots.lockedQuestionEntities(attemptId)) {
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
        ExamMediaSnapshotEntity media = examSnapshots.lockedMediaEntity(attemptId, mediaId);
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

    private ExamAttemptEntity reviewableAttempt(Integer userId, Integer attemptId) {
        activeUser(userId);
        ExamAttemptEntity attempt =
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
        ExamAttemptEntity attempt =
                examAttemptRepository
                        .findByIdAndUserIdForUpdate(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền với lượt thi này"));
        if ("CANCELLED".equals(attempt.getStatus()))
            throw new ResourceNotFoundException("Lượt làm dở đã được xóa.");
        MockExamEntity exam = examFor(attempt);
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
        Map<ExamSnapshotFactory.SectionWindow, List<ExamQuestionSnapshotEntity>> timedSections =
                new TreeMap<>(Comparator.comparingInt(ExamSnapshotFactory.SectionWindow::start));
        for (ExamQuestionSnapshotEntity question : lockedQuestions) {
            var section =
                    question.getSectionEndMinute() == null
                            ? null
                            : new ExamSnapshotFactory.SectionWindow(
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
        for (ExamQuestionSnapshotEntity question : lockedQuestions) {
            var window =
                    question.getSectionEndMinute() == null
                            ? null
                            : new ExamSnapshotFactory.SectionWindow(
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

    @Override
    public HistoryPageDTO history(Integer userId, int page, int size) {
        return history(userId, page, size, HistoryFilterDTO.empty());
    }

    @Override
    @Transactional
    public void preserveLegacyAttempts(Integer examId) {
        if (examId == null) return;
        var legacyAttempts = examAttemptRepository.findWithoutSnapshot(examId);
        if (!legacyAttempts.isEmpty()) {
            MockExamEntity exam =
                    examCatalogService
                            .findMockExamById(examId)
                            .orElseThrow(
                                    () -> new ResourceNotFoundException("Không tìm thấy đề thi"));
            var links = examCatalogService.findExamQuestions(examId);
            for (var attempt : legacyAttempts) {
                examSnapshots.preserveLegacyAttempt(attempt, exam, links);
            }
        }
    }

    @Override
    public void validateExamReady(MockExamEntity exam, List<MockExamQuestionEntity> links) {
        startValidator.validate(exam, links);
    }

    @Override
    public void validateExamForPublication(
            MockExamEntity exam, List<MockExamQuestionEntity> links) {
        startValidator.validateForPublication(exam, links);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalDateTime> findSubmittedDatesByUserId(Integer userId) {
        return examAttemptRepository.findByUserId(userId).stream()
                .map(ExamAttemptEntity::getSubmittedAt)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SnapshotSchemeInfo> findSnapshotSchemeInfo(Integer attemptId) {
        if (attemptId == null) return Optional.empty();
        return examSnapshots
                .findSnapshotByAttemptId(attemptId)
                .map(s -> new SnapshotSchemeInfo(s.getScoringSchemeId(), s.getSkill()));
    }

    private void refreshSnapshotAnswerVersion(ExamAttemptEntity attempt) {
        Integer attemptId = attempt.getId();
        int version =
                userAnswerRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId).stream()
                        .mapToInt(UserAnswerEntity::getAnswerVersion)
                        .max()
                        .orElse(1);
        attempt.setAnswerVersion(version);
        examSnapshots.updateAnswerVersion(attemptId, version);
    }

    private ExamAttemptEntity editableAttempt(Integer userId, Integer attemptId) {
        ExamAttemptEntity attempt =
                examAttemptRepository
                        .findByIdAndUserIdForUpdate(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền với lượt thi này"));
        if (!"IN_PROGRESS".equals(attempt.getStatus()))
            throw new BusinessRuleException("Lượt thi đã khóa");
        MockExamEntity exam = examFor(attempt);
        if (expireIfNeeded(attempt, exam)) {
            throw new BusinessRuleException("Lượt thi đã hết thời gian");
        }
        return attempt;
    }

    private void ensureMediaWindow(ExamAttemptEntity attempt) {
        if (!"IN_PROGRESS".equals(attempt.getStatus()))
            throw new BusinessRuleException("Lượt thi đã khóa");
        MockExamEntity exam = examFor(attempt);
        if (expireIfNeeded(attempt, exam)) {
            throw new BusinessRuleException("Lượt thi đã hết thời gian");
        }
    }

    private boolean expireIfNeeded(ExamAttemptEntity attempt, MockExamEntity exam) {
        if (!"IN_PROGRESS".equals(attempt.getStatus())) return false;
        LocalDateTime end = examDeadline(attempt, exam);
        if (end == null || !end.isBefore(LocalDateTime.now())) return false;
        attempt.setStatus("EXPIRED");
        attempt.setGradingStatus("EXPIRED");
        examAttemptRepository.save(attempt);
        return true;
    }

    private MockExamEntity publishedExam(Integer id) {
        return examCatalogService
                .findMockExamByIdForUpdate(id)
                .filter(x -> "PUBLISHED".equals(x.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Đề thi chưa được công bố"));
    }

    private MockExamEntity examFor(ExamAttemptEntity attempt) {
        return examSnapshots
                .frozenExam(attempt.getId())
                .orElseGet(
                        () ->
                                examCatalogService
                                        .findMockExamById(attempt.getMockExamId())
                                        .orElseThrow(
                                                () ->
                                                        new ResourceNotFoundException(
                                                                "Không tìm thấy đề thi")));
    }

    private User activeUser(Integer id) {
        User user =
                userService
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (user.getRole() != Role.STUDENT || user.getStatus() != UserStatus.ACTIVE)
            throw new AccessDeniedException("Tài khoản không còn hoạt động");
        return user;
    }

    private User activeUserForUpdate(Integer id) {
        User user =
                userService
                        .findByIdForUpdate(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (user.getRole() != Role.STUDENT || user.getStatus() != UserStatus.ACTIVE)
            throw new AccessDeniedException("Tài khoản không còn hoạt động");
        return user;
    }

    private AttemptHandleDTO handle(ExamAttemptEntity attempt, MockExamEntity exam) {
        return new AttemptHandleDTO(
                attempt.getId(),
                attempt.getStatus(),
                instant(
                        Objects.requireNonNullElseGet(
                                examDeadline(attempt, exam), attempt::getStartedAt)));
    }

    private LocalDateTime examDeadline(ExamAttemptEntity attempt, MockExamEntity exam) {
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

    private Map<Integer, UserAnswerDetailEntity> answerDetails(
            Integer attemptId, Integer questionId) {
        UserAnswerEntity answer =
                userAnswerRepository
                        .findByAttemptIdAndQuestionId(attemptId, questionId)
                        .orElse(null);
        if (answer == null) return Map.of();
        Map<Integer, UserAnswerDetailEntity> detailByItem = new HashMap<>();
        for (UserAnswerDetailEntity detail :
                userAnswerDetailRepository.findByUserAnswerIdOrderByQuestionItemIdAsc(
                        answer.getId())) {
            detailByItem.put(detail.getQuestionItemId(), detail);
        }
        return detailByItem;
    }

    private void validateWritingWordLimits(Integer attemptId) {
        ExamAttemptSnapshotEntity snapshot = examSnapshots.require(attemptId);
        for (ExamQuestionSnapshotEntity question : snapshot.getQuestions()) {
            if (!"WRITING_TEXT".equals(question.getQuestionType())) continue;
            UserAnswerEntity answer =
                    userAnswerRepository
                            .findByAttemptIdAndQuestionId(attemptId, question.getSourceQuestionId())
                            .orElse(null);
            Map<Integer, UserAnswerDetailEntity> details =
                    answerDetails(attemptId, question.getSourceQuestionId());
            for (ExamItemSnapshotEntity item : question.getItems()) {
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
            ExamQuestionSnapshotEntity question,
            Map<Integer, UserAnswerDetailEntity> detailByItem,
            int firstNumber) {
        List<ItemPage> itemPages = new ArrayList<>();
        for (ExamItemSnapshotEntity item : question.getItems()) {
            itemPages.add(
                    toItemPage(
                            item,
                            detailByItem.get(item.getSourceItemId()),
                            firstNumber + itemPages.size()));
        }
        List<MediaPage> mediaPages = new ArrayList<>();
        for (ExamMediaSnapshotEntity media : question.getMedia()) {
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

    private ItemPage toItemPage(
            ExamItemSnapshotEntity item, UserAnswerDetailEntity answer, int number) {
        List<OptionPage> options = new ArrayList<>();
        for (ExamOptionSnapshotEntity option : item.getOptions()) {
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

    @Override
    @Transactional(readOnly = true)
    public boolean hasAnswersForQuestion(Integer questionId) {
        return userAnswerRepository.existsByQuestionId(questionId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAttemptsForScoringScheme(Integer schemeId) {
        return examAttemptRepository.existsByScoringSchemeId(schemeId);
    }
}
