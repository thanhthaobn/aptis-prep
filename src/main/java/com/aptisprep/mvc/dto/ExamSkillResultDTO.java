package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class ExamSkillResultDTO {
    private Integer id;
    private Integer attemptId;
    private String skill;
    private Integer correctCount;
    private Integer totalItems;
    private BigDecimal rawMax;
    private BigDecimal rawScore;
    private BigDecimal scaleScore;
    private String cefrLevel;
    private String gradingMethod;
    private String gradingStatus;
    private BigDecimal objectiveRawScore;
    private BigDecimal subjectiveRawScore;
    private LocalDateTime createdAt;

    public ExamSkillResultDTO() {}

    public ExamSkillResultDTO(String skill, BigDecimal scaleScore, String cefrLevel) {
        this.skill = skill;
        this.scaleScore = scaleScore;
        this.cefrLevel = cefrLevel;
    }

    public ExamSkillResultDTO(
            Integer id,
            Integer attemptId,
            String skill,
            Integer correctCount,
            Integer totalItems,
            BigDecimal rawMax,
            BigDecimal rawScore,
            BigDecimal scaleScore,
            String cefrLevel,
            String gradingMethod,
            String gradingStatus,
            BigDecimal objectiveRawScore,
            BigDecimal subjectiveRawScore,
            LocalDateTime createdAt) {
        this.id = id;
        this.attemptId = attemptId;
        this.skill = skill;
        this.correctCount = correctCount;
        this.totalItems = totalItems;
        this.rawMax = rawMax;
        this.rawScore = rawScore;
        this.scaleScore = scaleScore;
        this.cefrLevel = cefrLevel;
        this.gradingMethod = gradingMethod;
        this.gradingStatus = gradingStatus;
        this.objectiveRawScore = objectiveRawScore;
        this.subjectiveRawScore = subjectiveRawScore;
        this.createdAt = createdAt;
    }

    public static ExamSkillResultDTO convertEntityToDto(ExamSkillResult entity) {
        if (entity == null) return null;
        return new ExamSkillResultDTO(
                entity.getId(),
                entity.getAttemptId(),
                entity.getSkill(),
                entity.getCorrectCount(),
                entity.getTotalItems(),
                entity.getRawMax(),
                entity.getRawScore(),
                entity.getScaleScore(),
                entity.getCefrLevel(),
                entity.getGradingMethod(),
                entity.getGradingStatus(),
                entity.getObjectiveRawScore(),
                entity.getSubjectiveRawScore(),
                entity.getCreatedAt());
    }

    public static ExamSkillResult convertDtoToEntity(ExamSkillResultDTO dto) {
        if (dto == null) return null;
        return new ExamSkillResult(
                dto.getId(),
                dto.getAttemptId(),
                dto.getSkill(),
                dto.getCorrectCount(),
                dto.getTotalItems(),
                dto.getRawMax(),
                dto.getRawScore(),
                dto.getScaleScore(),
                dto.getCefrLevel(),
                dto.getGradingMethod(),
                dto.getGradingStatus(),
                dto.getObjectiveRawScore(),
                dto.getSubjectiveRawScore(),
                dto.getCreatedAt());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Integer getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(Integer correctCount) {
        this.correctCount = correctCount;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public BigDecimal getRawMax() {
        return rawMax;
    }

    public void setRawMax(BigDecimal rawMax) {
        this.rawMax = rawMax;
    }

    public BigDecimal getRawScore() {
        return rawScore;
    }

    public void setRawScore(BigDecimal rawScore) {
        this.rawScore = rawScore;
    }

    public BigDecimal getScaleScore() {
        return scaleScore;
    }

    public void setScaleScore(BigDecimal scaleScore) {
        this.scaleScore = scaleScore;
    }

    public String getCefrLevel() {
        return cefrLevel;
    }

    public void setCefrLevel(String cefrLevel) {
        this.cefrLevel = cefrLevel;
    }

    public String getGradingMethod() {
        return gradingMethod;
    }

    public void setGradingMethod(String gradingMethod) {
        this.gradingMethod = gradingMethod;
    }

    public String getGradingStatus() {
        return gradingStatus;
    }

    public void setGradingStatus(String gradingStatus) {
        this.gradingStatus = gradingStatus;
    }

    public BigDecimal getObjectiveRawScore() {
        return objectiveRawScore;
    }

    public void setObjectiveRawScore(BigDecimal objectiveRawScore) {
        this.objectiveRawScore = objectiveRawScore;
    }

    public BigDecimal getSubjectiveRawScore() {
        return subjectiveRawScore;
    }

    public void setSubjectiveRawScore(BigDecimal subjectiveRawScore) {
        this.subjectiveRawScore = subjectiveRawScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}











