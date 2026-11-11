package com.aptisprep.dto;

public class SkillLevelDTO {
    private String skill;
    private String estimatedLevel;
    private Integer sourceAttemptId;

    public SkillLevelDTO() {}

    public SkillLevelDTO(String skill, String estimatedLevel, Integer sourceAttemptId) {
        this.skill = skill;
        this.estimatedLevel = estimatedLevel;
        this.sourceAttemptId = sourceAttemptId;
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

    public String getEstimatedLevel() {
        return estimatedLevel;
    }

    public void setEstimatedLevel(String estimatedLevel) {
        this.estimatedLevel = estimatedLevel;
    }

    public String estimatedLevel() {
        return estimatedLevel;
    }

    public Integer getSourceAttemptId() {
        return sourceAttemptId;
    }

    public void setSourceAttemptId(Integer sourceAttemptId) {
        this.sourceAttemptId = sourceAttemptId;
    }

    public Integer sourceAttemptId() {
        return sourceAttemptId;
    }
}
