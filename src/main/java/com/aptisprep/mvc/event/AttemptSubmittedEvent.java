package com.aptisprep.mvc.event;

import com.aptisprep.mvc.support.contract.IntegrationModels.LockedAttempt;

public record AttemptSubmittedEvent(
        Integer userId,
        Integer attemptId,
        Integer examId,
        String examType,
        LockedAttempt lockedAttempt) {}







