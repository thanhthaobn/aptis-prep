package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class UserAnswerDetailDTO {
    private Integer id;
    private Integer userAnswerId;
    private Integer questionItemId;
    private Integer selectedOptionId;
    private Integer userPosition;
    private String answerText;
    private String audioPath;
    private String transcript;
    private Integer recordingDurationSeconds;
    private BigDecimal autoScore;
    private String feedback;
    private String gradingStatus;
    private LocalDateTime gradedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserAnswerDetailDTO() {}

    public UserAnswerDetailDTO(
            Integer id,
            Integer userAnswerId,
            Integer questionItemId,
            Integer selectedOptionId,
            Integer userPosition,
            String answerText,
            String audioPath,
            String transcript,
            Integer recordingDurationSeconds,
            BigDecimal autoScore,
            String feedback,
            String gradingStatus,
            LocalDateTime gradedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.userAnswerId = userAnswerId;
        this.questionItemId = questionItemId;
        this.selectedOptionId = selectedOptionId;
        this.userPosition = userPosition;
        this.answerText = answerText;
        this.audioPath = audioPath;
        this.transcript = transcript;
        this.recordingDurationSeconds = recordingDurationSeconds;
        this.autoScore = autoScore;
        this.feedback = feedback;
        this.gradingStatus = gradingStatus;
        this.gradedAt = gradedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserAnswerDetailDTO convertEntityToDto(UserAnswerDetail entity) {
        if (entity == null) return null;
        return new UserAnswerDetailDTO(
                entity.getId(),
                entity.getUserAnswerId(),
                entity.getQuestionItemId(),
                entity.getSelectedOptionId(),
                entity.getUserPosition(),
                entity.getAnswerText(),
                entity.getAudioPath(),
                entity.getTranscript(),
                entity.getRecordingDurationSeconds(),
                entity.getAutoScore(),
                entity.getFeedback(),
                entity.getGradingStatus(),
                entity.getGradedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static UserAnswerDetail convertDtoToEntity(UserAnswerDetailDTO dto) {
        if (dto == null) return null;
        return new UserAnswerDetail(
                dto.getId(),
                dto.getUserAnswerId(),
                dto.getQuestionItemId(),
                dto.getSelectedOptionId(),
                dto.getUserPosition(),
                dto.getAnswerText(),
                dto.getAudioPath(),
                dto.getTranscript(),
                dto.getRecordingDurationSeconds(),
                dto.getAutoScore(),
                dto.getFeedback(),
                dto.getGradingStatus(),
                dto.getGradedAt(),
                dto.getCreatedAt(),
                dto.getUpdatedAt());
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

    public Integer getUserPosition() {
        return userPosition;
    }

    public void setUserPosition(Integer userPosition) {
        this.userPosition = userPosition;
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
}
