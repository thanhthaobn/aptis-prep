package com.aptisprep.dto;

public class GoalSkillDTO {
    private Integer id;
    private String skill;
    private String targetLevel;

    public GoalSkillDTO() {}

    public GoalSkillDTO(Integer id, String skill, String targetLevel) {
        this.id = id;
        this.skill = skill;
        this.targetLevel = targetLevel;
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
