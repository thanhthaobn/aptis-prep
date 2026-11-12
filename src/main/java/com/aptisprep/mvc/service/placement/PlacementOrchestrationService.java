package com.aptisprep.mvc.service.placement;

import com.aptisprep.mvc.event.AttemptCompletedEvent;

public interface PlacementOrchestrationService {
    public void onCompleted(AttemptCompletedEvent event);
}

