package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class LearningPathDTO {
    private Integer id;
    private User user;
    private Integer goalSkillId;
    private Integer sourceAttemptId;
    private String skill;
    private String fromLevel;
    private String targetLevel;
    private String pathSource;
    private Integer checkpointExamId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public LearningPathDTO() {}

    public LearningPathDTO(
            Integer id,
            User user,
            Integer goalSkillId,
            Integer sourceAttemptId,
            String skill,
            String fromLevel,
            String targetLevel,
            String pathSource,
            Integer checkpointExamId,
            String status,
            LocalDateTime createdAt,
            LocalDateTime completedAt) {
        this.id = id;
        this.user = user;
        this.goalSkillId = goalSkillId;
        this.sourceAttemptId = sourceAttemptId;
        this.skill = skill;
        this.fromLevel = fromLevel;
        this.targetLevel = targetLevel;
        this.pathSource = pathSource;
        this.checkpointExamId = checkpointExamId;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public static LearningPathDTO convertEntityToDto(LearningPath entity) {
        if (entity == null) return null;
        return new LearningPathDTO(
                entity.getId(),
                entity.getUser(),
                entity.getGoalSkillId(),
                entity.getSourceAttemptId(),
                entity.getSkill(),
                entity.getFromLevel(),
                entity.getTargetLevel(),
                entity.getPathSource(),
                entity.getCheckpointExamId(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getCompletedAt());
    }

    public static LearningPath convertDtoToEntity(LearningPathDTO dto) {
        if (dto == null) return null;
        return new LearningPath(
                dto.getId(),
                dto.getUser(),
                dto.getGoalSkillId(),
                dto.getSourceAttemptId(),
                dto.getSkill(),
                dto.getFromLevel(),
                dto.getTargetLevel(),
                dto.getPathSource(),
                dto.getCheckpointExamId(),
                dto.getStatus(),
                dto.getCreatedAt(),
                dto.getCompletedAt());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getGoalSkillId() {
        return goalSkillId;
    }

    public void setGoalSkillId(Integer goalSkillId) {
        this.goalSkillId = goalSkillId;
    }

    public Integer getSourceAttemptId() {
        return sourceAttemptId;
    }

    public void setSourceAttemptId(Integer sourceAttemptId) {
        this.sourceAttemptId = sourceAttemptId;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String getFromLevel() {
        return fromLevel;
    }

    public void setFromLevel(String fromLevel) {
        this.fromLevel = fromLevel;
    }

    public String getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(String targetLevel) {
        this.targetLevel = targetLevel;
    }

    public String getPathSource() {
        return pathSource;
    }

    public void setPathSource(String pathSource) {
        this.pathSource = pathSource;
    }

    public Integer getCheckpointExamId() {
        return checkpointExamId;
    }

    public void setCheckpointExamId(Integer checkpointExamId) {
        this.checkpointExamId = checkpointExamId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}











