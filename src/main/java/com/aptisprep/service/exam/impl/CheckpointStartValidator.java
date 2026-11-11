package com.aptisprep.service.exam.impl;

import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.service.learning.LearningPathService;
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
