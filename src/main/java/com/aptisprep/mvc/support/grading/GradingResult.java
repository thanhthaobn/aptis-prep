package com.aptisprep.mvc.support.grading;

import java.math.BigDecimal;

public record GradingResult(
        BigDecimal estimatedScore,
        String estimatedCefr,
        String feedback,
        String provider,
        String model,
        String transcript) {
    public GradingResult(
            BigDecimal score, String cefr, String feedback, String provider, String model) {
        this(score, cefr, feedback, provider, model, null);
    }
}







