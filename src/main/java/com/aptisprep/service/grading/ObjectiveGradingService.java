package com.aptisprep.service.grading;

import com.aptisprep.dto.LockedAttemptDTO;
import com.aptisprep.entity.MockExamEntity;

public interface ObjectiveGradingService {
    public void gradeObjective(LockedAttemptDTO locked, MockExamEntity exam);
}
