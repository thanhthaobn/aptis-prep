package com.aptisprep.service.grading;

import com.aptisprep.entity.ExamAttemptEntity;
import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.event.AiAssessmentCompletedEvent;
import com.aptisprep.event.AttemptSubmittedEvent;

public interface AttemptFinalizationService {
    public boolean finalizeIfReady(ExamAttemptEntity attempt, MockExamEntity exam);

    public void publishCompleted(ExamAttemptEntity attempt, MockExamEntity exam);

    public void applyAiAssessment(AiAssessmentCompletedEvent event);

    public void markSubjectiveUnavailable(AttemptSubmittedEvent event, String reason);

    public boolean accepts(AttemptSubmittedEvent event);

    public void retryFailedGrading(Integer userId, Integer attemptId);
}
