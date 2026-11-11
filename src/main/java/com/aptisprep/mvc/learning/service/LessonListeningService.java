package com.aptisprep.mvc.learning.service;

import com.aptisprep.mvc.learning.dto.LessonPlaybackDTO;

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
