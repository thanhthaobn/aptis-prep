package com.aptisprep.presentation;

public record LessonPlaybackDTO(
        String token, int sequence, double duration, double listened, boolean verified) {}
