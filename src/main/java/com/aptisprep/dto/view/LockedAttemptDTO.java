package com.aptisprep.dto.view;

import java.time.Instant;
import java.util.List;

/** Backend-only snapshot emitted after the attempt is locked. Never bind this from a form. */
public record LockedAttemptDTO(
        Integer attemptId,
        Integer userId,
        Integer examId,
        String examType,
        Integer answerVersion,
        Instant submittedAt,
        Integer scoringSchemeId,
        String policyVersion,
        String rubricVersion,
        String gradingContext,
        String gradingRequestKey,
        List<LockedAnswerDTO> answers) {
    public LockedAttemptDTO(
            Integer attemptId,
            Integer userId,
            Integer examId,
            String examType,
            Integer answerVersion,
            Instant submittedAt,
            Integer scoringSchemeId,
            String policyVersion,
            String gradingRequestKey,
            List<LockedAnswerDTO> answers) {
        this(
                attemptId,
                userId,
                examId,
                examType,
                answerVersion,
                submittedAt,
                scoringSchemeId,
                policyVersion,
                "aptis-rubric-v1",
                null,
                gradingRequestKey,
                answers);
    }
}
