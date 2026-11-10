package com.aptisprep.mvc.grading.service;

import com.aptisprep.mvc.grading.entity.ExamSkillResultEntity;
import java.util.List;

public interface GradingResultQueryService {
    List<ExamSkillResultEntity> findSkillResultsByAttemptId(Integer attemptId);

    boolean existsByAttemptId(Integer attemptId);
}
