package com.aptisprep.mvc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.ai")
public class AiProperties {
    private boolean enabled;
    private String apiKey = "";
    private String gradingModel = "gemini-3.5-flash-lite";

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
}
