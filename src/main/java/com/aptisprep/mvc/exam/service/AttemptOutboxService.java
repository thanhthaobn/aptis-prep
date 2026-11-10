package com.aptisprep.mvc.exam.service;

import com.aptisprep.mvc.common.event.AiAssessmentCompletedEvent;
import com.aptisprep.mvc.common.event.AttemptCompletedEvent;
import com.aptisprep.mvc.common.event.AttemptSubmittedEvent;

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
