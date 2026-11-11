package com.aptisprep.service.grading;

import com.aptisprep.entity.ExamSkillResultEntity;
import java.util.List;

public interface GradingResultQueryService {
    List<ExamSkillResultEntity> findSkillResultsByAttemptId(Integer attemptId);

    boolean existsByAttemptId(Integer attemptId);
}
