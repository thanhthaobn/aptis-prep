package com.aptisprep.service.learning;

import com.aptisprep.dto.LessonPlaybackDTO;

public interface LessonListeningService {
    public LessonPlaybackDTO start(Integer lessonId, String email, double position);

    public LessonPlaybackDTO heartbeat(
            Integer lessonId,
            String email,
            String token,
            int sequence,
            double position,
            boolean playing,
            boolean discontinuity);

    public record Playback(
            String token, int sequence, double duration, double listened, boolean verified) {}
}
