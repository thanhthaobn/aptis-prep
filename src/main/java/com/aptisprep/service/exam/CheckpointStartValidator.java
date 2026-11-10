package com.aptisprep.service.exam;

import com.aptisprep.entity.*;
import com.aptisprep.mvc.common.exception.BusinessRuleException;
import com.aptisprep.repository.LearningPathItemRepository;
import com.aptisprep.repository.LearningPathRepository;
import java.util.*;
import org.springframework.stereotype.Component;

/** Rechecks persisted path authorization inside the attempt transaction, after locking the learner. */
@Component
final class CheckpointStartValidator {
    private final LearningPathRepository learningPathRepository;
    private final LearningPathItemRepository learningPathItemRepository;

    CheckpointStartValidator(
            LearningPathRepository learningPathRepository,
            LearningPathItemRepository learningPathItemRepository) {
        this.learningPathRepository = learningPathRepository;
        this.learningPathItemRepository = learningPathItemRepository;
    }

    void validate(Integer userId, MockExamEntity exam, Integer pathId) {
        if (pathId == null) throw new BusinessRuleException("Checkpoint cần ngữ cảnh lộ trình học");
        LearningPath path =
                learningPathRepository
                        .findByIdAndUserId(pathId, userId)
                        .orElseThrow(
                                () -> new BusinessRuleException("Lộ trình không thuộc học viên"));
        if (!"ACTIVE".equals(path.getStatus())
                || !Objects.equals(path.getCheckpointExamId(), exam.getId())
                || !Objects.equals(path.getTargetLevel(), exam.getTargetLevel())
                || !Set.of("A1", "A2", "B1", "B2", "C1")
                        .contains(Objects.toString(path.getTargetLevel(), ""))
                || !ExamStartValidator.normalizeSkill(path.getSkill())
                        .equals(ExamStartValidator.normalizeSkill(exam.getSkill())))
            throw new BusinessRuleException("Checkpoint không khớp chặng học đang hoạt động");
        List<LearningPathItem> required =
                learningPathItemRepository.findByLearningPathIdOrderByPositionAsc(pathId).stream()
                        .filter(LearningPathItem::isRequired)
                        .toList();
        if (required.isEmpty()
                || required.stream()
                        .anyMatch(
                                item ->
                                        !"COMPLETED".equals(item.getStatus())
                                                || item.getCompletedAt() == null))
            throw new BusinessRuleException(
                    "Hãy hoàn thành các bài bắt buộc trước khi làm checkpoint");
    }
}
