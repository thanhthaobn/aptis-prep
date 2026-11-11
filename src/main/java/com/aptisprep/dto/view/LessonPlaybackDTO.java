package com.aptisprep.dto.view;

public record LessonPlaybackDTO(
        String token, int sequence, double duration, double listened, boolean verified) {}
