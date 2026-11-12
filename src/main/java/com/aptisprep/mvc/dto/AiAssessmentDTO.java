package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class AiAssessmentDTO {
    private Integer id;
    private AiAssessmentJob job;
    private String assessmentKey;
    private Integer answerId;
    private Integer answerVersion;
    private AiAssessmentState state;
    private BigDecimal score;
    private String estimatedCefr;
    private String feedback;
    private String transcript;
    private String rubricVersion;
    private String policyVersion;
    private String provider;
    private String model;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public AiAssessmentDTO() {}

    public AiAssessmentDTO(
            Integer id,
            String assessmentKey,
            Integer answerId,
            Integer answerVersion,
            String state,
            BigDecimal score,
            String estimatedCefr,
            String feedback,
            String transcript,
            String rubricVersion,
            String policyVersion,
            String provider,
            String model,
            String errorMessage,
            LocalDateTime createdAt,
            LocalDateTime completedAt) {
        this(
                id,
                null,
                assessmentKey,
                answerId,
                answerVersion,
                state == null ? null : AiAssessmentState.valueOf(state),
                score,
                estimatedCefr,
                feedback,
                transcript,
                rubricVersion,
                policyVersion,
                provider,
                model,
                errorMessage,
                createdAt,
                completedAt);
    }

    public AiAssessmentDTO(Integer id, AiAssessmentJob job, String assessmentKey, Integer answerId, Integer answerVersion, AiAssessmentState state, BigDecimal score, String estimatedCefr, String feedback, String transcript, String rubricVersion, String policyVersion, String provider, String model, String errorMessage, LocalDateTime createdAt, LocalDateTime completedAt){
        this.id = id;
        this.job = job;
        this.assessmentKey = assessmentKey;
        this.answerId = answerId;
        this.answerVersion = answerVersion;
        this.state = state;
        this.score = score;
        this.estimatedCefr = estimatedCefr;
        this.feedback = feedback;
        this.transcript = transcript;
        this.rubricVersion = rubricVersion;
        this.policyVersion = policyVersion;
        this.provider = provider;
        this.model = model;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public static AiAssessmentDTO convertEntityToDto(AiAssessment entity) {
        if (entity == null) return null;
        return new AiAssessmentDTO(
                entity.getId(),
                entity.getJob(),
                entity.getAssessmentKey(),
                entity.getAnswerId(),
                entity.getAnswerVersion(),
                entity.getState(),
                entity.getScore(),
                entity.getEstimatedCefr(),
                entity.getFeedback(),
                entity.getTranscript(),
                entity.getRubricVersion(),
                entity.getPolicyVersion(),
                entity.getProvider(),
                entity.getModel(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getCompletedAt());
    }

    public static AiAssessment convertDtoToEntity(AiAssessmentDTO dto) {
        if (dto == null) return null;
        return new AiAssessment(
                dto.getId(),
                dto.getJob(),
                dto.getAssessmentKey(),
                dto.getAnswerId(),
                dto.getAnswerVersion(),
                dto.getState(),
                dto.getScore(),
                dto.getEstimatedCefr(),
                dto.getFeedback(),
                dto.getTranscript(),
                dto.getRubricVersion(),
                dto.getPolicyVersion(),
                dto.getProvider(),
                dto.getModel(),
                dto.getErrorMessage(),
                dto.getCreatedAt(),
                dto.getCompletedAt());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public AiAssessmentJob getJob() {
        return job;
    }

    public void setJob(AiAssessmentJob job) {
        this.job = job;
    }

    public String getAssessmentKey() {
        return assessmentKey;
    }

    public void setAssessmentKey(String assessmentKey) {
        this.assessmentKey = assessmentKey;
    }

    public Integer getAnswerId() {
        return answerId;
    }

    public void setAnswerId(Integer answerId) {
        this.answerId = answerId;
    }

    public Integer getAnswerVersion() {
        return answerVersion;
    }

    public void setAnswerVersion(Integer answerVersion) {
        this.answerVersion = answerVersion;
    }

    public AiAssessmentState getState() {
        return state;
    }

    public void setState(AiAssessmentState state) {
        this.state = state;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getEstimatedCefr() {
        return estimatedCefr;
    }

    public void setEstimatedCefr(String estimatedCefr) {
        this.estimatedCefr = estimatedCefr;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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











