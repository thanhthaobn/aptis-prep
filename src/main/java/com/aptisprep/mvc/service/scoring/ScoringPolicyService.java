package com.aptisprep.mvc.service.scoring;

import com.aptisprep.mvc.dto.ExamAttemptDTO;
import com.aptisprep.mvc.dto.ExamSkillResultDTO;
import java.math.BigDecimal;
import java.util.List;

public interface ScoringPolicyService {
    public BigDecimal raw(Integer schemeId, String skill, int correct, int total);

    public BigDecimal raw(
            Integer schemeId, String skill, BigDecimal earnedRaw, BigDecimal rawMax);

    public BigDecimal scale(Integer schemeId, String skill, BigDecimal rawScore);

    public BigDecimal scale(
            Integer schemeId, String skill, BigDecimal rawScore, BigDecimal rawMax);

    public String estimatedLevel(Integer schemeId, String skill, BigDecimal scaleScore);

    public boolean belowConfiguredTarget(Integer schemeId, String skill, BigDecimal scaleScore);

    public String policyVersion(Integer schemeId);

    public void validatePolicy(Integer schemeId, String skill);

    public BigDecimal overallScale(
            Integer schemeId, List<ExamSkillResultDTO> skillResults);

    public String overallLevel(
            Integer schemeId, List<ExamSkillResultDTO> skillResults);

    public ExamAttemptDTO fullMockScore(
            Integer schemeId, List<ExamSkillResultDTO> skillResults);
}







