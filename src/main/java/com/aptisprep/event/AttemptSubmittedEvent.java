package com.aptisprep.event;

import com.aptisprep.service.contract.IntegrationModels.LockedAttempt;

public record AttemptSubmittedEvent(Integer userId, Integer attemptId, Integer examId, String examType,
                                    LockedAttempt lockedAttempt) {
}
