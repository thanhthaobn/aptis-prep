package com.aptisprep.dto;

public class LessonRecommendationDTO {
    private Integer lessonId;
    private String title;
    private String skill;
    private PathProgressDTO path;
    private String reason;

    public LessonRecommendationDTO() {}

    public LessonRecommendationDTO(
            Integer lessonId, String title, String skill, PathProgressDTO path, String reason) {
        this.lessonId = lessonId;
        this.title = title;
        this.skill = skill;
        this.path = path;
        this.reason = reason;
    }

    public Integer getLessonId() {
        return lessonId;
    }

    public void setLessonId(Integer lessonId) {
        this.lessonId = lessonId;
    }

    public Integer lessonId() {
        return lessonId;
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

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String skill() {
        return skill;
    }

    public PathProgressDTO getPath() {
        return path;
    }

    public void setPath(PathProgressDTO path) {
        this.path = path;
    }

    public PathProgressDTO path() {
        return path;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
