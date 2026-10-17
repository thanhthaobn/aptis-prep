package com.aptisprep.service.contract;

import java.math.BigDecimal;

/** Owner 1. Backend-only, versioned policy. Not a universal percent formula. */
public interface ScoringPolicyContract {
    BigDecimal scale(Integer schemeId, String skill, BigDecimal rawScore);
    String estimatedLevel(Integer schemeId, String skill, BigDecimal scaleScore);
}
