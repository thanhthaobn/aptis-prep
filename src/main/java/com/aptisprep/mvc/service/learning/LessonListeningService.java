package com.aptisprep.mvc.service.learning;

import com.aptisprep.mvc.entity.Lesson;
import com.aptisprep.mvc.entity.LessonEngagement;

public interface LessonListeningService {
    record Playback(
            String token, int sequence, double duration, double listened, boolean verified) {}

    public Playback start(Integer lessonId, String email, double position);

    public Playback heartbeat(
            Integer lessonId,
            String email,
            String token,
            int sequence,
            double position,
            boolean playing,
            boolean discontinuity);

    /** Explicit server-side acknowledgement used by the no-JavaScript lesson flow. */
    public void confirmManualListening(Integer lessonId, String email);

    public boolean verified(Lesson lesson, LessonEngagement value);
}







