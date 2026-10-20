package com.aptisprep.service.contract;

import java.math.BigDecimal;
import java.time.Instant;

/** Owner: Thông. Server-to-server contract; frontend never supplies CEFR, grading context or provider data. */
public interface AiAssessmentContract {
    AssessmentView request(AssessmentRequest request);
    AssessmentView process(String assessmentKey);
    AssessmentView retry(String assessmentKey, Integer requesterId, boolean admin);
    AssessmentView get(String assessmentKey, Integer requesterId, boolean admin);
    /** Internal grading callback read; never expose this method from a controller. */
    AssessmentView getForGrading(String assessmentKey);

    record AssessmentRequest(String assessmentKey, Integer userId, Integer attemptId, Integer answerId,
            Integer answerVersion, String skill, String taskType, String taskContent, String lockedContent,
            String rubricVersion, String policyVersion, BigDecimal maxScore, String transcript) {}
    record AssessmentView(Integer assessmentId, String assessmentKey, Integer answerId, Integer answerVersion,
            String state, BigDecimal score, String estimatedCefr, String feedback, String transcript,
            String rubricVersion, String policyVersion, String provider, String model, String errorMessage,
            Instant createdAt, Instant completedAt) {}
}
