package com.aptisprep.mvc.service.grading.impl;
import com.aptisprep.mvc.support.scoring.*;
import com.aptisprep.mvc.support.contract.*;

import com.aptisprep.mvc.service.grading.*;

import com.aptisprep.mvc.service.grading.AttemptFinalizationService;

import com.aptisprep.mvc.entity.AiAssessment;
import com.aptisprep.mvc.entity.AiAssessmentJob;
import com.aptisprep.mvc.entity.ExamAttempt;
import com.aptisprep.mvc.entity.ExamAttemptSnapshot;
import com.aptisprep.mvc.entity.ExamSkillResult;
import com.aptisprep.mvc.entity.MockExam;
import com.aptisprep.mvc.entity.Question;
import com.aptisprep.mvc.entity.UserAnswer;
import com.aptisprep.mvc.enums.AiAssessmentState;
import com.aptisprep.mvc.enums.AiJobState;
import com.aptisprep.mvc.event.AiAssessmentCompletedEvent;
import com.aptisprep.mvc.event.AttemptCompletedEvent;
import com.aptisprep.mvc.event.AttemptSubmittedEvent;
import com.aptisprep.mvc.exception.AccessDeniedException;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.exception.ResourceNotFoundException;
import com.aptisprep.mvc.repository.AiAssessmentJobRepository;
import com.aptisprep.mvc.repository.AiAssessmentRepository;
import com.aptisprep.mvc.repository.ExamAttemptRepository;
import com.aptisprep.mvc.repository.ExamAttemptSnapshotRepository;
import com.aptisprep.mvc.repository.ExamSkillResultRepository;
import com.aptisprep.mvc.repository.MockExamRepository;
import com.aptisprep.mvc.repository.QuestionRepository;
import com.aptisprep.mvc.repository.UserAnswerRepository;
import com.aptisprep.mvc.service.grading.AiAssessmentService;
import com.aptisprep.mvc.support.contract.GradingContextFormatter;
import com.aptisprep.mvc.support.contract.IntegrationModels.LockedAnswer;
import com.aptisprep.mvc.service.scoring.ScoringPolicyService;
import com.aptisprep.mvc.dto.ExamSkillResultDTO;
import com.aptisprep.mvc.dto.AiAssessmentDTO;
import com.aptisprep.mvc.service.exam.AttemptOutboxService;
import com.aptisprep.mvc.support.scoring.CheckpointScorePolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Gom các thành phần điểm hợp lệ và quản lý trạng thái cuối của attempt.
 *
 * <p>Điểm objective có thể xong trước điểm AI. Finalization chờ mọi skill result hoàn tất, áp dụng
 * bộ chấm được chọn, rồi mới công bố kết quả hoàn thành cho học viên.
 */
@Service
public class AttemptFinalizationServiceImpl implements AttemptFinalizationService {
    private ExamAttemptRepository examAttemptRepository;
    private ExamSkillResultRepository examSkillResultRepository;
    private MockExamRepository mockExamRepository;
    private UserAnswerRepository userAnswerRepository;
    private QuestionRepository questionRepository;
    private ExamAttemptSnapshotRepository examAttemptSnapshotRepository;
    private AiAssessmentService ai;
    private AiAssessmentJobRepository aiAssessmentJobRepository;
    private AiAssessmentRepository aiAssessmentRepository;
    private ScoringPolicyService scoring;
    private AttemptOutboxService outbox;
    private ApplicationEventPublisher events;
    private TransactionTemplate transactions;

    public AttemptFinalizationServiceImpl(
            ExamAttemptRepository examAttemptRepository,
            ExamSkillResultRepository examSkillResultRepository,
            MockExamRepository mockExamRepository,
            UserAnswerRepository userAnswerRepository,
            QuestionRepository questionRepository,
            ExamAttemptSnapshotRepository examAttemptSnapshotRepository,
            AiAssessmentService ai,
            AiAssessmentJobRepository aiAssessmentJobRepository,
            AiAssessmentRepository aiAssessmentRepository,
            ScoringPolicyService scoring,
            AttemptOutboxService outbox,
            ApplicationEventPublisher events,
            PlatformTransactionManager transactionManager) {
        this.examAttemptRepository = examAttemptRepository;
        this.examSkillResultRepository = examSkillResultRepository;
        this.mockExamRepository = mockExamRepository;
        this.userAnswerRepository = userAnswerRepository;
        this.questionRepository = questionRepository;
        this.examAttemptSnapshotRepository = examAttemptSnapshotRepository;
        this.ai = ai;
        this.aiAssessmentJobRepository = aiAssessmentJobRepository;
        this.aiAssessmentRepository = aiAssessmentRepository;
        this.scoring = scoring;
        this.outbox = outbox;
        this.events = events;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    /**
     * Chỉ finalize khi tất cả skill result đã hoàn tất và hợp lệ theo policy.
     *
     * <p>Với đề Aptis tổng hợp, bốn kỹ năng được tổng hợp; Grammar & Vocabulary vẫn hiển thị
     * riêng để hỗ trợ xét CEFR.
     */
    @Transactional
    public boolean finalizeIfReady(ExamAttempt attempt, MockExam exam) {
        if ("COMPLETED".equals(attempt.getStatus())
                || "COMPLETED".equals(attempt.getGradingStatus())) return false;
        List<ExamSkillResult> results =
                examSkillResultRepository.findByAttemptIdOrderBySkillAsc(attempt.getId());
        if (results.isEmpty()) return false;
        if (results.stream().anyMatch(streamValue -> hasPolicyError(streamValue))) {
            attempt.setStatus("ERROR");
            attempt.setGradingStatus("ERROR");
            examAttemptRepository.save(attempt);
            if ("PLACEMENT".equals(exam.getExamType())) publishCompleted(attempt, exam);
            return false;
        }
        if (results.stream().anyMatch(value -> !isComplete(value))) return false;
        Integer schemeId =
                attempt.getScoringSchemeId() == null
                        ? exam.getScoringSchemeId()
                        : attempt.getScoringSchemeId();
        List<ExamSkillResultDTO> scores =
                results.stream()
                        .map(value -> ExamSkillResultDTO.convertEntityToDto(value))
                        .toList();
        BigDecimal total = null;
        String overall = null;
        if (schemeId != null
                && ("FULL_MOCK".equals(exam.getExamType())
                        || ("PLACEMENT".equals(exam.getExamType())
                                && "FULL_MOCK".equals(exam.getSkill())))) {
            var summary = scoring.fullMockScore(schemeId, scores);
            total = summary.getFinalScaleScore();
            overall = summary.getEstimatedOverallCefr();
            for (int i = 0; i < results.size(); i++) {
                // Grammar & Vocabulary là skill lõi dùng để tính trình độ tổng hợp,
                // nhưng CEFR objective riêng của nó vẫn phải hiển thị ở trang chi tiết kết quả.
                if (!"GRAMMAR_VOCABULARY".equals(results.get(i).getSkill()))
                    results.get(i).setCefrLevel(summary.getSkillResults().get(i).getCefrLevel());
            }
        } else if (schemeId != null) {
            total = scoring.overallScale(schemeId, scores);
            overall = scoring.overallLevel(schemeId, scores);
        }
        attempt.setFinalScaleScore(total);
        attempt.setEstimatedOverallCefr(overall);
        attempt.setCompletedAt(LocalDateTime.now());
        attempt.setGradingStatus("COMPLETED");
        attempt.setStatus("EXPIRED".equals(attempt.getStatus()) ? "EXPIRED" : "COMPLETED");
        examAttemptRepository.save(attempt);
        return true;
    }

    public void publishCompleted(ExamAttempt attempt, MockExam exam) {
        AttemptCompletedEvent event =
                new AttemptCompletedEvent(attempt.getUserId(), attempt.getId(), exam.getExamType());
        outbox.enqueueCompleted(event);
        events.publishEvent(event);
    }

    /**
     * Ghi nhận một kết quả AI hợp lệ rồi thử finalize lại. Kiểm tra version giúp callback cũ hoặc
     * callback trùng không ảnh hưởng khi đáp án đã sửa hay việc chấm được retry.
     */
    @Transactional
    public void applyAiAssessment(AiAssessmentCompletedEvent event) {
        if (event.attemptId() == null || event.answerId() == null) return;
        AiAssessmentJob callbackJob =
                aiAssessmentJobRepository.findByAssessmentKey(event.assessmentKey()).orElse(null);
        if (!isCurrentExamCallback(callbackJob, event)) return;
        ExamAttempt attempt =
                examAttemptRepository
                        .findByIdAndUserIdForUpdate(event.attemptId(), event.userId())
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền cập nhật lượt thi này"));
        if ("COMPLETED".equals(attempt.getStatus())) return;
        if (!Objects.equals(event.answerVersion(), attempt.getAnswerVersion())
                || !Objects.equals(callbackJob.getPolicyVersion(), attempt.getPolicyVersion())
                || !Objects.equals(callbackJob.getRubricVersion(), attempt.getRubricVersion()))
            return;
        UserAnswer answer =
                userAnswerRepository
                        .findByIdAndAttemptId(event.answerId(), attempt.getId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy câu trả lời cần chấm"));
        Question question =
                questionRepository
                        .findById(answer.getQuestionId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy câu hỏi cần chấm"));
        ExamSkillResult result =
                examSkillResultRepository
                        .findByAttemptIdAndSkill(attempt.getId(), question.getSkill())
                        .orElseGet(
                                () -> {
                                    ExamSkillResult value = new ExamSkillResult();
                                    value.setAttemptId(attempt.getId());
                                    value.setSkill(question.getSkill());
                                    return value;
                                });
        AiAssessmentDTO assessment = ai.getForGrading(event.assessmentKey());
        if (assessment == null
                || !Objects.equals(assessment.getAssessmentKey(), event.assessmentKey())
                || !Objects.equals(assessment.getAnswerId(), event.answerId())
                || !Objects.equals(assessment.getAnswerVersion(), attempt.getAnswerVersion())
                || !Objects.equals(assessment.getPolicyVersion(), attempt.getPolicyVersion())
                || !Objects.equals(assessment.getRubricVersion(), attempt.getRubricVersion())) {
            return;
        }
        if (!"COMPLETED".equalsIgnoreCase(event.state())) {
            result.setGradingStatus("ERROR");
            result.setGradingMethod(
                    result.getObjectiveRawScore() == null ? "AI_ERROR" : "OBJECTIVE_ERROR");
            examSkillResultRepository.save(result);
            attempt.setStatus("ERROR");
            attempt.setGradingStatus("ERROR");
            examAttemptRepository.save(attempt);
            MockExam failedExam = exam(attempt);
            if ("PLACEMENT".equals(failedExam.getExamType())) publishCompleted(attempt, failedExam);
            return;
        }
        if (assessment.getScore() == null
                || assessment.getScore().compareTo(BigDecimal.ZERO) < 0
                || assessment.getScore().compareTo(BigDecimal.valueOf(50)) > 0) {
            return;
        }
        answer.setAiScore(assessment.getScore());
        answer.setAiFeedback(assessment.getFeedback());
        answer.setAiProvider(assessment.getProvider());
        answer.setAiModel(assessment.getModel());
        answer.setGradedAt(LocalDateTime.now());
        answer.setStatus("GRADED");
        if (assessment.getTranscript() != null) answer.setTranscript(assessment.getTranscript());
        userAnswerRepository.save(answer);

        List<AiAssessmentJob> skillJobs =
                aiAssessmentJobRepository
                        .findExamJobs(
                                attempt.getUserId(),
                                attempt.getId(),
                                attempt.getAnswerVersion(),
                                attempt.getPolicyVersion(),
                                attempt.getRubricVersion())
                        .stream()
                        .filter(job -> question.getSkill().equalsIgnoreCase(job.getSkill()))
                        .filter(
                                job ->
                                        isSubjectiveAnswerJob(
                                                job, attempt.getId(), question.getSkill()))
                        .toList();
        List<BigDecimal> subjectiveScores =
                new ArrayList<>(
                        skillJobs.stream()
                                .map(job -> acceptedAssessment(job).orElse(null))
                                .filter(streamValue -> Objects.nonNull(streamValue))
                                .filter(
                                        value ->
                                                value.getState() == AiAssessmentState.COMPLETED
                                                        && value.getScore() != null)
                                .map(streamValue -> streamValue.getScore())
                                .toList());
        BigDecimal subjectiveRawMax = subjectiveRawMax(attempt.getId(), question.getSkill());
        for (UserAnswer saved :
                userAnswerRepository.findByAttemptIdOrderByQuestionIdAsc(attempt.getId())) {
            if (!"GRADED".equals(saved.getStatus())
                    || !"RULE".equals(saved.getAiProvider())
                    || !"EMPTY_SUBMISSION".equals(saved.getAiModel())) continue;
            questionRepository
                    .findById(saved.getQuestionId())
                    .filter(value -> question.getSkill().equalsIgnoreCase(value.getSkill()))
                    .filter(
                            value ->
                                    "WRITING_TEXT".equals(value.getQuestionType())
                                            || "SPEAKING_AUDIO".equals(value.getQuestionType()))
                    .ifPresent(value -> subjectiveScores.add(BigDecimal.ZERO));
        }
        // Sau khi restart có thể mất một số callback dù kết quả chấm đã được lưu.
        // Đồng bộ lại mọi answer hợp lệ trước khi hoàn tất aggregate, không chỉ answer của event này.
        for (AiAssessmentJob job : skillJobs) {
            acceptedAssessment(job)
                    .filter(
                            value ->
                                    value.getState() == AiAssessmentState.COMPLETED
                                            && value.getScore() != null)
                    .ifPresent(
                            value ->
                                    userAnswerRepository
                                            .findByIdAndAttemptId(
                                                    job.getAnswerId(), attempt.getId())
                                            .filter(saved -> !"GRADED".equals(saved.getStatus()))
                                            .ifPresent(
                                                    saved -> {
                                                        saved.setAiScore(value.getScore());
                                                        saved.setAiFeedback(value.getFeedback());
                                                        saved.setAiProvider(value.getProvider());
                                                        saved.setAiModel(value.getModel());
                                                        saved.setGradedAt(LocalDateTime.now());
                                                        saved.setStatus("GRADED");
                                                        if (value.getTranscript() != null)
                                                            saved.setTranscript(
                                                                    value.getTranscript());
                                                        userAnswerRepository.save(saved);
                                                    }));
        }
        long expected = subjectiveAnswers(attempt.getId(), question.getSkill());
        if (subjectiveScores.size() < expected) {
            boolean failedAssessment =
                    skillJobs.stream()
                            .anyMatch(
                                    job -> {
                                        if (job.getState() == AiJobState.ERROR) return true;
                                        return acceptedAssessment(job)
                                                .map(
                                                        value ->
                                                                value.getState()
                                                                        == AiAssessmentState.ERROR)
                                                .orElse(false);
                                    });
            if (failedAssessment) {
                result.setGradingStatus("ERROR");
                result.setGradingMethod(
                        result.getObjectiveRawScore() == null ? "AI_ERROR" : "OBJECTIVE_ERROR");
                result.setRawScore(null);
                result.setScaleScore(null);
                result.setCefrLevel(null);
                examSkillResultRepository.save(result);
                attempt.setStatus("ERROR");
                attempt.setGradingStatus("ERROR");
                examAttemptRepository.save(attempt);
                return;
            }
            result.setGradingStatus("WAITING");
            result.setGradingMethod("AI_PENDING");
            result.setRawScore(null);
            result.setScaleScore(null);
            result.setCefrLevel(null);
            examSkillResultRepository.save(result);
            return;
        }
        BigDecimal subjective =
                subjectiveScores.stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);
        result.setSubjectiveRawScore(subjective);
        BigDecimal objectiveRawMax =
                result.getRawMax() == null ? BigDecimal.ZERO : result.getRawMax();
        result.setRawMax(objectiveRawMax.add(subjectiveRawMax).setScale(2, RoundingMode.HALF_UP));
        BigDecimal raw = aggregateRaw(result.getObjectiveRawScore(), subjective);
        result.setRawScore(raw);
        Integer schemeId =
                attempt.getScoringSchemeId() == null
                        ? exam(attempt).getScoringSchemeId()
                        : attempt.getScoringSchemeId();
        if (schemeId == null) {
            result.setScaleScore(null);
            result.setCefrLevel(null);
            result.setGradingMethod("UNSCALED");
            result.setGradingStatus("COMPLETED");
            examSkillResultRepository.save(result);
            if (finalizeIfReady(attempt, exam(attempt))) publishCompleted(attempt, exam(attempt));
            return;
        }
        result.setScaleScore(
                scoring.scale(schemeId, question.getSkill(), raw, result.getRawMax()));
        result.setCefrLevel(
                CheckpointScorePolicy.confirmedLevel(
                        exam(attempt).getExamType(),
                        exam(attempt).getTargetLevel(),
                        scoring.estimatedLevel(
                                schemeId, question.getSkill(), result.getScaleScore())));
        if (result.getCefrLevel() == null
                && scoring.belowConfiguredTarget(
                        schemeId, question.getSkill(), result.getScaleScore())) {
            result.setGradingMethod("BELOW_TARGET");
            result.setGradingStatus("COMPLETED");
        } else if (result.getCefrLevel() == null) {
            result.setGradingMethod("POLICY_ERROR");
            result.setGradingStatus("ERROR");
        } else {
            result.setGradingMethod(result.getObjectiveRawScore() == null ? "AI" : "OBJECTIVE_AI");
            result.setGradingStatus("COMPLETED");
        }
        examSkillResultRepository.save(result);
        if (finalizeIfReady(attempt, exam(attempt))) publishCompleted(attempt, exam(attempt));
    }

    @Transactional
    public void markSubjectiveUnavailable(AttemptSubmittedEvent event, String reason) {
        if (event == null || event.lockedAttempt() == null || !accepts(event)) return;
        if (reason == null) return;
        Set<String> unavailableSkills = new HashSet<>();
        for (LockedAnswer answer : event.lockedAttempt().answers()) {
            if (("WRITING_TEXT".equals(answer.questionType())
                            || "SPEAKING_AUDIO".equals(answer.questionType()))
                    && GradingContextFormatter.hasSubmission(answer)) {
                unavailableSkills.add(answer.skill());
            }
        }
        if (unavailableSkills.isEmpty()) return;
        ExamAttempt attempt =
                examAttemptRepository
                        .findByIdAndUserIdForUpdate(event.attemptId(), event.userId())
                        .orElseThrow();
        for (String skill : unavailableSkills) {
            ExamSkillResult result =
                    examSkillResultRepository
                            .findByAttemptIdAndSkill(attempt.getId(), skill)
                            .orElseGet(
                                    () -> {
                                        ExamSkillResult value = new ExamSkillResult();
                                        value.setAttemptId(attempt.getId());
                                        value.setSkill(skill);
                                        return value;
                                    });
            result.setGradingMethod(reason == null ? "AI_EMPTY" : reason);
            result.setGradingStatus("ERROR");
            result.setRawScore(null);
            result.setScaleScore(null);
            result.setCefrLevel(null);
            examSkillResultRepository.save(result);
        }
        if (!unavailableSkills.isEmpty()) {
            attempt.setStatus("ERROR");
            attempt.setGradingStatus("ERROR");
            examAttemptRepository.save(attempt);
            MockExam failedExam = exam(attempt);
            if ("PLACEMENT".equals(failedExam.getExamType())) publishCompleted(attempt, failedExam);
        }
    }

    /** Từ chối snapshot submission cũ, sai attempt hoặc bị client thay đổi trước khi chạy AI. */
    @Transactional(readOnly = true)
    public boolean accepts(AttemptSubmittedEvent event) {
        if (event == null
                || event.lockedAttempt() == null
                || !Objects.equals(event.userId(), event.lockedAttempt().userId())
                || !Objects.equals(event.attemptId(), event.lockedAttempt().attemptId())
                || !Objects.equals(event.examId(), event.lockedAttempt().examId())
                || !Objects.equals(event.examType(), event.lockedAttempt().examType())) {
            return false;
        }
        ExamAttempt attempt =
                examAttemptRepository
                        .findByIdAndUserId(event.attemptId(), event.userId())
                        .orElse(null);
        if (attempt == null
                || "COMPLETED".equals(attempt.getStatus())
                || "ERROR".equals(attempt.getStatus())) return false;
        if (!"SUBMITTED".equals(attempt.getStatus())
                && !"WAITING_GRADING".equals(attempt.getStatus())
                && !("EXPIRED".equals(attempt.getStatus())
                        && "WAITING".equals(attempt.getGradingStatus()))) return false;
        if (!Objects.equals(attempt.getMockExamId(), event.lockedAttempt().examId())
                || !Objects.equals(
                        attempt.getAnswerVersion(), event.lockedAttempt().answerVersion())
                || !Objects.equals(
                        attempt.getGradingRequestKey(), event.lockedAttempt().gradingRequestKey())
                || !Objects.equals(
                        attempt.getScoringSchemeId(), event.lockedAttempt().scoringSchemeId())
                || !Objects.equals(
                        attempt.getPolicyVersion(), event.lockedAttempt().policyVersion())
                || !Objects.equals(
                        attempt.getRubricVersion(), event.lockedAttempt().rubricVersion())) {
            return false;
        }
        ExamAttemptSnapshot lockedSnapshot =
                examAttemptSnapshotRepository.findByAttemptId(attempt.getId()).orElse(null);
        if (lockedSnapshot == null
                || !Objects.equals(
                        lockedSnapshot.getPolicyVersion(), event.lockedAttempt().gradingContext())
                || !Objects.equals(
                        lockedSnapshot.getAnswerVersion(), event.lockedAttempt().answerVersion())
                || !Objects.equals(
                        lockedSnapshot.getPolicyVersion(), event.lockedAttempt().policyVersion())) {
            return false;
        }
        if (event.lockedAttempt().answers() == null) return false;
        List<UserAnswer> expected =
                userAnswerRepository.findByAttemptIdOrderByQuestionIdAsc(attempt.getId());
        if (expected.size() != event.lockedAttempt().answers().size()) return false;
        Set<Integer> seen = new HashSet<>();
        for (LockedAnswer snapshot : event.lockedAttempt().answers()) {
            if (snapshot == null || !seen.add(snapshot.answerId())) return false;
            UserAnswer actual =
                    expected.stream()
                            .filter(value -> Objects.equals(value.getId(), snapshot.answerId()))
                            .findFirst()
                            .orElse(null);
            if (actual == null || !Objects.equals(actual.getQuestionId(), snapshot.questionId()))
                return false;
        }
        return true;
    }

    public void retryFailedGrading(Integer userId, Integer attemptId) {
        List<String> keys =
                transactions.execute(status -> prepareRetryInTransaction(userId, attemptId));
        if (keys == null) return;
        for (String key : keys) ai.retry(key, userId, false);
    }

    private List<String> prepareRetryInTransaction(Integer userId, Integer attemptId) {
        ExamAttempt attempt =
                examAttemptRepository
                        .findByIdAndUserIdForUpdate(attemptId, userId)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền với lượt thi này"));
        if (!"ERROR".equals(attempt.getStatus())) {
            throw new BusinessRuleException("Lượt thi này không có phần chấm lỗi");
        }
        List<AiAssessmentJob> failed =
                aiAssessmentJobRepository
                        .findExamJobsByState(
                                attempt.getUserId(),
                                attemptId,
                                attempt.getAnswerVersion(),
                                attempt.getPolicyVersion(),
                                attempt.getRubricVersion(),
                                AiJobState.ERROR)
                        .stream()
                        .filter(job -> isRetryableExamJob(job, attemptId))
                        .toList();
        if (failed.isEmpty()) {
            // Chấp nhận job được tạo bởi policy/rubric phiên bản cũ;
            // job bất biến vẫn chứa payload đúng để replay.
            failed =
                    aiAssessmentJobRepository
                            .findExamJobsByAttemptAndState(
                                    attempt.getUserId(), attemptId, AiJobState.ERROR)
                            .stream()
                            .filter(
                                    job ->
                                            Objects.equals(
                                                    job.getAnswerVersion(),
                                                    attempt.getAnswerVersion()))
                            .filter(job -> isRetryableExamJob(job, attemptId))
                            .toList();
        }
        if (failed.isEmpty())
            throw new BusinessRuleException("Không tìm thấy tác vụ AI lỗi để thử lại");
        List<String> keys = new ArrayList<>();
        for (AiAssessmentJob job : failed) {
            // Retry do học viên kích hoạt sẽ bắt đầu một cửa sổ retry mới sau khi
            // các lần tự động có giới hạn đã dùng hết.
            job.setRetryCount(0);
            aiAssessmentJobRepository.save(job);
            keys.add(job.getAssessmentKey());
            Question question =
                    userAnswerRepository
                            .findByIdAndAttemptId(job.getAnswerId(), attemptId)
                            .flatMap(answer -> questionRepository.findById(answer.getQuestionId()))
                            .orElse(null);
            if (question != null)
                examSkillResultRepository
                        .findByAttemptIdAndSkill(attemptId, question.getSkill())
                        .ifPresent(
                                result -> {
                                    result.setGradingStatus("WAITING");
                                    examSkillResultRepository.save(result);
                                });
        }
        attempt.setStatus("WAITING_GRADING");
        attempt.setGradingStatus("WAITING");
        examAttemptRepository.save(attempt);
        return keys;
    }

    private BigDecimal aggregateRaw(BigDecimal objective, BigDecimal subjective) {
        if (objective == null) return subjective;
        if (subjective == null) return objective;
        return objective.add(subjective).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal subjectiveRawMax(Integer attemptId, String skill) {
        return examAttemptSnapshotRepository
                .findByAttemptId(attemptId)
                .map(
                        snapshot ->
                                snapshot.getQuestions().stream()
                                        .filter(q -> skill.equalsIgnoreCase(q.getSkill()))
                                        .filter(
                                                q ->
                                                        "WRITING_TEXT".equals(q.getQuestionType())
                                                                || "SPEAKING_AUDIO".equals(
                                                                        q.getQuestionType()))
                                        .flatMap(q -> q.getItems().stream())
                                        .map(
                                                item ->
                                                        item.getPoints() == null
                                                                ? BigDecimal.ONE
                                                                : item.getPoints())
                                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .orElse(BigDecimal.ZERO);
    }

    private long subjectiveAnswers(Integer attemptId, String skill) {
        return userAnswerRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId).stream()
                .map(answer -> questionRepository.findById(answer.getQuestionId()).orElse(null))
                .filter(streamValue -> Objects.nonNull(streamValue))
                .filter(question -> skill.equalsIgnoreCase(question.getSkill()))
                .filter(
                        question ->
                                "WRITING_TEXT".equals(question.getQuestionType())
                                        || "SPEAKING_AUDIO".equals(question.getQuestionType()))
                .count();
    }

    private Optional<AiAssessment> acceptedAssessment(AiAssessmentJob job) {
        return aiAssessmentRepository
                .findFirstByJobIdAndAnswerVersionOrderByIdDesc(job.getId(), job.getAnswerVersion())
                .filter(
                        value ->
                                Objects.equals(value.getAssessmentKey(), job.getAssessmentKey())
                                        && Objects.equals(value.getAnswerId(), job.getAnswerId())
                                        && Objects.equals(
                                                value.getAnswerVersion(), job.getAnswerVersion())
                                        && Objects.equals(
                                                value.getPolicyVersion(), job.getPolicyVersion())
                                        && Objects.equals(
                                                value.getRubricVersion(), job.getRubricVersion()));
    }

    private boolean isCurrentExamCallback(AiAssessmentJob job, AiAssessmentCompletedEvent event) {
        if (job == null || !"EXAM_ANSWER".equals(job.getSourceType())) return false;
        if (!Objects.equals(job.getUserId(), event.userId())
                || !Objects.equals(job.getAttemptId(), event.attemptId())
                || !Objects.equals(job.getAnswerId(), event.answerId())
                || !Objects.equals(job.getSourceId(), event.answerId())
                || !Objects.equals(job.getAnswerVersion(), event.answerVersion())) return false;
        if ("COMPLETED".equalsIgnoreCase(event.state()))
            return job.getState() == AiJobState.COMPLETED;
        if ("ERROR".equalsIgnoreCase(event.state())) return job.getState() == AiJobState.ERROR;
        return false;
    }

    private boolean isSubjectiveAnswerJob(AiAssessmentJob job, Integer attemptId, String skill) {
        if (!"EXAM_ANSWER".equals(job.getSourceType())
                || job.getAnswerId() == null
                || !Objects.equals(job.getSourceId(), job.getAnswerId())) return false;
        return userAnswerRepository
                .findByIdAndAttemptId(job.getAnswerId(), attemptId)
                .flatMap(answer -> questionRepository.findById(answer.getQuestionId()))
                .filter(question -> skill.equalsIgnoreCase(question.getSkill()))
                .filter(
                        question ->
                                "WRITING_TEXT".equals(question.getQuestionType())
                                        || "SPEAKING_AUDIO".equals(question.getQuestionType()))
                .isPresent();
    }

    private boolean isRetryableExamJob(AiAssessmentJob job, Integer attemptId) {
        return isSubjectiveAnswerJob(job, attemptId, job.getSkill());
    }

    private boolean isComplete(ExamSkillResult result) {
        if ("UNSCALED".equals(result.getGradingMethod()))
            return result.getRawScore() != null && "COMPLETED".equals(result.getGradingStatus());
        return result.getRawScore() != null
                && result.getScaleScore() != null
                && (result.getCefrLevel() != null
                        || "BELOW_TARGET".equals(result.getGradingMethod()))
                && (result.getGradingStatus() == null
                        || "COMPLETED".equals(result.getGradingStatus()));
    }

    private boolean hasPolicyError(ExamSkillResult result) {
        if ("UNSCALED".equals(result.getGradingMethod())) return !isComplete(result);
        return "ERROR".equals(result.getGradingStatus())
                || ("COMPLETED".equals(result.getGradingStatus())
                        && (result.getRawScore() == null
                                || result.getScaleScore() == null
                                || (result.getCefrLevel() == null
                                        && !"BELOW_TARGET".equals(result.getGradingMethod()))));
    }

    private MockExam exam(ExamAttempt attempt) {
        return examAttemptSnapshotRepository
                .findByAttemptId(attempt.getId())
                .map(streamValue -> MockExam.fromSnapshot(streamValue))
                .orElseGet(
                        () ->
                                mockExamRepository
                                        .findById(attempt.getMockExamId())
                                        .orElseThrow(
                                                () ->
                                                        new ResourceNotFoundException(
                                                                "Không tìm thấy đề thi")));
    }
}







