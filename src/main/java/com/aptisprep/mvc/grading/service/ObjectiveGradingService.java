package com.aptisprep.mvc.grading.service;

import com.aptisprep.mvc.catalog.entity.MockExamEntity;
import com.aptisprep.service.contract.IntegrationModels.LockedAttempt;

public interface ObjectiveGradingService {
    void gradeObjective(LockedAttempt locked, MockExamEntity exam);
}
