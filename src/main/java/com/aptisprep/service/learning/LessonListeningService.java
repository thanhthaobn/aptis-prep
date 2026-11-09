package com.aptisprep.service.learning;

import com.aptisprep.entity.Lesson;
import com.aptisprep.entity.LessonEngagement;

public interface LessonListeningService {
    record Playback(String token, int sequence, double duration, double listened, boolean verified) {}

    Playback start(Integer lessonId, String email, double position);
    Playback heartbeat(Integer lessonId, String email, String token, int sequence, double position, boolean playing, boolean discontinuity);

    static boolean verified(Lesson lesson, LessonEngagement value) {
        return lesson.getAudioPath() != null
                && value != null
                && value.getAudioPlayedAt() != null
                && java.util.Objects.equals(lesson.getAudioPath(), value.getVerifiedAudioPath());
    }
}
