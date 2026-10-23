package com.aptisprep.service.exam;

import com.aptisprep.entity.*;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.event.AiAssessmentCompletedEvent;
import com.aptisprep.event.AttemptSubmittedEvent;
import com.aptisprep.exception.AccessDeniedException;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.*;
import com.aptisprep.service.contract.ExamAttemptContract;
import com.aptisprep.service.contract.AiAssessmentContract;
import com.aptisprep.service.contract.IntegrationModels.*;
import com.aptisprep.service.contract.GradingContextFormatter;
import com.aptisprep.service.contract.GradingTextLimits;
import com.aptisprep.service.contract.ScoringPolicyContract;
import com.aptisprep.service.grading.AttemptFinalizationService;
import com.aptisprep.service.grading.ObjectiveGradingService;
import com.aptisprep.service.storage.AudioUploadService;
import com.aptisprep.service.storage.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamAttemptService implements ExamAttemptContract {
    private static final String RUBRIC_VERSION = "aptis-rubric-v1";
    private static final Set<String> ACTIVE_STATES = Set.of("IN_PROGRESS", "SUBMITTED", "WAITING_GRADING");
    private static final Set<String> TERMINAL_STATES = Set.of("COMPLETED", "ERROR", "EXPIRED", "CANCELLED");
    private static final ConcurrentHashMap<String, Object> START_LOCKS = new ConcurrentHashMap<>();
    private final UserRepository users;
    private final MockExamRepository exams;
    private final MockExamQuestionRepository examQuestions;
    private final QuestionRepository questions;
    private final QuestionItemRepository items;
    private final QuestionOptionRepository options;
    private final QuestionMediaRepository questionMedia;
    private final UserMediaActivityRepository mediaActivities;
    private final ExamAttemptSnapshotRepository snapshots;
    private final ExamAttemptRepository attempts;
    private final UserAnswerRepository answers;
    private final UserAnswerDetailRepository details;
    private final ExamSkillResultRepository skillResults;
    private final ScoringPolicyContract scoring;
    private final AiAssessmentContract ai;
    private final AudioUploadService audioUploads;
    private final StorageService storage;
    private final AttemptOutboxService outbox;
    private final AttemptFinalizationService finalization;
    private final ObjectiveGradingService objectiveGrading;
    private final ApplicationEventPublisher events;
    private final JsonMapper json = JsonMapper.builder().build();

    public ExamAttemptService(UserRepository users, MockExamRepository exams, MockExamQuestionRepository examQuestions,
            QuestionRepository questions, QuestionItemRepository items, QuestionOptionRepository options,
            QuestionMediaRepository questionMedia, UserMediaActivityRepository mediaActivities, ExamAttemptSnapshotRepository snapshots,
            ExamAttemptRepository attempts, UserAnswerRepository answers, UserAnswerDetailRepository details,
            ExamSkillResultRepository skillResults, ScoringPolicyContract scoring, AiAssessmentContract ai,
            AudioUploadService audioUploads, StorageService storage, AttemptOutboxService outbox,
            AttemptFinalizationService finalization, ObjectiveGradingService objectiveGrading,
            ApplicationEventPublisher events) {
        this.users = users; this.exams = exams; this.examQuestions = examQuestions; this.questions = questions;
        this.items = items; this.options = options; this.questionMedia = questionMedia; this.mediaActivities = mediaActivities; this.snapshots = snapshots;
        this.attempts = attempts; this.answers = answers; this.details = details;
        this.skillResults = skillResults; this.scoring = scoring; this.ai = ai; this.audioUploads = audioUploads; this.storage = storage;
        this.outbox = outbox; this.finalization = finalization; this.objectiveGrading = objectiveGrading; this.events = events;
    }

    @Override
    @Transactional
    public AttemptHandle start(Integer userId, Integer examId, String requestKey) {
        // Lock the user row for the whole transaction. A JVM lock is not enough here:
        // another request can enter after the first synchronized block exits but before
        // the first transaction has committed its attempt row.
        activeUserForUpdate(userId);
        MockExamEntity exam = publishedExam(examId);
        if ("CHECKPOINT".equals(exam.getExamType())
                && (requestKey == null || !requestKey.trim().startsWith("checkpoint:"))) {
            throw new BusinessRuleException("Checkpoint chỉ được bắt đầu từ lộ trình học");
        }
        List<MockExamQuestionEntity> links = examQuestions.findByMockExamIdOrderByPosition(examId);
        if (links.isEmpty()) throw new BusinessRuleException("Đề thi chưa có câu hỏi");
        String key = normalizeRequestKey(requestKey, userId, examId);
        Object lock = START_LOCKS.computeIfAbsent(userId + ":" + key, ignored -> new Object());
        synchronized (lock) {
            Optional<ExamAttemptEntity> byKey = attempts.findByUserIdAndRequestKey(userId, key);
            if (byKey.isPresent()) {
                if (!Objects.equals(byKey.get().getMockExamId(), examId)) {
                    throw new BusinessRuleException("Request key đã được dùng cho một đề thi khác");
                }
                return handle(byKey.get(), examFor(byKey.get()));
            }
            if ("PLACEMENT".equals(exam.getExamType())) {
                Optional<ExamAttemptEntity> active = attempts.findActivePlacement(userId, exam.getSkill(), ACTIVE_STATES);
                if (active.isPresent()) {
                    MockExamEntity activeExam = examFor(active.get());
                    if (!expireIfNeeded(active.get(), activeExam)) return handle(active.get(), activeExam);
                }
            }
            ExamAttemptEntity attempt = new ExamAttemptEntity(); attempt.setUserId(userId); attempt.setMockExamId(examId);
            attempt.setRequestKey(key); attempt.setStatus("IN_PROGRESS"); attempt.setGradingStatus("NOT_STARTED");
            attempt.setAnswerVersion(1); attempt.setScoringSchemeId(exam.getScoringSchemeId());
            attempt.setPolicyVersion("scoring-scheme:" + exam.getScoringSchemeId() + ":v1");
            attempt.setRubricVersion(RUBRIC_VERSION);
            attempt = attempts.save(attempt);
            createSnapshot(attempt, exam, links);
            Integer newAttemptId = attempt.getId();
            for (MockExamQuestionEntity link : links) {
                UserAnswerEntity answer = new UserAnswerEntity(); answer.setAttemptId(newAttemptId); answer.setQuestionId(link.getQuestionId());
                answer.setStatus("NOT_STARTED"); answers.save(answer);
                questionMedia.findByQuestionIdOrderByPosition(link.getQuestionId()).stream()
                        .filter(media -> "AUDIO".equals(media.getMediaType())).forEach(media -> {
                            UserMediaActivityEntity activity = new UserMediaActivityEntity();
                            activity.setAttemptId(newAttemptId); activity.setMediaId(media.getId()); mediaActivities.save(activity);
                        });
            }
            return handle(attempt, exam);
        }
    }

    @Transactional
    public void saveSpeakingAudio(Integer userId, Integer attemptId, Integer questionId, Integer itemId, MultipartFile audioFile) {
        ExamAttemptEntity attempt = editableAttempt(userId, attemptId);
        QuestionEntity question = questions.findById(questionId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        if (!"SPEAKING_AUDIO".equals(question.getQuestionType())) throw new BusinessRuleException("Câu hỏi không yêu cầu file ghi âm");
        UserAnswerEntity answer = answers.findByAttemptIdAndQuestionId(attemptId, questionId).orElseThrow(() -> new BusinessRuleException("Câu hỏi không thuộc lượt thi này"));
        JsonNode lockedQuestion = lockedQuestions(attemptId).get(questionId);
        JsonNode lockedItem = chooseLockedItem(lockedQuestion, itemId);
        QuestionItemEntity item = items.findById(lockedItem.path("id").asInt()).orElseThrow(() -> new ResourceNotFoundException("Mục câu hỏi không còn tồn tại"));
        String path = audioUploads.store(audioFile, attemptId);
        UserAnswerDetailEntity detail = details.findByUserAnswerIdAndQuestionItemId(answer.getId(), item.getId()).orElseGet(() -> {
            UserAnswerDetailEntity value = new UserAnswerDetailEntity(); value.setUserAnswerId(answer.getId()); value.setQuestionItemId(item.getId()); return value;
        });
        detail.setAudioPath(path); detail.setGradingStatus("PENDING");
        // Keep the legacy answer-level pointer for the single-item endpoint,
        // but never delete or overwrite a previous item's recording.
        if (answer.getAudioUrl() == null || answer.getAudioUrl().isBlank()) answer.setAudioUrl(path);
        answer.setStatus("ANSWERED");
        if (answer.getStartedAt() == null) answer.setStartedAt(LocalDateTime.now());
        answer.setAnswerVersion(answer.getAnswerVersion() + 1);
        details.save(detail); answers.save(answer);
    }

    @Transactional
    public MediaPlayResult playMedia(Integer userId, Integer attemptId, Integer mediaId) {
        ExamAttemptEntity attempt = attempts.findByIdAndUserIdForUpdate(attemptId, userId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền với lượt thi này"));
        if (!"IN_PROGRESS".equals(attempt.getStatus())) throw new BusinessRuleException("Lượt thi đã khóa");
        MockExamEntity exam = examFor(attempt);
        if (expireIfNeeded(attempt, exam)) throw new BusinessRuleException("Lượt thi đã hết thời gian");
        JsonNode lockedMedia = lockedMedia(attemptId, mediaId);
        if (lockedMedia == null || !"AUDIO".equals(lockedMedia.path("type").asText())
                || !answers.findByAttemptIdAndQuestionId(attemptId, lockedMedia.path("questionId").asInt()).isPresent()) throw new AccessDeniedException("Media không thuộc lượt thi");
        ensureMediaWindow(attempt);
        int limit = Math.min(2, lockedMedia.path("maxPlayCount").asInt(2));
        UserMediaActivityEntity activity = mediaActivities.findByAttemptIdAndMediaIdForUpdate(attemptId, mediaId)
                .orElseThrow(() -> new BusinessRuleException("Không thể khởi tạo bộ đếm lượt nghe"));
        if (activity.getPlayCount() >= limit) throw new BusinessRuleException("Bạn đã sử dụng hết số lượt nghe cho đoạn audio này");
        String contentToken = UUID.randomUUID().toString();
        activity.setPlayCount(activity.getPlayCount() + 1); activity.setLastPlayedAt(LocalDateTime.now());
        activity.setPlayTokenHash(hashToken(contentToken)); activity.setPlayTokenExpiresAt(LocalDateTime.now().plusMinutes(1));
        mediaActivities.save(activity);
        return new MediaPlayResult(mediaId, activity.getPlayCount(), limit, contentToken);
    }

    @Transactional
    public MediaResource mediaContent(Integer userId, Integer attemptId, Integer mediaId, String contentToken) {
        ExamAttemptEntity attempt = attempts.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền xem media này"));
        MockExamEntity exam = examFor(attempt);
        if (expireIfNeeded(attempt, exam)) throw new BusinessRuleException("Lượt thi đã hết thời gian");
        JsonNode lockedMedia = lockedMedia(attemptId, mediaId);
        if (lockedMedia == null || !answers.findByAttemptIdAndQuestionId(attemptId, lockedMedia.path("questionId").asInt()).isPresent()) throw new AccessDeniedException("Media không thuộc lượt thi");
        ensureMediaWindow(attempt);
        if ("AUDIO".equals(lockedMedia.path("type").asText())) {
            UserMediaActivityEntity activity = mediaActivities.findByAttemptIdAndMediaIdForUpdate(attemptId, mediaId)
                    .orElseThrow(() -> new AccessDeniedException("Media chưa được cấp lượt phát"));
            if (contentToken == null || contentToken.isBlank() || !Objects.equals(activity.getPlayTokenHash(), hashToken(contentToken))
                    || activity.getPlayTokenExpiresAt() == null || activity.getPlayTokenExpiresAt().isBefore(LocalDateTime.now())) {
                throw new AccessDeniedException("Token phát audio không hợp lệ hoặc đã hết hạn");
            }
            // Keep the short-lived token valid for the browser's metadata and
            // range requests. A browser can request the same audio more than
            // once while building the native player, so invalidating it after
            // the first request leaves the player stuck at 0:00 / 0:00.
        }
        return new MediaResource(storage.load(lockedMedia.path("mediaUrl").asText()), lockedMedia.path("contentType").asText("application/octet-stream"));
    }

    @Transactional(readOnly = true)
    public MediaResource speakingAudio(Integer userId, Integer attemptId, Integer answerId) {
        attempts.findByIdAndUserId(attemptId, userId).orElseThrow(() -> new AccessDeniedException("Bạn không có quyền xem audio này"));
        UserAnswerEntity answer = answers.findByIdAndAttemptId(answerId, attemptId).orElseThrow(() -> new AccessDeniedException("Audio không thuộc lượt thi"));
        if (answer.getAudioUrl() == null) throw new ResourceNotFoundException("Chưa có file ghi âm");
        return new MediaResource(storage.load(answer.getAudioUrl()), "audio/*");
    }

    @Transactional
    public void saveAnswer(Integer userId, Integer attemptId, Integer questionId, Integer itemId,
            Integer selectedOptionId, Integer userPosition, String textAnswer) {
        ExamAttemptEntity attempt = editableAttempt(userId, attemptId);
        UserAnswerEntity answer = answers.findByAttemptIdAndQuestionId(attemptId, questionId)
                .orElseThrow(() -> new BusinessRuleException("Câu hỏi không thuộc lượt thi này"));
        JsonNode lockedQuestion = lockedQuestions(attemptId).get(questionId);
        if (lockedQuestion == null) throw new BusinessRuleException("Câu hỏi không thuộc snapshot của lượt thi");
        JsonNode lockedItem = chooseLockedItem(lockedQuestion, itemId);
        QuestionItemEntity item = items.findById(lockedItem.path("id").asInt()).orElseThrow(() -> new ResourceNotFoundException("Mục câu hỏi không còn tồn tại"));
        String questionType = lockedQuestion.path("type").asText();
        UserAnswerDetailEntity detail = details.findByUserAnswerIdAndQuestionItemId(answer.getId(), item.getId()).orElseGet(() -> {
            UserAnswerDetailEntity value = new UserAnswerDetailEntity(); value.setUserAnswerId(answer.getId()); value.setQuestionItemId(item.getId()); return value;
        });
        if ("ORDERING".equals(questionType)) {
            if (userPosition != null && (userPosition < 1 || userPosition > lockedQuestion.path("items").size())) throw new BusinessRuleException("Vị trí sắp xếp không hợp lệ");
            detail.setUserPosition(userPosition);
        } else if (usesOptions(questionType)) {
            if (selectedOptionId != null && !hasLockedOption(lockedItem, selectedOptionId)) {
                throw new BusinessRuleException("Phương án không thuộc câu hỏi");
            }
            detail.setSelectedOptionId(selectedOptionId);
        } else if ("WRITING_TEXT".equals(questionType)) {
            String value = textAnswer == null ? "" : textAnswer.trim();
            if (value.length() > GradingTextLimits.MAX_ITEM_CHARS) {
                throw new BusinessRuleException("Bài viết ở mục " + item.getPosition() + " vượt quá " + GradingTextLimits.MAX_ITEM_CHARS + " ký tự");
            }
            // The detail row is the source of truth: a question may contain
            // several writing items, so an answer-level field must not replace
            // the text of another item.
            detail.setAnswerText(value);
        } else if ("SPEAKING_AUDIO".equals(questionType)) {
            throw new BusinessRuleException("Speaking phải tải file bằng multipart upload");
        }
        if (answer.getStartedAt() == null) answer.setStartedAt(LocalDateTime.now());
        answer.setAnswerVersion(answer.getAnswerVersion() + 1);
        answer.setStatus("ANSWERED"); details.save(detail); answers.save(answer);
    }

    @Override
    @Transactional
    public void submit(Integer userId, Integer attemptId, String requestKey) {
        ExamAttemptEntity attempt = attempts.findByIdAndUserIdForUpdate(attemptId, userId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền với lượt thi này"));
        if ("COMPLETED".equals(attempt.getStatus()) || attempt.getSubmittedAt() != null) return;
        // A deadline check may have marked an unsubmitted attempt EXPIRED. It
        // still needs one final server-side submit so objective/AI grading can
        // finish and the learner can read the result.
        if (!"IN_PROGRESS".equals(attempt.getStatus()) && !"WAITING_GRADING".equals(attempt.getStatus())
                && !("EXPIRED".equals(attempt.getStatus()) && attempt.getSubmittedAt() == null)) {
            throw new BusinessRuleException("Lượt thi không thể nộp ở trạng thái hiện tại");
        }
        MockExamEntity exam = publishedExam(attempt.getMockExamId());
        LocalDateTime now = LocalDateTime.now();
        boolean expired = exam.getDurationMinutes() != null && attempt.getStartedAt().plusMinutes(exam.getDurationMinutes()).isBefore(now);
        refreshSnapshotAnswerVersion(attempt);
        attempt.setScoringSchemeId(attempt.getScoringSchemeId() == null ? exam.getScoringSchemeId() : attempt.getScoringSchemeId());
        attempt.setPolicyVersion(attempt.getPolicyVersion() == null ? "scoring-scheme:" + exam.getScoringSchemeId() + ":v1" : attempt.getPolicyVersion());
        attempt.setRubricVersion(attempt.getRubricVersion() == null ? RUBRIC_VERSION : attempt.getRubricVersion());
        attempt.setGradingRequestKey("attempt:" + attemptId + ":grading:v" + attempt.getAnswerVersion());
        LockedAttempt draftLocked = lockedAttempt(attempt, exam);
        validateAiPayload(draftLocked);
        attempt.setSubmittedAt(now); attempt.setGradingStatus("GRADING"); attempt.setStatus(expired ? "EXPIRED" : "SUBMITTED");
        LockedAttempt locked = lockedAttempt(attempt, exam);
        objectiveGrading.gradeObjective(locked, exam);
        boolean waiting = skillResults.findByAttemptIdOrderBySkillAsc(attempt.getId()).stream().anyMatch(x -> x.getScaleScore() == null);
        if (waiting) { attempt.setStatus(expired ? "EXPIRED" : "WAITING_GRADING"); attempt.setGradingStatus("WAITING"); }
        boolean completed = !waiting && finalization.finalizeIfReady(attempt, exam);
        attempts.save(attempt);
        if (completed) {
            finalization.publishCompleted(attempt, exam);
        } else if ("WAITING_GRADING".equals(attempt.getStatus()) || ("EXPIRED".equals(attempt.getStatus()) && "WAITING".equals(attempt.getGradingStatus()))) {
            AttemptSubmittedEvent event = new AttemptSubmittedEvent(userId, attemptId, exam.getId(), exam.getExamType(), locked);
            outbox.enqueueSubmitted(event); events.publishEvent(event);
        }
    }

    @Override
    @Transactional
    public AttemptResult result(Integer userId, Integer attemptId) {
        activeUser(userId);
        ExamAttemptEntity attempt = attempts.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền xem kết quả này"));
        MockExamEntity exam = examFor(attempt);
        expireIfNeeded(attempt, exam);
        return resultView(attempt, exam);
    }

    @Override
    @Transactional(readOnly = true)
    public HistoryPage history(Integer userId, int page, int size, HistoryFilter filter) {
        // History is also rendered by the admin's student-detail view, including
        // locked/inactive students. Student-facing endpoints validate the active
        // principal before calling this contract.
        int safePage = Math.max(0, page); int safeSize = Math.min(100, Math.max(1, size));
        HistoryFilter safeFilter = filter == null ? HistoryFilter.empty() : filter;
        String pattern = safeFilter.query() == null || safeFilter.query().isBlank() ? "" : "%" + safeFilter.query().trim().toLowerCase(Locale.ROOT) + "%";
        String skill = normalizedFilter(safeFilter.skill());
        String examType = normalizedFilter(safeFilter.examType());
        String state = normalizedFilter(safeFilter.state());
        var result = attempts.searchHistory(userId, pattern, skill, examType, state, PageRequest.of(safePage, safeSize));
        List<AttemptResult> values = result.stream().map(a -> resultView(a, examFor(a))).toList();
        return new HistoryPage(values, safePage, safeSize, result.getTotalElements());
    }

    @Override @Transactional(readOnly = true)
    public AdminHistoryPage adminHistory(String adminEmail, int page, int size, AdminHistoryFilter filter) {
        requireAdmin(adminEmail); int safePage=Math.max(0,page), safeSize=Math.min(100,Math.max(1,size));
        AdminHistoryFilter f=filter==null?new AdminHistoryFilter("","","","",null,null):filter;
        String pattern=f.query()==null||f.query().isBlank()?"":"%"+f.query().trim().toLowerCase(Locale.ROOT)+"%";
        LocalDateTime from=f.submittedFrom()==null?null:LocalDateTime.ofInstant(f.submittedFrom(),ZoneOffset.UTC);
        LocalDateTime to=f.submittedTo()==null?null:LocalDateTime.ofInstant(f.submittedTo(),ZoneOffset.UTC);
        var pageData=attempts.searchAllHistory(pattern,normalizedFilter(f.skill()),normalizedFilter(f.examType()),normalizedFilter(f.state()),from,to,PageRequest.of(safePage,safeSize));
        var values=pageData.stream().map(a->{var user=users.findById(a.getUserId()).orElseThrow();return new AdminHistoryItem(user.getFullName(),user.getEmail(),resultView(a,examFor(a)));}).toList();
        return new AdminHistoryPage(values,safePage,safeSize,pageData.getTotalElements());
    }

    @Override @Transactional(readOnly = true)
    public AttemptResult adminResult(String adminEmail,Integer attemptId){requireAdmin(adminEmail);var attempt=attempts.findById(attemptId).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy kết quả"));return resultView(attempt,examFor(attempt));}

    @Override
    public void retryFailedGrading(Integer userId, Integer attemptId, String requestKey) {
        finalization.retryFailedGrading(userId, attemptId);
    }

    @Transactional(readOnly = true)
    public boolean ownsAttempt(Integer userId, Integer attemptId) { return attempts.findByIdAndUserId(attemptId, userId).isPresent(); }

    @Transactional
    public void applyAiAssessment(AiAssessmentCompletedEvent event) {
        finalization.applyAiAssessment(event);
    }

    @Transactional
    public AttemptPage page(Integer userId, Integer attemptId) {
        activeUser(userId); ExamAttemptEntity attempt = attempts.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền với lượt thi này"));
        MockExamEntity exam = examFor(attempt);
        expireIfNeeded(attempt, exam);
        List<QuestionPage> questionsView = lockedQuestionList(attemptId).stream().map(q -> {
            Integer questionId = q.path("id").asInt();
            UserAnswerEntity answer = answers.findByAttemptIdAndQuestionId(attemptId, questionId).orElseThrow();
            Map<Integer, UserAnswerDetailEntity> detailByItem = new HashMap<>();
            details.findByUserAnswerIdOrderByQuestionItemIdAsc(answer.getId()).forEach(detail -> detailByItem.put(detail.getQuestionItemId(), detail));
            List<ItemPage> itemPages = new ArrayList<>();
            for (JsonNode item : q.path("items")) {
                Integer itemId = item.path("id").asInt();
                UserAnswerDetailEntity detail = detailByItem.get(itemId);
                List<OptionPage> itemOptions = new ArrayList<>();
                for (JsonNode option : item.path("options")) itemOptions.add(new OptionPage(option.path("id").asInt(), option.path("code").asText(), option.path("content").asText()));
                itemPages.add(new ItemPage(itemId, item.path("position").asInt(), item.path("content").asText(), itemOptions,
                        detail == null ? null : detail.getSelectedOptionId(), detail == null ? null : detail.getUserPosition(),
                        detail == null ? null : detail.getAnswerText(), detail != null && detail.getAudioPath() != null));
            }
            List<MediaPage> mediaPages = new ArrayList<>();
            for (JsonNode media : q.path("media")) mediaPages.add(new MediaPage(media.path("id").asInt(), media.path("type").asText(), media.path("filename").asText(), media.path("contentType").asText()));
            return new QuestionPage(questionId, q.path("content").asText(), q.path("instruction").asText(), q.path("readingPassage").asText(null), q.path("skill").asText(), q.path("type").asText(), q.path("position").asInt(), itemPages, mediaPages);
        }).toList();
        Instant deadline = exam.getDurationMinutes() == null || attempt.getStartedAt() == null ? null
                : instant(attempt.getStartedAt().plusMinutes(exam.getDurationMinutes()));
        return new AttemptPage(attempt.getId(), exam.getTitle(), exam.getExamType(), remainingSeconds(attempt, exam), questionsView, deadline);
    }

    public record AttemptPage(Integer attemptId, String title, String examType, Long remainingSeconds, List<QuestionPage> questions, Instant deadline) {
        public AttemptPage(Integer attemptId, String title, Long remainingSeconds, List<QuestionPage> questions, Instant deadline) {
            this(attemptId, title, null, remainingSeconds, questions, deadline);
        }
        public AttemptPage(Integer attemptId, String title, Long remainingSeconds, List<QuestionPage> questions) {
            this(attemptId, title, null, remainingSeconds, questions, null);
        }
        public int totalItems() {
            return questions == null ? 0 : questions.stream().mapToInt(question -> question.items() == null ? 0 : question.items().size()).sum();
        }
        public int answeredItems() {
            return questions == null ? 0 : questions.stream().flatMap(question -> question.items() == null ? java.util.stream.Stream.empty() : question.items().stream()).mapToInt(item -> item.selectedOptionId() != null || item.userPosition() != null || (item.textAnswer() != null && !item.textAnswer().isBlank()) || item.audioSaved() ? 1 : 0).sum();
        }
    }
    public record QuestionPage(Integer questionId, String content, String instruction, String readingPassage, String skill, String type, int position,
            List<ItemPage> items, List<MediaPage> media) {}
    public record ItemPage(Integer itemId, int position, String content, List<OptionPage> options,
            Integer selectedOptionId, Integer userPosition, String textAnswer, boolean audioSaved) {}
    public record OptionPage(Integer id, String code, String content) {}
    public record MediaPage(Integer id, String type, String filename, String contentType) {}
    public record MediaPlayResult(Integer mediaId, int playCount, int maxPlayCount, String contentToken) {}
    public record MediaResource(Resource resource, String contentType) {}

    private void createSnapshot(ExamAttemptEntity attempt, MockExamEntity exam, List<MockExamQuestionEntity> links) {
        ExamAttemptSnapshotEntity snapshot = new ExamAttemptSnapshotEntity();
        snapshot.setAttemptId(attempt.getId()); snapshot.setExamId(exam.getId()); snapshot.setCertificateId(exam.getCertificateId());
        snapshot.setScoringSchemeId(exam.getScoringSchemeId()); snapshot.setExamType(exam.getExamType()); snapshot.setSkill(exam.getSkill());
        snapshot.setTargetLevel(exam.getTargetLevel()); snapshot.setTitle(exam.getTitle()); snapshot.setDurationMinutes(exam.getDurationMinutes());
        snapshot.setAnswerVersion(1); snapshot.setPolicyVersion("scoring-scheme:" + exam.getScoringSchemeId() + ":v1");
        Map<String, Object> grading = new LinkedHashMap<>(); grading.put("certificateId", exam.getCertificateId()); grading.put("scoringSchemeId", exam.getScoringSchemeId()); grading.put("policyVersion", snapshot.getPolicyVersion()); grading.put("answerVersion", 1);
        List<Map<String, Object>> lockedQuestions = new ArrayList<>();
        for (MockExamQuestionEntity link : links) {
            QuestionEntity question = questions.findById(link.getQuestionId()).orElseThrow(); Map<String, Object> q = new LinkedHashMap<>();
            q.put("id", question.getId()); q.put("position", link.getPosition()); q.put("content", question.getContent()); q.put("instruction", question.getInstruction()); q.put("readingPassage", question.getReadingPassage()); q.put("type", question.getQuestionType()); q.put("skill", question.getSkill());
            List<Map<String, Object>> lockedItems = new ArrayList<>();
            for (QuestionItemEntity item : items.findByQuestionIdOrderByPosition(question.getId())) {
                Map<String, Object> i = new LinkedHashMap<>(); i.put("id", item.getId()); i.put("position", item.getPosition()); i.put("content", item.getContent()); i.put("correctPosition", item.getCorrectPosition());
                i.put("minWordCount", item.getMinWordCount()); i.put("maxWordCount", item.getMaxWordCount()); i.put("responseSeconds", item.getResponseSeconds());
                List<Map<String, Object>> lockedOptions = new ArrayList<>(); options.findByQuestionItemIdOrderByPosition(item.getId()).forEach(option -> { Map<String, Object> o = new LinkedHashMap<>(); o.put("id", option.getId()); o.put("code", option.getOptionCode()); o.put("position", option.getPosition()); o.put("content", option.getContent()); o.put("correct", option.getCorrect()); lockedOptions.add(o); });
                i.put("options", lockedOptions); lockedItems.add(i);
            }
            List<Map<String, Object>> lockedMedia = new ArrayList<>();
            for (QuestionMediaEntity media : questionMedia.findByQuestionIdOrderByPosition(question.getId())) {
                Map<String, Object> m = new LinkedHashMap<>(); m.put("id", media.getId()); m.put("questionId", question.getId());
                m.put("type", media.getMediaType()); m.put("mediaUrl", media.getMediaUrl()); m.put("filename", media.getOriginalFilename());
                m.put("contentType", media.getContentType()); m.put("maxPlayCount", media.getMaxPlayCount() == null ? 2 : media.getMaxPlayCount());
                lockedMedia.add(m);
            }
            q.put("items", lockedItems); q.put("media", lockedMedia); lockedQuestions.add(q);
        }
        try { snapshot.setGradingContext(json.writeValueAsString(grading)); snapshot.setLockedQuestionSnapshot(json.writeValueAsString(lockedQuestions)); }
        catch (RuntimeException exception) { throw new BusinessRuleException("Không thể khóa nội dung đề thi cho lượt làm bài"); }
        snapshots.save(snapshot);
    }

    private Map<Integer, JsonNode> lockedQuestions(Integer attemptId) {
        ExamAttemptSnapshotEntity snapshot = snapshots.findByAttemptId(attemptId)
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy snapshot của lượt thi"));
        try {
            Map<Integer, JsonNode> result = new LinkedHashMap<>();
            for (JsonNode question : json.readTree(snapshot.getLockedQuestionSnapshot())) result.put(question.path("id").asInt(), question);
            return result;
        } catch (RuntimeException exception) {
            throw new BusinessRuleException("Snapshot lượt thi không hợp lệ");
        }
    }

    private List<JsonNode> lockedQuestionList(Integer attemptId) {
        return lockedQuestions(attemptId).values().stream()
                .sorted(Comparator.comparingInt(value -> value.path("position").asInt()))
                .toList();
    }

    private JsonNode lockedMedia(Integer attemptId, Integer mediaId) {
        for (JsonNode question : lockedQuestionList(attemptId)) {
            for (JsonNode media : question.path("media")) if (media.path("id").asInt() == mediaId) return media;
        }
        return null;
    }

    private JsonNode chooseLockedItem(JsonNode question, Integer itemId) {
        if (question == null || question.isMissingNode()) throw new BusinessRuleException("Câu hỏi không thuộc snapshot của lượt thi");
        for (JsonNode item : question.path("items")) if (itemId == null || item.path("id").asInt() == itemId) return item;
        throw new ResourceNotFoundException("Mục câu hỏi không thuộc câu hỏi");
    }

    private boolean hasLockedOption(JsonNode item, Integer optionId) {
        for (JsonNode option : item.path("options")) if (option.path("id").asInt() == optionId) return true;
        return false;
    }

    private void refreshSnapshotAnswerVersion(ExamAttemptEntity attempt) {
        Integer attemptId = attempt.getId();
        int version = answers.findByAttemptIdOrderByQuestionIdAsc(attemptId).stream()
                    .mapToInt(UserAnswerEntity::getAnswerVersion).max().orElse(1);
        attempt.setAnswerVersion(version);
        snapshots.findByAttemptId(attemptId).ifPresent(snapshot -> {
            snapshot.setAnswerVersion(version);
            snapshots.save(snapshot);
        });
    }

    private AttemptResult resultView(ExamAttemptEntity attempt, MockExamEntity exam) {
        List<SkillResult> skills = skillResults.findByAttemptIdOrderBySkillAsc(attempt.getId()).stream().map(value -> {
            String confirmedLevel = com.aptisprep.service.scoring.CheckpointScorePolicy.confirmedLevel(
                    exam.getExamType(), exam.getTargetLevel(), value.getCefrLevel());
            String state = value.getGradingStatus();
            if (state == null) state = value.getScaleScore() == null ? "PENDING" : "COMPLETED";
            else if ("WAITING".equals(state)) state = "PENDING";
            else if ("ERROR".equals(state)) state = "ERROR";
            boolean passed = "COMPLETED".equals(state) && com.aptisprep.service.scoring.CheckpointScorePolicy.passed(
                    exam.getExamType(), exam.getTargetLevel(), confirmedLevel, value.getScaleScore());
            return new SkillResult(value.getSkill(), value.getRawScore(), value.getScaleScore(), confirmedLevel, state,
                    feedbackFor(attempt.getId(), value.getSkill()), passed,
                    "ERROR".equals(state) && !"POLICY_ERROR".equals(value.getGradingMethod()));
        }).toList();
        Integer schemeId = attempt.getScoringSchemeId() == null ? exam.getScoringSchemeId() : attempt.getScoringSchemeId();
        return new AttemptResult(attempt.getId(), attempt.getUserId(), exam.getId(), exam.getExamType(), exam.getTitle(), schemeId,
                attempt.getStatus(), skills, attempt.getFinalScaleScore(), attempt.getEstimatedOverallCefr(),
                instant(attempt.getSubmittedAt()));
    }

    private String feedbackFor(Integer attemptId, String skill) {
        StringJoiner feedback = new StringJoiner("\n\n");
        for (UserAnswerEntity answer : answers.findByAttemptIdOrderByQuestionIdAsc(attemptId)) {
            if (answer.getAiFeedback() == null || answer.getAiFeedback().isBlank()) continue;
            QuestionEntity question = questions.findById(answer.getQuestionId()).orElse(null);
            if (question != null && Objects.equals(question.getSkill(), skill)) feedback.add(answer.getAiFeedback().trim());
        }
        return feedback.length() == 0 ? null : feedback.toString();
    }

    private LockedAttempt lockedAttempt(ExamAttemptEntity attempt, MockExamEntity exam) {
        ExamAttemptSnapshotEntity snapshot = snapshots.findByAttemptId(attempt.getId())
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy snapshot của lượt thi"));
        Map<Integer, JsonNode> lockedByQuestion = lockedQuestions(attempt.getId());
        List<LockedAnswer> lockedAnswers = answers.findByAttemptIdOrderByQuestionIdAsc(attempt.getId()).stream().map(answer -> {
            JsonNode question = lockedByQuestion.get(answer.getQuestionId());
            if (question == null) throw new BusinessRuleException("Snapshot không chứa câu hỏi của lượt thi");
            String instruction = question.path("instruction").asText("");
            String content = question.path("content").asText("");
            String task = instruction.isBlank() ? content : instruction + "\n" + content;
            List<LockedItem> lockedItems = new ArrayList<>();
            for (JsonNode item : question.path("items")) {
                UserAnswerDetailEntity detail = details.findByUserAnswerIdAndQuestionItemId(answer.getId(), item.path("id").asInt()).orElse(null);
                String itemContent = detail == null ? null : firstNonBlank(detail.getAnswerText(), detail.getAudioPath(), detail.getTranscript());
                Integer correctOption = null;
                for (JsonNode option : item.path("options")) {
                    if (option.path("correct").asBoolean(false)) {
                        correctOption = option.path("id").asInt();
                        break;
                    }
                }
                lockedItems.add(new LockedItem(item.path("id").asInt(), item.path("position").asInt(), itemPrompt(item), itemContent,
                        correctOption, item.path("correctPosition").isNull() ? null : item.path("correctPosition").asInt()));
            }
            String lockedContent = lockedItems.stream().map(LockedItem::lockedContent).filter(Objects::nonNull)
                    .filter(value -> !value.isBlank()).collect(java.util.stream.Collectors.joining("\n--- ITEM ---\n"));
            if (lockedContent.isBlank()) lockedContent = firstNonBlank(answer.getTextAnswer(), answer.getAudioUrl(), answer.getTranscript());
            return new LockedAnswer(answer.getId(), answer.getQuestionId(), question.path("skill").asText(), question.path("type").asText(),
                    task, lockedContent, answer.getTranscript(), lockedItems);
        }).toList();
        return new LockedAttempt(attempt.getId(), attempt.getUserId(), exam.getId(), exam.getExamType(),
                attempt.getAnswerVersion() == null ? 1 : attempt.getAnswerVersion(), instant(attempt.getSubmittedAt()),
                attempt.getScoringSchemeId() == null ? exam.getScoringSchemeId() : attempt.getScoringSchemeId(),
                attempt.getPolicyVersion(), attempt.getRubricVersion() == null ? RUBRIC_VERSION : attempt.getRubricVersion(),
                snapshot.getGradingContext(), attempt.getGradingRequestKey(), lockedAnswers);
    }

    private void validateAiPayload(LockedAttempt locked) {
        for (LockedAnswer answer : locked.answers()) {
            if (!Set.of("WRITING_TEXT", "SPEAKING_AUDIO").contains(answer.questionType())) continue;
            String task = GradingContextFormatter.task(answer);
            String submission = GradingContextFormatter.submission(answer);
            for (LockedItem item : answer.items()) {
                if (GradingTextLimits.length(item.lockedContent()) > GradingTextLimits.MAX_ITEM_CHARS) {
                    throw new BusinessRuleException("Bài làm ở mục " + item.position() + " vượt quá " + GradingTextLimits.MAX_ITEM_CHARS + " ký tự");
                }
            }
            if (GradingTextLimits.length(task) + GradingTextLimits.length(submission) > GradingTextLimits.MAX_QUESTION_CHARS) {
                throw new BusinessRuleException("Nội dung câu " + answer.questionId() + " vượt quá " + GradingTextLimits.MAX_QUESTION_CHARS + " ký tự cho phép");
            }
            if (GradingTextLimits.length(task) > GradingTextLimits.MAX_AI_TASK_CHARS
                    || GradingTextLimits.length(submission) > GradingTextLimits.MAX_AI_SUBMISSION_CHARS
                    || GradingTextLimits.length(task) + GradingTextLimits.length(submission) > GradingTextLimits.MAX_AI_PAYLOAD_CHARS) {
                throw new BusinessRuleException("Nội dung câu " + answer.questionId() + " vượt giới hạn ngữ cảnh chấm AI");
            }
        }
    }

    private String itemPrompt(JsonNode item) {
        StringBuilder prompt = new StringBuilder(item.path("content").asText(""));
        if (item.path("minWordCount").isNumber() || item.path("maxWordCount").isNumber()) {
            prompt.append("\nGiới hạn từ: ");
            if (item.path("minWordCount").isNumber()) prompt.append("tối thiểu ").append(item.path("minWordCount").asInt()).append(" ");
            if (item.path("maxWordCount").isNumber()) prompt.append("tối đa ").append(item.path("maxWordCount").asInt()).append(" ");
            prompt.append("từ.");
        }
        if (item.path("responseSeconds").isNumber()) prompt.append("\nThời gian trả lời: ").append(item.path("responseSeconds").asInt()).append(" giây.");
        return prompt.toString();
    }

    private ExamAttemptEntity editableAttempt(Integer userId, Integer attemptId) {
        ExamAttemptEntity attempt = attempts.findByIdAndUserIdForUpdate(attemptId, userId).orElseThrow(() -> new AccessDeniedException("Bạn không có quyền với lượt thi này"));
        if (!"IN_PROGRESS".equals(attempt.getStatus())) throw new BusinessRuleException("Lượt thi đã khóa");
        MockExamEntity exam = examFor(attempt);
        if (expireIfNeeded(attempt, exam)) {
            throw new BusinessRuleException("Lượt thi đã hết thời gian");
        }
        return attempt;
    }
    private void ensureMediaWindow(ExamAttemptEntity attempt) {
        if (!"IN_PROGRESS".equals(attempt.getStatus())) throw new BusinessRuleException("Lượt thi đã khóa");
        MockExamEntity exam = examFor(attempt);
        if (expireIfNeeded(attempt, exam)) {
            throw new BusinessRuleException("Lượt thi đã hết thời gian");
        }
    }
    private boolean expireIfNeeded(ExamAttemptEntity attempt, MockExamEntity exam) {
        if (!"IN_PROGRESS".equals(attempt.getStatus()) || exam.getDurationMinutes() == null || attempt.getStartedAt() == null) return false;
        if (!attempt.getStartedAt().plusMinutes(exam.getDurationMinutes()).isBefore(LocalDateTime.now())) return false;
        attempt.setStatus("EXPIRED"); attempt.setGradingStatus("EXPIRED"); attempts.save(attempt); return true;
    }
    private QuestionItemEntity chooseItem(Integer questionId, Integer itemId) { return items.findByQuestionIdOrderByPosition(questionId).stream().filter(x -> itemId == null || Objects.equals(x.getId(), itemId)).findFirst().orElseThrow(() -> new ResourceNotFoundException("Câu hỏi chưa có mục nào")); }
    private MockExamEntity publishedExam(Integer id) { return exams.findById(id).filter(x -> "PUBLISHED".equals(x.getStatus())).orElseThrow(() -> new ResourceNotFoundException("Đề thi chưa được công bố")); }
    private MockExamEntity examFor(ExamAttemptEntity attempt) { return exams.findById(attempt.getMockExamId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi")); }
    private User activeUser(Integer id) { User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản")); if (user.getRole() != Role.STUDENT || user.getStatus() != UserStatus.ACTIVE) throw new AccessDeniedException("Tài khoản không còn hoạt động"); return user; }
    private User activeUserForUpdate(Integer id) { User user = users.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản")); if (user.getRole() != Role.STUDENT || user.getStatus() != UserStatus.ACTIVE) throw new AccessDeniedException("Tài khoản không còn hoạt động"); return user; }
    private User requireAdmin(String email) {
        User user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new AccessDeniedException("Không có quyền quản trị"));
        if (user.getRole() != Role.ADMIN || user.getStatus() != UserStatus.ACTIVE) throw new AccessDeniedException("Không có quyền quản trị");
        return user;
    }
    private AttemptHandle handle(ExamAttemptEntity attempt, MockExamEntity exam) { return new AttemptHandle(attempt.getId(), attempt.getStatus(), instant(attempt.getStartedAt().plusMinutes(exam.getDurationMinutes() == null ? 0 : exam.getDurationMinutes()))); }
    private Long remainingSeconds(ExamAttemptEntity attempt, MockExamEntity exam) {
        if (exam.getDurationMinutes() == null || attempt.getStartedAt() == null) return null;
        return Math.max(0L, Duration.between(LocalDateTime.now(), attempt.getStartedAt().plusMinutes(exam.getDurationMinutes())).getSeconds());
    }
    private Instant instant(LocalDateTime value) { return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant(); }
    private String normalizeRequestKey(String value, Integer userId, Integer examId) { return value == null || value.isBlank() ? "attempt:" + userId + ":" + examId : value.trim(); }
    private boolean usesOptions(String type) { return "SINGLE_CHOICE".equals(type) || "MATCHING".equals(type) || "ORDERING".equals(type); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String firstNonBlank(String... values) { for (String value : values) if (value != null && !value.isBlank()) return value; return null; }
    private String normalizedFilter(String value) { return value == null || value.isBlank() ? "" : value.trim().toUpperCase(Locale.ROOT); }
    private int levelOrder(String value) { return switch (value == null ? "" : value) { case "A1" -> 1; case "A2" -> 2; case "B1" -> 3; case "B2" -> 4; case "C1" -> 5; case "C2" -> 6; default -> 0; }; }
    private String hashToken(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 unavailable", exception); }
    }
}
