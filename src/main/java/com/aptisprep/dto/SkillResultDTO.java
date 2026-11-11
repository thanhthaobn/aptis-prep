package com.aptisprep.dto;

import com.aptisprep.entity.ExamSkillResultEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SkillResultDTO {
    private Integer id;
    private Integer attemptId;
    private String skill;
    private Integer correctCount;
    private Integer totalItems;
    private BigDecimal rawScore;
    private BigDecimal scaleScore;
    private String cefrLevel;
    private String estimatedLevel;
    private String gradingMethod;
    private String gradingStatus;
    private String gradingState;
    private BigDecimal objectiveRawScore;
    private BigDecimal subjectiveRawScore;
    private String feedback;
    private Boolean passed = true;
    private Boolean retryable = false;
    private LocalDateTime createdAt;

    public SkillResultDTO() {}

    public SkillResultDTO(
            String skill,
            BigDecimal rawScore,
            BigDecimal scaleScore,
            String estimatedLevel,
            String gradingState,
            String feedback) {
        this(skill, rawScore, scaleScore, estimatedLevel, gradingState, feedback, true, false);
    }

    public SkillResultDTO(
            String skill,
            BigDecimal rawScore,
            BigDecimal scaleScore,
            String estimatedLevel,
            String gradingState,
            String feedback,
            boolean passed) {
        this(skill, rawScore, scaleScore, estimatedLevel, gradingState, feedback, passed, false);
    }

    public SkillResultDTO(
            String skill,
            BigDecimal rawScore,
            BigDecimal scaleScore,
            String estimatedLevel,
            String gradingState,
            String feedback,
            boolean passed,
            boolean retryable) {
        this.skill = skill;
        this.rawScore = rawScore;
        this.scaleScore = scaleScore;
        this.estimatedLevel = estimatedLevel;
        this.cefrLevel = estimatedLevel;
        this.gradingState = gradingState;
        this.gradingStatus = gradingState;
        this.feedback = feedback;
        this.passed = passed;
        this.retryable = retryable;
    }

    public SkillResultDTO(
            Integer id,
            Integer attemptId,
            String skill,
            Integer correctCount,
            Integer totalItems,
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
        this.rawScore = rawScore;
        this.scaleScore = scaleScore;
        this.cefrLevel = cefrLevel;
        this.estimatedLevel = cefrLevel;
        this.gradingMethod = gradingMethod;
        this.gradingStatus = gradingStatus;
        this.gradingState = gradingStatus;
        this.objectiveRawScore = objectiveRawScore;
        this.subjectiveRawScore = subjectiveRawScore;
        this.createdAt = createdAt;
    }

    public static SkillResultDTO convertEntityToDto(ExamSkillResultEntity entity) {
        if (entity == null) return null;
        return new SkillResultDTO(
                entity.getId(),
                entity.getAttemptId(),
                entity.getSkill(),
                entity.getCorrectCount(),
                entity.getTotalItems(),
                entity.getRawScore(),
                entity.getScaleScore(),
                entity.getCefrLevel(),
                entity.getGradingMethod(),
                entity.getGradingStatus(),
                entity.getObjectiveRawScore(),
                entity.getSubjectiveRawScore(),
                entity.getCreatedAt());
    }

    public ExamSkillResultEntity convertDtoToEntity() {
        ExamSkillResultEntity entity =
                new ExamSkillResultEntity(
                        this.attemptId,
                        this.skill,
                        this.correctCount != null ? this.correctCount : 0,
                        this.totalItems != null ? this.totalItems : 0,
                        this.rawScore,
                        this.scaleScore,
                        this.cefrLevel,
                        this.gradingMethod,
                        this.gradingStatus != null ? this.gradingStatus : "PENDING",
                        this.objectiveRawScore,
                        this.subjectiveRawScore,
                        this.createdAt != null ? this.createdAt : LocalDateTime.now());
        if (this.id != null) entity.setId(this.id);
        return entity;
    }

    public void updateEntityFromDto(ExamSkillResultEntity entity) {
        if (entity == null) return;
        entity.setSkill(this.skill);
        if (this.correctCount != null) entity.setCorrectCount(this.correctCount);
        if (this.totalItems != null) entity.setTotalItems(this.totalItems);
        entity.setRawScore(this.rawScore);
        entity.setScaleScore(this.scaleScore);
        entity.setCefrLevel(this.cefrLevel != null ? this.cefrLevel : this.estimatedLevel);
        entity.setGradingMethod(this.gradingMethod);
        entity.setGradingStatus(
                this.gradingStatus != null ? this.gradingStatus : this.gradingState);
        entity.setObjectiveRawScore(this.objectiveRawScore);
        entity.setSubjectiveRawScore(this.subjectiveRawScore);
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
        return cefrLevel != null ? cefrLevel : estimatedLevel;
    }

    public void setCefrLevel(String cefrLevel) {
        this.cefrLevel = cefrLevel;
    }

    public String getEstimatedLevel() {
        return estimatedLevel != null ? estimatedLevel : cefrLevel;
    }

    public void setEstimatedLevel(String estimatedLevel) {
        this.estimatedLevel = estimatedLevel;
    }

    public String getGradingMethod() {
        return gradingMethod;
    }

    public void setGradingMethod(String gradingMethod) {
        this.gradingMethod = gradingMethod;
    }

    public String getGradingStatus() {
        return gradingStatus != null ? gradingStatus : gradingState;
    }

    public void setGradingStatus(String gradingStatus) {
        this.gradingStatus = gradingStatus;
    }

    public String getGradingState() {
        return gradingState != null ? gradingState : gradingStatus;
    }

    public void setGradingState(String gradingState) {
        this.gradingState = gradingState;
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

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public Boolean getRetryable() {
        return retryable;
    }

    public void setRetryable(Boolean retryable) {
        this.retryable = retryable;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String skill() {
        return getSkill();
    }

    public BigDecimal rawScore() {
        return getRawScore();
    }

    public BigDecimal scaleScore() {
        return getScaleScore();
    }

    public String estimatedLevel() {
        return getEstimatedLevel();
    }

    public String cefrLevel() {
        return getCefrLevel();
    }

    public String gradingState() {
        return getGradingState();
    }

    public String feedback() {
        return getFeedback();
    }

    public boolean passed() {
        return passed != null && passed;
    }

    public boolean retryable() {
        return retryable != null && retryable;
    }
}
