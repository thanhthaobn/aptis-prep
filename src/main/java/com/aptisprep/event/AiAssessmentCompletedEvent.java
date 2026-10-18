package com.aptisprep.event;

public record AiAssessmentCompletedEvent(String assessmentKey, Integer userId, Integer attemptId, Integer answerId,
        String state, String errorMessage) {
    public AiAssessmentCompletedEvent(String assessmentKey, Integer userId, Integer attemptId, Integer answerId) {
        this(assessmentKey, userId, attemptId, answerId, "COMPLETED", null);
    }
}
