package com.aptisprep.mvc.service.learning;


import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import com.aptisprep.mvc.exception.*;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.support.contract.IntegrationModels.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.*;


public interface LearningPathService {
        public record Stage(
                Integer pathId,
                String skill,
                String fromLevel,
                String targetLevel,
                String goalLevel,
                String currentLevel,
                String status,
                List<StageLesson> lessons) {
            public StageProgress progress() {
                if (currentLevel == null || lessons == null || lessons.isEmpty()) return null;
                long completedLessons = lessons.stream().filter(streamValue -> streamValue.completed()).count();
                int totalLessons = lessons.size();
                return new StageProgress(
                        completedLessons + "/" + totalLessons + " bài",
                        (int) (completedLessons * 100 / totalLessons));
            }
    
            public List<StageLesson> missingRequiredLessons() {
                return lessons.stream()
                        .filter(lesson -> lesson.required() && !lesson.completed())
                        .toList();
            }
        }
    
        public record StageProgress(String label, int percent) {}
    
        public record StageLesson(
                Integer id,
                String title,
                String level,
                String status,
                boolean available,
                String topic,
                boolean required,
                boolean completed) {}
    
    public void ensurePaths(Integer userId);
    public List<PathProgress> ensureAndSummaries(String email);
    public int refreshPaths(String email);
    public Integer userId(String email);
    public List<PathProgress> summaries(Integer userId);
    public PathProgress summary(Integer userId, Integer pathId);
    public List<Stage> stages(String email, String skill);
    public void completeLesson(Integer userId, Integer lessonId);
    public Integer startCheckpoint(Integer userId, Integer pathId, String requestKey);
    public Integer retryCheckpoint(Integer userId, Integer attemptId);
    public AttemptResult checkpointResult(String email, Integer attemptId);
    public void applyCheckpoint(Integer userId, Integer attemptId);
}

