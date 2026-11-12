package com.aptisprep.mvc.service.grading.impl;

import com.aptisprep.mvc.entity.ExamSkillResult;
import com.aptisprep.mvc.entity.MockExam;
import com.aptisprep.mvc.entity.UserAnswer;
import com.aptisprep.mvc.entity.UserAnswerDetail;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.exception.ResourceNotFoundException;
import com.aptisprep.mvc.repository.ExamSkillResultRepository;
import com.aptisprep.mvc.repository.UserAnswerDetailRepository;
import com.aptisprep.mvc.repository.UserAnswerRepository;
import com.aptisprep.mvc.service.grading.*;
import com.aptisprep.mvc.service.grading.ObjectiveGradingService;
import com.aptisprep.mvc.service.scoring.ScoringPolicyService;
import com.aptisprep.mvc.support.contract.*;
import com.aptisprep.mvc.support.contract.GradingContextFormatter;
import com.aptisprep.mvc.support.contract.IntegrationModels.LockedAnswer;
import com.aptisprep.mvc.support.contract.IntegrationModels.LockedAttempt;
import com.aptisprep.mvc.support.contract.IntegrationModels.LockedItem;
import com.aptisprep.mvc.support.scoring.*;
import com.aptisprep.mvc.support.scoring.CheckpointScorePolicy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Người phụ trách: Thông. Chấm objective và đổi raw/scale/CEFR từ snapshot phía server. */
@Service
public class ObjectiveGradingServiceImpl implements ObjectiveGradingService {
    private UserAnswerRepository userAnswerRepository;
    private UserAnswerDetailRepository userAnswerDetailRepository;
    private ExamSkillResultRepository examSkillResultRepository;
    private ScoringPolicyService scoring;

    public ObjectiveGradingServiceImpl(
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
    public void gradeObjective(LockedAttempt locked, MockExam exam) {
        if (locked == null || locked.answers() == null) return;
        Map<String, List<LockedAnswer>> grouped =
                locked.answers().stream()
                        .collect(
                                Collectors.groupingBy(
                                        (LockedAnswer streamValue) -> streamValue.skill(),
                                        () -> new LinkedHashMap<String, List<LockedAnswer>>(),
                                        Collectors.toList()));
        grouped.forEach((skill, skillAnswers) -> gradeSkill(locked, exam, skill, skillAnswers));
    }

    private void gradeSkill(
            LockedAttempt locked, MockExam exam, String skill, List<LockedAnswer> skillAnswers) {
        int total = 0;
        int correct = 0;
        BigDecimal totalPoints = BigDecimal.ZERO;
        BigDecimal earnedPoints = BigDecimal.ZERO;
        boolean subjective = false;
        int emptySubjective = 0;
        for (LockedAnswer lockedAnswer : skillAnswers) {
            UserAnswer answer =
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
                BigDecimal itemPoints = item.points() == null ? BigDecimal.ONE : item.points();
                if (itemPoints.signum() <= 0)
                    throw new BusinessRuleException("Điểm câu hỏi trong snapshot không hợp lệ");
                totalPoints = totalPoints.add(itemPoints);
                UserAnswerDetail detail =
                        userAnswerDetailRepository
                                .findByUserAnswerIdAndQuestionItemId(answer.getId(), item.itemId())
                                .orElse(null);
                if (isCorrect(lockedAnswer.questionType(), item, detail)) {
                    correct++;
                    earnedPoints = earnedPoints.add(itemPoints);
                }
            }
        }

        ExamSkillResult result =
                examSkillResultRepository
                        .findByAttemptIdAndSkill(locked.attemptId(), skill)
                        .orElseGet(
                                () -> {
                                    ExamSkillResult value = new ExamSkillResult();
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
            result.setRawMax(BigDecimal.ZERO);
            result.setObjectiveRawScore(null);
            result.setSubjectiveRawScore(BigDecimal.ZERO);
            applyPolicy(result, schemeId, skill, BigDecimal.ZERO, BigDecimal.ZERO, exam);
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
            result.setRawMax(BigDecimal.ZERO);
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
                            ? earnedPoints
                            : scoring.raw(schemeId, skill, earnedPoints, totalPoints);
            result.setCorrectCount(correct);
            result.setTotalItems(total);
            result.setRawMax(totalPoints);
            result.setObjectiveRawScore(raw);
            if (subjective) {
                // Giữ lại thành phần objective trong lúc chấm subjective chưa hoàn tất.
                result.setRawScore(null);
                result.setScaleScore(null);
                result.setCefrLevel(null);
                result.setGradingMethod("OBJECTIVE_PENDING");
                result.setGradingStatus("WAITING");
            } else {
                applyPolicy(result, schemeId, skill, raw, totalPoints, exam);
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
            ExamSkillResult result,
            Integer schemeId,
            String skill,
            BigDecimal raw,
            BigDecimal rawMax,
            MockExam exam) {
        result.setRawScore(raw);
        if (schemeId == null) {
            result.setScaleScore(null);
            result.setCefrLevel(null);
            return;
        }
        try {
            result.setScaleScore(
                    rawMax != null && rawMax.signum() > 0
                            ? scoring.scale(schemeId, skill, raw, rawMax)
                            : scoring.scale(schemeId, skill, raw));
            result.setCefrLevel(
                    CheckpointScorePolicy.confirmedLevel(
                            exam.getExamType(),
                            exam.getTargetLevel(),
                            scoring.estimatedLevel(schemeId, skill, result.getScaleScore()),
                            result.getScaleScore()));
        } catch (BusinessRuleException exception) {
            if (!"GRAMMAR_VOCABULARY".equals(skill)) throw exception;
            // Scheme cũ có thể có rule raw-scale cho Grammar nhưng chưa có CEFR band.
            // Giữ kết quả objective vẫn dùng được với mức ước lượng A1-C1 nội bộ đã duyệt.
            result.setScaleScore(raw.setScale(2, java.math.RoundingMode.HALF_UP));
            result.setCefrLevel(
                    CheckpointScorePolicy.confirmedLevel(
                            exam.getExamType(),
                            exam.getTargetLevel(),
                            grammarLevel(result.getScaleScore()),
                            result.getScaleScore()));
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

    private boolean isCorrect(String type, LockedItem item, UserAnswerDetail detail) {
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
