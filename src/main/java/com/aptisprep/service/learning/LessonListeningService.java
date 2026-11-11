package com.aptisprep.service.learning;

import com.aptisprep.presentation.LessonPlaybackDTO;

public interface LessonListeningService {
    LessonPlaybackDTO start(Integer lessonId, String email, double position);

    LessonPlaybackDTO heartbeat(
            Integer lessonId,
            String email,
            String token,
            int sequence,
            double position,
            boolean playing,
            boolean discontinuity);

    record Playback(
            String token, int sequence, double duration, double listened, boolean verified) {}
}
