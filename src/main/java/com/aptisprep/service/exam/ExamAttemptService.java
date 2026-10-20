package com.aptisprep.service.exam;

import com.aptisprep.entity.*;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.event.AttemptCompletedEvent;
import com.aptisprep.event.AttemptSubmittedEvent;
import com.aptisprep.event.AiAssessmentCompletedEvent;
import com.aptisprep.exception.AccessDeniedException;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.*;
import com.aptisprep.service.contract.ExamAttemptContract;
import com.aptisprep.service.contract.AiAssessmentContract;
import com.aptisprep.service.contract.IntegrationModels.*;
import com.aptisprep.service.contract.ScoringPolicyContract;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamAttemptService implements ExamAttemptContract {
    private static final Set<String> ACTIVE_STATES = Set.of("IN_PROGRESS", "SUBMITTED", "WAITING_GRADING");
    private static final Set<String> TERMINAL_STATES = Set.of("COMPLETED", "ERROR", "EXPIRED");
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
    private final AiAssessmentContract ai;
    private final ApplicationEventPublisher events;

    public ExamAttemptService(UserRepository users, MockExamRepository exams, MockExamQuestionRepository examQuestions,
            QuestionRepository questions, QuestionItemRepository items, QuestionOptionRepository options,
            ExamAttemptRepository attempts, UserAnswerRepository answers, UserAnswerDetailRepository details,
            ExamSkillResultRepository skillResults, ScoringPolicyContract scoring, AiAssessmentContract ai, ApplicationEventPublisher events) {
        this.users = users; this.exams = exams; this.examQuestions = examQuestions; this.questions = questions;
        this.items = items; this.options = options; this.attempts = attempts; this.answers = answers; this.details = details;
        this.skillResults = skillResults; this.scoring = scoring; this.ai = ai; this.events = events;
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
        if ("COMPLETED".equals(attempt.getStatus())) return;
        if (!"IN_PROGRESS".equals(attempt.getStatus()) && !"WAITING_GRADING".equals(attempt.getStatus())) {
            throw new BusinessRuleException("Lượt thi không thể nộp ở trạng thái hiện tại");
        }
        MockExamEntity exam = publishedExam(attempt.getMockExamId());
        LocalDateTime now = LocalDateTime.now();
        boolean expired = exam.getDurationMinutes() != null && attempt.getStartedAt().plusMinutes(exam.getDurationMinutes()).isBefore(now);
        attempt.setSubmittedAt(now); attempt.setGradingStatus("GRADING"); attempt.setStatus(expired ? "EXPIRED" : "SUBMITTED");
        gradeObjective(attempt, exam);
        boolean waiting = skillResults.findByAttemptIdOrderBySkillAsc(attempt.getId()).stream().anyMatch(x -> x.getScaleScore() == null);
        if (waiting) { attempt.setStatus(expired ? "EXPIRED" : "WAITING_GRADING"); attempt.setGradingStatus("WAITING"); }
        else complete(attempt, exam);
        attempts.save(attempt);
        if ("COMPLETED".equals(attempt.getStatus())) events.publishEvent(new AttemptCompletedEvent(userId, attemptId, exam.getExamType()));
        else if ("WAITING_GRADING".equals(attempt.getStatus())) events.publishEvent(new AttemptSubmittedEvent(userId, attemptId, exam.getId(), exam.getExamType()));
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
    public HistoryPage history(Integer userId, int page, int size) {
        // History is also rendered by the admin's student-detail view, including
        // locked/inactive students. Student-facing endpoints validate the active
        // principal before calling this contract.
        int safePage = Math.max(0, page); int safeSize = Math.min(100, Math.max(1, size));
        var result = attempts.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(safePage, safeSize));
        List<AttemptResult> values = result.stream().map(a -> resultView(a, examFor(a))).toList();
        return new HistoryPage(values, safePage, safeSize, result.getTotalElements());
    }

    @Override
    @Transactional
    public void retryFailedGrading(Integer userId, Integer attemptId, String requestKey) {
        ExamAttemptEntity attempt = attempts.findByIdAndUserIdForUpdate(attemptId, userId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền với lượt thi này"));
        if (!"ERROR".equals(attempt.getStatus())) throw new BusinessRuleException("Lượt thi này không có phần chấm lỗi");
        throw new BusinessRuleException("Chưa có phần chấm AI lỗi để thử lại");
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
        if (results.stream().allMatch(value -> value.getScaleScore() != null)) { complete(attempt, exam); attempts.save(attempt); events.publishEvent(new AttemptCompletedEvent(attempt.getUserId(), attempt.getId(), exam.getExamType())); }
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
            return new QuestionPage(q.getId(), q.getContent(), q.getSkill(), q.getQuestionType(), link.getPosition(), itemPages);
        }).toList();
        return new AttemptPage(attempt.getId(), exam.getTitle(), exam.getDurationMinutes(), handle(attempt, exam).deadline(), questionsView);
    }

    public record AttemptPage(Integer attemptId, String title, Integer durationMinutes, Instant deadline, List<QuestionPage> questions) {}
    public record QuestionPage(Integer questionId, String content, String skill, String type, int position,
            List<ItemPage> items) {}
    public record ItemPage(Integer itemId, int position, String content, List<OptionPage> options,
            Integer selectedOptionId, Integer userPosition, String textAnswer) {}
    public record OptionPage(Integer id, String code, String content) {}

    private void gradeObjective(ExamAttemptEntity attempt, MockExamEntity exam) {
        Map<String, List<MockExamQuestionEntity>> grouped = examQuestions.findByMockExamIdOrderByPosition(exam.getId()).stream()
                .collect(Collectors.groupingBy(link -> questions.findById(link.getQuestionId()).orElseThrow().getSkill(), LinkedHashMap::new, Collectors.toList()));
        grouped.forEach((skill, links) -> {
            int total = 0, correct = 0; boolean subjective = false;
            for (MockExamQuestionEntity link : links) {
                QuestionEntity q = questions.findById(link.getQuestionId()).orElseThrow();
                UserAnswerEntity answer = answers.findByAttemptIdAndQuestionId(attempt.getId(), q.getId()).orElseThrow();
                if ("WRITING_TEXT".equals(q.getQuestionType()) || "SPEAKING_AUDIO".equals(q.getQuestionType())) { subjective = true; continue; }
                for (QuestionItemEntity item : items.findByQuestionIdOrderByPosition(q.getId())) {
                    total++; UserAnswerDetailEntity detail = details.findByUserAnswerIdAndQuestionItemId(answer.getId(), item.getId()).orElse(null);
                    boolean right = detail != null && detail.getSelectedOptionId() != null && options.findById(detail.getSelectedOptionId()).map(o -> Boolean.TRUE.equals(o.getCorrect())).orElse(false);
                    if (!right && detail != null && item.getCorrectPosition() != null) right = Objects.equals(detail.getUserPosition(), item.getCorrectPosition());
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

    private void complete(ExamAttemptEntity attempt, MockExamEntity exam) {
        List<ExamSkillResultEntity> results = skillResults.findByAttemptIdOrderBySkillAsc(attempt.getId());
        BigDecimal average = results.stream().map(ExamSkillResultEntity::getScaleScore).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(results.size()), 2, RoundingMode.HALF_UP);
        attempt.setFinalScaleScore(average); attempt.setEstimatedOverallCefr(results.stream().map(ExamSkillResultEntity::getCefrLevel).filter(Objects::nonNull).min(Comparator.comparingInt(this::levelOrder)).orElse(null));
        attempt.setCompletedAt(LocalDateTime.now()); attempt.setGradingStatus("COMPLETED"); attempt.setStatus("COMPLETED");
    }

    private AttemptResult resultView(ExamAttemptEntity attempt, MockExamEntity exam) {
        List<SkillResult> skills = skillResults.findByAttemptIdOrderBySkillAsc(attempt.getId()).stream().map(value -> {
            boolean passed = value.getCefrLevel() != null && exam.getTargetLevel() != null && levelOrder(value.getCefrLevel()) >= levelOrder(exam.getTargetLevel());
            return new SkillResult(value.getSkill(), value.getRawScore(), value.getScaleScore(), value.getCefrLevel(), value.getScaleScore() == null ? "PENDING" : "COMPLETED", null, passed);
        }).toList();
        return new AttemptResult(attempt.getId(), attempt.getUserId(), exam.getId(), exam.getExamType(), exam.getScoringSchemeId(), attempt.getStatus(), skills, instant(attempt.getSubmittedAt()));
    }

    private ExamAttemptEntity editableAttempt(Integer userId, Integer attemptId) {
        ExamAttemptEntity attempt = attempts.findByIdAndUserIdForUpdate(attemptId, userId).orElseThrow(() -> new AccessDeniedException("Bạn không có quyền với lượt thi này"));
        if (!"IN_PROGRESS".equals(attempt.getStatus())) throw new BusinessRuleException("Lượt thi đã khóa");
        MockExamEntity exam = examFor(attempt);
        if (exam.getDurationMinutes() != null && attempt.getStartedAt().plusMinutes(exam.getDurationMinutes()).isBefore(LocalDateTime.now())) throw new BusinessRuleException("Lượt thi đã hết thời gian");
        return attempt;
    }
    private QuestionItemEntity chooseItem(Integer questionId, Integer itemId) { return items.findByQuestionIdOrderByPosition(questionId).stream().filter(x -> itemId == null || Objects.equals(x.getId(), itemId)).findFirst().orElseThrow(() -> new ResourceNotFoundException("Câu hỏi chưa có ý hỏi")); }
    private MockExamEntity publishedExam(Integer id) { return exams.findById(id).filter(x -> "PUBLISHED".equals(x.getStatus())).orElseThrow(() -> new ResourceNotFoundException("Đề thi chưa được công bố")); }
    private MockExamEntity examFor(ExamAttemptEntity attempt) { return exams.findById(attempt.getMockExamId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi")); }
    private User activeUser(Integer id) { User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản")); if (user.getRole() != Role.STUDENT || user.getStatus() != UserStatus.ACTIVE) throw new AccessDeniedException("Tài khoản không còn hoạt động"); return user; }
    private AttemptHandle handle(ExamAttemptEntity attempt, MockExamEntity exam) { return new AttemptHandle(attempt.getId(), attempt.getStatus(), instant(attempt.getStartedAt().plusMinutes(exam.getDurationMinutes() == null ? 0 : exam.getDurationMinutes()))); }
    private Instant instant(LocalDateTime value) { return value == null ? null : value.toInstant(ZoneOffset.UTC); }
    private String normalizeRequestKey(String value, Integer userId, Integer examId) { return value == null || value.isBlank() ? "attempt:" + userId + ":" + examId : value.trim(); }
    private boolean usesOptions(String type) { return "SINGLE_CHOICE".equals(type) || "MATCHING".equals(type) || "ORDERING".equals(type); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private int levelOrder(String value) { return switch (value == null ? "" : value) { case "A1" -> 1; case "A2" -> 2; case "B1" -> 3; case "B2" -> 4; case "C1" -> 5; case "C2" -> 6; default -> 0; }; }
}
