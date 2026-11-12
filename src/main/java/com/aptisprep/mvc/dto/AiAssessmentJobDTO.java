package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class AiAssessmentJobDTO {
    private Integer id;
    private String assessmentKey;
    private Integer userId;
    private Integer attemptId;
    private Integer answerId;
    private Integer sourceId;
    private Integer answerVersion;
    private String skill;
    private String taskType;
    private String taskContent;
    private String lockedContent;
    private String rubricVersion;
    private String policyVersion;
    private BigDecimal maxScore;
    private String transcript;
    private AiJobState state;
    private int retryCount;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime nextRetryAt;
    private String processingToken;
    private String sourceType;

    public AiAssessmentJobDTO() {}

    public AiAssessmentJobDTO(
            String assessmentKey,
            Integer userId,
            Integer attemptId,
            Integer answerId,
            Integer answerVersion,
            String skill,
            String taskType,
            String taskContent,
            String lockedContent,
            String rubricVersion,
            String policyVersion,
            BigDecimal maxScore,
            String transcript) {
        this(
                assessmentKey,
                userId,
                attemptId,
                answerId,
                answerVersion,
                skill,
                taskType,
                taskContent,
                lockedContent,
                rubricVersion,
                policyVersion,
                maxScore,
                transcript,
                "EXAM_ANSWER",
                answerId);
    }

    public AiAssessmentJobDTO(
            String assessmentKey,
            Integer userId,
            Integer attemptId,
            Integer answerId,
            Integer answerVersion,
            String skill,
            String taskType,
            String taskContent,
            String lockedContent,
            String rubricVersion,
            String policyVersion,
            BigDecimal maxScore,
            String transcript,
            String sourceType,
            Integer sourceId) {
        this(
                null,
                assessmentKey,
                userId,
                attemptId,
                answerId,
                sourceId,
                answerVersion,
                skill,
                taskType,
                taskContent,
                lockedContent,
                rubricVersion,
                policyVersion,
                maxScore,
                transcript,
                AiJobState.PENDING,
                0,
                null,
                null,
                null,
                null,
                null,
                sourceType);
    }

    public AiAssessmentJobDTO(
            Integer id,
            String assessmentKey,
            Integer userId,
            Integer attemptId,
            Integer answerId,
            Integer sourceId,
            Integer answerVersion,
            String skill,
            String taskType,
            String taskContent,
            String lockedContent,
            String rubricVersion,
            String policyVersion,
            BigDecimal maxScore,
            String transcript,
            AiJobState state,
            int retryCount,
            String lastError,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime nextRetryAt,
            String processingToken,
            String sourceType) {
        this.id = id;
        this.assessmentKey = assessmentKey;
        this.userId = userId;
        this.attemptId = attemptId;
        this.answerId = answerId;
        this.sourceId = sourceId;
        this.answerVersion = answerVersion;
        this.skill = skill;
        this.taskType = taskType;
        this.taskContent = taskContent;
        this.lockedContent = lockedContent;
        this.rubricVersion = rubricVersion;
        this.policyVersion = policyVersion;
        this.maxScore = maxScore;
        this.transcript = transcript;
        this.state = state;
        this.retryCount = retryCount;
        this.lastError = lastError;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.nextRetryAt = nextRetryAt;
        this.processingToken = processingToken;
        this.sourceType = sourceType;
    }

    public static AiAssessmentJobDTO convertEntityToDto(AiAssessmentJob entity) {
        if (entity == null) return null;
        return new AiAssessmentJobDTO(
                entity.getId(),
                entity.getAssessmentKey(),
                entity.getUserId(),
                entity.getAttemptId(),
                entity.getAnswerId(),
                entity.getSourceId(),
                entity.getAnswerVersion(),
                entity.getSkill(),
                entity.getTaskType(),
                entity.getTaskContent(),
                entity.getLockedContent(),
                entity.getRubricVersion(),
                entity.getPolicyVersion(),
                entity.getMaxScore(),
                entity.getTranscript(),
                entity.getState(),
                entity.getRetryCount(),
                entity.getLastError(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getNextRetryAt(),
                entity.getProcessingToken(),
                entity.getSourceType());
    }

    public static AiAssessmentJob convertDtoToEntity(AiAssessmentJobDTO dto) {
        if (dto == null) return null;
        return new AiAssessmentJob(
                dto.getId(),
                dto.getAssessmentKey(),
                dto.getUserId(),
                dto.getAttemptId(),
                dto.getAnswerId(),
                dto.getSourceType(),
                dto.getSourceId(),
                dto.getAnswerVersion(),
                dto.getSkill(),
                dto.getTaskType(),
                dto.getTaskContent(),
                dto.getLockedContent(),
                dto.getRubricVersion(),
                dto.getPolicyVersion(),
                dto.getMaxScore(),
                dto.getTranscript(),
                dto.getState(),
                dto.getRetryCount(),
                dto.getLastError(),
                dto.getCreatedAt(),
                dto.getUpdatedAt(),
                dto.getNextRetryAt(),
                dto.getProcessingToken());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAssessmentKey() {
        return assessmentKey;
    }

    public void setAssessmentKey(String assessmentKey) {
        this.assessmentKey = assessmentKey;
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

    public Integer getAnswerId() {
        return answerId;
    }

    public void setAnswerId(Integer answerId) {
        this.answerId = answerId;
    }

    public Integer getSourceId() {
        return sourceId;
    }

    public void setSourceId(Integer sourceId) {
        this.sourceId = sourceId;
    }

    public Integer getAnswerVersion() {
        return answerVersion;
    }

    public void setAnswerVersion(Integer answerVersion) {
        this.answerVersion = answerVersion;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getTaskContent() {
        return taskContent;
    }

    public void setTaskContent(String taskContent) {
        this.taskContent = taskContent;
    }

    public String getLockedContent() {
        return lockedContent;
    }

    public void setLockedContent(String lockedContent) {
        this.lockedContent = lockedContent;
    }

    public String getRubricVersion() {
        return rubricVersion;
    }

    public void setRubricVersion(String rubricVersion) {
        this.rubricVersion = rubricVersion;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(String policyVersion) {
        this.policyVersion = policyVersion;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public AiJobState getState() {
        return state;
    }

    public void setState(AiJobState state) {
        this.state = state;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public String getProcessingToken() {
        return processingToken;
    }

    public void setProcessingToken(String processingToken) {
        this.processingToken = processingToken;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
}
