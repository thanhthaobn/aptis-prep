package com.aptisprep.dto;

import java.util.List;

public final class LearningPathViewsDTO {
    private LearningPathViewsDTO() {}

    public static class StageProgressDTO {
        private String label;
        private int percent;

        public StageProgressDTO() {}

        public StageProgressDTO(String label, int percent) {
            this.label = label;
            this.percent = percent;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public int getPercent() {
            return percent;
        }

        public void setPercent(int percent) {
            this.percent = percent;
        }

        public int percent() {
            return percent;
        }
    }

    public static class StageLessonDTO {
        private Integer id;
        private String title;
        private String level;
        private String status;
        private boolean available;
        private String topic;
        private boolean required;
        private boolean completed;

        public StageLessonDTO() {}

        public StageLessonDTO(
                Integer id,
                String title,
                String level,
                String status,
                boolean available,
                String topic,
                boolean required,
                boolean completed) {
            this.id = id;
            this.title = title;
            this.level = level;
            this.status = status;
            this.available = available;
            this.topic = topic;
            this.required = required;
            this.completed = completed;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer id() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String level() {
            return level;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String status() {
            return status;
        }

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public boolean available() {
            return available;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String topic() {
            return topic;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public boolean required() {
            return required;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        public boolean completed() {
            return completed;
        }
    }

    public static class StageDTO {
        private Integer pathId;
        private String skill;
        private String fromLevel;
        private String targetLevel;
        private String goalLevel;
        private String currentLevel;
        private String status;
        private List<StageLessonDTO> lessons;

        public StageDTO() {}

        public StageDTO(
                Integer pathId,
                String skill,
                String fromLevel,
                String targetLevel,
                String goalLevel,
                String currentLevel,
                String status,
                List<StageLessonDTO> lessons) {
            this.pathId = pathId;
            this.skill = skill;
            this.fromLevel = fromLevel;
            this.targetLevel = targetLevel;
            this.goalLevel = goalLevel;
            this.currentLevel = currentLevel;
            this.status = status;
            this.lessons = lessons;
        }

        public StageProgressDTO progress() {
            if (currentLevel == null || lessons == null || lessons.isEmpty()) return null;
            long completedLessons = lessons.stream().filter(StageLessonDTO::isCompleted).count();
            int totalLessons = lessons.size();
            return new StageProgressDTO(
                    completedLessons + "/" + totalLessons + " bài",
                    (int) (completedLessons * 100 / totalLessons));
        }

        public List<StageLessonDTO> missingRequiredLessons() {
            if (lessons == null) return List.of();
            return lessons.stream()
                    .filter(lesson -> lesson.isRequired() && !lesson.isCompleted())
                    .toList();
        }

        public Integer getPathId() {
            return pathId;
        }

        public void setPathId(Integer pathId) {
            this.pathId = pathId;
        }

        public Integer pathId() {
            return pathId;
        }

        public String getSkill() {
            return skill;
        }

        public void setSkill(String skill) {
            this.skill = skill;
        }

        public String skill() {
            return skill;
        }

        public String getFromLevel() {
            return fromLevel;
        }

        public void setFromLevel(String fromLevel) {
            this.fromLevel = fromLevel;
        }

        public String fromLevel() {
            return fromLevel;
        }

        public String getTargetLevel() {
            return targetLevel;
        }

        public void setTargetLevel(String targetLevel) {
            this.targetLevel = targetLevel;
        }

        public String targetLevel() {
            return targetLevel;
        }

        public String getGoalLevel() {
            return goalLevel;
        }

        public void setGoalLevel(String goalLevel) {
            this.goalLevel = goalLevel;
        }

        public String goalLevel() {
            return goalLevel;
        }

        public String getCurrentLevel() {
            return currentLevel;
        }

        public void setCurrentLevel(String currentLevel) {
            this.currentLevel = currentLevel;
        }

        public String currentLevel() {
            return currentLevel;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String status() {
            return status;
        }

        public List<StageLessonDTO> getLessons() {
            return lessons;
        }

        public void setLessons(List<StageLessonDTO> lessons) {
            this.lessons = lessons;
        }

        public List<StageLessonDTO> lessons() {
            return lessons;
        }
    }
}
