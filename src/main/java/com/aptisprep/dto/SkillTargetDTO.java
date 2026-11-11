package com.aptisprep.dto;

public class SkillTargetDTO {
    private String skill;
    private String targetLevel;

    public SkillTargetDTO() {}

    public SkillTargetDTO(String skill, String targetLevel) {
        this.skill = skill;
        this.targetLevel = targetLevel;
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

    public String getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(String targetLevel) {
        this.targetLevel = targetLevel;
    }

    public String targetLevel() {
        return targetLevel;
    }
}
