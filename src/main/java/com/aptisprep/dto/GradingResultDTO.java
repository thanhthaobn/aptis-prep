package com.aptisprep.dto;

import java.math.BigDecimal;

public class GradingResultDTO {
    private BigDecimal estimatedScore;
    private String estimatedCefr;
    private String feedback;
    private String provider;
    private String model;
    private String transcript;

    public GradingResultDTO() {}

    public GradingResultDTO(
            BigDecimal estimatedScore,
            String estimatedCefr,
            String feedback,
            String provider,
            String model,
            String transcript) {
        this.estimatedScore = estimatedScore;
        this.estimatedCefr = estimatedCefr;
        this.feedback = feedback;
        this.provider = provider;
        this.model = model;
        this.transcript = transcript;
    }

    public GradingResultDTO(
            BigDecimal score, String cefr, String feedback, String provider, String model) {
        this(score, cefr, feedback, provider, model, null);
    }

    public BigDecimal getEstimatedScore() {
        return estimatedScore;
    }

    public void setEstimatedScore(BigDecimal estimatedScore) {
        this.estimatedScore = estimatedScore;
    }

    public BigDecimal estimatedScore() {
        return estimatedScore;
    }

    public String getEstimatedCefr() {
        return estimatedCefr;
    }

    public void setEstimatedCefr(String estimatedCefr) {
        this.estimatedCefr = estimatedCefr;
    }

    public String estimatedCefr() {
        return estimatedCefr;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String feedback() {
        return feedback;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String provider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String model() {
        return model;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public String transcript() {
        return transcript;
    }
}
