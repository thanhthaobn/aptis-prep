package com.aptisprep.dto.view;

import java.math.BigDecimal;
import java.time.Instant;

public final class AiAssessmentDTO {
    private AiAssessmentDTO() {}

    public record AssessmentRequest(
            String assessmentKey,
            Integer userId,
            Integer attemptId,
            Integer answerId,
            Integer answerVersion,
            String skill,
            String taskType,
            String taskContent,
            String lockedContent,
            String rubricVersion,
            String policyVersion,
            BigDecimal maxScore,
            String transcript,
            String sourceType,
            Integer sourceId) {
        public AssessmentRequest(
                String assessmentKey,
                Integer userId,
                Integer attemptId,
                Integer answerId,
                Integer answerVersion,
                String skill,
                String taskType,
                String taskContent,
                String lockedContent,
                String rubricVersion,
                String policyVersion,
                BigDecimal maxScore,
                String transcript) {
            this(
                    assessmentKey,
                    userId,
                    attemptId,
                    answerId,
                    answerVersion,
                    skill,
                    taskType,
                    taskContent,
                    lockedContent,
                    rubricVersion,
                    policyVersion,
                    maxScore,
                    transcript,
                    "EXAM_ANSWER",
                    answerId);
        }
    }

    public record AssessmentView(
            Integer assessmentId,
            String assessmentKey,
            Integer answerId,
            Integer answerVersion,
            String state,
            BigDecimal score,
            String estimatedCefr,
            String feedback,
            String transcript,
            String rubricVersion,
            String policyVersion,
            String provider,
            String model,
            String errorMessage,
            Instant createdAt,
            Instant completedAt) {}
}
