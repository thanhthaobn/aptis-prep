package com.aptisprep.mvc.learning.service;

import com.aptisprep.mvc.learning.dto.LearningPathViewsDTO.StageDTO;
import com.aptisprep.mvc.learning.dto.PathProgressDTO;
import java.util.List;

public interface LearningPathService {
    void ensurePaths(Integer userId);

    List<PathProgressDTO> summaries(Integer userId);

    PathProgressDTO summary(Integer userId, Integer pathId);

    void completeLesson(Integer userId, Integer lessonId);

    Integer startCheckpoint(Integer userId, Integer pathId, String requestKey);

    void applyCheckpoint(Integer userId, Integer attemptId);

    int refreshPaths(String email);

    List<StageDTO> stages(String email, String skill);

    List<PathProgressDTO> ensureAndSummaries(String email);

    Integer userId(String email);

    String checkpointDestination(String email, Integer attemptId);

    void validateCheckpointStart(
            Integer userId, Integer examId, String targetLevel, String skill, Integer pathId);

    record StageLesson(
            Integer id,
            String title,
            String level,
            String status,
            boolean available,
            String topic,
            boolean required,
            boolean completed) {}
}
