package com.aptisprep.mvc.exam.service.impl;

import com.aptisprep.mvc.catalog.entity.MockExamEntity;
import com.aptisprep.mvc.learning.service.LearningPathService;
import org.springframework.stereotype.Component;

/** Rechecks persisted path authorization inside the attempt transaction, after locking the learner. */
@Component
class CheckpointStartValidator {
    private final LearningPathService learningPathService;

    CheckpointStartValidator(LearningPathService learningPathService) {
        this.learningPathService = learningPathService;
    }

    void validate(Integer userId, MockExamEntity exam, Integer pathId) {
        learningPathService.validateCheckpointStart(
                userId, exam.getId(), exam.getTargetLevel(), exam.getSkill(), pathId);
    }
}
