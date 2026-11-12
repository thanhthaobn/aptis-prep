package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class UserSkillProfileDTO {
    private Integer id;
    private User user;
    private String skill;
    private String currentLevel;
    private Integer sourceAttemptId;
    private String checkpointFloorLevel;
    private Integer checkpointSourceAttemptId;
    private Integer checkpointLastAttemptId;
    private LocalDateTime checkpointLastSubmittedAt;
    private String placementLevel;
    private LocalDateTime placementSubmittedAt;
    private LocalDateTime updatedAt;

    public UserSkillProfileDTO() {}

    public UserSkillProfileDTO(
            Integer id,
            User user,
            String skill,
            String currentLevel,
            Integer sourceAttemptId,
            String checkpointFloorLevel,
            Integer checkpointSourceAttemptId,
            Integer checkpointLastAttemptId,
            LocalDateTime checkpointLastSubmittedAt,
            String placementLevel,
            LocalDateTime placementSubmittedAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.skill = skill;
        this.currentLevel = currentLevel;
        this.sourceAttemptId = sourceAttemptId;
        this.checkpointFloorLevel = checkpointFloorLevel;
        this.checkpointSourceAttemptId = checkpointSourceAttemptId;
        this.checkpointLastAttemptId = checkpointLastAttemptId;
        this.checkpointLastSubmittedAt = checkpointLastSubmittedAt;
        this.placementLevel = placementLevel;
        this.placementSubmittedAt = placementSubmittedAt;
        this.updatedAt = updatedAt;
    }

    public static UserSkillProfileDTO convertEntityToDto(UserSkillProfile entity) {
        if (entity == null) return null;
        return new UserSkillProfileDTO(
                entity.getId(),
                entity.getUser(),
                entity.getSkill(),
                entity.getCurrentLevel(),
                entity.getSourceAttemptId(),
                entity.getCheckpointFloorLevel(),
                entity.getCheckpointSourceAttemptId(),
                entity.getCheckpointLastAttemptId(),
                entity.getCheckpointLastSubmittedAt(),
                entity.getPlacementLevel(),
                entity.getPlacementSubmittedAt(),
                entity.getUpdatedAt());
    }

    public static UserSkillProfile convertDtoToEntity(UserSkillProfileDTO dto) {
        if (dto == null) return null;
        return new UserSkillProfile(
                dto.getId(),
                dto.getUser(),
                dto.getSkill(),
                dto.getCurrentLevel(),
                dto.getSourceAttemptId(),
                dto.getCheckpointFloorLevel(),
                dto.getCheckpointSourceAttemptId(),
                dto.getCheckpointLastAttemptId(),
                dto.getCheckpointLastSubmittedAt(),
                dto.getPlacementLevel(),
                dto.getPlacementSubmittedAt(),
                dto.getUpdatedAt());
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

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(String currentLevel) {
        this.currentLevel = currentLevel;
    }

    public Integer getSourceAttemptId() {
        return sourceAttemptId;
    }

    public void setSourceAttemptId(Integer sourceAttemptId) {
        this.sourceAttemptId = sourceAttemptId;
    }

    public String getCheckpointFloorLevel() {
        return checkpointFloorLevel;
    }

    public void setCheckpointFloorLevel(String checkpointFloorLevel) {
        this.checkpointFloorLevel = checkpointFloorLevel;
    }

    public Integer getCheckpointSourceAttemptId() {
        return checkpointSourceAttemptId;
    }

    public void setCheckpointSourceAttemptId(Integer checkpointSourceAttemptId) {
        this.checkpointSourceAttemptId = checkpointSourceAttemptId;
    }

    public Integer getCheckpointLastAttemptId() {
        return checkpointLastAttemptId;
    }

    public void setCheckpointLastAttemptId(Integer checkpointLastAttemptId) {
        this.checkpointLastAttemptId = checkpointLastAttemptId;
    }

    public LocalDateTime getCheckpointLastSubmittedAt() {
        return checkpointLastSubmittedAt;
    }

    public void setCheckpointLastSubmittedAt(LocalDateTime checkpointLastSubmittedAt) {
        this.checkpointLastSubmittedAt = checkpointLastSubmittedAt;
    }

    public String getPlacementLevel() {
        return placementLevel;
    }

    public void setPlacementLevel(String placementLevel) {
        this.placementLevel = placementLevel;
    }

    public LocalDateTime getPlacementSubmittedAt() {
        return placementSubmittedAt;
    }

    public void setPlacementSubmittedAt(LocalDateTime placementSubmittedAt) {
        this.placementSubmittedAt = placementSubmittedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
