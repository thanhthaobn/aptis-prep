package com.aptisprep.service.grading;

import com.aptisprep.dto.view.LockedAttemptDTO;
import com.aptisprep.entity.MockExamEntity;

public interface ObjectiveGradingService {
    void gradeObjective(LockedAttemptDTO locked, MockExamEntity exam);
}
