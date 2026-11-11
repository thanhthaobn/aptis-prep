package com.aptisprep.dto;

public class PathProgressDTO {
    private Integer pathId;
    private String skill;
    private String pathSource;
    private String pathStatus;
    private int completedLessons;
    private int totalLessons;
    private Integer progressPercent;
    private int completedRequiredLessons;
    private int totalRequiredLessons;
    private Integer nextLessonId;
    private String nextLessonTitle;
    private String checkpointState;

    public PathProgressDTO() {}

    public PathProgressDTO(
            Integer pathId,
            String skill,
            String pathSource,
            String pathStatus,
            int completedLessons,
            int totalLessons,
            Integer progressPercent,
            int completedRequiredLessons,
            int totalRequiredLessons,
            Integer nextLessonId,
            String nextLessonTitle,
            String checkpointState) {
        this.pathId = pathId;
        this.skill = skill;
        this.pathSource = pathSource;
        this.pathStatus = pathStatus;
        this.completedLessons = completedLessons;
        this.totalLessons = totalLessons;
        this.progressPercent = progressPercent;
        this.completedRequiredLessons = completedRequiredLessons;
        this.totalRequiredLessons = totalRequiredLessons;
        this.nextLessonId = nextLessonId;
        this.nextLessonTitle = nextLessonTitle;
        this.checkpointState = checkpointState;
    }

    public String checkpointActionLabel() {
        return switch (checkpointState == null ? "" : checkpointState) {
            case "in_progress" -> "Tiếp tục bài đang làm";
            case "grading" -> "Xem trạng thái chấm";
            case "failed" -> "Làm lại kiểm tra cuối chặng";
            default -> "Bắt đầu bài kiểm tra cuối chặng";
        };
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

    public String getPathSource() {
        return pathSource;
    }

    public void setPathSource(String pathSource) {
        this.pathSource = pathSource;
    }

    public String pathSource() {
        return pathSource;
    }

    public String getPathStatus() {
        return pathStatus;
    }

    public void setPathStatus(String pathStatus) {
        this.pathStatus = pathStatus;
    }

    public String pathStatus() {
        return pathStatus;
    }

    public int getCompletedLessons() {
        return completedLessons;
    }

    public void setCompletedLessons(int completedLessons) {
        this.completedLessons = completedLessons;
    }

    public int completedLessons() {
        return completedLessons;
    }

    public int getTotalLessons() {
        return totalLessons;
    }

    public void setTotalLessons(int totalLessons) {
        this.totalLessons = totalLessons;
    }

    public int totalLessons() {
        return totalLessons;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    public Integer progressPercent() {
        return progressPercent;
    }

    public int getCompletedRequiredLessons() {
        return completedRequiredLessons;
    }

    public void setCompletedRequiredLessons(int completedRequiredLessons) {
        this.completedRequiredLessons = completedRequiredLessons;
    }

    public int completedRequiredLessons() {
        return completedRequiredLessons;
    }

    public int getTotalRequiredLessons() {
        return totalRequiredLessons;
    }

    public void setTotalRequiredLessons(int totalRequiredLessons) {
        this.totalRequiredLessons = totalRequiredLessons;
    }

    public int totalRequiredLessons() {
        return totalRequiredLessons;
    }

    public Integer getNextLessonId() {
        return nextLessonId;
    }

    public void setNextLessonId(Integer nextLessonId) {
        this.nextLessonId = nextLessonId;
    }

    public Integer nextLessonId() {
        return nextLessonId;
    }

    public String getNextLessonTitle() {
        return nextLessonTitle;
    }

    public void setNextLessonTitle(String nextLessonTitle) {
        this.nextLessonTitle = nextLessonTitle;
    }

    public String nextLessonTitle() {
        return nextLessonTitle;
    }

    public String getCheckpointState() {
        return checkpointState;
    }

    public void setCheckpointState(String checkpointState) {
        this.checkpointState = checkpointState;
    }

    public String checkpointState() {
        return checkpointState;
    }
}
