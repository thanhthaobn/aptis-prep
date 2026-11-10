package com.aptisprep.service.learning;

import static com.aptisprep.mvc.common.security.SessionMvcRequests.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aptisprep.entity.*;
import com.aptisprep.mvc.common.enums.*;
import com.aptisprep.mvc.storage.service.AudioDurationService;
import com.aptisprep.repository.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LessonListeningFlowTests {
    @TestConfiguration
    static class PlaybackConfig {
        @Bean
        LessonListeningServiceTests.MutableClock playbackClock() {
            return new LessonListeningServiceTests.MutableClock();
        }

        @Bean
        @Primary
        LessonListeningService controlledListening(
                UserRepository users,
                LessonRepository lessons,
                LessonEngagementRepository engagements,
                AudioDurationService durations,
                LessonListeningServiceTests.MutableClock clock) {
            return new com.aptisprep.service.learning.impl.LessonListeningServiceImpl(
                    users, lessons, engagements, durations, clock);
        }
    }

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired LessonRepository lessons;
    @Autowired LessonProgressRepository progress;
    @Autowired LessonTeachingService teaching;
    @Autowired LessonListeningServiceTests.MutableClock clock;
    @MockitoBean AudioDurationService durations;
    Lesson lesson;
    User learner;

    @BeforeEach
    void setup() {
        clock.current = Instant.parse("2026-10-23T00:00:00Z");
        learner = createUser();
        lesson = new Lesson();
        lesson.setTitle("Verified listening");
        lesson.setSkill("Listening");
        lesson.setCefrLevel("A1");
        lesson.setDisplayOrder(9900);
        lesson.setStatus("PUBLISHED");
        lesson.setAudioPath("server-audio.wav");
        lesson = lessons.saveAndFlush(lesson);
        when(durations.seconds("server-audio.wav")).thenReturn(100.0);
    }

    private User createUser() {
        var user = new User();
        user.setFullName("Listener");
        user.setEmail(UUID.randomUUID() + "@test.local");
        user.setPasswordHash("unused");
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        return users.saveAndFlush(user);
    }

    private String start() throws Exception {
        var body =
                mvc.perform(
                                post("/student/lessons/" + lesson.getId() + "/listening/start")
                                        .with(csrf())
                                        .with(user(learner.getEmail()).roles("STUDENT"))
                                        .param("position", "0")
                                        .param("duration", "1"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.duration").value(100))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return JsonMapper.builder().build().readTree(body).path("token").asText();
    }

    private void heartbeat(String token, int sequence, int position, boolean expected)
            throws Exception {
        mvc.perform(
                        post("/student/lessons/" + lesson.getId() + "/listening/heartbeat")
                                .with(csrf())
                                .with(user(learner.getEmail()).roles("STUDENT"))
                                .param("token", token)
                                .param("sequence", String.valueOf(sequence))
                                .param("position", String.valueOf(position))
                                .param("playing", "true")
                                .param("duration", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(expected));
    }

    @Test
    void serverVerifiedNinetyPercentUnlocksActualCompletionButEightyNineDoesNot() throws Exception {
        String token = start();
        int sequence = 0;
        for (int position = 4; position <= 88; position += 4) {
            clock.advance(4);
            heartbeat(token, ++sequence, position, false);
        }
        clock.advance(1);
        heartbeat(token, ++sequence, 89, false);
        mvc.perform(
                        post("/student/lessons/" + lesson.getId() + "/complete")
                                .with(csrf())
                                .with(user(learner.getEmail()).roles("STUDENT")))
                .andExpect(redirectedUrl("/student/lessons/" + lesson.getId()));
        assertTrue(progress.findByUserIdAndLessonId(learner.getId(), lesson.getId()).isEmpty());
        clock.advance(1);
        heartbeat(token, ++sequence, 90, true);
        mvc.perform(
                        post("/student/lessons/" + lesson.getId() + "/complete")
                                .with(csrf())
                                .with(user(learner.getEmail()).roles("STUDENT")))
                .andExpect(status().is3xxRedirection());
        assertEquals(
                "COMPLETED",
                progress.findByUserIdAndLessonId(learner.getId(), lesson.getId())
                        .orElseThrow()
                        .getStatus());
    }

    @Test
    void inventedDurationsOldEndpointAndStolenTokensDoNotUnlockLessons() throws Exception {
        String token = start();
        heartbeat(token, 1, 90, false);
        assertFalse(teaching.completion(lesson.getId(), learner.getEmail()).audioPlayed());
        mvc.perform(
                        post("/student/lessons/" + lesson.getId() + "/audio-played")
                                .with(csrf())
                                .with(user(learner.getEmail()).roles("STUDENT"))
                                .param("duration", "1")
                                .param("listened", "1"))
                .andExpect(status().isBadRequest());
        var other = createUser();
        mvc.perform(
                        post("/student/lessons/" + lesson.getId() + "/listening/heartbeat")
                                .with(csrf())
                                .with(user(other.getEmail()).roles("STUDENT"))
                                .param("token", token)
                                .param("sequence", "2")
                                .param("position", "94")
                                .param("playing", "true"))
                .andExpect(status().isBadRequest());
        assertFalse(teaching.completion(lesson.getId(), other.getEmail()).audioPlayed());
        mvc.perform(
                        post("/student/lessons/" + lesson.getId() + "/listening/start")
                                .with(user(learner.getEmail()).roles("STUDENT"))
                                .param("position", "0"))
                .andExpect(status().isForbidden());
    }
}
