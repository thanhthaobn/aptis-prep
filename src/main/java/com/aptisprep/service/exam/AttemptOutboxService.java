package com.aptisprep.service.exam;

import com.aptisprep.event.AiAssessmentCompletedEvent;
import com.aptisprep.event.AttemptCompletedEvent;
import com.aptisprep.event.AttemptSubmittedEvent;

public interface AttemptOutboxService {
    public void enqueueSubmitted(AttemptSubmittedEvent event);

    public void enqueueCompleted(AttemptCompletedEvent event);

    public void enqueueAiAssessmentCompleted(AiAssessmentCompletedEvent event);

    public void ensureAiAssessmentCompleted(AiAssessmentCompletedEvent event);

    public void recoverPending();

    public void markDelivered(AttemptSubmittedEvent event);

    public void markDelivered(AttemptCompletedEvent event);

    public void markDelivered(AiAssessmentCompletedEvent event);
}
