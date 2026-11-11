package com.aptisprep.event;

import com.aptisprep.presentation.LockedAttemptDTO;

public record AttemptSubmittedEvent(
        Integer userId,
        Integer attemptId,
        Integer examId,
        String examType,
        LockedAttemptDTO lockedAttempt) {}
