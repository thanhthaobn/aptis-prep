package com.aptisprep.service.exam;

import com.aptisprep.event.AiAssessmentCompletedEvent;
import com.aptisprep.event.AttemptCompletedEvent;
import com.aptisprep.event.AttemptSubmittedEvent;

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
