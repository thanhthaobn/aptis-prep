package com.aptisprep.service.grading;

import com.aptisprep.entity.AiAssessmentJob;
import com.aptisprep.entity.ExamAttemptEntity;
import com.aptisprep.entity.ExamSkillResultEntity;
import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.entity.QuestionEntity;
import com.aptisprep.entity.UserAnswerEntity;
import com.aptisprep.enums.AiJobState;
import com.aptisprep.event.AiAssessmentCompletedEvent;
import com.aptisprep.event.AttemptSubmittedEvent;
import com.aptisprep.event.AttemptCompletedEvent;
import com.aptisprep.exception.AccessDeniedException;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.AiAssessmentJobRepository;
import com.aptisprep.repository.ExamAttemptRepository;
import com.aptisprep.repository.ExamSkillResultRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.repository.QuestionRepository;
import com.aptisprep.repository.ExamAttemptSnapshotRepository;
import com.aptisprep.repository.UserAnswerRepository;
import com.aptisprep.service.exam.AttemptOutboxService;
import com.aptisprep.service.contract.AiAssessmentContract;
import com.aptisprep.service.contract.ScoringPolicyContract;
import com.aptisprep.service.contract.ScoringPolicyContract.SkillScore;
import com.aptisprep.service.contract.IntegrationModels.LockedAnswer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owner: Thông. Combines accepted grading components and owns the final attempt state. */
@Service
public class AttemptFinalizationService {
    private final ExamAttemptRepository attempts;
    private final ExamSkillResultRepository skillResults;
    private final MockExamRepository exams;
    private final UserAnswerRepository answers;
    private final QuestionRepository questions;
    private final ExamAttemptSnapshotRepository snapshots;
    private final AiAssessmentContract ai;
    private final AiAssessmentJobRepository jobs;
    private final ScoringPolicyContract scoring;
    private final AttemptOutboxService outbox;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transactions;

    public AttemptFinalizationService(ExamAttemptRepository attempts, ExamSkillResultRepository skillResults,
            MockExamRepository exams, UserAnswerRepository answers, QuestionRepository questions,
            ExamAttemptSnapshotRepository snapshots, AiAssessmentContract ai,
            AiAssessmentJobRepository jobs, ScoringPolicyContract scoring, AttemptOutboxService outbox,
            ApplicationEventPublisher events,
            PlatformTransactionManager transactionManager) {
        this.attempts = attempts;
        this.skillResults = skillResults;
        this.exams = exams;
        this.answers = answers;
        this.questions = questions;
        this.snapshots = snapshots;
        this.ai = ai;
        this.jobs = jobs;
        this.scoring = scoring;
        this.outbox = outbox;
        this.events = events;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public boolean finalizeIfReady(ExamAttemptEntity attempt, MockExamEntity exam) {
        if ("COMPLETED".equals(attempt.getStatus())) return false;
        List<ExamSkillResultEntity> results = skillResults.findByAttemptIdOrderBySkillAsc(attempt.getId());
        if (results.isEmpty()) return false;
        if (results.stream().anyMatch(this::hasPolicyError)) {
            attempt.setStatus("ERROR");
            attempt.setGradingStatus("ERROR");
            attempts.save(attempt);
            return false;
        }
        if (results.stream().anyMatch(value -> !isComplete(value))) return false;
        Integer schemeId = attempt.getScoringSchemeId() == null ? exam.getScoringSchemeId() : attempt.getScoringSchemeId();
        List<SkillScore> scores = results.stream()
                .map(value -> new SkillScore(value.getSkill(), value.getScaleScore(), value.getCefrLevel())).toList();
        BigDecimal average = scoring.overallScale(schemeId, scores);
        String overall = scoring.overallLevel(schemeId, scores);
        attempt.setFinalScaleScore(average);
        attempt.setEstimatedOverallCefr(overall);
        attempt.setCompletedAt(LocalDateTime.now());
        attempt.setGradingStatus("COMPLETED");
        attempt.setStatus("EXPIRED".equals(attempt.getStatus()) ? "EXPIRED" : "COMPLETED");
        attempts.save(attempt);
        return true;
    }

    public void publishCompleted(ExamAttemptEntity attempt, MockExamEntity exam) {
        AttemptCompletedEvent event = new AttemptCompletedEvent(attempt.getUserId(), attempt.getId(), exam.getExamType());
        outbox.enqueueCompleted(event);
        events.publishEvent(event);
    }

    @Transactional
    public void applyAiAssessment(AiAssessmentCompletedEvent event) {
        ExamAttemptEntity attempt = attempts.findByIdAndUserIdForUpdate(event.attemptId(), event.userId())
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền cập nhật lượt thi này"));
        if ("COMPLETED".equals(attempt.getStatus())) return;
        if (!Objects.equals(event.answerVersion(), attempt.getAnswerVersion())) return;
        UserAnswerEntity answer = answers.findByIdAndAttemptId(event.answerId(), attempt.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu trả lời cần chấm"));
        QuestionEntity question = questions.findById(answer.getQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu hỏi cần chấm"));
        ExamSkillResultEntity result = skillResults.findByAttemptIdAndSkill(attempt.getId(), question.getSkill())
                .orElseGet(() -> {
                    ExamSkillResultEntity value = new ExamSkillResultEntity();
                    value.setAttemptId(attempt.getId());
                    value.setSkill(question.getSkill());
                    return value;
                });
        AiAssessmentContract.AssessmentView assessment = ai.getForGrading(event.assessmentKey());
        if (assessment == null
                || !Objects.equals(assessment.assessmentKey(), event.assessmentKey())
                || !Objects.equals(assessment.answerId(), event.answerId())
                || !Objects.equals(assessment.answerVersion(), attempt.getAnswerVersion())
                || !Objects.equals(assessment.policyVersion(), attempt.getPolicyVersion())
                || !Objects.equals(assessment.rubricVersion(), attempt.getRubricVersion())) {
            return;
        }
        if (!"COMPLETED".equalsIgnoreCase(event.state())) {
            result.setGradingStatus("ERROR");
            result.setGradingMethod(result.getObjectiveRawScore() == null ? "AI_ERROR" : "OBJECTIVE_ERROR");
            skillResults.save(result);
            attempt.setStatus("ERROR");
            attempt.setGradingStatus("ERROR");
            attempts.save(attempt);
            return;
        }
        if (assessment.score() == null || assessment.score().compareTo(BigDecimal.ZERO) < 0
                || assessment.score().compareTo(BigDecimal.valueOf(50)) > 0) {
            return;
        }
        answer.setAiScore(assessment.score());
        answer.setAiFeedback(assessment.feedback());
        answer.setAiProvider(assessment.provider());
        answer.setAiModel(assessment.model());
        answer.setGradedAt(LocalDateTime.now());
        answer.setStatus("GRADED");
        if (assessment.transcript() != null) answer.setTranscript(assessment.transcript());
        answers.save(answer);

        result.setSubjectiveRawScore(assessment.score());
        BigDecimal raw = aggregateRaw(result.getObjectiveRawScore(), assessment.score());
        result.setRawScore(raw);
        Integer schemeId = attempt.getScoringSchemeId() == null ? exam(attempt).getScoringSchemeId() : attempt.getScoringSchemeId();
        result.setScaleScore(scoring.scale(schemeId, question.getSkill(), raw));
        result.setCefrLevel(com.aptisprep.service.scoring.CheckpointScorePolicy.confirmedLevel(
                exam(attempt).getExamType(), exam(attempt).getTargetLevel(),
                scoring.estimatedLevel(schemeId, question.getSkill(), result.getScaleScore())));
        if (result.getCefrLevel() == null) {
            result.setGradingMethod("POLICY_ERROR");
            result.setGradingStatus("ERROR");
        } else {
            result.setGradingMethod(result.getObjectiveRawScore() == null ? "AI" : "OBJECTIVE_AI");
            result.setGradingStatus("COMPLETED");
        }
        skillResults.save(result);
        if (finalizeIfReady(attempt, exam(attempt))) publishCompleted(attempt, exam(attempt));
    }

    /** Rejects stale, cross-attempt or client-shaped submission snapshots before AI work starts. */
    @Transactional(readOnly = true)
    public boolean accepts(AttemptSubmittedEvent event) {
        if (event == null || event.lockedAttempt() == null
                || !Objects.equals(event.userId(), event.lockedAttempt().userId())
                || !Objects.equals(event.attemptId(), event.lockedAttempt().attemptId())
                || !Objects.equals(event.examId(), event.lockedAttempt().examId())
                || !Objects.equals(event.examType(), event.lockedAttempt().examType())) {
            return false;
        }
        ExamAttemptEntity attempt = attempts.findByIdAndUserId(event.attemptId(), event.userId()).orElse(null);
        if (attempt == null || "COMPLETED".equals(attempt.getStatus()) || "ERROR".equals(attempt.getStatus())) return false;
        if (!"SUBMITTED".equals(attempt.getStatus()) && !"WAITING_GRADING".equals(attempt.getStatus())
                && !("EXPIRED".equals(attempt.getStatus()) && "WAITING".equals(attempt.getGradingStatus()))) return false;
        if (!Objects.equals(attempt.getMockExamId(), event.lockedAttempt().examId())
                || !Objects.equals(attempt.getAnswerVersion(), event.lockedAttempt().answerVersion())
                || !Objects.equals(attempt.getGradingRequestKey(), event.lockedAttempt().gradingRequestKey())
                || !Objects.equals(attempt.getScoringSchemeId(), event.lockedAttempt().scoringSchemeId())
                || !Objects.equals(attempt.getPolicyVersion(), event.lockedAttempt().policyVersion())
                || !Objects.equals(attempt.getRubricVersion(), event.lockedAttempt().rubricVersion())) {
            return false;
        }
        var lockedSnapshot = snapshots.findByAttemptId(attempt.getId()).orElse(null);
        if (lockedSnapshot == null || !Objects.equals(lockedSnapshot.getGradingContext(), event.lockedAttempt().gradingContext())
                || !Objects.equals(lockedSnapshot.getAnswerVersion(), event.lockedAttempt().answerVersion())
                || !Objects.equals(lockedSnapshot.getPolicyVersion(), event.lockedAttempt().policyVersion())) {
            return false;
        }
        if (event.lockedAttempt().answers() == null) return false;
        List<UserAnswerEntity> expected = answers.findByAttemptIdOrderByQuestionIdAsc(attempt.getId());
        if (expected.size() != event.lockedAttempt().answers().size()) return false;
        Set<Integer> seen = new HashSet<>();
        for (LockedAnswer snapshot : event.lockedAttempt().answers()) {
            if (snapshot == null || !seen.add(snapshot.answerId())) return false;
            UserAnswerEntity actual = expected.stream().filter(value -> Objects.equals(value.getId(), snapshot.answerId())).findFirst().orElse(null);
            if (actual == null || !Objects.equals(actual.getQuestionId(), snapshot.questionId())) return false;
        }
        return true;
    }

    public void retryFailedGrading(Integer userId, Integer attemptId) {
        List<String> keys = transactions.execute(status -> prepareRetryInTransaction(userId, attemptId));
        if (keys == null) return;
        for (String key : keys) ai.retry(key, userId, false);
    }

    private List<String> prepareRetryInTransaction(Integer userId, Integer attemptId) {
        ExamAttemptEntity attempt = attempts.findByIdAndUserIdForUpdate(attemptId, userId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền với lượt thi này"));
        if (!"ERROR".equals(attempt.getStatus())) {
            throw new BusinessRuleException("Lượt thi này không có phần chấm lỗi");
        }
        List<AiAssessmentJob> failed = jobs.findByAttemptIdAndState(attemptId, AiJobState.ERROR);
        if (failed.isEmpty()) throw new BusinessRuleException("Không tìm thấy tác vụ AI lỗi để thử lại");
        List<String> keys = new ArrayList<>();
        for (AiAssessmentJob job : failed) {
            keys.add(job.getAssessmentKey());
            QuestionEntity question = answers.findByIdAndAttemptId(job.getAnswerId(), attemptId)
                    .flatMap(answer -> questions.findById(answer.getQuestionId())).orElse(null);
            if (question != null) skillResults.findByAttemptIdAndSkill(attemptId, question.getSkill()).ifPresent(result -> {
                result.setGradingStatus("WAITING");
                skillResults.save(result);
            });
        }
        attempt.setStatus("WAITING_GRADING");
        attempt.setGradingStatus("WAITING");
        attempts.save(attempt);
        return keys;
    }

    private BigDecimal aggregateRaw(BigDecimal objective, BigDecimal subjective) {
        if (objective == null) return subjective;
        if (subjective == null) return objective;
        return objective.add(subjective).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    private boolean isComplete(ExamSkillResultEntity result) {
        return result.getRawScore() != null
                && result.getScaleScore() != null
                && result.getCefrLevel() != null
                && (result.getGradingStatus() == null || "COMPLETED".equals(result.getGradingStatus()));
    }

    private boolean hasPolicyError(ExamSkillResultEntity result) {
        return "ERROR".equals(result.getGradingStatus())
                || ("COMPLETED".equals(result.getGradingStatus())
                        && (result.getRawScore() == null || result.getScaleScore() == null || result.getCefrLevel() == null));
    }

    private MockExamEntity exam(ExamAttemptEntity attempt) {
        return exams.findById(attempt.getMockExamId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
    }

}
