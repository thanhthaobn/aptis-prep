package com.aptisprep.service.grading;

import com.aptisprep.entity.ExamAttemptEntity;
import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.event.AiAssessmentCompletedEvent;
import com.aptisprep.event.AttemptSubmittedEvent;

public interface AttemptFinalizationService {
    boolean finalizeIfReady(ExamAttemptEntity attempt, MockExamEntity exam);

    void publishCompleted(ExamAttemptEntity attempt, MockExamEntity exam);

    void applyAiAssessment(AiAssessmentCompletedEvent event);

    void markSubjectiveUnavailable(AttemptSubmittedEvent event, String reason);

    boolean accepts(AttemptSubmittedEvent event);

    void retryFailedGrading(Integer userId, Integer attemptId);
}
