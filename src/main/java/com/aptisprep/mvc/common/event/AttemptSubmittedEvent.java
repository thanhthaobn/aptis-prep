package com.aptisprep.mvc.common.event;

import com.aptisprep.mvc.exam.dto.LockedAttemptDTO;

public record AttemptSubmittedEvent(
        Integer userId,
        Integer attemptId,
        Integer examId,
        String examType,
        LockedAttemptDTO lockedAttempt) {}
