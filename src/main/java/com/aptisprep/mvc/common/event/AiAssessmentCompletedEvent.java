package com.aptisprep.mvc.common.event;

public record AiAssessmentCompletedEvent(
        String assessmentKey,
        Integer userId,
        Integer attemptId,
        Integer answerId,
        String state,
        String errorMessage,
        Integer answerVersion) {}
