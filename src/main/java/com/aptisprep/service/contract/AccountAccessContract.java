package com.aptisprep.service.contract;

import com.aptisprep.service.contract.IntegrationModels.LearnerContext;

/** Owner: Thông (Profile). Caller must obtain IDs from authenticated/authorized server context. */
public interface AccountAccessContract {
    Integer requireActiveStudent(String principalEmail);
    LearnerContext context(Integer userId);
    /** Applies a valid Placement or Checkpoint once; rechecks result source and ownership. */
    void applyAssessment(Integer userId, Integer attemptId);
}
