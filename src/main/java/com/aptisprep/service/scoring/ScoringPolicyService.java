package com.aptisprep.service.scoring;

import java.math.BigDecimal;
import java.util.List;

/** Owner: Thông. Backend-only, versioned policy; not a universal percent formula. */
public interface ScoringPolicyService {
    public BigDecimal raw(Integer schemeId, String skill, int correct, int total);

    public BigDecimal scale(Integer schemeId, String skill, BigDecimal rawScore);

    public String estimatedLevel(Integer schemeId, String skill, BigDecimal scaleScore);

    public boolean belowConfiguredTarget(Integer schemeId, String skill, BigDecimal scaleScore);

    public String policyVersion(Integer schemeId);

    public void validatePolicy(Integer schemeId, String skill);

    public BigDecimal overallScale(Integer schemeId, List<SkillScore> skillScores);

    public String overallLevel(Integer schemeId, List<SkillScore> skillScores);

    public FullMockScore fullMockScore(Integer schemeId, List<SkillScore> skillScores);

    public record FullMockScore(BigDecimal total, String cefrLevel, List<SkillScore> skills) {}

    public record SkillScore(String skill, BigDecimal scaleScore, String cefrLevel) {}
}
