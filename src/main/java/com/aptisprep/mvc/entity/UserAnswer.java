package com.aptisprep.mvc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_answers",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_user_answers_attempt_question",
                        columnNames = {"exam_attempt_id", "question_id"}))
public class UserAnswer {
    public UserAnswer() {}

    @Lob
    @Column(name = "audio_manifest", columnDefinition = "NVARCHAR(MAX)")
    private String audioManifest;

    public String getAudioManifest() {
        return audioManifest;
    }

    public void setAudioManifest(String value) {
        audioManifest = value;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "exam_attempt_id", nullable = false)
    private Integer attemptId;

    @Column(name = "question_id", nullable = false)
    private Integer questionId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "answer_version", nullable = false)
    private int answerVersion = 1;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "text_answer", columnDefinition = "nvarchar(max)")
    private String textAnswer;

    @Column(name = "audio_url", length = 500)
    private String audioUrl;

    @Column(name = "ai_score", precision = 5, scale = 2)
    private BigDecimal aiScore;

    @Column(name = "ai_feedback", columnDefinition = "nvarchar(max)")
    private String aiFeedback;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @Column(name = "transcript", columnDefinition = "nvarchar(max)")
    private String transcript;

    @Column(name = "ai_provider", length = 30)
    private String aiProvider;

    @Column(name = "ai_model", length = 100)
    private String aiModel;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UserAnswer(
            String audioManifest,
            Integer id,
            Integer attemptId,
            Integer questionId,
            String status,
            int answerVersion,
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
            LocalDateTime updatedAt) {
        this.audioManifest = audioManifest;
        this.id = id;
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.status = status;
        this.answerVersion = answerVersion;
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

    public Integer getAttemptId() {
        return attemptId;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public String getStatus() {
        return status;
    }

    public int getAnswerVersion() {
        return answerVersion;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public String getTextAnswer() {
        return textAnswer;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public BigDecimal getAiScore() {
        return aiScore;
    }

    public String getAiFeedback() {
        return aiFeedback;
    }

    public LocalDateTime getGradedAt() {
        return gradedAt;
    }

    public String getTranscript() {
        return transcript;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAttemptId(Integer value) {
        attemptId = value;
    }

    public void setQuestionId(Integer value) {
        questionId = value;
    }

    public void setStatus(String value) {
        status = value;
    }

    public void setAnswerVersion(int value) {
        answerVersion = Math.max(1, value);
    }

    public void setStartedAt(LocalDateTime value) {
        startedAt = value;
    }

    public void setCompletedAt(LocalDateTime value) {
        completedAt = value;
    }

    public void setTextAnswer(String value) {
        textAnswer = value;
    }

    public void setAudioUrl(String value) {
        audioUrl = value;
    }

    public void setAiScore(BigDecimal value) {
        aiScore = value;
    }

    public void setAiFeedback(String value) {
        aiFeedback = value;
    }

    public void setGradedAt(LocalDateTime value) {
        gradedAt = value;
    }

    public void setTranscript(String value) {
        transcript = value;
    }

    public void setAiProvider(String value) {
        aiProvider = value;
    }

    public void setAiModel(String value) {
        aiModel = value;
    }

    public void setId(Integer value) {
        this.id = value;
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
