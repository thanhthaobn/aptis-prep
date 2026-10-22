package com.aptisprep.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.ai")
public class AiProperties {
    private boolean enabled;
    private String apiKey = "";
    private String gradingModel = "gpt-4.1-mini";
    private String transcriptionModel = "gpt-4o-mini-transcribe";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getGradingModel() {
        return gradingModel;
    }

    public void setGradingModel(String gradingModel) {
        this.gradingModel = gradingModel;
    }

    public String getTranscriptionModel() {
        return transcriptionModel;
    }

    public void setTranscriptionModel(String transcriptionModel) {
        this.transcriptionModel = transcriptionModel;
    }
}
