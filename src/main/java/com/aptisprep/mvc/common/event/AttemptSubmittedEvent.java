package com.aptisprep.mvc.common.event;

import com.aptisprep.service.contract.IntegrationModels.LockedAttempt;

public record AttemptSubmittedEvent(
        Integer userId,
        Integer attemptId,
        Integer examId,
        String examType,
        LockedAttempt lockedAttempt) {}
