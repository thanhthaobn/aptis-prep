package com.aptisprep.service.grading;

import com.aptisprep.config.AiProperties;
import com.aptisprep.exception.AiGradingException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GeminiGradingServiceTests {
    private final AiProperties properties = new AiProperties();
    private MockRestServiceServer server;
    private GeminiGradingService service;

    @BeforeEach
    void setup() {
        properties.setEnabled(true);
        properties.setApiKey("test-key-not-real");
        var builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new GeminiGradingService(properties, builder.build());
    }

    private String response(String grade) {
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":"
                + JsonMapper.builder().build().writeValueAsString(grade) + "}]}}]}";
    }

    @Test
    void writingUsesGeminiStructuredOutputAndReturnsValidatedEstimate() {
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key-not-real"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("responseMimeType")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("responseSchema")))
                .andRespond(withSuccess(response("{\"score\":32.5,\"cefr\":\"B1\",\"feedback\":\"Cần cải thiện ngữ pháp.\",\"transcript\":\"\"}"), MediaType.APPLICATION_JSON));
        var result = service.grade("Write about your hometown", "My hometown is a quiet town.");
        assertEquals(new BigDecimal("32.5"), result.estimatedScore());
        assertEquals("B1", result.estimatedCefr());
        assertEquals("Gemini", result.provider());
        assertNull(result.transcript());
        server.verify();
    }

    @Test
    void missingKeyDoesNotCallProvider() {
        properties.setApiKey("");
        assertThrows(AiGradingException.class, () -> service.grade("Task", "Answer"));
        server.verify();
    }

    @Test
    void disabledDoesNotCallProvider() {
        properties.setEnabled(false);
        assertThrows(AiGradingException.class, () -> service.grade("Task", "Answer"));
        server.verify();
    }

    @Test
    void outOfRangeScoreIsRejected() {
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"))
                .andRespond(withSuccess(response("{\"score\":80,\"cefr\":\"B2\",\"feedback\":\"x\",\"transcript\":\"\"}"), MediaType.APPLICATION_JSON));
        assertThrows(AiGradingException.class, () -> service.grade("Task", "Answer"));
        server.verify();
    }

    @Test
    void blockedResponseDoesNotBecomeScore() {
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"))
                .andRespond(withSuccess("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}", MediaType.APPLICATION_JSON));
        assertThrows(AiGradingException.class, () -> service.grade("Task", "Answer"));
        server.verify();
    }

    @Test
    void quotaFailureDoesNotExposeResponseOrInventScore() {
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("PRIVATE PROVIDER DETAILS"));
        var exception = assertThrows(AiGradingException.class, () -> service.grade("Task", "Answer"));
        assertFalse(exception.getMessage().contains("PRIVATE"));
        server.verify();
    }

    @Test
    void speakingSendsAudioInlineAndReturnsTranscript(@TempDir Path directory) throws Exception {
        Path audio = directory.resolve("sample.wav");
        Files.write(audio, new byte[]{82, 73, 70, 70});
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("inlineData")))
                .andRespond(withSuccess(response("{\"score\":30,\"cefr\":\"B1\",\"feedback\":\"Đánh giá nội dung, chưa đánh giá phát âm.\",\"transcript\":\"I enjoy reading books.\"}"), MediaType.APPLICATION_JSON));
        var result = service.grade("Talk about hobbies", audio);
        assertEquals("I enjoy reading books.", result.transcript());
        server.verify();
    }

    @Test
    void invalidAudioIsRejectedBeforeNetwork(@TempDir Path directory) throws Exception {
        Path audio = directory.resolve("file.exe");
        Files.writeString(audio, "not audio");
        assertThrows(AiGradingException.class, () -> service.grade("Task", audio));
        server.verify();
    }
}
