package com.aptisprep.mvc.service.grading;

import com.aptisprep.mvc.entity.MockExam;
import com.aptisprep.mvc.support.contract.IntegrationModels.LockedAttempt;

/** Grades the objective portion of an attempt. */
public interface ObjectiveGradingService {
    void gradeObjective(LockedAttempt lockedAttempt, MockExam exam);
}







