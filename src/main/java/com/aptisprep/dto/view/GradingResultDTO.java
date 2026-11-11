package com.aptisprep.dto.view;

import java.math.BigDecimal;

public record GradingResultDTO(
        BigDecimal estimatedScore,
        String estimatedCefr,
        String feedback,
        String provider,
        String model,
        String transcript) {
    public GradingResultDTO(
            BigDecimal score, String cefr, String feedback, String provider, String model) {
        this(score, cefr, feedback, provider, model, null);
    }
}
