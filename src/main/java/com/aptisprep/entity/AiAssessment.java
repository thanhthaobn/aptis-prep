package com.aptisprep.entity;

import com.aptisprep.enums.AiAssessmentState;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_assessments",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_ai_assessment_job_version",
                        columnNames = {"job_id", "answer_version"}))
public class AiAssessment {
    public AiAssessment() {}

    public AiAssessment(
            Integer id,
            AiAssessmentJob job,
            String assessmentKey,
            Integer answerId,
            Integer answerVersion,
            AiAssessmentState state,
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private AiAssessmentJob job;

    @Column(name = "assessment_key", nullable = false, length = 255)
    private String assessmentKey;

    @Column(name = "answer_id")
    private Integer answerId;

    @Column(name = "answer_version", nullable = false)
    private Integer answerVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiAssessmentState state;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "estimated_cefr", length = 10)
    private String estimatedCefr;

    @Lob
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String feedback;

    @Lob
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String transcript;

    @Column(name = "rubric_version", nullable = false, length = 100)
    private String rubricVersion;

    @Column(name = "policy_version", nullable = false, length = 100)
    private String policyVersion;

    @Column(length = 50)
    private String provider;

    @Column(length = 100)
    private String model;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public AiAssessmentJob getJob() {
        return job;
    }

    public String getAssessmentKey() {
        return assessmentKey;
    }

    public Integer getAnswerId() {
        return answerId;
    }

    public Integer getAnswerVersion() {
        return answerVersion;
    }

    public AiAssessmentState getState() {
        return state;
    }

    public BigDecimal getScore() {
        return score;
    }

    public String getEstimatedCefr() {
        return estimatedCefr;
    }

    public String getFeedback() {
        return feedback;
    }

    public String getTranscript() {
        return transcript;
    }

    public String getRubricVersion() {
        return rubricVersion;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setJob(AiAssessmentJob value) {
        job = value;
    }

    public void setAssessmentKey(String value) {
        assessmentKey = value;
    }

    public void setAnswerId(Integer value) {
        answerId = value;
    }

    public void setAnswerVersion(Integer value) {
        answerVersion = value;
    }

    public void setState(AiAssessmentState value) {
        state = value;
    }

    public void setScore(BigDecimal value) {
        score = value;
    }

    public void setEstimatedCefr(String value) {
        estimatedCefr = value;
    }

    public void setFeedback(String value) {
        feedback = value;
    }

    public void setTranscript(String value) {
        transcript = value;
    }

    public void setRubricVersion(String value) {
        rubricVersion = value;
    }

    public void setPolicyVersion(String value) {
        policyVersion = value;
    }

    public void setProvider(String value) {
        provider = value;
    }

    public void setModel(String value) {
        model = value;
    }

    public void setErrorMessage(String value) {
        errorMessage = value;
    }

    public void setCompletedAt(LocalDateTime value) {
        completedAt = value;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
