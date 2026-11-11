package com.aptisprep.event;

import com.aptisprep.dto.LockedAttemptDTO;

public record AttemptSubmittedEvent(
        Integer userId,
        Integer attemptId,
        Integer examId,
        String examType,
        LockedAttemptDTO lockedAttempt) {}
