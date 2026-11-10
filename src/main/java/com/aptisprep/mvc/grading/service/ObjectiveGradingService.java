package com.aptisprep.mvc.grading.service;

import com.aptisprep.mvc.catalog.entity.MockExamEntity;
import com.aptisprep.mvc.exam.dto.LockedAttemptDTO;

public interface ObjectiveGradingService {
    void gradeObjective(LockedAttemptDTO locked, MockExamEntity exam);
}
