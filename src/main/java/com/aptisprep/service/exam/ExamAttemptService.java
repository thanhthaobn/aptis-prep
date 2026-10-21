package com.aptisprep.service.exam;

import com.aptisprep.entity.*;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.event.AiAssessmentCompletedEvent;
import com.aptisprep.event.AttemptCompletedEvent;
import com.aptisprep.event.AttemptSubmittedEvent;
import com.aptisprep.exception.AccessDeniedException;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.*;
import com.aptisprep.service.contract.ExamAttemptContract;
import com.aptisprep.service.contract.AiAssessmentContract;
import com.aptisprep.service.contract.IntegrationModels.*;
import com.aptisprep.service.contract.ScoringPolicyContract;
import com.aptisprep.service.grading.AttemptFinalizationService;
import com.aptisprep.service.storage.AudioUploadService;
import com.aptisprep.service.storage.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamAttemptService implements ExamAttemptContract {
    private static final String RUBRIC_VERSION = "aptis-rubric-v1";
    private static final int SAVE_GRACE_SECONDS = 5;
    private static final Set<String> ACTIVE_STATES = Set.of("IN_PROGRESS", "SUBMITTED", "WAITING_GRADING");
    private static final Set<String> TERMINAL_STATES = Set.of("COMPLETED", "ERROR", "EXPIRED");
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
    private final ApplicationEventPublisher events;
    private final JsonMapper json = JsonMapper.builder().build();

    public ExamAttemptService(UserRepository users, MockExamRepository exams, MockExamQuestionRepository examQuestions,
            QuestionRepository questions, QuestionItemRepository items, QuestionOptionRepository options,
            QuestionMediaRepository questionMedia, UserMediaActivityRepository mediaActivities, ExamAttemptSnapshotRepository snapshots,
            ExamAttemptRepository attempts, UserAnswerRepository answers, UserAnswerDetailRepository details,
            ExamSkillResultRepository skillResults, ScoringPolicyContract scoring, AiAssessmentContract ai,
            AudioUploadService audioUploads, StorageService storage, AttemptOutboxService outbox,
            AttemptFinalizationService finalization, ApplicationEventPublisher events) {
        this.users = users; this.exams = exams; this.examQuestions = examQuestions; this.questions = questions;
        this.items = items; this.options = options; this.questionMedia = questionMedia; this.mediaActivities = mediaActivities; this.snapshots = snapshots;
        this.attempts = attempts; this.answers = answers; this.details = details;
        this.skillResults = skillResults; this.scoring = scoring; this.ai = ai; this.audioUploads = audioUploads; this.storage = storage;
        this.outbox = outbox; this.finalization = finalization; this.events = events;
    }

    @Override
    @Transactional
    public AttemptHandle start(Integer userId, Integer examId, String requestKey) {
        // Lock the user row for the whole transaction. A JVM lock is not enough here:
        // another request can enter after the first synchronized block exits but before
        // the first transaction has committed its attempt row.
        activeUserForUpdate(userId);
        MockExamEntity exam = publishedExam(examId);
        List<MockExamQuestionEntity> links = examQuestions.findByMockExamIdOrderByPosition(examId);
        if (links.isEmpty()) throw new BusinessRuleException("Đề thi chưa có câu hỏi");
        String key = normalizeRequestKey(requestKey, userId, examId);
        Object lock = START_LOCKS.computeIfAbsent(userId + ":" + key, ignored -> new Object());
        synchronized (lock) {
            Optional<ExamAttemptEntity> byKey = attempts.findByUserIdAndRequestKey(userId, key);
            if (byKey.isPresent() && !TERMINAL_STATES.contains(byKey.get().getStatus())) return handle(byKey.get(), exam);
            if ("PLACEMENT".equals(exam.getExamType())) {
                Optional<ExamAttemptEntity> active = attempts.findActivePlacement(userId, exam.getSkill(), ACTIVE_STATES);
                if (active.isPresent()) return handle(active.get(), examFor(active.get()));
            }
            if (byKey.isPresent()) key = key + "-" + UUID.randomUUID();
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
        QuestionItemEntity item = chooseItem(questionId, itemId);
        String oldPath = answer.getAudioUrl();
        String path = audioUploads.store(audioFile, attemptId);
        UserAnswerDetailEntity detail = details.findByUserAnswerIdAndQuestionItemId(answer.getId(), item.getId()).orElseGet(() -> {
            UserAnswerDetailEntity value = new UserAnswerDetailEntity(); value.setUserAnswerId(answer.getId()); value.setQuestionItemId(item.getId()); return value;
        });
        detail.setAudioPath(path); detail.setGradingStatus("PENDING"); answer.setAudioUrl(path); answer.setStatus("ANSWERED");
        if (answer.getStartedAt() == null) answer.setStartedAt(LocalDateTime.now());
        answer.setAnswerVersion(answer.getAnswerVersion() + 1);
        details.save(detail); answers.save(answer); if (oldPath != null && !oldPath.equals(path)) storage.delete(oldPath);
    }

    @Transactional
    public MediaPlayResult playMedia(Integer userId, Integer attemptId, Integer mediaId) {
        ExamAttemptEntity attempt = attempts.findByIdAndUserIdForUpdate(attemptId, userId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền với lượt thi này"));
        if (!"IN_PROGRESS".equals(attempt.getStatus())) throw new BusinessRuleException("Lượt thi đã khóa");
        QuestionMediaEntity media = questionMedia.findById(mediaId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy media"));
        if (!"AUDIO".equals(media.getMediaType()) || !answers.findByAttemptIdAndQuestionId(attemptId, media.getQuestionId()).isPresent()) throw new AccessDeniedException("Media không thuộc lượt thi");
        int limit = Math.min(2, media.getMaxPlayCount() == null ? 2 : media.getMaxPlayCount());
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
        attempts.findByIdAndUserId(attemptId, userId).orElseThrow(() -> new AccessDeniedException("Bạn không có quyền xem media này"));
        QuestionMediaEntity media = questionMedia.findById(mediaId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy media"));
        if (!answers.findByAttemptIdAndQuestionId(attemptId, media.getQuestionId()).isPresent()) throw new AccessDeniedException("Media không thuộc lượt thi");
        if ("AUDIO".equals(media.getMediaType())) {
            UserMediaActivityEntity activity = mediaActivities.findByAttemptIdAndMediaIdForUpdate(attemptId, mediaId)
                    .orElseThrow(() -> new AccessDeniedException("Media chưa được cấp lượt phát"));
            if (contentToken == null || contentToken.isBlank() || !Objects.equals(activity.getPlayTokenHash(), hashToken(contentToken))
                    || activity.getPlayTokenExpiresAt() == null || activity.getPlayTokenExpiresAt().isBefore(LocalDateTime.now())) {
                throw new AccessDeniedException("Token phát audio không hợp lệ hoặc đã hết hạn");
            }
            activity.setPlayTokenHash(null); activity.setPlayTokenExpiresAt(null); mediaActivities.save(activity);
        }
        return new MediaResource(storage.load(media.getMediaUrl()), media.getContentType());
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
        QuestionEntity question = questions.findById(questionId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi"));
        QuestionItemEntity item = chooseItem(questionId, itemId);
        UserAnswerDetailEntity detail = details.findByUserAnswerIdAndQuestionItemId(answer.getId(), item.getId()).orElseGet(() -> {
            UserAnswerDetailEntity value = new UserAnswerDetailEntity(); value.setUserAnswerId(answer.getId()); value.setQuestionItemId(item.getId()); return value;
        });
        if ("ORDERING".equals(question.getQuestionType())) {
            if (userPosition != null && (userPosition < 1 || userPosition > items.findByQuestionIdOrderByPosition(questionId).size())) throw new BusinessRuleException("Vị trí sắp xếp không hợp lệ");
            detail.setUserPosition(userPosition);
        } else if (usesOptions(question.getQuestionType())) {
            if (selectedOptionId != null && options.findById(selectedOptionId).filter(x -> Objects.equals(x.getQuestionItemId(), item.getId())).isEmpty()) {
                throw new BusinessRuleException("Phương án không thuộc câu hỏi");
            }
            detail.setSelectedOptionId(selectedOptionId);
        } else if ("WRITING_TEXT".equals(question.getQuestionType())) {
            String value = textAnswer == null ? "" : textAnswer.trim();
            if (value.length() > 20000) throw new BusinessRuleException("Bài viết quá dài");
            detail.setAnswerText(value); answer.setTextAnswer(value);
        } else if ("SPEAKING_AUDIO".equals(question.getQuestionType())) {
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
        if (!"IN_PROGRESS".equals(attempt.getStatus()) && !"WAITING_GRADING".equals(attempt.getStatus())) {
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
        attempt.setSubmittedAt(now); attempt.setGradingStatus("GRADING"); attempt.setStatus(expired ? "EXPIRED" : "SUBMITTED");
        gradeObjective(attempt, exam);
        boolean waiting = skillResults.findByAttemptIdOrderBySkillAsc(attempt.getId()).stream().anyMatch(x -> x.getScaleScore() == null);
        if (waiting) { attempt.setStatus(expired ? "EXPIRED" : "WAITING_GRADING"); attempt.setGradingStatus("WAITING"); }
        else complete(attempt, exam, expired);
        attempts.save(attempt);
        if (isGradedTerminal(attempt)) { AttemptCompletedEvent event = new AttemptCompletedEvent(userId, attemptId, exam.getExamType()); outbox.enqueueCompleted(event); events.publishEvent(event); }
        else if ("WAITING_GRADING".equals(attempt.getStatus()) || ("EXPIRED".equals(attempt.getStatus()) && "WAITING".equals(attempt.getGradingStatus()))) {
            AttemptSubmittedEvent event = new AttemptSubmittedEvent(userId, attemptId, exam.getId(), exam.getExamType(), lockedAttempt(attempt, exam));
            outbox.enqueueSubmitted(event); events.publishEvent(event);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AttemptResult result(Integer userId, Integer attemptId) {
        activeUser(userId);
        ExamAttemptEntity attempt = attempts.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền xem kết quả này"));
        MockExamEntity exam = examFor(attempt);
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
        ExamAttemptEntity attempt = attempts.findByIdAndUserIdForUpdate(event.attemptId(), event.userId())
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền cập nhật lượt thi này"));
        UserAnswerEntity answer = answers.findByIdAndAttemptId(event.answerId(), attempt.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu trả lời cần chấm"));
        if (!"COMPLETED".equalsIgnoreCase(event.state())) {
            attempt.setStatus("ERROR"); attempt.setGradingStatus("ERROR"); attempts.save(attempt); return;
        }
        AiAssessmentContract.AssessmentView assessment = ai.get(event.assessmentKey(), event.userId(), false);
        if (assessment.score() == null || assessment.estimatedCefr() == null) {
            attempt.setStatus("ERROR"); attempt.setGradingStatus("ERROR"); attempts.save(attempt); return;
        }
        QuestionEntity question = questions.findById(answer.getQuestionId()).orElseThrow();
        answer.setAiScore(assessment.score()); answer.setAiFeedback(assessment.feedback()); answer.setAiProvider(assessment.provider()); answer.setAiModel(assessment.model()); answer.setGradedAt(LocalDateTime.now()); answer.setStatus("GRADED"); answers.save(answer);
        MockExamEntity exam = examFor(attempt);
        ExamSkillResultEntity result = skillResults.findByAttemptIdAndSkill(attempt.getId(), question.getSkill()).orElseGet(() -> { ExamSkillResultEntity value = new ExamSkillResultEntity(); value.setAttemptId(attempt.getId()); value.setSkill(question.getSkill()); return value; });
        result.setGradingMethod("AI"); result.setCorrectCount(null); result.setTotalItems(null); result.setRawScore(assessment.score()); result.setScaleScore(scoring.scale(exam.getScoringSchemeId(), question.getSkill(), assessment.score())); result.setCefrLevel(scoring.estimatedLevel(exam.getScoringSchemeId(), question.getSkill(), result.getScaleScore())); skillResults.save(result);
        List<ExamSkillResultEntity> results = skillResults.findByAttemptIdOrderBySkillAsc(attempt.getId());
        if (results.stream().allMatch(value -> value.getScaleScore() != null)) { complete(attempt, exam, "EXPIRED".equals(attempt.getStatus())); attempts.save(attempt); if (isGradedTerminal(attempt)) { AttemptCompletedEvent completed = new AttemptCompletedEvent(attempt.getUserId(), attempt.getId(), exam.getExamType()); outbox.enqueueCompleted(completed); events.publishEvent(completed); } }
    }

    @Transactional(readOnly = true)
    public AttemptPage page(Integer userId, Integer attemptId) {
        activeUser(userId); ExamAttemptEntity attempt = attempts.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền với lượt thi này"));
        MockExamEntity exam = examFor(attempt);
        List<QuestionPage> questionsView = examQuestions.findByMockExamIdOrderByPosition(exam.getId()).stream().map(link -> {
            QuestionEntity q = questions.findById(link.getQuestionId()).orElseThrow();
            UserAnswerEntity answer = answers.findByAttemptIdAndQuestionId(attemptId, q.getId()).orElseThrow();
            List<ItemPage> itemPages = items.findByQuestionIdOrderByPosition(q.getId()).stream().map(item -> {
                UserAnswerDetailEntity detail = details.findByUserAnswerIdAndQuestionItemId(answer.getId(), item.getId()).orElse(null);
                List<OptionPage> itemOptions = options.findByQuestionItemIdOrderByPosition(item.getId()).stream().map(o -> new OptionPage(o.getId(), o.getOptionCode(), o.getContent())).toList();
                return new ItemPage(item.getId(), item.getPosition(), item.getContent(), itemOptions,
                        detail == null ? null : detail.getSelectedOptionId(), detail == null ? null : detail.getUserPosition(),
                        detail == null ? null : detail.getAnswerText());
            }).toList();
            List<MediaPage> mediaPages = questionMedia.findByQuestionIdOrderByPosition(q.getId()).stream()
                    .map(media -> new MediaPage(media.getId(), media.getMediaType(), media.getOriginalFilename(), media.getContentType())).toList();
            return new QuestionPage(q.getId(), q.getContent(), q.getSkill(), q.getQuestionType(), link.getPosition(), itemPages, mediaPages);
        }).toList();
        Instant deadline = exam.getDurationMinutes() == null || attempt.getStartedAt() == null ? null
                : instant(attempt.getStartedAt().plusMinutes(exam.getDurationMinutes()));
        return new AttemptPage(attempt.getId(), exam.getTitle(), remainingSeconds(attempt, exam), questionsView, deadline);
    }

    public record AttemptPage(Integer attemptId, String title, Long remainingSeconds, List<QuestionPage> questions, Instant deadline) {
        public AttemptPage(Integer attemptId, String title, Long remainingSeconds, List<QuestionPage> questions) {
            this(attemptId, title, remainingSeconds, questions, null);
        }
    }
    public record QuestionPage(Integer questionId, String content, String skill, String type, int position,
            List<ItemPage> items, List<MediaPage> media) {}
    public record ItemPage(Integer itemId, int position, String content, List<OptionPage> options,
            Integer selectedOptionId, Integer userPosition, String textAnswer) {}
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
            q.put("id", question.getId()); q.put("position", link.getPosition()); q.put("content", question.getContent()); q.put("instruction", question.getInstruction()); q.put("type", question.getQuestionType()); q.put("skill", question.getSkill());
            List<Map<String, Object>> lockedItems = new ArrayList<>();
            for (QuestionItemEntity item : items.findByQuestionIdOrderByPosition(question.getId())) {
                Map<String, Object> i = new LinkedHashMap<>(); i.put("id", item.getId()); i.put("position", item.getPosition()); i.put("content", item.getContent()); i.put("correctPosition", item.getCorrectPosition());
                List<Map<String, Object>> lockedOptions = new ArrayList<>(); options.findByQuestionItemIdOrderByPosition(item.getId()).forEach(option -> { Map<String, Object> o = new LinkedHashMap<>(); o.put("id", option.getId()); o.put("position", option.getPosition()); o.put("content", option.getContent()); o.put("correct", option.getCorrect()); lockedOptions.add(o); });
                i.put("options", lockedOptions); lockedItems.add(i);
            }
            q.put("items", lockedItems); lockedQuestions.add(q);
        }
        try { snapshot.setGradingContext(json.writeValueAsString(grading)); snapshot.setLockedQuestionSnapshot(json.writeValueAsString(lockedQuestions)); }
        catch (RuntimeException exception) { throw new BusinessRuleException("Không thể khóa nội dung đề thi cho lượt làm bài"); }
        snapshots.save(snapshot);
    }

    private void gradeObjective(ExamAttemptEntity attempt, MockExamEntity exam) {
        Map<Integer, JsonNode> lockedByQuestion = lockedQuestions(attempt.getId());
        Map<String, List<MockExamQuestionEntity>> grouped = examQuestions.findByMockExamIdOrderByPosition(exam.getId()).stream()
                .collect(Collectors.groupingBy(link -> lockedByQuestion.get(link.getQuestionId()).path("skill").asText(), LinkedHashMap::new, Collectors.toList()));
        grouped.forEach((skill, links) -> {
            int total = 0, correct = 0; boolean subjective = false;
            for (MockExamQuestionEntity link : links) {
                JsonNode question = lockedByQuestion.get(link.getQuestionId());
                UserAnswerEntity answer = answers.findByAttemptIdAndQuestionId(attempt.getId(), link.getQuestionId()).orElseThrow();
                String questionType = question.path("type").asText();
                if ("WRITING_TEXT".equals(questionType) || "SPEAKING_AUDIO".equals(questionType)) { subjective = true; continue; }
                for (JsonNode item : question.path("items")) {
                    int itemId = item.path("id").asInt();
                    total++; UserAnswerDetailEntity detail = details.findByUserAnswerIdAndQuestionItemId(answer.getId(), itemId).orElse(null);
                    boolean right = false;
                    if (detail != null && detail.getSelectedOptionId() != null) {
                        for (JsonNode option : item.path("options")) {
                            if (option.path("id").asInt() == detail.getSelectedOptionId() && option.path("correct").asBoolean(false)) { right = true; break; }
                        }
                    }
                    if (!right && detail != null && !item.path("correctPosition").isNull()) right = detail.getUserPosition() != null && detail.getUserPosition() == item.path("correctPosition").asInt();
                    if (right) correct++;
                }
            }
            ExamSkillResultEntity result = skillResults.findByAttemptIdAndSkill(attempt.getId(), skill).orElseGet(() -> {
                ExamSkillResultEntity value = new ExamSkillResultEntity(); value.setAttemptId(attempt.getId()); value.setSkill(skill); return value;
            });
            if (subjective) { result.setGradingMethod("PENDING"); result.setCorrectCount(null); result.setTotalItems(null); result.setRawScore(null); result.setScaleScore(null); result.setCefrLevel(null); }
            else {
                result.setCorrectCount(correct); result.setTotalItems(total); result.setGradingMethod("OBJECTIVE");
                BigDecimal raw = total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(correct * 50.0 / total).setScale(2, RoundingMode.HALF_UP);
                result.setRawScore(raw); result.setScaleScore(scoring.scale(exam.getScoringSchemeId(), skill, raw));
                result.setCefrLevel(scoring.estimatedLevel(exam.getScoringSchemeId(), skill, result.getScaleScore()));
            }
            skillResults.save(result);
        });
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

    private void complete(ExamAttemptEntity attempt, MockExamEntity exam, boolean expired) {
        List<ExamSkillResultEntity> results = skillResults.findByAttemptIdOrderBySkillAsc(attempt.getId());
        BigDecimal average = results.stream().map(ExamSkillResultEntity::getScaleScore).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(results.size()), 2, RoundingMode.HALF_UP);
        attempt.setFinalScaleScore(average); attempt.setEstimatedOverallCefr(results.stream().map(ExamSkillResultEntity::getCefrLevel).filter(Objects::nonNull).min(Comparator.comparingInt(this::levelOrder)).orElse(null));
        attempt.setCompletedAt(LocalDateTime.now()); attempt.setGradingStatus("COMPLETED"); attempt.setStatus(expired ? "EXPIRED" : "COMPLETED");
    }

    private AttemptResult resultView(ExamAttemptEntity attempt, MockExamEntity exam) {
        List<SkillResult> skills = skillResults.findByAttemptIdOrderBySkillAsc(attempt.getId()).stream().map(value -> {
            boolean passed = value.getCefrLevel() != null && exam.getTargetLevel() != null && levelOrder(value.getCefrLevel()) >= levelOrder(exam.getTargetLevel());
            String state = value.getGradingStatus();
            if (state == null) state = value.getScaleScore() == null ? "PENDING" : "COMPLETED";
            else if ("WAITING".equals(state)) state = "PENDING";
            else if ("ERROR".equals(state)) state = "ERROR";
            return new SkillResult(value.getSkill(), value.getRawScore(), value.getScaleScore(), value.getCefrLevel(), state,
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
        List<LockedAnswer> snapshots = answers.findByAttemptIdOrderByQuestionIdAsc(attempt.getId()).stream().map(answer -> {
            QuestionEntity question = questions.findById(answer.getQuestionId()).orElseThrow();
            String task = question.getInstruction() == null || question.getInstruction().isBlank()
                    ? question.getContent() : question.getInstruction() + "\n" + question.getContent();
            String lockedContent = firstNonBlank(answer.getTextAnswer(), answer.getAudioUrl(), answer.getTranscript());
            List<LockedItem> lockedItems = items.findByQuestionIdOrderByPosition(question.getId()).stream()
                    .map(item -> new LockedItem(item.getId(), options.findByQuestionItemIdOrderByPosition(item.getId()).stream()
                            .filter(option -> Boolean.TRUE.equals(option.getCorrect())).map(QuestionOptionEntity::getId).findFirst().orElse(null),
                            item.getCorrectPosition()))
                    .toList();
            return new LockedAnswer(answer.getId(), question.getId(), question.getSkill(), question.getQuestionType(),
                    task, lockedContent, answer.getTranscript(), lockedItems);
        }).toList();
        return new LockedAttempt(attempt.getId(), attempt.getUserId(), exam.getId(), exam.getExamType(),
                attempt.getAnswerVersion() == null ? 1 : attempt.getAnswerVersion(), instant(attempt.getSubmittedAt()),
                attempt.getScoringSchemeId() == null ? exam.getScoringSchemeId() : attempt.getScoringSchemeId(),
                attempt.getPolicyVersion(), attempt.getRubricVersion() == null ? RUBRIC_VERSION : attempt.getRubricVersion(),
                attempt.getGradingRequestKey(), snapshots);
    }

    private ExamAttemptEntity editableAttempt(Integer userId, Integer attemptId) {
        ExamAttemptEntity attempt = attempts.findByIdAndUserIdForUpdate(attemptId, userId).orElseThrow(() -> new AccessDeniedException("Bạn không có quyền với lượt thi này"));
        if (!"IN_PROGRESS".equals(attempt.getStatus())) throw new BusinessRuleException("Lượt thi đã khóa");
        MockExamEntity exam = examFor(attempt);
        if (exam.getDurationMinutes() != null
                && attempt.getStartedAt().plusMinutes(exam.getDurationMinutes()).plusSeconds(SAVE_GRACE_SECONDS).isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Lượt thi đã hết thời gian");
        }
        return attempt;
    }
    private QuestionItemEntity chooseItem(Integer questionId, Integer itemId) { return items.findByQuestionIdOrderByPosition(questionId).stream().filter(x -> itemId == null || Objects.equals(x.getId(), itemId)).findFirst().orElseThrow(() -> new ResourceNotFoundException("Câu hỏi chưa có ý hỏi")); }
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
    private boolean isGradedTerminal(ExamAttemptEntity attempt) { return ("COMPLETED".equals(attempt.getStatus()) || "EXPIRED".equals(attempt.getStatus())) && "COMPLETED".equals(attempt.getGradingStatus()); }
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
