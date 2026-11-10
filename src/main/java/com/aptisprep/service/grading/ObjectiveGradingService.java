package com.aptisprep.service.grading;

import com.aptisprep.entity.ExamSkillResultEntity;
import com.aptisprep.entity.UserAnswerDetailEntity;
import com.aptisprep.entity.UserAnswerEntity;
import com.aptisprep.mvc.catalog.entity.*;
import com.aptisprep.mvc.catalog.entity.MockExamEntity;
import com.aptisprep.mvc.common.exception.BusinessRuleException;
import com.aptisprep.mvc.common.exception.ResourceNotFoundException;
import com.aptisprep.mvc.scoring.service.ScoringPolicyService;
import com.aptisprep.mvc.scoring.service.impl.CheckpointScorePolicy;
import com.aptisprep.repository.ExamSkillResultRepository;
import com.aptisprep.repository.UserAnswerDetailRepository;
import com.aptisprep.repository.UserAnswerRepository;
import com.aptisprep.service.contract.GradingContextFormatter;
import com.aptisprep.service.contract.IntegrationModels.LockedAnswer;
import com.aptisprep.service.contract.IntegrationModels.LockedAttempt;
import com.aptisprep.service.contract.IntegrationModels.LockedItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owner: Thông. Objective grading and raw/scale/CEFR conversion from a server snapshot. */
@Service
public class ObjectiveGradingService {
    private final UserAnswerRepository userAnswerRepository;
    private final UserAnswerDetailRepository userAnswerDetailRepository;
    private final ExamSkillResultRepository examSkillResultRepository;
    private final ScoringPolicyService scoring;

    public ObjectiveGradingService(
            UserAnswerRepository userAnswerRepository,
            UserAnswerDetailRepository userAnswerDetailRepository,
            ExamSkillResultRepository examSkillResultRepository,
            ScoringPolicyService scoring) {
        this.userAnswerRepository = userAnswerRepository;
        this.userAnswerDetailRepository = userAnswerDetailRepository;
        this.examSkillResultRepository = examSkillResultRepository;
        this.scoring = scoring;
    }

    @Transactional
    public void gradeObjective(LockedAttempt locked, MockExamEntity exam) {
        if (locked == null || locked.answers() == null) return;
        Map<String, List<LockedAnswer>> grouped =
                locked.answers().stream()
                        .collect(
                                Collectors.groupingBy(
                                        LockedAnswer::skill,
                                        LinkedHashMap::new,
                                        Collectors.toList()));
        grouped.forEach((skill, skillAnswers) -> gradeSkill(locked, exam, skill, skillAnswers));
    }

    private void gradeSkill(
            LockedAttempt locked,
            MockExamEntity exam,
            String skill,
            List<LockedAnswer> skillAnswers) {
        int total = 0;
        int correct = 0;
        boolean subjective = false;
        int emptySubjective = 0;
        for (LockedAnswer lockedAnswer : skillAnswers) {
            UserAnswerEntity answer =
                    userAnswerRepository
                            .findByIdAndAttemptId(lockedAnswer.answerId(), locked.attemptId())
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Không tìm thấy câu trả lời của lượt thi"));
            if (isSubjective(lockedAnswer.questionType())) {
                if (GradingContextFormatter.hasSubmission(lockedAnswer)) {
                    subjective = true;
                } else {
                    emptySubjective++;
                    answer.setAiScore(BigDecimal.ZERO);
                    answer.setAiFeedback("Không có câu trả lời khi nộp bài: 0 điểm.");
                    answer.setAiProvider("RULE");
                    answer.setAiModel("EMPTY_SUBMISSION");
                    answer.setGradedAt(LocalDateTime.now());
                    answer.setStatus("GRADED");
                    userAnswerRepository.save(answer);
                }
                continue;
            }
            List<LockedItem> lockedItems =
                    lockedAnswer.items() == null ? List.of() : lockedAnswer.items();
            for (LockedItem item : lockedItems) {
                total++;
                UserAnswerDetailEntity detail =
                        userAnswerDetailRepository
                                .findByUserAnswerIdAndQuestionItemId(answer.getId(), item.itemId())
                                .orElse(null);
                if (isCorrect(lockedAnswer.questionType(), item, detail)) correct++;
            }
        }

        ExamSkillResultEntity result =
                examSkillResultRepository
                        .findByAttemptIdAndSkill(locked.attemptId(), skill)
                        .orElseGet(
                                () -> {
                                    ExamSkillResultEntity value = new ExamSkillResultEntity();
                                    value.setAttemptId(locked.attemptId());
                                    value.setSkill(skill);
                                    return value;
                                });
        if (total == 0 && !subjective && emptySubjective > 0) {
            Integer schemeId =
                    locked.scoringSchemeId() == null
                            ? exam.getScoringSchemeId()
                            : locked.scoringSchemeId();
            result.setCorrectCount(null);
            result.setTotalItems(null);
            result.setObjectiveRawScore(null);
            result.setSubjectiveRawScore(BigDecimal.ZERO);
            applyPolicy(result, schemeId, skill, BigDecimal.ZERO, exam);
            result.setGradingMethod(schemeId == null ? "UNSCALED" : "EMPTY_SUBMISSION");
            result.setGradingStatus("COMPLETED");
            if (schemeId != null && result.getCefrLevel() == null) {
                if (scoring.belowConfiguredTarget(schemeId, skill, result.getScaleScore())) {
                    result.setGradingMethod("BELOW_TARGET");
                } else {
                    result.setGradingMethod("POLICY_ERROR");
                    result.setGradingStatus("ERROR");
                }
            }
        } else if (total == 0) {
            result.setCorrectCount(null);
            result.setTotalItems(null);
            result.setObjectiveRawScore(null);
            result.setRawScore(null);
            result.setScaleScore(null);
            result.setCefrLevel(null);
            result.setGradingMethod(subjective ? "PENDING" : "OBJECTIVE_EMPTY");
            result.setGradingStatus(subjective ? "WAITING" : "COMPLETED");
        } else {
            Integer schemeId =
                    locked.scoringSchemeId() == null
                            ? exam.getScoringSchemeId()
                            : locked.scoringSchemeId();
            BigDecimal raw =
                    schemeId == null
                            ? BigDecimal.valueOf(correct)
                            : scoring.raw(schemeId, skill, correct, total);
            result.setCorrectCount(correct);
            result.setTotalItems(total);
            result.setObjectiveRawScore(raw);
            if (subjective) {
                // Keep the objective component while subjective grading is pending.
                result.setRawScore(null);
                result.setScaleScore(null);
                result.setCefrLevel(null);
                result.setGradingMethod("OBJECTIVE_PENDING");
                result.setGradingStatus("WAITING");
            } else {
                applyPolicy(result, schemeId, skill, raw, exam);
                if (schemeId == null) {
                    result.setGradingMethod("UNSCALED");
                    result.setGradingStatus("COMPLETED");
                } else if (result.getCefrLevel() == null
                        && scoring.belowConfiguredTarget(schemeId, skill, result.getScaleScore())) {
                    result.setGradingMethod("BELOW_TARGET");
                    result.setGradingStatus("COMPLETED");
                } else if (result.getCefrLevel() == null) {
                    result.setGradingMethod("POLICY_ERROR");
                    result.setGradingStatus("ERROR");
                } else {
                    result.setGradingMethod("OBJECTIVE");
                    result.setGradingStatus("COMPLETED");
                }
            }
        }
        examSkillResultRepository.save(result);
    }

    private void applyPolicy(
            ExamSkillResultEntity result,
            Integer schemeId,
            String skill,
            BigDecimal raw,
            MockExamEntity exam) {
        result.setRawScore(raw);
        if (schemeId == null) {
            result.setScaleScore(null);
            result.setCefrLevel(null);
            return;
        }
        try {
            result.setScaleScore(scoring.scale(schemeId, skill, raw));
            result.setCefrLevel(
                    CheckpointScorePolicy.confirmedLevel(
                            exam.getExamType(),
                            exam.getTargetLevel(),
                            scoring.estimatedLevel(schemeId, skill, result.getScaleScore())));
        } catch (BusinessRuleException exception) {
            if (!"GRAMMAR_VOCABULARY".equals(skill)) throw exception;
            // Older schemes may contain the Grammar raw-scale rule but no CEFR bands.
            // Keep the objective result usable with the approved internal A1-C1 estimate.
            result.setScaleScore(raw.setScale(2, java.math.RoundingMode.HALF_UP));
            result.setCefrLevel(
                    CheckpointScorePolicy.confirmedLevel(
                            exam.getExamType(),
                            exam.getTargetLevel(),
                            grammarLevel(result.getScaleScore())));
        }
    }

    private String grammarLevel(BigDecimal score) {
        if (score == null) return null;
        if (score.compareTo(BigDecimal.valueOf(10)) < 0) return "A1";
        if (score.compareTo(BigDecimal.valueOf(20)) < 0) return "A2";
        if (score.compareTo(BigDecimal.valueOf(30)) < 0) return "B1";
        if (score.compareTo(BigDecimal.valueOf(40)) < 0) return "B2";
        return "C1";
    }

    private boolean isCorrect(String type, LockedItem item, UserAnswerDetailEntity detail) {
        if (detail == null) return false;
        if ("ORDERING".equals(type))
            return detail.getUserPosition() != null
                    && item.correctPosition() != null
                    && Objects.equals(detail.getUserPosition(), item.correctPosition());
        if ("SINGLE_CHOICE".equals(type) || "MATCHING".equals(type)) {
            return detail.getSelectedOptionId() != null
                    && Objects.equals(detail.getSelectedOptionId(), item.correctOptionId());
        }
        return false;
    }

    private boolean isSubjective(String type) {
        return "WRITING_TEXT".equals(type) || "SPEAKING_AUDIO".equals(type);
    }
}
