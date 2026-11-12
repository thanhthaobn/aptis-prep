package com.aptisprep.mvc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_answer_details",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_answer_details_answer_item",
                        columnNames = {"user_answer_id", "question_item_id"}))
public class UserAnswerDetail {
    public UserAnswerDetail() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_answer_id", nullable = false)
    private Integer userAnswerId;

    @Column(name = "question_item_id", nullable = false)
    private Integer questionItemId;

    @Column(name = "selected_option_id")
    private Integer selectedOptionId;

    @Column(name = "user_position")
    private Integer userPosition;

    @Column(name = "answer_text", columnDefinition = "nvarchar(max)")
    private String answerText;

    @Column(name = "audio_path", length = 500)
    private String audioPath;

    @Column(name = "transcript", columnDefinition = "nvarchar(max)")
    private String transcript;

    @Column(name = "recording_duration_seconds")
    private Integer recordingDurationSeconds;

    @Column(name = "auto_score", precision = 5, scale = 2)
    private BigDecimal autoScore;

    @Column(name = "feedback", columnDefinition = "nvarchar(max)")
    private String feedback;

    @Column(name = "grading_status", length = 30)
    private String gradingStatus;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UserAnswerDetail(
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


    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public Integer getUserAnswerId() {
        return userAnswerId;
    }

    public Integer getQuestionItemId() {
        return questionItemId;
    }

    public Integer getSelectedOptionId() {
        return selectedOptionId;
    }

    public Integer getUserPosition() {
        return userPosition;
    }

    public String getAnswerText() {
        return answerText;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public String getTranscript() {
        return transcript;
    }

    public String getGradingStatus() {
        return gradingStatus;
    }

    public BigDecimal getAutoScore() {
        return autoScore;
    }

    public void setUserAnswerId(Integer value) {
        userAnswerId = value;
    }

    public void setQuestionItemId(Integer value) {
        questionItemId = value;
    }

    public void setSelectedOptionId(Integer value) {
        selectedOptionId = value;
    }

    public void setUserPosition(Integer value) {
        userPosition = value;
    }

    public void setAnswerText(String value) {
        answerText = value;
    }

    public void setAudioPath(String value) {
        audioPath = value;
    }

    public void setGradingStatus(String value) {
        gradingStatus = value;
    }

    public void setAutoScore(BigDecimal value) {
        autoScore = value;
    }

    public void setFeedback(String value) {
        feedback = value;
    }

    public void setGradedAt(LocalDateTime value) {
        gradedAt = value;
    }


    public void setId(Integer value) {
        this.id = value;
    }

    public void setTranscript(String value) {
        this.transcript = value;
    }

    public Integer getRecordingDurationSeconds() {
        return recordingDurationSeconds;
    }

    public void setRecordingDurationSeconds(Integer value) {
        this.recordingDurationSeconds = value;
    }

    public String getFeedback() {
        return feedback;
    }

    public LocalDateTime getGradedAt() {
        return gradedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime value) {
        this.createdAt = value;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime value) {
        this.updatedAt = value;
    }
}







