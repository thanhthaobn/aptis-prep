package com.aptisprep.service.grading.impl;

import com.aptisprep.dto.AiAssessmentDTO.AssessmentView;
import com.aptisprep.dto.LockedAnswerDTO;
import com.aptisprep.entity.AiAssessment;
import com.aptisprep.entity.AiAssessmentJob;
import com.aptisprep.entity.ExamAttemptEntity;
import com.aptisprep.entity.ExamAttemptSnapshotEntity;
import com.aptisprep.entity.ExamSkillResultEntity;
import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.entity.QuestionEntity;
import com.aptisprep.entity.UserAnswerEntity;
import com.aptisprep.enums.AiAssessmentState;
import com.aptisprep.enums.AiJobState;
import com.aptisprep.event.AiAssessmentCompletedEvent;
import com.aptisprep.event.AttemptCompletedEvent;
import com.aptisprep.event.AttemptSubmittedEvent;
import com.aptisprep.exception.AccessDeniedException;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.AiAssessmentJobRepository;
import com.aptisprep.repository.AiAssessmentRepository;
import com.aptisprep.repository.ExamSkillResultRepository;
import com.aptisprep.service.admin.ExamCatalogService;
import com.aptisprep.service.exam.AttemptOutboxService;
import com.aptisprep.service.exam.ExamGradingPersistenceService;
import com.aptisprep.service.grading.AiAssessmentService;
import com.aptisprep.service.grading.AttemptFinalizationService;
import com.aptisprep.service.scoring.ScoringPolicyService;
import com.aptisprep.service.scoring.ScoringPolicyService.SkillScore;
import com.aptisprep.service.scoring.impl.CheckpointScorePolicy;
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

@Service
public class AttemptFinalizationServiceImpl implements AttemptFinalizationService {
    private final ExamGradingPersistenceService examGrading;
    private final ExamSkillResultRepository examSkillResultRepository;
    private final ExamCatalogService examCatalogService;
    private final AiAssessmentService ai;
    private final AiAssessmentJobRepository aiAssessmentJobRepository;
    private final AiAssessmentRepository aiAssessmentRepository;
    private final ScoringPolicyService scoring;
    private final AttemptOutboxService outbox;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transactions;

    public AttemptFinalizationServiceImpl(
            ExamGradingPersistenceService examGrading,
            ExamSkillResultRepository examSkillResultRepository,
            ExamCatalogService examCatalogService,
            AiAssessmentService ai,
            AiAssessmentJobRepository aiAssessmentJobRepository,
            AiAssessmentRepository aiAssessmentRepository,
            ScoringPolicyService scoring,
            AttemptOutboxService outbox,
            ApplicationEventPublisher events,
            PlatformTransactionManager transactionManager) {
        this.examGrading = examGrading;
        this.examSkillResultRepository = examSkillResultRepository;
        this.examCatalogService = examCatalogService;
        this.ai = ai;
        this.aiAssessmentJobRepository = aiAssessmentJobRepository;
        this.aiAssessmentRepository = aiAssessmentRepository;
        this.scoring = scoring;
        this.outbox = outbox;
        this.events = events;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    @Transactional
    public boolean finalizeIfReady(ExamAttemptEntity attempt, MockExamEntity exam) {
        if ("EXPIRED".equals(attempt.getStatus()) && !"WAITING".equals(attempt.getGradingStatus()))
            return false;
        if ("COMPLETED".equals(attempt.getStatus())
                || "COMPLETED".equals(attempt.getGradingStatus())) return false;
        List<ExamSkillResultEntity> results =
                examSkillResultRepository.findByAttemptIdOrderBySkillAsc(attempt.getId());
        if (results.isEmpty()) return false;
        if (results.stream().anyMatch(this::hasPolicyError)) {
            attempt.setStatus("ERROR");
            attempt.setGradingStatus("ERROR");
            examGrading.saveAttempt(attempt);
            if ("PLACEMENT".equals(exam.getExamType())) publishCompleted(attempt, exam);
            return false;
        }
        if (results.stream().anyMatch(value -> !isComplete(value))) return false;
        Integer schemeId =
                attempt.getScoringSchemeId() == null
                        ? exam.getScoringSchemeId()
                        : attempt.getScoringSchemeId();
        List<SkillScore> scores =
                results.stream()
                        .map(
                                value ->
                                        new SkillScore(
                                                value.getSkill(),
                                                value.getScaleScore(),
                                                value.getCefrLevel()))
                        .toList();
        BigDecimal total = null;
        String overall = null;
        if (schemeId != null
                && ("FULL_MOCK".equals(exam.getExamType())
                        || ("PLACEMENT".equals(exam.getExamType())
                                && "FULL_MOCK".equals(exam.getSkill())))) {
            var summary = scoring.fullMockScore(schemeId, scores);
            total = summary.total();
            overall = summary.cefrLevel();
            for (int i = 0; i < results.size(); i++) {
                if (!"GRAMMAR_VOCABULARY".equals(results.get(i).getSkill()))
                    results.get(i).setCefrLevel(summary.skills().get(i).cefrLevel());
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
        examGrading.saveAttempt(attempt);
        return true;
    }

    @Override
    public void publishCompleted(ExamAttemptEntity attempt, MockExamEntity exam) {
        AttemptCompletedEvent event =
                new AttemptCompletedEvent(attempt.getUserId(), attempt.getId(), exam.getExamType());
        outbox.enqueueCompleted(event);
        events.publishEvent(event);
    }

    @Override
    @Transactional
    public void applyAiAssessment(AiAssessmentCompletedEvent event) {
        if (event == null || event.attemptId() == null || event.answerId() == null) return;
        AiAssessmentJob callbackJob =
                aiAssessmentJobRepository.findByAssessmentKey(event.assessmentKey()).orElse(null);
        if (!isCurrentExamCallback(callbackJob, event)) return;
        ExamAttemptEntity attempt =
                examGrading
                        .findAttemptByIdAndUserIdForUpdate(event.attemptId(), event.userId())
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Bạn không có quyền cập nhật lượt thi này"));
        if ("COMPLETED".equals(attempt.getStatus())) return;
        if (!Objects.equals(event.answerVersion(), attempt.getAnswerVersion())
                || !Objects.equals(callbackJob.getPolicyVersion(), attempt.getPolicyVersion())
                || !Objects.equals(callbackJob.getRubricVersion(), attempt.getRubricVersion()))
            return;
        UserAnswerEntity answer =
                examGrading
                        .findAnswerByIdAndAttemptId(event.answerId(), attempt.getId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy câu trả lời cần chấm"));
        QuestionEntity question =
                examCatalogService
                        .findQuestionById(answer.getQuestionId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy câu hỏi cần chấm"));
        ExamSkillResultEntity result =
                examSkillResultRepository
                        .findByAttemptIdAndSkill(attempt.getId(), question.getSkill())
                        .orElseGet(
                                () -> {
                                    ExamSkillResultEntity value = new ExamSkillResultEntity();
                                    value.setAttemptId(attempt.getId());
                                    value.setSkill(question.getSkill());
                                    return value;
                                });
        AssessmentView assessment = ai.getForGrading(event.assessmentKey());
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
            result.setGradingMethod(
                    result.getObjectiveRawScore() == null ? "AI_ERROR" : "OBJECTIVE_ERROR");
            examSkillResultRepository.save(result);
            attempt.setStatus("ERROR");
            attempt.setGradingStatus("ERROR");
            examGrading.saveAttempt(attempt);
            MockExamEntity failedExam = exam(attempt);
            if ("PLACEMENT".equals(failedExam.getExamType())) publishCompleted(attempt, failedExam);
            return;
        }
        if (assessment.score() == null
                || assessment.score().compareTo(BigDecimal.ZERO) < 0
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
        examGrading.saveAnswer(answer);

        List<AiAssessmentJob> skillJobs =
                aiAssessmentJobRepository
                        .findExamJobs(
                                attempt.getUserId(),
                                attempt.getId(),
                                attempt.getAnswerVersion(),
                                attempt.getPolicyVersion(),
                                attempt.getRubricVersion())
                        .stream()
                        .filter(j -> question.getSkill().equalsIgnoreCase(j.getSkill()))
                        .filter(j -> isSubjectiveAnswerJob(j, attempt.getId(), question.getSkill()))
                        .toList();
        List<BigDecimal> subjectiveScores =
                new ArrayList<>(
                        skillJobs.stream()
                                .map(j -> acceptedAssessment(j).orElse(null))
                                .filter(Objects::nonNull)
                                .filter(
                                        value ->
                                                value.getState() == AiAssessmentState.COMPLETED
                                                        && value.getScore() != null)
                                .map(AiAssessment::getScore)
                                .toList());
        for (UserAnswerEntity saved : examGrading.findAnswersByAttemptId(attempt.getId())) {
            if (!"GRADED".equals(saved.getStatus())
                    || !"RULE".equals(saved.getAiProvider())
                    || !"EMPTY_SUBMISSION".equals(saved.getAiModel())) continue;
            examCatalogService
                    .findQuestionById(saved.getQuestionId())
                    .filter(value -> question.getSkill().equalsIgnoreCase(value.getSkill()))
                    .filter(
                            value ->
                                    "WRITING_TEXT".equals(value.getQuestionType())
                                            || "SPEAKING_AUDIO".equals(value.getQuestionType()))
                    .ifPresent(value -> subjectiveScores.add(BigDecimal.ZERO));
        }
        for (AiAssessmentJob j : skillJobs) {
            acceptedAssessment(j)
                    .filter(
                            value ->
                                    value.getState() == AiAssessmentState.COMPLETED
                                            && value.getScore() != null)
                    .ifPresent(
                            value ->
                                    examGrading
                                            .findAnswerByIdAndAttemptId(
                                                    j.getAnswerId(), attempt.getId())
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
                                                        examGrading.saveAnswer(saved);
                                                    }));
        }
        long expected = subjectiveAnswers(attempt.getId(), question.getSkill());
        if (subjectiveScores.size() < expected) {
            boolean failedAssessment =
                    skillJobs.stream()
                            .anyMatch(
                                    j -> {
                                        if (j.getState() == AiJobState.ERROR) return true;
                                        return acceptedAssessment(j)
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
                examGrading.saveAttempt(attempt);
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
                        .divide(
                                BigDecimal.valueOf(subjectiveScores.size()),
                                2,
                                RoundingMode.HALF_UP);
        result.setSubjectiveRawScore(subjective);
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
        result.setScaleScore(scoring.scale(schemeId, question.getSkill(), raw));
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

    @Override
    @Transactional
    public void markSubjectiveUnavailable(AttemptSubmittedEvent event, String reason) {
        if (event == null || event.lockedAttempt() == null || !accepts(event)) return;
        if (reason == null) return;
        Set<String> unavailableSkills = new HashSet<>();
        for (LockedAnswerDTO answer : event.lockedAttempt().answers()) {
            if (("WRITING_TEXT".equals(answer.questionType())
                            || "SPEAKING_AUDIO".equals(answer.questionType()))
                    && answer.skill() != null) {
                unavailableSkills.add(answer.skill());
            }
        }
        ExamAttemptEntity attempt = examGrading.findAttemptById(event.attemptId()).orElse(null);
        if (attempt == null) return;
        MockExamEntity exam = exam(attempt);
        for (String skill : unavailableSkills) {
            ExamSkillResultEntity result =
                    examSkillResultRepository
                            .findByAttemptIdAndSkill(attempt.getId(), skill)
                            .orElseGet(
                                    () -> {
                                        ExamSkillResultEntity value = new ExamSkillResultEntity();
                                        value.setAttemptId(attempt.getId());
                                        value.setSkill(skill);
                                        return value;
                                    });
            result.setGradingStatus("ERROR");
            result.setGradingMethod("AI_UNAVAILABLE");
            result.setRawScore(null);
            result.setScaleScore(null);
            result.setCefrLevel(null);
            examSkillResultRepository.save(result);
        }
        attempt.setStatus("ERROR");
        attempt.setGradingStatus("ERROR");
        examGrading.saveAttempt(attempt);
        if ("PLACEMENT".equals(exam.getExamType())) publishCompleted(attempt, exam);
    }

    @Override
    public boolean accepts(AttemptSubmittedEvent event) {
        if (event == null
                || event.lockedAttempt() == null
                || !Objects.equals(event.userId(), event.lockedAttempt().userId())
                || !Objects.equals(event.attemptId(), event.lockedAttempt().attemptId())
                || !Objects.equals(event.examId(), event.lockedAttempt().examId())
                || !Objects.equals(event.examType(), event.lockedAttempt().examType())) {
            return false;
        }
        ExamAttemptEntity attempt =
                examGrading
                        .findAttemptByIdAndUserId(event.attemptId(), event.userId())
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
        ExamAttemptSnapshotEntity lockedSnapshot =
                examGrading.findSnapshotByAttemptId(attempt.getId()).orElse(null);
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
        List<UserAnswerEntity> expected = examGrading.findAnswersByAttemptId(attempt.getId());
        if (expected.size() != event.lockedAttempt().answers().size()) return false;
        Set<Integer> seen = new HashSet<>();
        for (LockedAnswerDTO snapshot : event.lockedAttempt().answers()) {
            if (snapshot == null || !seen.add(snapshot.answerId())) return false;
            UserAnswerEntity actual =
                    expected.stream()
                            .filter(value -> Objects.equals(value.getId(), snapshot.answerId()))
                            .findFirst()
                            .orElse(null);
            if (actual == null || !Objects.equals(actual.getQuestionId(), snapshot.questionId()))
                return false;
        }
        return true;
    }

    @Override
    public void retryFailedGrading(Integer userId, Integer attemptId) {
        List<String> keys =
                transactions.execute(status -> prepareRetryInTransaction(userId, attemptId));
        if (keys == null) return;
        for (String key : keys) ai.retry(key, userId, false);
    }

    private List<String> prepareRetryInTransaction(Integer userId, Integer attemptId) {
        ExamAttemptEntity attempt =
                examGrading
                        .findAttemptByIdAndUserIdForUpdate(attemptId, userId)
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
            job.setRetryCount(0);
            aiAssessmentJobRepository.save(job);
            keys.add(job.getAssessmentKey());
            QuestionEntity question =
                    examGrading
                            .findAnswerByIdAndAttemptId(job.getAnswerId(), attemptId)
                            .flatMap(
                                    answer ->
                                            examCatalogService.findQuestionById(
                                                    answer.getQuestionId()))
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
        examGrading.saveAttempt(attempt);
        return keys;
    }

    private BigDecimal aggregateRaw(BigDecimal objective, BigDecimal subjective) {
        if (objective == null) return subjective;
        if (subjective == null) return objective;
        return objective.add(subjective).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    private long subjectiveAnswers(Integer attemptId, String skill) {
        return examGrading.findAnswersByAttemptId(attemptId).stream()
                .map(
                        answer ->
                                examCatalogService
                                        .findQuestionById(answer.getQuestionId())
                                        .orElse(null))
                .filter(Objects::nonNull)
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
        return examGrading
                .findAnswerByIdAndAttemptId(job.getAnswerId(), attemptId)
                .flatMap(answer -> examCatalogService.findQuestionById(answer.getQuestionId()))
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

    private boolean isComplete(ExamSkillResultEntity result) {
        if ("UNSCALED".equals(result.getGradingMethod()))
            return result.getRawScore() != null && "COMPLETED".equals(result.getGradingStatus());
        return result.getRawScore() != null
                && result.getScaleScore() != null
                && (result.getCefrLevel() != null
                        || "BELOW_TARGET".equals(result.getGradingMethod()))
                && (result.getGradingStatus() == null
                        || "COMPLETED".equals(result.getGradingStatus()));
    }

    private boolean hasPolicyError(ExamSkillResultEntity result) {
        if ("UNSCALED".equals(result.getGradingMethod())) return !isComplete(result);
        return "ERROR".equals(result.getGradingStatus())
                || ("COMPLETED".equals(result.getGradingStatus())
                        && (result.getRawScore() == null
                                || result.getScaleScore() == null
                                || (result.getCefrLevel() == null
                                        && !"BELOW_TARGET".equals(result.getGradingMethod()))));
    }

    private MockExamEntity exam(ExamAttemptEntity attempt) {
        return examGrading
                .findSnapshotByAttemptId(attempt.getId())
                .map(MockExamEntity::fromSnapshot)
                .orElseGet(
                        () ->
                                examCatalogService
                                        .findMockExamById(attempt.getMockExamId())
                                        .orElseThrow(
                                                () ->
                                                        new ResourceNotFoundException(
                                                                "Không tìm thấy đề thi")));
    }
}
