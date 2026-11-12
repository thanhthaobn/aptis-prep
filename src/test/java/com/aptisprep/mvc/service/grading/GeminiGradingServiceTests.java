package com.aptisprep.mvc.service.grading;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.aptisprep.mvc.config.AiProperties;
import com.aptisprep.mvc.exception.AiGradingException;
import com.aptisprep.mvc.service.grading.impl.GeminiGradingServiceImpl;
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

class GeminiGradingServiceTests {
    @Test
    void twoElevenThousandCharacterAnswersReachGeminiWithoutTruncation() {
        String submission = "Mục 1:\n" + "a".repeat(11_000) + "\n\nMục 2:\n" + "b".repeat(11_000);
        server.expect(method(HttpMethod.POST))
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("a".repeat(11_000))))
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("b".repeat(11_000))))
                .andRespond(
                        withSuccess(
                                response(
                                        "{\"score\":30,\"cefr\":\"B1\",\"feedback\":\"Đã đọc đủ hai"
                                                + " mục.\",\"transcript\":\"\"}"),
                                MediaType.APPLICATION_JSON));
        assertEquals(
                new BigDecimal("30"), service.grade("Two prompts", submission).estimatedScore());
        server.verify();
    }

    @Test
    void combinedBoundaryIncludesTaskAndRejectsOneExtraCharacterBeforeHttp() {
        String prompt = "t".repeat(11_000);
        assertThrows(AiGradingException.class, () -> service.grade(prompt, "a".repeat(39_001)));
        server.expect(method(HttpMethod.POST))
                .andRespond(
                        withSuccess(
                                response(
                                        "{\"score\":30,\"cefr\":\"B1\",\"feedback\":\"Valid"
                                                + " boundary.\",\"transcript\":\"\"}"),
                                MediaType.APPLICATION_JSON));
        assertEquals(
                new BigDecimal("30"), service.grade(prompt, "a".repeat(39_000)).estimatedScore());
        server.verify();
    }

    @Test
    void speakingTranscriptUsesTheSameSharedBoundary() {
        server.expect(method(HttpMethod.POST))
                .andRespond(
                        withSuccess(
                                response(
                                        "{\"score\":30,\"cefr\":\"B1\",\"feedback\":\"Chưa đánh giá"
                                                + " phát âm.\",\"transcript\":\"Accepted"
                                                + " transcript\"}"),
                                MediaType.APPLICATION_JSON));
        assertNotNull(service.gradeTranscript("Task", "a".repeat(22_000)));
        assertThrows(
                AiGradingException.class,
                () -> service.gradeTranscript("Task", "a".repeat(50_000)));
        server.verify();
    }

    private final AiProperties properties = new AiProperties();
    private MockRestServiceServer server;
    private GeminiGradingService service;

    @BeforeEach
    void setup() {
        properties.setEnabled(true);
        properties.setApiKey("test-key-not-real");
        var builder =
                RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta");
        server = MockRestServiceServer.bindTo(builder).build();
        service =
                new GeminiGradingServiceImpl(
                        properties,
                        builder.build(),
                        tools.jackson.databind.json.JsonMapper.builder().build());
    }

    private String response(String grade) {
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":"
                + JsonMapper.builder().build().writeValueAsString(grade)
                + "}]}}]}";
    }

    @Test
    void writingUsesGeminiStructuredOutputAndReturnsValidatedEstimate() {
        server.expect(
                        requestTo(
                                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key-not-real"))
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("responseMimeType")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("responseSchema")))
                .andRespond(
                        withSuccess(
                                response(
                                        "{\"score\":32.5,\"cefr\":\"B1\",\"feedback\":\"Cần cải"
                                                + " thiện ngữ pháp.\",\"transcript\":\"\"}"),
                                MediaType.APPLICATION_JSON));
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
        server.expect(
                        requestTo(
                                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"))
                .andRespond(
                        withSuccess(
                                response(
                                        "{\"score\":80,\"cefr\":\"B2\",\"feedback\":\"x\",\"transcript\":\"\"}"),
                                MediaType.APPLICATION_JSON));
        assertThrows(AiGradingException.class, () -> service.grade("Task", "Answer"));
        server.verify();
    }

    @Test
    void blockedResponseDoesNotBecomeScore() {
        server.expect(
                        requestTo(
                                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"))
                .andRespond(
                        withSuccess(
                                "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}",
                                MediaType.APPLICATION_JSON));
        assertThrows(AiGradingException.class, () -> service.grade("Task", "Answer"));
        server.verify();
    }

    @Test
    void quotaFailureDoesNotExposeResponseOrInventScore() {
        server.expect(
                        requestTo(
                                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"))
                .andRespond(
                        withStatus(HttpStatus.TOO_MANY_REQUESTS).body("PRIVATE PROVIDER DETAILS"));
        var exception =
                assertThrows(AiGradingException.class, () -> service.grade("Task", "Answer"));
        assertFalse(exception.getMessage().contains("PRIVATE"));
        server.verify();
    }

    @Test
    void transientProviderErrorRetriesOnce() {
        server.expect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(method(HttpMethod.POST))
                .andRespond(
                        withSuccess(
                                response(
                                        "{\"score\":30,\"cefr\":\"B1\",\"feedback\":\"OK\",\"transcript\":\"\"}"),
                                MediaType.APPLICATION_JSON));
        assertEquals(new BigDecimal("30"), service.grade("Task", "Answer").estimatedScore());
        server.verify();
    }

    @Test
    void speakingSendsAudioInlineAndReturnsTranscript(@TempDir Path directory) throws Exception {
        Path audio = directory.resolve("sample.wav");
        Files.write(audio, new byte[] {82, 73, 70, 70});
        server.expect(
                        requestTo(
                                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("inlineData")))
                .andRespond(
                        withSuccess(
                                response(
                                        "{\"score\":30,\"cefr\":\"B1\",\"feedback\":\"Đánh giá nội"
                                                + " dung, chưa đánh giá phát âm.\",\"transcript\":\"I"
                                                + " enjoy reading books.\"}"),
                                MediaType.APPLICATION_JSON));
        var result = service.grade("Talk about hobbies", audio);
        assertEquals("I enjoy reading books.", result.transcript());
        server.verify();
    }

    @Test
    void transcribeSupportsTheExamAudioPipeline(@TempDir Path directory) throws Exception {
        Path audio = directory.resolve("sample.wav");
        Files.write(audio, new byte[] {82, 73, 70, 70});
        server.expect(
                        requestTo(
                                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("inlineData")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString("\"score\""))))
                .andRespond(
                        withSuccess(
                                response("{\"transcript\":\"I enjoy reading books.\"}"),
                                MediaType.APPLICATION_JSON));
        assertEquals("I enjoy reading books.", service.transcribe(audio));
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
