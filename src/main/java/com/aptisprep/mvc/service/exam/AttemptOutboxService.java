package com.aptisprep.mvc.service.exam;

import com.aptisprep.mvc.event.AiAssessmentCompletedEvent;
import com.aptisprep.mvc.event.AttemptCompletedEvent;
import com.aptisprep.mvc.event.AttemptSubmittedEvent;

/** Persists and delivers attempt-related outbox events. */
public interface AttemptOutboxService {
    void enqueueSubmitted(AttemptSubmittedEvent event);
    void enqueueCompleted(AttemptCompletedEvent event);
    void enqueueAiAssessmentCompleted(AiAssessmentCompletedEvent event);
    void ensureAiAssessmentCompleted(AiAssessmentCompletedEvent event);
    void recoverPending();
    void markDelivered(AttemptSubmittedEvent event);
    void markDelivered(AttemptCompletedEvent event);
    void markDelivered(AiAssessmentCompletedEvent event);
}







