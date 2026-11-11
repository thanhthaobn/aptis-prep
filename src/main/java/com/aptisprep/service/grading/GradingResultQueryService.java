package com.aptisprep.service.grading;

import com.aptisprep.entity.ExamSkillResultEntity;
import java.util.List;

public interface GradingResultQueryService {
    public List<ExamSkillResultEntity> findSkillResultsByAttemptId(Integer attemptId);

    public boolean existsByAttemptId(Integer attemptId);
}
