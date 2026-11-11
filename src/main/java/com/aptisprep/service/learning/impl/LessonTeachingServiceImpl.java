package com.aptisprep.service.learning.impl;

import com.aptisprep.config.AiProperties;
import com.aptisprep.dto.form.LessonFormDTO;
import com.aptisprep.dto.view.AiAssessmentDTO.AssessmentRequest;
import com.aptisprep.dto.view.LessonContextSnapshotEntityDTO;
import com.aptisprep.dto.view.LessonMediaResourceDTO;
import com.aptisprep.dto.view.LessonResponseDTO;
import com.aptisprep.dto.view.LessonTeachingViewsDTO.*;
import com.aptisprep.dto.view.LessonWorkSubmissionDTO;
import com.aptisprep.entity.*;
import com.aptisprep.entity.User;
import com.aptisprep.enums.*;
import com.aptisprep.exception.*;
import com.aptisprep.repository.*;
import com.aptisprep.service.account.UserService;
import com.aptisprep.service.grading.AiAssessmentService;
import com.aptisprep.service.learning.LessonTeachingService;
import com.aptisprep.service.storage.StorageService;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class LessonTeachingServiceImpl implements LessonTeachingService {
    private final LessonCheckService checkService;
    private final LessonRepository lessonRepository;
    private final UserService userRepository;
    private final LessonResponseRepository lessonResponseRepository;
    private final LessonCheckAttemptRepository lessonCheckAttemptRepository;
    private final LessonEngagementRepository lessonEngagementRepository;
    private final StorageService storage;
    private final LessonProgressRepository lessonProgressRepository;
    private final AiAssessmentService assessments;
    @PersistenceContext private EntityManager em;
    private final AiProperties aiProperties;

    public LessonTeachingServiceImpl(
            LessonCheckService c,
            LessonRepository lessonRepository,
            UserService userRepository,
            LessonResponseRepository lessonResponseRepository,
            LessonCheckAttemptRepository lessonCheckAttemptRepository,
            LessonEngagementRepository lessonEngagementRepository,
            StorageService s,
            LessonProgressRepository lessonProgressRepository,
            AiAssessmentService ai,
            AiProperties aiProperties) {
        checkService = c;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.lessonResponseRepository = lessonResponseRepository;
        this.lessonCheckAttemptRepository = lessonCheckAttemptRepository;
        this.lessonEngagementRepository = lessonEngagementRepository;
        storage = s;
        this.lessonProgressRepository = lessonProgressRepository;
        assessments = ai;
        this.aiProperties = aiProperties;
    }

    @Override
    public ReleaseStateDTO releaseState(Integer id, String email) {
        User u = user(email);
        Lesson l = lessonRepository.findById(id).orElseThrow();
        LessonMaterial m = l.getMaterial();
        boolean completed =
                lessonProgressRepository
                        .findByUserIdAndLessonId(u.getId(), id)
                        .map(p -> "COMPLETED".equals(p.getStatus()))
                        .orElse(false);
        boolean played =
                lessonEngagementRepository
                        .findByUserIdAndLessonId(u.getId(), id)
                        .map(e -> LessonListeningServiceImpl.verified(l, e))
                        .orElse(false);
        boolean productive = List.of("Writing", "Speaking").contains(l.getSkill());
        boolean submitted =
                productive
                        ? !submissionHistory(id, email).isEmpty()
                        : !lessonCheckAttemptRepository
                                .findByUserIdAndLessonIdOrderBySubmittedAtDesc(u.getId(), id)
                                .isEmpty();
        boolean noSubmission = !productive && !checkService.hasChecks(id);
        String transcript = policy(m == null ? null : m.getTranscriptVisibility(), "AFTER_LISTEN"),
                sample = policy(m == null ? null : m.getSampleVisibility(), "AFTER_SUBMIT");
        return new ReleaseStateDTO(
                released(transcript, submitted, completed, played, noSubmission),
                released(sample, submitted, completed, played, noSubmission),
                releaseHint(transcript),
                releaseHint(sample));
    }

    private static String policy(String value, String fallback) {
        return has(value) ? value : fallback;
    }

    private static boolean released(
            String rule,
            boolean submitted,
            boolean completed,
            boolean played,
            boolean noSubmission) {
        return switch (rule) {
            case "ALWAYS" -> true;
            case "AFTER_LISTEN" -> played;
            case "AFTER_SUBMIT" -> submitted || noSubmission && completed;
            case "AFTER_COMPLETE" -> completed;
            default -> false;
        };
    }

    private static String releaseHint(String rule) {
        return switch (rule) {
            case "AFTER_LISTEN" -> "Mở sau khi nghe ít nhất 90% bài nghe.";
            case "AFTER_SUBMIT" ->
                    "Mở sau khi nộp bài. Nếu bài không có phần nộp, mở sau khi hoàn thành.";
            case "AFTER_COMPLETE" -> "Mở sau khi hoàn thành bài học.";
            default -> "Có thể xem ngay.";
        };
    }

    @Override
    public boolean canAssess(Integer id, String email) {
        return aiAvailable() && !assessableSubmissions(id, email).isEmpty();
    }

    @Override
    public boolean aiAvailable() {
        return aiProperties.isEnabled() && has(aiProperties.getApiKey());
    }

    @Override
    public HistoricalMediaDTO historicalMedia(
            Integer lessonId, Integer historyId, String kind, String email, boolean quiz) {
        User u = user(email);
        LessonContextSnapshotEntityDTO snapshot;
        if (quiz) {
            LessonCheckAttempt a =
                    lessonCheckAttemptRepository
                            .findById(historyId)
                            .filter(
                                    attempt ->
                                            Objects.equals(attempt.getUserId(), u.getId())
                                                    && Objects.equals(
                                                            attempt.getLessonId(), lessonId))
                            .orElseThrow(
                                    () -> new ResourceNotFoundException("Không tìm thấy bài làm"));
            snapshot = a.getContext();
        } else {
            LessonWorkSubmission s =
                    Optional.ofNullable(em.find(LessonWorkSubmission.class, historyId))
                            .filter(
                                    submission ->
                                            Objects.equals(submission.getUserId(), u.getId())
                                                    && Objects.equals(
                                                            submission.getLessonId(), lessonId))
                            .orElseThrow(
                                    () -> new ResourceNotFoundException("Không tìm thấy bài làm"));
            snapshot = s.getContext();
        }
        if (snapshot == null)
            throw new ResourceNotFoundException("Không tìm thấy thông tin bài lúc làm");
        String path =
                switch (kind) {
                    case "image" -> snapshot.getImagePath();
                    case "audio" -> snapshot.getAudioPath();
                    case "video" -> snapshot.getVideoPath();
                    default -> null;
                };
        if (!has(path)) throw new ResourceNotFoundException("Không tìm thấy tệp của đề bài");
        return new HistoricalMediaDTO(path, kind);
    }

    @Override
    public void resetAudioEngagement(Integer id) {
        em.createQuery("update LessonEngagement e set e.audioPlayedAt=null where e.lessonId=:id")
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public LessonResponse savedWork(Integer lessonId, String email) {
        User u = user(email);
        return lessonResponseRepository.findByUserIdAndLessonId(u.getId(), lessonId).orElse(null);
    }

    @Override
    public LessonResponseDTO savedWorkDto(Integer lessonId, String email) {
        LessonResponse resp = savedWork(lessonId, email);
        if (resp == null) return null;
        return new LessonResponseDTO(resp.getId(), resp.getAudioPath(), resp.getText());
    }

    @Override
    public void saveRecording(Integer lessonId, MultipartFile file, String email) {
        User u = user(email);
        if (u.getStatus() != UserStatus.ACTIVE || u.getRole() != Role.STUDENT)
            throw new BusinessRuleException("Tài khoản không được phép học");
        em.lock(u, LockModeType.PESSIMISTIC_WRITE);
        Lesson lesson =
                lessonRepository
                        .findById(lessonId)
                        .filter(
                                l ->
                                        "PUBLISHED".equals(l.getStatus())
                                                && "Speaking".equals(l.getSkill()))
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy bài Speaking"));
        String type = file == null ? "" : Objects.toString(file.getContentType(), "");
        String name =
                file == null
                        ? ""
                        : Objects.toString(file.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
        if (file == null
                || file.isEmpty()
                || file.getSize() > 20L * 1024 * 1024
                || !type.startsWith("audio/")
                || !name.matches(".*\\.(webm|wav|mp3|ogg|m4a)$"))
            throw new BusinessRuleException("Bản ghi âm không hợp lệ hoặc lớn hơn 20 MB");
        LessonResponse r =
                lessonResponseRepository
                        .findByUserIdAndLessonId(u.getId(), lessonId)
                        .orElseGet(LessonResponse::new);
        String path = storage.store(file, "lesson-recordings/" + u.getId() + "/" + lesson.getId());
        r.setUserId(u.getId());
        r.setLessonId(lessonId);
        r.setAudioPath(path);
        r.setAudioName("Bản ghi luyện nói");
        r.setText(null);
        r.setResponseVersion((r.getResponseVersion() == null ? 0 : r.getResponseVersion()) + 1);
        r.setAssessmentKey(null);
        r.setUpdatedAt(LocalDateTime.now());
        lessonResponseRepository.save(r);
        em.persist(new LessonWorkSubmission(u.getId(), lesson, r));
    }

    @Override
    public List<AttemptViewDTO> history(Integer lessonId, String email) {
        User u = user(email);
        return lessonCheckAttemptRepository
                .findByUserIdAndLessonIdOrderBySubmittedAtDesc(u.getId(), lessonId)
                .stream()
                .map(
                        a ->
                                new AttemptViewDTO(
                                        a.getId(),
                                        a.getScore(),
                                        a.getTotal(),
                                        a.getSubmittedAt(),
                                        !a.getFeedback().isEmpty()
                                                ? a.getFeedback().stream()
                                                        .map(
                                                                value ->
                                                                        new AnswerFeedbackDTO(
                                                                                value.getPrompt(),
                                                                                value.getSelected(),
                                                                                value.getCorrect(),
                                                                                value
                                                                                        .isRightAnswer(),
                                                                                value
                                                                                        .getExplanation(),
                                                                                value
                                                                                                        .getOptionsText()
                                                                                                == null
                                                                                        ? List.of()
                                                                                        : Arrays
                                                                                                .asList(
                                                                                                        value.getOptionsText()
                                                                                                                .split(
                                                                                                                        "\\u001f"))))
                                                        .toList()
                                                : List.of(),
                                        a.getContext()))
                .toList();
    }

    @Override
    public List<LessonWorkSubmission> submissionHistory(Integer lessonId, String email) {
        User u = user(email);
        return em.createQuery(
                        "select s from LessonWorkSubmission s where s.userId=:user and"
                                + " s.lessonId=:lesson order by s.submittedAt desc,s.id desc",
                        LessonWorkSubmission.class)
                .setParameter("user", u.getId())
                .setParameter("lesson", lessonId)
                .getResultList();
    }

    @Override
    public List<LessonWorkSubmissionDTO> submissionHistoryDto(Integer lessonId, String email) {
        return submissionHistory(lessonId, email).stream()
                .map(
                        s ->
                                new LessonWorkSubmissionDTO(
                                        s.getId(),
                                        s.getSubmittedAt(),
                                        s.getText(),
                                        s.getSkill(),
                                        s.getPrompt(),
                                        s.getContext(),
                                        s.getAudioPath()))
                .toList();
    }

    @Override
    public List<LessonWorkSubmission> assessableSubmissions(Integer lessonId, String email) {
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        return submissionHistory(lessonId, email).stream()
                .filter(
                        s ->
                                has(s.getText())
                                        && (!"Speaking".equals(lesson.getSkill())
                                                || has(s.getAudioPath())))
                .toList();
    }

    @Override
    public List<LessonWorkSubmissionDTO> assessableSubmissionsDto(Integer lessonId, String email) {
        return assessableSubmissions(lessonId, email).stream()
                .map(
                        s ->
                                new LessonWorkSubmissionDTO(
                                        s.getId(),
                                        s.getSubmittedAt(),
                                        s.getText(),
                                        s.getSkill(),
                                        s.getPrompt(),
                                        s.getContext(),
                                        s.getAudioPath()))
                .toList();
    }

    @Override
    public String submissionAudio(Integer lessonId, Integer submissionId, String email) {
        User u = user(email);
        LessonWorkSubmission s = em.find(LessonWorkSubmission.class, submissionId);
        if (s == null
                || !Objects.equals(s.getUserId(), u.getId())
                || !Objects.equals(s.getLessonId(), lessonId)
                || !has(s.getAudioPath()))
            throw new ResourceNotFoundException("Không tìm thấy bản ghi");
        return s.getAudioPath();
    }

    @Override
    public void submitResponse(Integer lessonId, String text, String email) {
        saveResponse(lessonId, text, email);
        User u = user(email);
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        LessonResponse work =
                lessonResponseRepository.findByUserIdAndLessonId(u.getId(), lessonId).orElseThrow();
        if (!has(text)) throw new BusinessRuleException("Hãy nhập bài làm trước khi lưu");
        if ("Writing".equals(lesson.getSkill())) {
            int words = text.trim().split("\\s+").length;
            if ((lesson.getMinWordCount() != null && words < lesson.getMinWordCount())
                    || (lesson.getMaxWordCount() != null && words > lesson.getMaxWordCount()))
                throw new BusinessRuleException("Bài viết chưa đáp ứng số từ yêu cầu");
        }
        if ("Speaking".equals(lesson.getSkill()) && !has(work.getAudioPath()))
            throw new BusinessRuleException("Hãy lưu bản ghi trước khi lưu transcript");
        em.persist(new LessonWorkSubmission(u.getId(), lesson, work));
    }

    @Override
    public CompletionStateDTO completion(Integer lessonId, String email) {
        User learner = user(email);
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        boolean hasKnowledgeCheck = checkService.hasChecks(lessonId);
        boolean passedKnowledgeCheck = passedKnowledgeCheck(learner.getId(), lessonId);
        boolean requiresSubmission = List.of("Writing", "Speaking").contains(lesson.getSkill());
        boolean hasSubmission = hasPracticeSubmission(lesson, email);
        boolean requiresListening =
                "Listening".equals(lesson.getSkill()) && has(lesson.getAudioPath());
        boolean verifiedListening =
                lessonEngagementRepository
                        .findByUserIdAndLessonId(learner.getId(), lessonId)
                        .map(engagement -> LessonListeningServiceImpl.verified(lesson, engagement))
                        .orElse(false);
        boolean eligible =
                (!hasKnowledgeCheck || passedKnowledgeCheck)
                        && (!requiresSubmission || hasSubmission)
                        && (!requiresListening || verifiedListening);
        return new CompletionStateDTO(
                hasKnowledgeCheck,
                passedKnowledgeCheck,
                requiresSubmission,
                hasSubmission,
                requiresListening,
                verifiedListening,
                eligible);
    }

    private boolean passedKnowledgeCheck(Integer userId, Integer lessonId) {
        for (LessonCheckAttempt attempt :
                lessonCheckAttemptRepository.findByUserIdAndLessonIdOrderBySubmittedAtDesc(
                        userId, lessonId)) {
            if (LessonCheckService.passed(attempt.getScore(), attempt.getTotal())) return true;
        }
        return false;
    }

    private boolean hasPracticeSubmission(Lesson lesson, String email) {
        for (LessonWorkSubmission submission : submissionHistory(lesson.getId(), email)) {
            if ("Speaking".equals(lesson.getSkill())
                    ? has(submission.getAudioPath())
                    : has(submission.getText())) return true;
        }
        return false;
    }

    @Override
    public void section(Integer lessonId, String section, String email) {
        if (!Set.of("overview", "media", "method", "example", "practice").contains(section))
            throw new BusinessRuleException("Phần bài học không hợp lệ");
        User u = user(email);
        lessonRepository
                .findById(lessonId)
                .filter(l -> "PUBLISHED".equals(l.getStatus()))
                .orElseThrow();
        LessonEngagement e =
                lessonEngagementRepository
                        .findByUserIdAndLessonId(u.getId(), lessonId)
                        .orElseGet(LessonEngagement::new);
        e.setUserId(u.getId());
        e.setLessonId(lessonId);
        e.setLastSection(section);
        lessonEngagementRepository.save(e);
    }

    @Override
    public String lastSection(Integer lessonId, String email) {
        User u = user(email);
        return lessonEngagementRepository
                .findByUserIdAndLessonId(u.getId(), lessonId)
                .map(LessonEngagement::getLastSection)
                .orElse(null);
    }

    @Override
    public LessonStatsDTO stats(Integer lessonId) {
        List<LessonCheckAttempt> all = lessonCheckAttemptRepository.findByLessonId(lessonId);
        double avg =
                all.stream()
                        .mapToDouble(
                                a -> a.getTotal() == 0 ? 0 : (a.getScore() * 100.0 / a.getTotal()))
                        .average()
                        .orElse(0);
        return new LessonStatsDTO(
                lessonProgressRepository.countByLessonId(lessonId),
                lessonProgressRepository.countByLessonIdAndStatus(lessonId, "COMPLETED"),
                lessonEngagementRepository.countByLessonIdAndAudioPlayedAtIsNotNull(lessonId),
                all.size(),
                avg);
    }

    @Override
    public String response(Integer lessonId, String email) {
        User u = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        return lessonResponseRepository
                .findByUserIdAndLessonId(u.getId(), lessonId)
                .map(LessonResponse::getText)
                .orElse("");
    }

    @Override
    public void saveResponse(Integer lessonId, String text, String email) {
        User u =
                userRepository
                        .findByEmailIgnoreCase(email)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        em.lock(u, LockModeType.PESSIMISTIC_WRITE);
        em.refresh(u);
        if (u.getStatus() != UserStatus.ACTIVE || u.getRole() != Role.STUDENT)
            throw new BusinessRuleException("Tài khoản không được phép học");
        lessonRepository
                .findById(lessonId)
                .filter(
                        l ->
                                "PUBLISHED".equals(l.getStatus())
                                        && List.of("Writing", "Speaking").contains(l.getSkill()))
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Chỉ lưu bài viết hoặc transcript Speaking"));
        if (text == null || text.length() > 10000)
            throw new BusinessRuleException("Bài thực hành tối đa 10000 ký tự");
        LessonResponse r =
                lessonResponseRepository
                        .findByUserIdAndLessonId(u.getId(), lessonId)
                        .orElseGet(LessonResponse::new);
        if (!Objects.equals(r.getText(), text)) {
            r.setResponseVersion((r.getResponseVersion() == null ? 0 : r.getResponseVersion()) + 1);
            r.setAssessmentKey(null);
        }
        r.setUserId(u.getId());
        r.setLessonId(lessonId);
        r.setText(text);
        r.setUpdatedAt(LocalDateTime.now());
        lessonResponseRepository.save(r);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String assess(Integer lessonId, Integer submissionId, String email) {
        if (!aiProperties.isEnabled() || !has(aiProperties.getApiKey()))
            throw new BusinessRuleException("Phản hồi AI chưa được bật");
        User u = user(email);
        if (u.getStatus() != UserStatus.ACTIVE || u.getRole() != Role.STUDENT)
            throw new BusinessRuleException("Tài khoản không được phép học");
        Lesson lesson =
                lessonRepository
                        .findById(lessonId)
                        .filter(
                                l ->
                                        "PUBLISHED".equals(l.getStatus())
                                                && List.of("Writing", "Speaking")
                                                        .contains(l.getSkill()))
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy bài tự luận"));
        lessonResponseRepository
                .findByUserIdAndLessonId(u.getId(), lessonId)
                .orElseThrow(
                        () -> new BusinessRuleException("Hãy lưu bài làm trước khi yêu cầu chấm"));
        LessonWorkSubmission submitted =
                Optional.ofNullable(em.find(LessonWorkSubmission.class, submissionId))
                        .filter(
                                s ->
                                        Objects.equals(s.getUserId(), u.getId())
                                                && Objects.equals(s.getLessonId(), lessonId))
                        .orElseThrow(
                                () -> new BusinessRuleException("Bài làm được chọn không hợp lệ"));
        if (!has(submitted.getText()))
            throw new BusinessRuleException("Bài làm được chọn chưa có nội dung để chấm");
        if ("Speaking".equals(lesson.getSkill()) && !has(submitted.getAudioPath()))
            throw new BusinessRuleException("Bản Speaking được chọn chưa có bản ghi âm");
        int version = submitted.getId();
        String key = "lesson-" + lessonId + "-submission-" + submitted.getId();
        String prompt = has(submitted.getPrompt()) ? submitted.getPrompt() : lesson.getTitle();
        AssessmentRequest request =
                new AssessmentRequest(
                        key,
                        u.getId(),
                        null,
                        null,
                        version,
                        lesson.getSkill().toUpperCase(Locale.ROOT),
                        lesson.getSkill().toUpperCase(Locale.ROOT),
                        prompt,
                        submitted.getText(),
                        "lesson-v1",
                        "learning-feedback-v1",
                        BigDecimal.valueOf(50),
                        "Speaking".equals(lesson.getSkill()) ? submitted.getText() : null,
                        "LESSON_SUBMISSION",
                        submitted.getId());
        assessments.request(request);
        assessments.process(key);
        return key;
    }

    @Override
    public List<CheckViewDTO> questions(Integer id) {
        return checkService.questions(id);
    }

    @Override
    public List<CheckViewDTO> questions(Integer id, String email) {
        return checkService.questions(id, email);
    }

    @Override
    public List<LessonFormDTO.CheckForm> editorQuestions(Integer id) {
        return checkService.editorQuestions(id);
    }

    @Override
    public void validate(LessonFormDTO f) {
        videoId(f.getVideoUrl());
        safeLink(f.getSourceUrl());
        checkService.validateChecks(f.getChecks());
        for (String rule :
                List.of(
                        policy(f.getTranscriptVisibility(), "AFTER_LISTEN"),
                        policy(f.getSampleVisibility(), "AFTER_SUBMIT")))
            if (!List.of("ALWAYS", "AFTER_LISTEN", "AFTER_SUBMIT", "AFTER_COMPLETE").contains(rule))
                throw new BusinessRuleException("Thời điểm mở nội dung không hợp lệ");
        if (!"Listening".equals(f.getSkill()) && "AFTER_LISTEN".equals(f.getSampleVisibility()))
            throw new BusinessRuleException("Chỉ Listening có thể mở bài mẫu sau khi nghe");
        if ("PUBLISHED".equals(f.getStatus())
                && List.of("Writing", "Speaking").contains(f.getSkill())
                && !has(f.getPracticePrompt()))
            throw new BusinessRuleException(
                    "Bài Writing / Speaking cần đề thực hành trước khi công bố");
    }

    @Override
    public void saveChecks(Integer id, List<LessonFormDTO.CheckForm> forms) {
        checkService.saveChecks(id, forms);
    }

    @Override
    public FeedbackDTO grade(Integer id, Map<String, String> answers, String email) {
        return checkService.grade(id, answers, email);
    }

    @Override
    public String message(String skill) {
        return "Writing".equals(skill)
                ? "Hãy nhập và nộp bài viết để hoàn thành bài học"
                : "Hãy ghi âm và lưu bản ghi để hoàn thành bài nói";
    }

    @Override
    public LessonMediaResourceDTO getLessonAudio(Integer lessonId, String email) {
        Lesson lesson =
                lessonRepository
                        .findById(lessonId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
        User user = user(email);
        if (user.getRole() != Role.ADMIN && !"PUBLISHED".equals(lesson.getStatus()))
            throw new ResourceNotFoundException("Không tìm thấy âm thanh");
        if (lesson.getAudioPath() == null)
            throw new ResourceNotFoundException("Bài học chưa có âm thanh");
        return new LessonMediaResourceDTO(lesson.getAudioPath(), lesson.getAudioName());
    }

    @Override
    public String getStudentRecordingPath(Integer lessonId, String email) {
        User user = user(email);
        LessonResponse work =
                lessonResponseRepository
                        .findByUserIdAndLessonId(user.getId(), lessonId)
                        .filter(r -> r.getAudioPath() != null)
                        .orElseThrow(() -> new ResourceNotFoundException("Chưa có bản ghi"));
        return work.getAudioPath();
    }

    @Override
    public String getLessonImagePath(Integer lessonId, String email) {
        Lesson lesson =
                lessonRepository
                        .findById(lessonId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
        User user = user(email);
        if (user.getRole() != Role.ADMIN && !"PUBLISHED".equals(lesson.getStatus()))
            throw new ResourceNotFoundException("Không tìm thấy hình ảnh");
        if (lesson.getImagePath() == null)
            throw new ResourceNotFoundException("Bài học chưa có hình ảnh");
        return lesson.getImagePath();
    }

    @Override
    public String getLessonVideoPath(Integer lessonId, String email) {
        Lesson lesson =
                lessonRepository
                        .findById(lessonId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));
        User user = user(email);
        if (user.getRole() != Role.ADMIN && !"PUBLISHED".equals(lesson.getStatus())
                || lesson.getVideoPath() == null)
            throw new ResourceNotFoundException("Không tìm thấy video");
        return lesson.getVideoPath();
    }

    @Override
    public String videoId(String url) {
        if (!has(url)) return null;
        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || host == null
                    || uri.getUserInfo() != null
                    || uri.getPort() != -1) throw new IllegalArgumentException();
            String id = null;
            if (host.equalsIgnoreCase("youtu.be")) id = uri.getPath().substring(1);
            else if (List.of("www.youtube.com", "youtube.com", "www.youtube-nocookie.com")
                    .contains(host.toLowerCase())) {
                if (uri.getPath().startsWith("/embed/")) id = uri.getPath().substring(7);
                else if ("/watch".equals(uri.getPath()) && uri.getRawQuery() != null)
                    for (String part : uri.getRawQuery().split("&"))
                        if (part.startsWith("v=")) id = part.substring(2);
            }
            if (id == null || !id.matches("[A-Za-z0-9_-]{11}"))
                throw new IllegalArgumentException();
            return id;
        } catch (Exception ex) {
            throw new BusinessRuleException(
                    "Nhập liên kết HTTPS YouTube hợp lệ cho video hướng dẫn");
        }
    }

    private static void safeLink(String value) {
        if (!has(value)) return;
        try {
            URI u = URI.create(value);
            if (!"https".equalsIgnoreCase(u.getScheme())
                    || u.getHost() == null
                    || u.getUserInfo() != null) throw new IllegalArgumentException();
        } catch (Exception e) {
            throw new BusinessRuleException("Nguồn tham khảo cần là liên kết HTTPS hợp lệ");
        }
    }

    private static boolean has(String s) {
        return s != null && !s.isBlank();
    }

    private User user(String email) {
        return userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
    }
}
