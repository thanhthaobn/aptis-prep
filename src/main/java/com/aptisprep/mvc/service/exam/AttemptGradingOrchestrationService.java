package com.aptisprep.mvc.service.exam;

import com.aptisprep.mvc.event.AttemptSubmittedEvent;

public interface AttemptGradingOrchestrationService {
    public void onSubmitted(AttemptSubmittedEvent event);
}
