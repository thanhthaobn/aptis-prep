package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class ProfileApplicationDTO {
    private Integer id;
    private Integer userId;
    private Integer attemptId;
    private String skill;
    private LocalDateTime appliedAt;

    public ProfileApplicationDTO() {}

    public ProfileApplicationDTO(
            Integer id,
            Integer userId,
            Integer attemptId,
            String skill,
            LocalDateTime appliedAt) {
        this.id = id;
        this.userId = userId;
        this.attemptId = attemptId;
        this.skill = skill;
        this.appliedAt = appliedAt;
    }

    public static ProfileApplicationDTO convertEntityToDto(ProfileApplication entity) {
        if (entity == null) return null;
        return new ProfileApplicationDTO(
                entity.getId(),
                entity.getUserId(),
                entity.getAttemptId(),
                entity.getSkill(),
                entity.getAppliedAt());
    }

    public static ProfileApplication convertDtoToEntity(ProfileApplicationDTO dto) {
        if (dto == null) return null;
        return new ProfileApplication(
                dto.getId(),
                dto.getUserId(),
                dto.getAttemptId(),
                dto.getSkill(),
                dto.getAppliedAt());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }
}











