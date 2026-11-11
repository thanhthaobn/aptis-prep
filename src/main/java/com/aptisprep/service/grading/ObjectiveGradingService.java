package com.aptisprep.service.grading;

import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.presentation.LockedAttemptDTO;

public interface ObjectiveGradingService {
    void gradeObjective(LockedAttemptDTO locked, MockExamEntity exam);
}
