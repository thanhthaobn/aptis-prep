package com.aptisprep.service.learning;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aptisprep.entity.*;
import com.aptisprep.enums.*;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.repository.*;
import com.aptisprep.service.storage.AudioDurationService;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LessonListeningServiceTests {
    final UserRepository users = mock(UserRepository.class);
    final LessonRepository lessons = mock(LessonRepository.class);
    final LessonEngagementRepository engagements = mock(LessonEngagementRepository.class);
    final AudioDurationService durations = mock(AudioDurationService.class);
    final MutableClock clock = new MutableClock();
    LessonListeningService service;
    LessonEngagement progress;
    Lesson lesson;
    String token;
    int sequence;

    @BeforeEach
    void setup() {
        var user = new User();
        user.setId(1);
        user.setEmail("a@test.local");
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(users.findByIdForUpdate(1)).thenReturn(Optional.of(user));
        lesson = new Lesson();
        lesson.setStatus("PUBLISHED");
        lesson.setAudioPath("lesson.wav");
        when(lessons.findById(2)).thenReturn(Optional.of(lesson));
        when(durations.seconds("lesson.wav")).thenReturn(100.0);
        progress = new LessonEngagement();
        progress.setUserId(1);
        progress.setLessonId(2);
        when(engagements.findByUserIdAndLessonId(1, 2)).thenReturn(Optional.of(progress));
        service =
                new LessonListeningService(
                        users,
                        lessons,
                        engagements,
                        durations,
                        clock,
                        tools.jackson.databind.json.JsonMapper.builder().build());
        token = service.start(2, "a@test.local", 0).token();
    }

    private LessonListeningService.Playback ping(
            double position, double elapsed, boolean playing, boolean seek) {
        clock.advance(elapsed);
        return service.heartbeat(2, "a@test.local", token, ++sequence, position, playing, seek);
    }

    private void listenTo(int position) {
        double current = progress.getPlaybackPositionSeconds();
        while (current < position) {
            double next = Math.min(position, current + 4);
            ping(next, next - current, true, false);
            current = next;
        }
    }

    @Test
    void eightyNinePercentIsNotEnoughAndNinetyPercentIsVerified() {
        listenTo(89);
        assertFalse(LessonListeningService.verified(lesson, progress));
        var result = ping(90, 1, false, false);
        assertTrue(result.verified());
        assertEquals(90, result.listened(), .0001);
    }

    @Test
    void jumpingAheadCannotClaimPlaybackEvenAfterAPlaybackRequest() {
        var result = ping(90, .01, true, false);
        assertEquals(0, result.listened());
        assertFalse(result.verified());
    }

    @Test
    void seekingAndReplayingTheSameRangeDoNotAddUniqueCoverage() {
        listenTo(10);
        ping(0, 1, true, true);
        listenTo(10);
        assertEquals(
                10,
                service.heartbeat(2, "a@test.local", token, sequence, 10, true, false).listened(),
                .0001);
    }

    @Test
    void repeatedRequestCannotAddCreditOrExtendTheClockWindow() {
        var first = ping(4, 4, true, false);
        clock.advance(10);
        var replay = service.heartbeat(2, "a@test.local", token, sequence, 4, true, false);
        assertEquals(first.listened(), replay.listened());
        assertThrows(
                BusinessRuleException.class,
                () -> service.heartbeat(2, "a@test.local", token, sequence, 90, true, false));
    }

    @Test
    void pausedTimeDoesNotCountAsPlayback() {
        ping(0, 1, false, false);
        ping(10, 10, true, false);
        assertEquals("[]", progress.getPlaybackRangesJson());
    }

    @Test
    void expiryInvalidTokensAndNonFinitePositionsAreRejected() {
        assertThrows(
                BusinessRuleException.class,
                () -> service.heartbeat(2, "a@test.local", "wrong", 1, 4, true, false));
        assertThrows(
                BusinessRuleException.class,
                () -> service.heartbeat(2, "a@test.local", token, 1, Double.NaN, true, false));
        clock.advance(301);
        assertThrows(
                BusinessRuleException.class,
                () -> service.heartbeat(2, "a@test.local", token, 1, 4, true, false));
    }

    @Test
    void aSecondTabInvalidatesTheFirstTokenButKeepsVerifiedRanges() {
        listenTo(12);
        var replacement = service.start(2, "a@test.local", 12);
        assertEquals(12, replacement.listened(), .0001);
        assertThrows(
                BusinessRuleException.class,
                () -> service.heartbeat(2, "a@test.local", token, sequence + 1, 16, true, false));
    }

    @Test
    void anotherUsersTokenCannotCreditThisUser() {
        var other = new User();
        other.setId(3);
        other.setRole(Role.STUDENT);
        other.setStatus(UserStatus.ACTIVE);
        when(users.findByEmailIgnoreCase("b@test.local")).thenReturn(Optional.of(other));
        when(users.findByIdForUpdate(3)).thenReturn(Optional.of(other));
        when(engagements.findByUserIdAndLessonId(3, 2)).thenReturn(Optional.empty());
        assertThrows(
                BusinessRuleException.class,
                () -> service.heartbeat(2, "b@test.local", token, 1, 4, true, false));
    }

    @Test
    void changingTheAudioInvalidatesOldSessionsAndVerification() {
        listenTo(90);
        lesson.setAudioPath("new.wav");
        assertFalse(LessonListeningService.verified(lesson, progress));
        assertThrows(
                BusinessRuleException.class,
                () -> service.heartbeat(2, "a@test.local", token, sequence + 1, 91, true, false));
    }

    @Test
    void oldUnverifiedAudioPlayedMarkerDoesNotBypassTheNewGate() {
        progress.setAudioPlayedAt(LocalDateTime.now(clock));
        assertFalse(LessonListeningService.verified(lesson, progress));
    }

    @Test
    void decoderRoundingIsClampedButCannotCreditPastTheServerDuration() {
        listenTo(96);
        var result = ping(100.1, 4, false, false);
        assertEquals(100, result.listened(), .0001);
        assertTrue(result.verified());
        assertThrows(
                BusinessRuleException.class,
                () -> service.heartbeat(2, "a@test.local", token, sequence + 1, 101, false, false));
    }

    static class MutableClock extends Clock {
        Instant current = Instant.parse("2026-10-23T00:00:00Z");

        void advance(double seconds) {
            current = current.plusNanos((long) (seconds * 1_000_000_000));
        }

        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        public Clock withZone(ZoneId zone) {
            return this;
        }

        public Instant instant() {
            return current;
        }
    }
}
