package com.aptisprep.event;

import com.aptisprep.dto.view.LockedAttemptDTO;

public record AttemptSubmittedEvent(
        Integer userId,
        Integer attemptId,
        Integer examId,
        String examType,
        LockedAttemptDTO lockedAttempt) {}
