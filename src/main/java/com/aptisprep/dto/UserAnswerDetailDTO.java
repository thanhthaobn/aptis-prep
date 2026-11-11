package com.aptisprep.dto;

import com.aptisprep.entity.UserAnswerDetailEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UserAnswerDetailDTO {
    private Integer id;
    private Integer userAnswerId;
    private Integer questionItemId;
    private Integer selectedOptionId;
    private Integer correctOptionId;
    private Integer userPosition;
    private Integer correctPosition;
    private String answerText;
    private String audioPath;
    private String transcript;
    private String taskContent;
    private String lockedContent;
    private Integer recordingDurationSeconds;
    private BigDecimal autoScore;
    private String feedback;
    private String gradingStatus;
    private LocalDateTime gradedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserAnswerDetailDTO() {}

    public UserAnswerDetailDTO(
            Integer itemId,
            int position,
            String taskContent,
            String lockedContent,
            Integer correctOptionId,
            Integer correctPosition) {
        this.questionItemId = itemId;
        this.userPosition = position;
        this.taskContent = taskContent;
        this.lockedContent = lockedContent;
        this.correctOptionId = correctOptionId;
        this.correctPosition = correctPosition;
    }

    public static UserAnswerDetailDTO convertEntityToDto(UserAnswerDetailEntity entity) {
        if (entity == null) return null;
        UserAnswerDetailDTO dto = new UserAnswerDetailDTO();
        dto.setId(entity.getId());
        dto.setUserAnswerId(entity.getUserAnswerId());
        dto.setQuestionItemId(entity.getQuestionItemId());
        dto.setSelectedOptionId(entity.getSelectedOptionId());
        dto.setUserPosition(entity.getUserPosition());
        dto.setAnswerText(entity.getAnswerText());
        dto.setAudioPath(entity.getAudioPath());
        dto.setTranscript(entity.getTranscript());
        dto.setRecordingDurationSeconds(entity.getRecordingDurationSeconds());
        dto.setAutoScore(entity.getAutoScore());
        dto.setFeedback(entity.getFeedback());
        dto.setGradingStatus(entity.getGradingStatus());
        dto.setGradedAt(entity.getGradedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public UserAnswerDetailEntity convertDtoToEntity() {
        UserAnswerDetailEntity entity =
                new UserAnswerDetailEntity(
                        this.userAnswerId,
                        this.questionItemId,
                        this.selectedOptionId,
                        this.userPosition,
                        this.answerText,
                        this.audioPath,
                        this.transcript,
                        this.recordingDurationSeconds,
                        this.autoScore,
                        this.feedback,
                        this.gradingStatus,
                        this.gradedAt,
                        this.createdAt != null ? this.createdAt : LocalDateTime.now(),
                        this.updatedAt != null ? this.updatedAt : LocalDateTime.now());
        if (this.id != null) entity.setId(this.id);
        return entity;
    }

    public void updateEntityFromDto(UserAnswerDetailEntity entity) {
        if (entity == null) return;
        entity.setSelectedOptionId(this.selectedOptionId);
        entity.setUserPosition(this.userPosition);
        entity.setAnswerText(this.answerText);
        entity.setAudioPath(this.audioPath);
        entity.setTranscript(this.transcript);
        entity.setRecordingDurationSeconds(this.recordingDurationSeconds);
        entity.setAutoScore(this.autoScore);
        entity.setFeedback(this.feedback);
        entity.setGradingStatus(this.gradingStatus);
        entity.setGradedAt(this.gradedAt);
        entity.setUpdatedAt(LocalDateTime.now());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserAnswerId() {
        return userAnswerId;
    }

    public void setUserAnswerId(Integer userAnswerId) {
        this.userAnswerId = userAnswerId;
    }

    public Integer getQuestionItemId() {
        return questionItemId;
    }

    public void setQuestionItemId(Integer questionItemId) {
        this.questionItemId = questionItemId;
    }

    public Integer getSelectedOptionId() {
        return selectedOptionId;
    }

    public void setSelectedOptionId(Integer selectedOptionId) {
        this.selectedOptionId = selectedOptionId;
    }

    public Integer getCorrectOptionId() {
        return correctOptionId;
    }

    public void setCorrectOptionId(Integer correctOptionId) {
        this.correctOptionId = correctOptionId;
    }

    public Integer getUserPosition() {
        return userPosition;
    }

    public void setUserPosition(Integer userPosition) {
        this.userPosition = userPosition;
    }

    public Integer getCorrectPosition() {
        return correctPosition;
    }

    public void setCorrectPosition(Integer correctPosition) {
        this.correctPosition = correctPosition;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
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

    public Integer getRecordingDurationSeconds() {
        return recordingDurationSeconds;
    }

    public void setRecordingDurationSeconds(Integer recordingDurationSeconds) {
        this.recordingDurationSeconds = recordingDurationSeconds;
    }

    public BigDecimal getAutoScore() {
        return autoScore;
    }

    public void setAutoScore(BigDecimal autoScore) {
        this.autoScore = autoScore;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getGradingStatus() {
        return gradingStatus;
    }

    public void setGradingStatus(String gradingStatus) {
        this.gradingStatus = gradingStatus;
    }

    public LocalDateTime getGradedAt() {
        return gradedAt;
    }

    public void setGradedAt(LocalDateTime gradedAt) {
        this.gradedAt = gradedAt;
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

    public Integer itemId() {
        return getQuestionItemId();
    }

    public int position() {
        return userPosition != null ? userPosition : 0;
    }

    public String taskContent() {
        return getTaskContent();
    }

    public String lockedContent() {
        return getLockedContent();
    }

    public Integer correctOptionId() {
        return getCorrectOptionId();
    }

    public Integer correctPosition() {
        return getCorrectPosition();
    }
}
