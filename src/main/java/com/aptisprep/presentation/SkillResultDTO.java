package com.aptisprep.presentation;

import java.math.BigDecimal;

public record SkillResultDTO(
        String skill,
        BigDecimal rawScore,
        BigDecimal scaleScore,
        String estimatedLevel,
        String gradingState,
        String feedback,
        boolean passed,
        boolean retryable) {
    public SkillResultDTO(
            String skill,
            BigDecimal rawScore,
            BigDecimal scaleScore,
            String estimatedLevel,
            String gradingState,
            String feedback) {
        this(skill, rawScore, scaleScore, estimatedLevel, gradingState, feedback, false, false);
    }

    public SkillResultDTO(
            String skill,
            BigDecimal rawScore,
            BigDecimal scaleScore,
            String estimatedLevel,
            String gradingState,
            String feedback,
            boolean passed) {
        this(skill, rawScore, scaleScore, estimatedLevel, gradingState, feedback, passed, false);
    }
}
