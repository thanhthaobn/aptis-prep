package com.aptisprep.mvc.grading.service;

import com.aptisprep.mvc.catalog.entity.MockExamEntity;
import com.aptisprep.mvc.common.event.AiAssessmentCompletedEvent;
import com.aptisprep.mvc.common.event.AttemptSubmittedEvent;
import com.aptisprep.mvc.exam.entity.ExamAttemptEntity;

public interface AttemptFinalizationService {
    boolean finalizeIfReady(ExamAttemptEntity attempt, MockExamEntity exam);

    void publishCompleted(ExamAttemptEntity attempt, MockExamEntity exam);

    void applyAiAssessment(AiAssessmentCompletedEvent event);

    void markSubjectiveUnavailable(AttemptSubmittedEvent event, String reason);

    boolean accepts(AttemptSubmittedEvent event);

    void retryFailedGrading(Integer userId, Integer attemptId);
}
