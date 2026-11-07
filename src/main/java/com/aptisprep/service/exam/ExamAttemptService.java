package com.aptisprep.service.exam;

import static com.aptisprep.service.contract.GradingContextFormatter.DEFAULT_RUBRIC_VERSION;

import com.aptisprep.entity.*;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.event.AttemptSubmittedEvent;
import com.aptisprep.exception.AccessDeniedException;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.*;
import com.aptisprep.service.contract.ExamAttemptContract;
import com.aptisprep.service.contract.GradingContextFormatter;
import com.aptisprep.service.contract.IntegrationModels.*;
import com.aptisprep.service.contract.ScoringPolicyContract;
import com.aptisprep.service.grading.AttemptFinalizationService;
import com.aptisprep.service.grading.ObjectiveGradingService;
import com.aptisprep.service.storage.StorageService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

@Service
public class ExamAttemptService implements ExamAttemptContract {
    private static final Set<String> ACTIVE_STATES =
            Set.of("IN_PROGRESS", "SUBMITTED", "WAITING_GRADING");
    private final UserRepository userRepository;
    private final MockExamRepository mockExamRepository;
    private final MockExamQuestionRepository mockExamQuestionRepository;
    private final QuestionMediaRepository questionMediaRepository;
    private final UserMediaActivityRepository userMediaActivityRepository;
    private final ExamSnapshotService examSnapshots;
    private final ExamGradingPayloadFactory gradingPayloads;
    private final AttemptResultReader resultReader;
    private final ExamAttemptRepository examAttemptRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final UserAnswerDetailRepository userAnswerDetailRepository;
    private final ExamSkillResultRepository examSkillResultRepository;
    private final ScoringPolicyContract scoring;
    private final StorageService storage;
    private final AttemptOutboxService outbox;
    private final AttemptFinalizationService finalization;
    private final ObjectiveGradingService objectiveGrading;
    private final ApplicationEventPublisher events;
    private final ExamAnswerWriter answerWriter;
    private final ExamStartValidator startValidator;
    private final CheckpointStartValidator checkpointValidator;

    public ExamAttemptService(
            UserRepository userRepository,
            MockExamRepository mockExamRepository,
            MockExamQuestionRepository mockExamQuestionRepository,
            QuestionMediaRepository questionMediaRepository,
            UserMediaActivityRepository userMediaActivityRepository,
            ExamSnapshotService examSnapshots,
            ExamGradingPayloadFactory gradingPayloads,
            AttemptResultReader resultReader,
            ExamAttemptRepository examAttemptRepository,
            UserAnswerRepository userAnswerRepository,
            UserAnswerDetailRepository userAnswerDetailRepository,
            ExamSkillResultRepository examSkillResultRepository,
            ScoringPolicyContract scoring,
            StorageService storage,
            AttemptOutboxService outbox,
            AttemptFinalizationService finalization,
            ObjectiveGradingService objectiveGrading,
            ApplicationEventPublisher events,
            ExamAnswerWriter answerWriter,
            ExamStartValidator startValidator,
            CheckpointStartValidator checkpointValidator) {
        this.userRepository = userRepository;
        this.mockExamRepository = mockExamRepository;
        this.mockExamQuestionRepository = mockExamQuestionRepository;
        this.questionMediaRepository = questionMediaRepository;
        this.userMediaActivityRepository = userMediaActivityRepository;
        this.examSnapshots = examSnapshots;
        this.gradingPayloads = gradingPayloads;
        this.resultReader = resultReader;
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
    }

    @Override
    @Transactional
    public AttemptHandle start(Integer userId, Integer examId, String requestKey) {
        return startInternal(userId, examId, requestKey, null, false);
    }

    @Override
    @Transactional
    public AttemptHandle startPlacement(Integer userId, Integer examId, String requestKey) {
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
    @Transactional
    public AttemptHandle startCheckpoint(
            Integer userId, Integer examId, Integer pathId, String requestKey) {
        if (pathId == null) throw new BusinessRuleException("Checkpoint cần ngữ cảnh lộ trình học");
        return startInternal(userId, examId, requestKey, pathId, false);
    }

    private AttemptHandle startInternal(
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
        List<MockExamQuestionEntity> links =
                mockExamQuestionRepository.findByMockExamIdOrderByPosition(examId);
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
            Optional<ExamAttemptEntity> active =
                    examAttemptRepository.findActivePlacement(
                            userId, exam.getSkill(), ACTIVE_STATES);
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
            questionMediaRepository.findByQuestionIdOrderByPosition(link.getQuestionId()).stream()
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
        JsonNode lockedMedia = examSnapshots.lockedMedia(attemptId, mediaId);
        if (lockedMedia == null
                || !"AUDIO".equals(lockedMedia.path("type").asText())
                || !userAnswerRepository
                        .findByAttemptIdAndQuestionId(
                                attemptId, lockedMedia.path("questionId").asInt())
                        .isPresent()) throw new AccessDeniedException("Media không thuộc lượt thi");
        ensureMediaWindow(attempt);
        ExamSnapshotService.requireOpenSection(
                attempt,
                examSnapshots
                        .lockedQuestions(attemptId)
                        .get(lockedMedia.path("questionId").asInt()));
        startValidator.validateAudio(lockedMedia.path("mediaUrl").asText());
        int limit = Math.min(2, lockedMedia.path("maxPlayCount").asInt(2));
        UserMediaActivityEntity activity =
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
        JsonNode lockedMedia = examSnapshots.lockedMedia(attemptId, mediaId);
        if (lockedMedia == null
                || !userAnswerRepository
                        .findByAttemptIdAndQuestionId(
                                attemptId, lockedMedia.path("questionId").asInt())
                        .isPresent()) throw new AccessDeniedException("Media không thuộc lượt thi");
        ensureMediaWindow(attempt);
        ExamSnapshotService.requireOpenSection(
                attempt,
                examSnapshots
                        .lockedQuestions(attemptId)
                        .get(lockedMedia.path("questionId").asInt()));
        if ("AUDIO".equals(lockedMedia.path("type").asText())) {
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
        String mediaUrl = lockedMedia.path("mediaUrl").asText();
        String contentType = lockedMedia.path("contentType").asText("application/octet-stream");
        Resource resource;
        try {
            resource = storage.load(mediaUrl);
        } catch (BusinessRuleException exception) {
            if (!"IMAGE".equals(lockedMedia.path("type").asText())) throw exception;
            resource = new ClassPathResource("static/images/speaking-community.svg");
            contentType = "image/svg+xml";
        }
        return new MediaResource(resource, contentType);
    }

    @Transactional(readOnly = true)
    public MediaResource speakingAudio(Integer userId, Integer attemptId, Integer answerId) {
        examAttemptRepository
                .findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền xem audio này"));
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
        LockedAttempt draftLocked = gradingPayloads.build(attempt, exam);
        gradingPayloads.validate(draftLocked);
        attempt.setSubmittedAt(now);
        attempt.setGradingStatus("GRADING");
        attempt.setStatus(expired ? "EXPIRED" : "SUBMITTED");
        LockedAttempt locked = gradingPayloads.build(attempt, exam);
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

    @Override
    @Transactional
    public AttemptResult result(Integer userId, Integer attemptId) {
        activeUser(userId);
        ExamAttemptEntity attempt =
                examAttemptRepository
                        .findByIdAndUserId(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền xem kết quả này"));
        MockExamEntity exam = examFor(attempt);
        expireIfNeeded(attempt, exam);
        return resultReader.view(attempt, exam);
    }

    @Override
    @Transactional(readOnly = true)
    public HistoryPage history(Integer userId, int page, int size, HistoryFilter filter) {
        return resultReader.history(userId, page, size, filter);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminHistoryPage adminHistory(
            String adminEmail, int page, int size, AdminHistoryFilter filter) {
        return resultReader.adminHistory(adminEmail, page, size, filter);
    }

    @Override
    @Transactional(readOnly = true)
    public AttemptResult adminResult(String adminEmail, Integer attemptId) {
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
                examSkillResultRepository.findByAttemptIdOrderBySkillAsc(attempt.getId());
        boolean emptyError = false;
        for (ExamSkillResultEntity result : results) {
            if (!"ERROR".equals(result.getGradingStatus())) continue;
            if (!"AI_EMPTY".equals(result.getGradingMethod())
                    && !"AI_DISABLED".equals(result.getGradingMethod())) return false;
            emptyError = true;
        }
        if (!emptyError) return false;
        LockedAttempt locked;
        try {
            locked = gradingPayloads.build(attempt, examFor(attempt));
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
        LockedAttempt locked = gradingPayloads.build(attempt, exam);
        objectiveGrading.gradeObjective(locked, exam);
        if (!finalization.finalizeIfReady(attempt, exam)) {
            throw new BusinessRuleException(
                    "Bộ chấm điểm chưa thể tổng hợp kết quả. Dữ liệu cũ được giữ nguyên.");
        }
        finalization.publishCompleted(attempt, exam);
    }

    @Transactional
    public AttemptPage page(Integer userId, Integer attemptId) {
        activeUser(userId);
        ExamAttemptEntity attempt =
                examAttemptRepository
                        .findByIdAndUserId(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền với lượt thi này"));
        MockExamEntity exam = examFor(attempt);
        expireIfNeeded(attempt, exam);
        List<QuestionPage> questionPages = new ArrayList<>();
        List<QuestionPage> allQuestionPages = new ArrayList<>();
        List<SectionPage> sectionPages = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime examEnd = examDeadline(attempt, exam);
        Instant deadline = instant(examEnd);
        Map<SectionKey, List<JsonNode>> groupedQuestions = new LinkedHashMap<>();
        for (JsonNode question : examSnapshots.lockedQuestionList(attemptId)) {
            var window = ExamSnapshotService.SectionWindow.of(question);
            SectionKey key =
                    window == null
                            ? new SectionKey(question.path("skill").asText(), -1, -1)
                            : new SectionKey(
                                    question.path("skill").asText(), window.start(), window.end());
            groupedQuestions.computeIfAbsent(key, ignored -> new ArrayList<>()).add(question);
        }

        int currentSectionIndex = -1;
        String sectionSkill = null;
        List<Map.Entry<SectionKey, List<JsonNode>>> groupedEntries =
                new ArrayList<>(groupedQuestions.entrySet());
        groupedEntries.sort(
                Comparator.comparingInt(
                        entry -> entry.getKey().start() < 0 ? 0 : entry.getKey().start()));
        for (Map.Entry<SectionKey, List<JsonNode>> entry : groupedEntries) {
            SectionKey key = entry.getKey();
            var window = key.window();
            String state = sectionState(window, attempt, now);
            boolean open = "OPEN".equals(state);
            int sectionIndex = sectionPages.size();
            Integer minutes =
                    window == null
                            ? Objects.requireNonNullElse(exam.getDurationMinutes(), 0)
                            : window.end() - window.start();
            sectionPages.add(
                    new SectionPage(
                            sectionIndex,
                            key.skill(),
                            minutes,
                            entry.getValue().size(),
                            state,
                            open));

            if (open) {
                currentSectionIndex = sectionIndex;
                sectionSkill = key.skill();
                if (window != null) {
                    deadline = instant(attempt.getStartedAt().plusMinutes(window.end()));
                }
            }
            for (JsonNode question : entry.getValue()) {
                QuestionPage page = toQuestionPage(attemptId, question, sectionIndex, open);
                allQuestionPages.add(page);
                if (open) questionPages.add(page);
            }
        }
        boolean nextSection =
                currentSectionIndex >= 0 && currentSectionIndex < sectionPages.size() - 1;
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
                List.copyOf(allQuestionPages),
                List.copyOf(sectionPages),
                currentSectionIndex,
                deadline,
                nextSection,
                sectionSkill);
    }

    private String sectionState(
            ExamSnapshotService.SectionWindow window,
            ExamAttemptEntity attempt,
            LocalDateTime now) {
        if (!"IN_PROGRESS".equals(attempt.getStatus())) return "COMPLETED";
        if (window == null) return "OPEN";
        if (window.contains(attempt.getStartedAt(), now)) return "OPEN";
        return now.isBefore(attempt.getStartedAt().plusMinutes(window.start()))
                ? "UPCOMING"
                : "COMPLETED";
    }

    private record SectionKey(String skill, int start, int end) {
        private ExamSnapshotService.SectionWindow window() {
            return start < 0 ? null : new ExamSnapshotService.SectionWindow(start, end);
        }
    }

    public record SectionPage(
            int index,
            String skill,
            Integer durationMinutes,
            int questionCount,
            String state,
            boolean open) {}

    public record AttemptPage(
            Integer attemptId,
            String title,
            String examType,
            Long remainingSeconds,
            List<QuestionPage> questions,
            List<QuestionPage> allQuestions,
            List<SectionPage> sections,
            int currentSectionIndex,
            Instant deadline,
            boolean nextSection,
            String sectionSkill) {
        public AttemptPage(
                Integer attemptId,
                String title,
                String examType,
                Long remainingSeconds,
                List<QuestionPage> questions,
                Instant deadline) {
            this(
                    attemptId,
                    title,
                    examType,
                    remainingSeconds,
                    questions,
                    questions,
                    List.of(),
                    0,
                    deadline,
                    false,
                    null);
        }

        public AttemptPage(
                Integer attemptId,
                String title,
                Long remainingSeconds,
                List<QuestionPage> questions,
                Instant deadline) {
            this(attemptId, title, null, remainingSeconds, questions, deadline);
        }

        public AttemptPage(
                Integer attemptId,
                String title,
                Long remainingSeconds,
                List<QuestionPage> questions) {
            this(attemptId, title, null, remainingSeconds, questions, null);
        }

        private List<QuestionPage> progressQuestions() {
            return allQuestions == null || allQuestions.isEmpty() ? questions : allQuestions;
        }

        public int totalItems() {
            List<QuestionPage> source = progressQuestions();
            return source == null
                    ? 0
                    : source.stream()
                            .mapToInt(
                                    question ->
                                            question.items() == null ? 0 : question.items().size())
                            .sum();
        }

        public int answeredItems() {
            List<QuestionPage> source = progressQuestions();
            return source == null
                    ? 0
                    : source.stream()
                            .flatMap(
                                    question ->
                                            question.items() == null
                                                    ? Stream.empty()
                                                    : question.items().stream())
                            .mapToInt(
                                    item ->
                                            item.selectedOptionId() != null
                                                            || item.userPosition() != null
                                                            || (item.textAnswer() != null
                                                                    && !item.textAnswer().isBlank())
                                                            || item.audioSaved()
                                                    ? 1
                                                    : 0)
                            .sum();
        }
    }

    public record QuestionPage(
            Integer questionId,
            String content,
            String instruction,
            String readingPassage,
            String skill,
            String type,
            int position,
            List<ItemPage> items,
            List<MediaPage> media,
            int sectionIndex,
            boolean sectionOpen) {}

    public record ItemPage(
            Integer itemId,
            int position,
            String content,
            Integer responseSeconds,
            List<OptionPage> options,
            Integer selectedOptionId,
            Integer userPosition,
            String textAnswer,
            boolean audioSaved) {}

    public record OptionPage(Integer id, String code, String content) {}

    public record MediaPage(Integer id, String type, String filename, String contentType) {}

    public record MediaPlayResult(
            Integer mediaId, int playCount, int maxPlayCount, String contentToken) {}

    public record MediaResource(Resource resource, String contentType) {}

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
        return mockExamRepository
                .findById(id)
                .filter(x -> "PUBLISHED".equals(x.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Đề thi chưa được công bố"));
    }

    private MockExamEntity examFor(ExamAttemptEntity attempt) {
        return mockExamRepository
                .findById(attempt.getMockExamId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
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

    private AttemptHandle handle(ExamAttemptEntity attempt, MockExamEntity exam) {
        return new AttemptHandle(
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
                : attempt.getStartedAt().plusMinutes(minutes);
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

    private QuestionPage toQuestionPage(
            Integer attemptId, JsonNode question, int sectionIndex, boolean sectionOpen) {
        Integer questionId = question.path("id").asInt();
        UserAnswerEntity answer =
                userAnswerRepository
                        .findByAttemptIdAndQuestionId(attemptId, questionId)
                        .orElseThrow();
        Map<Integer, UserAnswerDetailEntity> detailByItem = new HashMap<>();
        for (UserAnswerDetailEntity detail :
                userAnswerDetailRepository.findByUserAnswerIdOrderByQuestionItemIdAsc(
                        answer.getId())) {
            detailByItem.put(detail.getQuestionItemId(), detail);
        }
        List<ItemPage> itemPages = new ArrayList<>();
        for (JsonNode item : question.path("items")) {
            itemPages.add(toItemPage(item, detailByItem.get(item.path("id").asInt())));
        }
        List<MediaPage> mediaPages = new ArrayList<>();
        for (JsonNode media : question.path("media")) {
            mediaPages.add(
                    new MediaPage(
                            media.path("id").asInt(),
                            media.path("type").asText(),
                            media.path("filename").asText(),
                            media.path("contentType").asText()));
        }
        return new QuestionPage(
                questionId,
                question.path("content").asText(),
                question.path("instruction").asText(),
                question.path("readingPassage").asText(null),
                question.path("skill").asText(),
                question.path("type").asText(),
                question.path("position").asInt(),
                itemPages,
                mediaPages,
                sectionIndex,
                sectionOpen);
    }

    private ItemPage toItemPage(JsonNode item, UserAnswerDetailEntity answer) {
        List<OptionPage> options = new ArrayList<>();
        for (JsonNode option : item.path("options")) {
            options.add(
                    new OptionPage(
                            option.path("id").asInt(),
                            option.path("code").asText(),
                            option.path("content").asText()));
        }
        Integer responseSeconds =
                item.path("responseSeconds").isNull() ? null : item.path("responseSeconds").asInt();
        return new ItemPage(
                item.path("id").asInt(),
                item.path("position").asInt(),
                item.path("content").asText(),
                responseSeconds,
                options,
                answer == null ? null : answer.getSelectedOptionId(),
                answer == null ? null : answer.getUserPosition(),
                answer == null ? null : answer.getAnswerText(),
                answer != null && answer.getAudioPath() != null);
    }
}
