package com.aptisprep.service.exam;

import com.aptisprep.entity.*;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.event.AttemptSubmittedEvent;
import com.aptisprep.exception.AccessDeniedException;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.*;
import com.aptisprep.service.contract.ExamAttemptContract;
import com.aptisprep.service.contract.IntegrationModels.*;
import com.aptisprep.service.contract.ScoringPolicyContract;
import com.aptisprep.service.grading.AttemptFinalizationService;
import com.aptisprep.service.grading.ObjectiveGradingService;
import java.time.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamAttemptService implements ExamAttemptContract {
    private static final String RUBRIC_VERSION = "aptis-rubric-v1";
    private static final Set<String> ACTIVE_STATES = Set.of("IN_PROGRESS", "SUBMITTED", "WAITING_GRADING");
    private static final Set<String> TERMINAL_STATES = Set.of("COMPLETED", "ERROR", "EXPIRED");
    private static final long SAVE_GRACE_SECONDS = 10;
    private final UserRepository users;
    private final MockExamRepository exams;
    private final MockExamQuestionRepository examQuestions;
    private final QuestionRepository questions;
    private final QuestionItemRepository items;
    private final QuestionOptionRepository options;
    private final ExamAttemptRepository attempts;
    private final UserAnswerRepository answers;
    private final UserAnswerDetailRepository details;
    private final ExamSkillResultRepository skillResults;
    private final ScoringPolicyContract scoring;
    private final ObjectiveGradingService objectiveGrading;
    private final AttemptFinalizationService finalization;
    private final ApplicationEventPublisher events;

    public ExamAttemptService(UserRepository users, MockExamRepository exams, MockExamQuestionRepository examQuestions,
            QuestionRepository questions, QuestionItemRepository items, QuestionOptionRepository options,
            ExamAttemptRepository attempts, UserAnswerRepository answers, UserAnswerDetailRepository details,
            ExamSkillResultRepository skillResults, ScoringPolicyContract scoring, ObjectiveGradingService objectiveGrading,
            AttemptFinalizationService finalization, ApplicationEventPublisher events) {
        this.users = users; this.exams = exams; this.examQuestions = examQuestions; this.questions = questions;
        this.items = items; this.options = options; this.attempts = attempts; this.answers = answers; this.details = details;
        this.skillResults = skillResults; this.scoring = scoring; this.objectiveGrading = objectiveGrading;
        this.finalization = finalization; this.events = events;
    }

    @Override
    @Transactional
    public AttemptHandle start(Integer userId, Integer examId, String requestKey) {
        activeUser(userId);
        MockExamEntity exam = publishedExam(examId);
        List<MockExamQuestionEntity> links = examQuestions.findByMockExamIdOrderByPosition(examId);
        if (links.isEmpty()) throw new BusinessRuleException("Đề thi chưa có câu hỏi");
        String key = normalizeRequestKey(requestKey, userId, examId);
        Optional<ExamAttemptEntity> byKey = attempts.findByUserIdAndRequestKey(userId, key);
        if (byKey.isPresent() && !TERMINAL_STATES.contains(byKey.get().getStatus())) return handle(byKey.get(), exam);
        if ("PLACEMENT".equals(exam.getExamType())) {
            Optional<ExamAttemptEntity> active = attempts.findActivePlacement(userId, exam.getSkill(), ACTIVE_STATES);
            if (active.isPresent()) return handle(active.get(), examFor(active.get()));
        }
        if (byKey.isPresent()) key = key + "-" + UUID.randomUUID();
        ExamAttemptEntity attempt = new ExamAttemptEntity(); attempt.setUserId(userId); attempt.setMockExamId(examId);
        attempt.setRequestKey(key); attempt.setStatus("IN_PROGRESS"); attempt.setGradingStatus("NOT_STARTED");
        attempt.setAnswerVersion(1);
        attempt.setScoringSchemeId(exam.getScoringSchemeId());
        attempt.setPolicyVersion(scoring.policyVersion(exam.getScoringSchemeId()));
        attempt.setRubricVersion(RUBRIC_VERSION);
        attempt = attempts.save(attempt);
        for (MockExamQuestionEntity link : links) {
            UserAnswerEntity answer = new UserAnswerEntity(); answer.setAttemptId(attempt.getId()); answer.setQuestionId(link.getQuestionId());
            answer.setStatus("NOT_STARTED"); answers.save(answer);
        }
        return handle(attempt, exam);
    }

    @Transactional
    public void saveAnswer(Integer userId, Integer attemptId, Integer questionId, Integer itemId,
            Integer selectedOptionId, Integer userPosition, String textAnswer, String audioUrl) {
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
            if (audioUrl != null && audioUrl.length() > 500) throw new BusinessRuleException("Đường dẫn ghi âm không hợp lệ");
            detail.setAudioPath(blankToNull(audioUrl)); answer.setAudioUrl(blankToNull(audioUrl));
        }
        if (answer.getStartedAt() == null) answer.setStartedAt(LocalDateTime.now());
        answer.setStatus("ANSWERED"); details.save(detail); answers.save(answer);
    }

    @Override
    @Transactional
    public void submit(Integer userId, Integer attemptId, String requestKey) {
        ExamAttemptEntity attempt = attempts.findByIdAndUserIdForUpdate(attemptId, userId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền với lượt thi này"));
        if ("COMPLETED".equals(attempt.getStatus()) || "WAITING_GRADING".equals(attempt.getStatus())
                || "SUBMITTED".equals(attempt.getStatus()) || "EXPIRED".equals(attempt.getStatus())) return;
        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            throw new BusinessRuleException("Lượt thi không thể nộp ở trạng thái hiện tại");
        }
        MockExamEntity exam = publishedExam(attempt.getMockExamId());
        LocalDateTime now = LocalDateTime.now();
        boolean expired = exam.getDurationMinutes() != null && attempt.getStartedAt().plusMinutes(exam.getDurationMinutes()).isBefore(now);
        attempt.setSubmittedAt(now);
        attempt.setAnswerVersion((attempt.getAnswerVersion() == null ? 1 : attempt.getAnswerVersion()) + 1);
        attempt.setGradingRequestKey(normalizeRequestKey(requestKey, userId, attemptId));
        attempt.setGradingStatus("GRADING");
        attempt.setStatus(expired ? "EXPIRED" : "SUBMITTED");
        attempts.save(attempt);
        LockedAttempt locked = lockedAttempt(attempt, exam);
        objectiveGrading.gradeObjective(locked, exam);
        boolean finalized = !expired && finalization.finalizeIfReady(attempt, exam);
        if (!finalized) {
            if ("ERROR".equals(attempt.getStatus())) return;
            attempt.setStatus(expired ? "EXPIRED" : "WAITING_GRADING");
            attempt.setGradingStatus(expired ? "EXPIRED" : "WAITING");
            attempts.save(attempt);
            if (!expired) events.publishEvent(new AttemptSubmittedEvent(userId, attemptId, exam.getId(), exam.getExamType(), locked));
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
            return new QuestionPage(q.getId(), q.getContent(), q.getSkill(), q.getQuestionType(), link.getPosition(), itemPages);
        }).toList();
        return new AttemptPage(attempt.getId(), exam.getTitle(), remainingSeconds(attempt, exam), questionsView);
    }

    public record AttemptPage(Integer attemptId, String title, Long remainingSeconds, List<QuestionPage> questions) {}
    public record QuestionPage(Integer questionId, String content, String skill, String type, int position,
            List<ItemPage> items) {}
    public record ItemPage(Integer itemId, int position, String content, List<OptionPage> options,
            Integer selectedOptionId, Integer userPosition, String textAnswer) {}
    public record OptionPage(Integer id, String code, String content) {}

    private Long remainingSeconds(ExamAttemptEntity attempt, MockExamEntity exam) {
        Integer duration = exam.getDurationMinutes();
        if (duration == null) return null;
        LocalDateTime deadline = attempt.getStartedAt().plusMinutes(duration);
        return Math.max(0L, Duration.between(LocalDateTime.now(), deadline).getSeconds());
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
    private User requireAdmin(String email){User user=users.findByEmailIgnoreCase(email).orElseThrow(()->new AccessDeniedException("Không có quyền xem kết quả"));if(user.getRole()!=Role.ADMIN||user.getStatus()!=UserStatus.ACTIVE)throw new AccessDeniedException("Chỉ quản trị viên đang hoạt động mới được xem kết quả");return user;}
    private AttemptHandle handle(ExamAttemptEntity attempt, MockExamEntity exam) {
        Integer duration = exam.getDurationMinutes();
        Instant deadline = duration == null ? null : instant(attempt.getStartedAt().plusMinutes(duration));
        return new AttemptHandle(attempt.getId(), attempt.getStatus(), deadline);
    }
    private Instant instant(LocalDateTime value) { return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant(); }
    private String normalizeRequestKey(String value, Integer userId, Integer examId) { return value == null || value.isBlank() ? "attempt:" + userId + ":" + examId : value.trim(); }
    private boolean usesOptions(String type) { return "SINGLE_CHOICE".equals(type) || "MATCHING".equals(type) || "ORDERING".equals(type); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String firstNonBlank(String... values) { for (String value : values) if (value != null && !value.isBlank()) return value; return null; }
    private String normalizedFilter(String value) { return value == null || value.isBlank() ? "" : value.trim().toUpperCase(Locale.ROOT); }
    private int levelOrder(String value) { return switch (value == null ? "" : value) { case "A1" -> 1; case "A2" -> 2; case "B1" -> 3; case "B2" -> 4; case "C1" -> 5; case "C2" -> 6; default -> 0; }; }
}
