package com.aptisprep.dto;

import java.util.List;

public class LearnerContextDTO {
    private Integer userId;
    private List<SkillTargetDTO> targets;
    private List<SkillLevelDTO> levels;

    public LearnerContextDTO() {}

    public LearnerContextDTO(
            Integer userId, List<SkillTargetDTO> targets, List<SkillLevelDTO> levels) {
        this.userId = userId;
        this.targets = targets;
        this.levels = levels;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer userId() {
        return userId;
    }

    public List<SkillTargetDTO> getTargets() {
        return targets;
    }

    public void setTargets(List<SkillTargetDTO> targets) {
        this.targets = targets;
    }

    public List<SkillTargetDTO> targets() {
        return targets;
    }

    public List<SkillLevelDTO> getLevels() {
        return levels;
    }

    public void setLevels(List<SkillLevelDTO> levels) {
        this.levels = levels;
    }

    public List<SkillLevelDTO> levels() {
        return levels;
    }
}
