package com.aptisprep.mvc.service.exam.impl;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.repository.LearningPathItemRepository;
import com.aptisprep.mvc.repository.LearningPathRepository;
import com.aptisprep.mvc.service.exam.CheckpointStartService;
import java.util.*;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Kiểm tra lại quyền truy cập path đã lưu bên trong transaction attempt, sau khi khóa học viên. */
@Service
public class CheckpointStartServiceImpl implements CheckpointStartService {
    private LearningPathRepository learningPathRepository;
    private LearningPathItemRepository learningPathItemRepository;

    public CheckpointStartServiceImpl(
            LearningPathRepository learningPathRepository,
            LearningPathItemRepository learningPathItemRepository) {
        this.learningPathRepository = learningPathRepository;
        this.learningPathItemRepository = learningPathItemRepository;
    }

    @Override
    public void validate(Integer userId, MockExam exam, Integer pathId) {
        validatePath(userId, exam, pathId, false);
    }

    @Override
    public void validateForRetake(Integer userId, MockExam exam, Integer pathId) {
        validatePath(userId, exam, pathId, true);
    }

    private void validatePath(Integer userId, MockExam exam, Integer pathId, boolean retake) {
        if (pathId == null) throw new BusinessRuleException("Checkpoint cần ngữ cảnh lộ trình học");
        LearningPath path =
                learningPathRepository
                        .findByIdAndUserId(pathId, userId)
                        .orElseThrow(
                                () -> new BusinessRuleException("Lộ trình không thuộc học viên"));
        boolean validPathStatus =
                retake ? "COMPLETED".equals(path.getStatus()) : "ACTIVE".equals(path.getStatus());
        if (!validPathStatus
                || !Objects.equals(path.getCheckpointExamId(), exam.getId())
                || !Objects.equals(path.getTargetLevel(), exam.getTargetLevel())
                || !Set.of("A1", "A2", "B1", "B2", "C1")
                        .contains(Objects.toString(path.getTargetLevel(), ""))
                || !normalizeSkill(path.getSkill()).equals(normalizeSkill(exam.getSkill())))
            throw new BusinessRuleException(
                    retake
                            ? "Checkpoint cũ không khớp chặng đã hoàn thành"
                            : "Checkpoint không khớp chặng học đang hoạt động");
        List<LearningPathItem> required =
                learningPathItemRepository.findByLearningPathIdOrderByPositionAsc(pathId).stream()
                        .filter(streamValue -> streamValue.isRequired())
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

    private String normalizeSkill(String value) {
        return Objects.toString(value, "")
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_");
    }
}
