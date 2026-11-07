package com.aptisprep.service.contract;

import java.math.BigDecimal;
import java.util.List;

/** Owner: Thông. Backend-only, versioned policy; not a universal percent formula. */
public interface ScoringPolicyContract {
    BigDecimal raw(Integer schemeId, String skill, int correct, int total);

    BigDecimal scale(Integer schemeId, String skill, BigDecimal rawScore);

    String estimatedLevel(Integer schemeId, String skill, BigDecimal scaleScore);

    String policyVersion(Integer schemeId);

    void validatePolicy(Integer schemeId, String skill);

    BigDecimal overallScale(Integer schemeId, List<SkillScore> skillScores);

    String overallLevel(Integer schemeId, List<SkillScore> skillScores);

    FullMockScore fullMockScore(Integer schemeId, List<SkillScore> skillScores);

    record FullMockScore(BigDecimal total, String cefrLevel, List<SkillScore> skills) {}

    record SkillScore(String skill, BigDecimal scaleScore, String cefrLevel) {}
}
