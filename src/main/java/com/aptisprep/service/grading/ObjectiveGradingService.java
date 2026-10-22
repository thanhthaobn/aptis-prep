package com.aptisprep.service.grading;

import com.aptisprep.entity.ExamSkillResultEntity;
import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.entity.UserAnswerDetailEntity;
import com.aptisprep.entity.UserAnswerEntity;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.ExamSkillResultRepository;
import com.aptisprep.repository.UserAnswerDetailRepository;
import com.aptisprep.repository.UserAnswerRepository;
import com.aptisprep.service.contract.IntegrationModels.LockedAttempt;
import com.aptisprep.service.contract.IntegrationModels.LockedAnswer;
import com.aptisprep.service.contract.IntegrationModels.LockedItem;
import com.aptisprep.service.contract.ScoringPolicyContract;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owner: Thông. Objective grading and raw/scale/CEFR conversion from a server snapshot. */
@Service
public class ObjectiveGradingService {
    private final UserAnswerRepository answers;
    private final UserAnswerDetailRepository details;
    private final ExamSkillResultRepository skillResults;
    private final ScoringPolicyContract scoring;

    public ObjectiveGradingService(UserAnswerRepository answers, UserAnswerDetailRepository details,
            ExamSkillResultRepository skillResults, ScoringPolicyContract scoring) {
        this.answers = answers;
        this.details = details;
        this.skillResults = skillResults;
        this.scoring = scoring;
    }

    @Transactional
    public void gradeObjective(LockedAttempt locked, MockExamEntity exam) {
        if (locked == null || locked.answers() == null) return;
        Map<String, List<LockedAnswer>> grouped = locked.answers().stream()
                .collect(Collectors.groupingBy(LockedAnswer::skill, java.util.LinkedHashMap::new, Collectors.toList()));
        grouped.forEach((skill, skillAnswers) -> gradeSkill(locked, exam, skill, skillAnswers));
    }

    private void gradeSkill(LockedAttempt locked, MockExamEntity exam, String skill,
            List<LockedAnswer> skillAnswers) {
        int total = 0;
        int correct = 0;
        boolean subjective = false;
        for (LockedAnswer lockedAnswer : skillAnswers) {
            UserAnswerEntity answer = answers.findByIdAndAttemptId(lockedAnswer.answerId(), locked.attemptId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy câu trả lời của lượt thi"));
            if (isSubjective(lockedAnswer.questionType())) {
                subjective = true;
                continue;
            }
            List<LockedItem> lockedItems = lockedAnswer.items() == null ? List.of() : lockedAnswer.items();
            for (LockedItem item : lockedItems) {
                total++;
                UserAnswerDetailEntity detail = details
                        .findByUserAnswerIdAndQuestionItemId(answer.getId(), item.itemId()).orElse(null);
                if (isCorrect(lockedAnswer.questionType(), item, detail)) correct++;
            }
        }

        ExamSkillResultEntity result = skillResults.findByAttemptIdAndSkill(locked.attemptId(), skill)
                .orElseGet(() -> {
                    ExamSkillResultEntity value = new ExamSkillResultEntity();
                    value.setAttemptId(locked.attemptId());
                    value.setSkill(skill);
                    return value;
                });
        if (total == 0) {
            result.setCorrectCount(null);
            result.setTotalItems(null);
            result.setObjectiveRawScore(null);
            result.setRawScore(null);
            result.setScaleScore(null);
            result.setCefrLevel(null);
            result.setGradingMethod(subjective ? "PENDING" : "OBJECTIVE_EMPTY");
            result.setGradingStatus(subjective ? "WAITING" : "COMPLETED");
        } else {
            Integer schemeId = locked.scoringSchemeId() == null ? exam.getScoringSchemeId() : locked.scoringSchemeId();
            BigDecimal raw = scoring.raw(schemeId, skill, correct, total);
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
                applyPolicy(result, schemeId, skill, raw);
                if (result.getCefrLevel() == null) {
                    result.setGradingMethod("POLICY_ERROR");
                    result.setGradingStatus("ERROR");
                } else {
                    result.setGradingMethod("OBJECTIVE");
                    result.setGradingStatus("COMPLETED");
                }
            }
        }
        skillResults.save(result);
    }

    private void applyPolicy(ExamSkillResultEntity result, Integer schemeId, String skill, BigDecimal raw) {
        result.setRawScore(raw);
        result.setScaleScore(scoring.scale(schemeId, skill, raw));
        result.setCefrLevel(scoring.estimatedLevel(schemeId, skill, result.getScaleScore()));
    }

    private boolean isCorrect(String type, LockedItem item, UserAnswerDetailEntity detail) {
        if (detail == null) return false;
        if ("ORDERING".equals(type)) return Objects.equals(detail.getUserPosition(), item.correctPosition());
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
