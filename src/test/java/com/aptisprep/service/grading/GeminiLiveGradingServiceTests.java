package com.aptisprep.service.grading;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aptisprep.mvc.common.config.AiProperties;
import com.aptisprep.mvc.grading.service.impl.GeminiGradingService;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

/** Opt-in smoke test for a real Gemini provider call; never runs in the normal suite. */
@EnabledIfEnvironmentVariable(named = "APTIS_GEMINI_LIVE_TESTS", matches = "true")
class GeminiLiveGradingServiceTests {
    private static final Set<String> LEVELS = Set.of("A1", "A2", "B1", "B2", "C1", "C2");

    @Test
    void writingUsesTheConfiguredGeminiProvider() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        Assumptions.assumeTrue(
                apiKey != null && !apiKey.isBlank(), "GEMINI_API_KEY is not configured");

        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setApiKey(apiKey);
        String model = System.getenv("GEMINI_GRADING_MODEL");
        if (model != null && !model.isBlank()) properties.setGradingModel(model);

        GeminiGradingService service =
                new GeminiGradingService(
                        properties,
                        RestClient.builder()
                                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                                .build(),
                        tools.jackson.databind.json.JsonMapper.builder().build());
        var result =
                service.grade(
                        "Write a short message to a classmate about a study plan.",
                        "Hi Lan, I will study English for thirty minutes each evening. I will"
                                + " review vocabulary first and then practise writing. Would you like"
                                + " to join me on Monday?");

        assertNotNull(result);
        assertNotNull(result.estimatedScore());
        assertTrue(result.estimatedScore().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(result.estimatedScore().compareTo(BigDecimal.valueOf(50)) <= 0);
        assertTrue(LEVELS.contains(result.estimatedCefr()));
        assertFalse(result.feedback().isBlank());
    }
}
