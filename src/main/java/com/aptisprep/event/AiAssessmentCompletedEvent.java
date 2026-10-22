package com.aptisprep.event;

public record AiAssessmentCompletedEvent(String assessmentKey, Integer userId, Integer attemptId, Integer answerId,
                                         String state, String errorMessage, Integer answerVersion) {
    public AiAssessmentCompletedEvent(String assessmentKey, Integer userId, Integer attemptId, Integer answerId) {
        this(assessmentKey, userId, attemptId, answerId, "COMPLETED", null, null);
    }

    public AiAssessmentCompletedEvent(String assessmentKey, Integer userId, Integer attemptId, Integer answerId,
                                      String state, String errorMessage) {
        this(assessmentKey, userId, attemptId, answerId, state, errorMessage, null);
    }
}
