package com.aptisprep.mvc.learning.dto;

public record LessonPlaybackDTO(
        String token, int sequence, double duration, double listened, boolean verified) {}
