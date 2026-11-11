package com.aptisprep.dto.view;

import java.util.List;

public final class LearningPathViewsDTO {
    private LearningPathViewsDTO() {}

    public record StageProgressDTO(String label, int percent) {}

    public record StageLessonDTO(
            Integer id,
            String title,
            String level,
            String status,
            boolean available,
            String topic,
            boolean required,
            boolean completed) {}

    public record StageDTO(
            Integer pathId,
            String skill,
            String fromLevel,
            String targetLevel,
            String goalLevel,
            String currentLevel,
            String status,
            List<StageLessonDTO> lessons) {
        public StageProgressDTO progress() {
            if (currentLevel == null || lessons == null || lessons.isEmpty()) return null;
            long completedLessons = lessons.stream().filter(StageLessonDTO::completed).count();
            int totalLessons = lessons.size();
            return new StageProgressDTO(
                    completedLessons + "/" + totalLessons + " bài",
                    (int) (completedLessons * 100 / totalLessons));
        }

        public List<StageLessonDTO> missingRequiredLessons() {
            return lessons.stream()
                    .filter(lesson -> lesson.required() && !lesson.completed())
                    .toList();
        }
    }
}
