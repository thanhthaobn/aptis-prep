package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class UserAnswerDTO {
    private String audioManifest;
    private Integer id;
    private Integer attemptId;
    private Integer questionId;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String textAnswer;
    private String audioUrl;
    private BigDecimal aiScore;
    private String aiFeedback;
    private LocalDateTime gradedAt;
    private String transcript;
    private String aiProvider;
    private String aiModel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int answerVersion;

    public UserAnswerDTO() {}

    public UserAnswerDTO(
            String audioManifest,
            Integer id,
            Integer attemptId,
            Integer questionId,
            String status,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            String textAnswer,
            String audioUrl,
            BigDecimal aiScore,
            String aiFeedback,
            LocalDateTime gradedAt,
            String transcript,
            String aiProvider,
            String aiModel,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int answerVersion) {
        this.audioManifest = audioManifest;
        this.id = id;
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.textAnswer = textAnswer;
        this.audioUrl = audioUrl;
        this.aiScore = aiScore;
        this.aiFeedback = aiFeedback;
        this.gradedAt = gradedAt;
        this.transcript = transcript;
        this.aiProvider = aiProvider;
        this.aiModel = aiModel;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.answerVersion = answerVersion;
    }

    public static UserAnswerDTO convertEntityToDto(UserAnswer entity) {
        if (entity == null) return null;
        return new UserAnswerDTO(
                entity.getAudioManifest(),
                entity.getId(),
                entity.getAttemptId(),
                entity.getQuestionId(),
                entity.getStatus(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getTextAnswer(),
                entity.getAudioUrl(),
                entity.getAiScore(),
                entity.getAiFeedback(),
                entity.getGradedAt(),
                entity.getTranscript(),
                entity.getAiProvider(),
                entity.getAiModel(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getAnswerVersion());
    }

    public static UserAnswer convertDtoToEntity(UserAnswerDTO dto) {
        if (dto == null) return null;
        return new UserAnswer(
                dto.getAudioManifest(),
                dto.getId(),
                dto.getAttemptId(),
                dto.getQuestionId(),
                dto.getStatus(),
                dto.getAnswerVersion(),
                dto.getStartedAt(),
                dto.getCompletedAt(),
                dto.getTextAnswer(),
                dto.getAudioUrl(),
                dto.getAiScore(),
                dto.getAiFeedback(),
                dto.getGradedAt(),
                dto.getTranscript(),
                dto.getAiProvider(),
                dto.getAiModel(),
                dto.getCreatedAt(),
                dto.getUpdatedAt());
    }

    public String getAudioManifest() {
        return audioManifest;
    }

    public void setAudioManifest(String audioManifest) {
        this.audioManifest = audioManifest;
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

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getTextAnswer() {
        return textAnswer;
    }

    public void setTextAnswer(String textAnswer) {
        this.textAnswer = textAnswer;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public BigDecimal getAiScore() {
        return aiScore;
    }

    public void setAiScore(BigDecimal aiScore) {
        this.aiScore = aiScore;
    }

    public String getAiFeedback() {
        return aiFeedback;
    }

    public void setAiFeedback(String aiFeedback) {
        this.aiFeedback = aiFeedback;
    }

    public LocalDateTime getGradedAt() {
        return gradedAt;
    }

    public void setGradedAt(LocalDateTime gradedAt) {
        this.gradedAt = gradedAt;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public void setAiProvider(String aiProvider) {
        this.aiProvider = aiProvider;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
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

    public int getAnswerVersion() {
        return answerVersion;
    }

    public void setAnswerVersion(int answerVersion) {
        this.answerVersion = answerVersion;
    }
}











