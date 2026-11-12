package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class UserGoalSkillDTO {
    private Integer id;
    private UserGoal goal;
    private String skill;
    private String targetLevel;
    private BigDecimal targetScore;

    public UserGoalSkillDTO() {}

    public UserGoalSkillDTO(
            Integer id,
            UserGoal goal,
            String skill,
            String targetLevel,
            BigDecimal targetScore) {
        this.id = id;
        this.goal = goal;
        this.skill = skill;
        this.targetLevel = targetLevel;
        this.targetScore = targetScore;
    }

    public static UserGoalSkillDTO convertEntityToDto(UserGoalSkill entity) {
        if (entity == null) return null;
        return new UserGoalSkillDTO(
                entity.getId(),
                entity.getGoal(),
                entity.getSkill(),
                entity.getTargetLevel(),
                entity.getTargetScore());
    }

    public static UserGoalSkill convertDtoToEntity(UserGoalSkillDTO dto) {
        if (dto == null) return null;
        return new UserGoalSkill(
                dto.getId(),
                dto.getGoal(),
                dto.getSkill(),
                dto.getTargetLevel(),
                dto.getTargetScore());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public UserGoal getGoal() {
        return goal;
    }

    public void setGoal(UserGoal goal) {
        this.goal = goal;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(String targetLevel) {
        this.targetLevel = targetLevel;
    }

    public BigDecimal getTargetScore() {
        return targetScore;
    }

    public void setTargetScore(BigDecimal targetScore) {
        this.targetScore = targetScore;
    }
}











