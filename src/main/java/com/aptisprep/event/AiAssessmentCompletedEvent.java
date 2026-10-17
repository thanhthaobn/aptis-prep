package com.aptisprep.event;

public record AiAssessmentCompletedEvent(String assessmentKey, Integer userId, Integer attemptId, Integer answerId) {}
