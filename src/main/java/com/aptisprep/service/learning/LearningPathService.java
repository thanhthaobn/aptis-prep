package com.aptisprep.service.learning;

import com.aptisprep.dto.LearningPathViewsDTO.StageDTO;
import com.aptisprep.dto.PathProgressDTO;
import java.util.List;

public interface LearningPathService {
    public void ensurePaths(Integer userId);

    public List<PathProgressDTO> summaries(Integer userId);

    public PathProgressDTO summary(Integer userId, Integer pathId);

    public void completeLesson(Integer userId, Integer lessonId);

    public Integer startCheckpoint(Integer userId, Integer pathId, String requestKey);

    public void applyCheckpoint(Integer userId, Integer attemptId);

    public int refreshPaths(String email);

    public List<StageDTO> stages(String email, String skill);

    public List<PathProgressDTO> ensureAndSummaries(String email);

    public Integer userId(String email);

    public String checkpointDestination(String email, Integer attemptId);

    public void validateCheckpointStart(
            Integer userId, Integer examId, String targetLevel, String skill, Integer pathId);

    public record StageLesson(
            Integer id,
            String title,
            String level,
            String status,
            boolean available,
            String topic,
            boolean required,
            boolean completed) {}
}
