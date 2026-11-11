package com.aptisprep.dto;

import com.aptisprep.entity.UserAnswerEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserAnswerDTO {
    private Integer id;
    private Integer attemptId;
    private Integer questionId;
    private String skill;
    private String questionType;
    private String taskContent;
    private String lockedContent;
    private String transcript;
    private String status;
    private Integer answerVersion = 1;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String textAnswer;
    private String audioUrl;
    private BigDecimal aiScore;
    private String aiFeedback;
    private LocalDateTime gradedAt;
    private String aiProvider;
    private String aiModel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<UserAnswerDetailDTO> items = new ArrayList<>();

    public UserAnswerDTO() {}

    public UserAnswerDTO(
            Integer answerId,
            Integer questionId,
            String skill,
            String questionType,
            String taskContent,
            String lockedContent,
            String transcript,
            List<UserAnswerDetailDTO> items) {
        this.id = answerId;
        this.questionId = questionId;
        this.skill = skill;
        this.questionType = questionType;
        this.taskContent = taskContent;
        this.lockedContent = lockedContent;
        this.transcript = transcript;
        this.items = items != null ? items : new ArrayList<>();
    }

    public static UserAnswerDTO convertEntityToDto(UserAnswerEntity entity) {
        if (entity == null) return null;
        UserAnswerDTO dto = new UserAnswerDTO();
        dto.setId(entity.getId());
        dto.setAttemptId(entity.getAttemptId());
        dto.setQuestionId(entity.getQuestionId());
        dto.setStatus(entity.getStatus());
        dto.setAnswerVersion(entity.getAnswerVersion());
        dto.setStartedAt(entity.getStartedAt());
        dto.setCompletedAt(entity.getCompletedAt());
        dto.setTextAnswer(entity.getTextAnswer());
        dto.setAudioUrl(entity.getAudioUrl());
        dto.setAiScore(entity.getAiScore());
        dto.setAiFeedback(entity.getAiFeedback());
        dto.setGradedAt(entity.getGradedAt());
        dto.setTranscript(entity.getTranscript());
        dto.setAiProvider(entity.getAiProvider());
        dto.setAiModel(entity.getAiModel());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public UserAnswerEntity convertDtoToEntity() {
        UserAnswerEntity entity =
                new UserAnswerEntity(
                        null,
                        this.attemptId,
                        this.questionId,
                        this.status != null ? this.status : "IN_PROGRESS",
                        this.answerVersion != null ? this.answerVersion : 1,
                        this.startedAt,
                        this.completedAt,
                        this.textAnswer,
                        this.audioUrl,
                        this.aiScore,
                        this.aiFeedback,
                        this.gradedAt,
                        this.transcript,
                        this.aiProvider,
                        this.aiModel,
                        this.createdAt != null ? this.createdAt : LocalDateTime.now(),
                        this.updatedAt != null ? this.updatedAt : LocalDateTime.now());
        if (this.id != null) entity.setId(this.id);
        return entity;
    }

    public void updateEntityFromDto(UserAnswerEntity entity) {
        if (entity == null) return;
        entity.setStatus(this.status);
        if (this.answerVersion != null) entity.setAnswerVersion(this.answerVersion);
        entity.setCompletedAt(this.completedAt);
        entity.setTextAnswer(this.textAnswer);
        entity.setAudioUrl(this.audioUrl);
        entity.setAiScore(this.aiScore);
        entity.setAiFeedback(this.aiFeedback);
        entity.setGradedAt(this.gradedAt);
        entity.setTranscript(this.transcript);
        entity.setUpdatedAt(LocalDateTime.now());
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

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
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

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAnswerVersion() {
        return answerVersion;
    }

    public void setAnswerVersion(Integer answerVersion) {
        this.answerVersion = answerVersion;
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

    public List<UserAnswerDetailDTO> getItems() {
        return items;
    }

    public void setItems(List<UserAnswerDetailDTO> items) {
        this.items = items;
    }

    public Integer answerId() {
        return getId();
    }

    public Integer questionId() {
        return getQuestionId();
    }

    public String skill() {
        return getSkill();
    }

    public String questionType() {
        return getQuestionType();
    }

    public String taskContent() {
        return getTaskContent();
    }

    public String lockedContent() {
        return getLockedContent();
    }

    public String transcript() {
        return getTranscript();
    }

    public List<UserAnswerDetailDTO> items() {
        return getItems();
    }
}
