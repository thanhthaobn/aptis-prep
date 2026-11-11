package com.aptisprep.service.grading.impl;

import com.aptisprep.dto.LockedAnswerDTO;
import com.aptisprep.dto.LockedAttemptDTO;
import com.aptisprep.dto.LockedItemDTO;
import com.aptisprep.entity.ExamSkillResultEntity;
import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.entity.UserAnswerDetailEntity;
import com.aptisprep.entity.UserAnswerEntity;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.ExamSkillResultRepository;
import com.aptisprep.service.exam.ExamGradingPersistenceService;
import com.aptisprep.service.grading.ObjectiveGradingService;
import com.aptisprep.service.scoring.ScoringPolicyService;
import com.aptisprep.service.scoring.impl.CheckpointScorePolicy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObjectiveGradingServiceImpl implements ObjectiveGradingService {
    private final ExamGradingPersistenceService examGrading;
    private final ExamSkillResultRepository examSkillResultRepository;
    private final ScoringPolicyService scoring;

    public ObjectiveGradingServiceImpl(
            ExamGradingPersistenceService examGrading,
            ExamSkillResultRepository examSkillResultRepository,
            ScoringPolicyService scoring) {
        this.examGrading = examGrading;
        this.examSkillResultRepository = examSkillResultRepository;
        this.scoring = scoring;
    }

    @Override
    @Transactional
    public void gradeObjective(LockedAttemptDTO locked, MockExamEntity exam) {
        if (locked == null || locked.answers() == null) return;
        Map<String, List<LockedAnswerDTO>> grouped =
                locked.answers().stream()
                        .collect(
                                Collectors.groupingBy(
                                        LockedAnswerDTO::skill,
                                        LinkedHashMap::new,
                                        Collectors.toList()));
        grouped.forEach((skill, skillAnswers) -> gradeSkill(locked, exam, skill, skillAnswers));
    }

    private void gradeSkill(
            LockedAttemptDTO locked,
            MockExamEntity exam,
            String skill,
            List<LockedAnswerDTO> skillAnswers) {
        int total = 0;
        int correct = 0;
        boolean subjective = false;
        int emptySubjective = 0;
        for (LockedAnswerDTO lockedAnswer : skillAnswers) {
            UserAnswerEntity answer =
                    examGrading
                            .findAnswerByIdAndAttemptId(lockedAnswer.answerId(), locked.attemptId())
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
                    examGrading.saveAnswer(answer);
                }
                continue;
            }
            List<LockedItemDTO> lockedItems =
                    lockedAnswer.items() == null ? List.of() : lockedAnswer.items();
            for (LockedItemDTO item : lockedItems) {
                total++;
                UserAnswerDetailEntity detail =
                        examGrading.findAnswerDetail(answer.getId(), item.itemId()).orElse(null);
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

    private boolean isCorrect(String type, LockedItemDTO item, UserAnswerDetailEntity detail) {
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
