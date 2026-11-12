package com.aptisprep.mvc.service.grading;

import com.aptisprep.mvc.entity.ExamAttempt;
import com.aptisprep.mvc.entity.MockExam;
import com.aptisprep.mvc.event.AiAssessmentCompletedEvent;
import com.aptisprep.mvc.event.AttemptSubmittedEvent;

/** Finalizes objective and subjective grading outcomes for an attempt. */
public interface AttemptFinalizationService {
    boolean finalizeIfReady(ExamAttempt attempt, MockExam exam);
    void publishCompleted(ExamAttempt attempt, MockExam exam);
    void applyAiAssessment(AiAssessmentCompletedEvent event);
    void markSubjectiveUnavailable(AttemptSubmittedEvent event, String reason);
    boolean accepts(AttemptSubmittedEvent event);
    void retryFailedGrading(Integer userId, Integer attemptId);
}







