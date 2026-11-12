package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class ExamAttemptDTO {
    private Integer id;
    private Integer userId;
    private Integer mockExamId;
    private String status;
    private String gradingStatus;
    private Integer scoringSchemeId;
    private String policyVersion;
    private String rubricVersion;
    private String gradingRequestKey;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;
    private BigDecimal finalScaleScore;
    private String estimatedOverallCefr;
    private String requestKey;
    private LocalDateTime createdAt;
    private Integer answerVersion;
    private List<ExamSkillResultDTO> skillResults;

    public ExamAttemptDTO() {}

    public ExamAttemptDTO(
            Integer id,
            Integer userId,
            Integer mockExamId,
            String status,
            String gradingStatus,
            Integer scoringSchemeId,
            String policyVersion,
            String rubricVersion,
            String gradingRequestKey,
            LocalDateTime startedAt,
            LocalDateTime submittedAt,
            LocalDateTime completedAt,
            BigDecimal finalScaleScore,
            String estimatedOverallCefr,
            String requestKey,
            LocalDateTime createdAt,
            Integer answerVersion) {
        this.id = id;
        this.userId = userId;
        this.mockExamId = mockExamId;
        this.status = status;
        this.gradingStatus = gradingStatus;
        this.scoringSchemeId = scoringSchemeId;
        this.policyVersion = policyVersion;
        this.rubricVersion = rubricVersion;
        this.gradingRequestKey = gradingRequestKey;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.completedAt = completedAt;
        this.finalScaleScore = finalScaleScore;
        this.estimatedOverallCefr = estimatedOverallCefr;
        this.requestKey = requestKey;
        this.createdAt = createdAt;
        this.answerVersion = answerVersion;
    }

    public static ExamAttemptDTO convertEntityToDto(ExamAttempt entity) {
        if (entity == null) return null;
        return new ExamAttemptDTO(
                entity.getId(),
                entity.getUserId(),
                entity.getMockExamId(),
                entity.getStatus(),
                entity.getGradingStatus(),
                entity.getScoringSchemeId(),
                entity.getPolicyVersion(),
                entity.getRubricVersion(),
                entity.getGradingRequestKey(),
                entity.getStartedAt(),
                entity.getSubmittedAt(),
                entity.getCompletedAt(),
                entity.getFinalScaleScore(),
                entity.getEstimatedOverallCefr(),
                entity.getRequestKey(),
                entity.getCreatedAt(),
                entity.getAnswerVersion());
    }

    public static ExamAttempt convertDtoToEntity(ExamAttemptDTO dto) {
        if (dto == null) return null;
        return new ExamAttempt(
                dto.getId(),
                dto.getUserId(),
                dto.getMockExamId(),
                dto.getStatus(),
                dto.getGradingStatus(),
                dto.getAnswerVersion(),
                dto.getScoringSchemeId(),
                dto.getPolicyVersion(),
                dto.getRubricVersion(),
                dto.getGradingRequestKey(),
                dto.getStartedAt(),
                dto.getSubmittedAt(),
                dto.getCompletedAt(),
                dto.getFinalScaleScore(),
                dto.getEstimatedOverallCefr(),
                dto.getRequestKey(),
                dto.getCreatedAt());
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

    public Integer getMockExamId() {
        return mockExamId;
    }

    public void setMockExamId(Integer mockExamId) {
        this.mockExamId = mockExamId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGradingStatus() {
        return gradingStatus;
    }

    public void setGradingStatus(String gradingStatus) {
        this.gradingStatus = gradingStatus;
    }

    public Integer getScoringSchemeId() {
        return scoringSchemeId;
    }

    public void setScoringSchemeId(Integer scoringSchemeId) {
        this.scoringSchemeId = scoringSchemeId;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(String policyVersion) {
        this.policyVersion = policyVersion;
    }

    public String getRubricVersion() {
        return rubricVersion;
    }

    public void setRubricVersion(String rubricVersion) {
        this.rubricVersion = rubricVersion;
    }

    public String getGradingRequestKey() {
        return gradingRequestKey;
    }

    public void setGradingRequestKey(String gradingRequestKey) {
        this.gradingRequestKey = gradingRequestKey;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public BigDecimal getFinalScaleScore() {
        return finalScaleScore;
    }

    public void setFinalScaleScore(BigDecimal finalScaleScore) {
        this.finalScaleScore = finalScaleScore;
    }

    public String getEstimatedOverallCefr() {
        return estimatedOverallCefr;
    }

    public void setEstimatedOverallCefr(String estimatedOverallCefr) {
        this.estimatedOverallCefr = estimatedOverallCefr;
    }

    public String getRequestKey() {
        return requestKey;
    }

    public void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getAnswerVersion() {
        return answerVersion;
    }

    public void setAnswerVersion(Integer answerVersion) {
        this.answerVersion = answerVersion;
    }

    public List<ExamSkillResultDTO> getSkillResults() {
        return skillResults;
    }

    public void setSkillResults(List<ExamSkillResultDTO> skillResults) {
        this.skillResults = skillResults;
    }
}











