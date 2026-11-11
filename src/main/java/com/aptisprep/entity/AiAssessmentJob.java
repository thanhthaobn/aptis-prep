package com.aptisprep.entity;

import com.aptisprep.enums.AiJobState;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_assessment_jobs",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_ai_job_assessment_key",
                        columnNames = "assessment_key"))
public class AiAssessmentJob {
    public AiAssessmentJob() {}

    public AiAssessmentJob(
            Integer id,
            String assessmentKey,
            Integer userId,
            Integer attemptId,
            Integer answerId,
            String sourceType,
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
            String processingToken) {
        this.id = id;
        this.assessmentKey = assessmentKey;
        this.userId = userId;
        this.attemptId = attemptId;
        this.answerId = answerId;
        this.sourceType = sourceType;
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
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "assessment_key", nullable = false, unique = true, length = 255)
    private String assessmentKey;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "attempt_id")
    private Integer attemptId;

    @Column(name = "answer_id")
    private Integer answerId;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType = "EXAM_ANSWER";

    @Column(name = "source_id")
    private Integer sourceId;

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String value) {
        sourceType = value;
    }

    public Integer getSourceId() {
        return sourceId;
    }

    public void setSourceId(Integer value) {
        sourceId = value;
    }

    @Column(name = "answer_version", nullable = false)
    private Integer answerVersion;

    @Column(nullable = false, length = 30)
    private String skill;

    @Column(name = "task_type", nullable = false, length = 30)
    private String taskType;

    @Lob
    @Column(name = "task_content", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String taskContent;

    @Lob
    @Column(name = "locked_content", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String lockedContent;

    @Column(name = "rubric_version", nullable = false, length = 100)
    private String rubricVersion;

    @Column(name = "policy_version", nullable = false, length = 100)
    private String policyVersion;

    @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxScore;

    @Lob
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String transcript;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiJobState state;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "processing_token", length = 64)
    private String processingToken;

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

    public String getAssessmentKey() {
        return assessmentKey;
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public Integer getAnswerId() {
        return answerId;
    }

    public Integer getAnswerVersion() {
        return answerVersion;
    }

    public String getSkill() {
        return skill;
    }

    public String getTaskType() {
        return taskType;
    }

    public String getTaskContent() {
        return taskContent;
    }

    public String getLockedContent() {
        return lockedContent;
    }

    public String getRubricVersion() {
        return rubricVersion;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public String getTranscript() {
        return transcript;
    }

    public AiJobState getState() {
        return state;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public String getProcessingToken() {
        return processingToken;
    }

    public void setAssessmentKey(String value) {
        assessmentKey = value;
    }

    public void setUserId(Integer value) {
        userId = value;
    }

    public void setAttemptId(Integer value) {
        attemptId = value;
    }

    public void setAnswerId(Integer value) {
        answerId = value;
    }

    public void setAnswerVersion(Integer value) {
        answerVersion = value;
    }

    public void setSkill(String value) {
        skill = value;
    }

    public void setTaskType(String value) {
        taskType = value;
    }

    public void setTaskContent(String value) {
        taskContent = value;
    }

    public void setLockedContent(String value) {
        lockedContent = value;
    }

    public void setRubricVersion(String value) {
        rubricVersion = value;
    }

    public void setPolicyVersion(String value) {
        policyVersion = value;
    }

    public void setMaxScore(BigDecimal value) {
        maxScore = value;
    }

    public void setTranscript(String value) {
        transcript = value;
    }

    public void setState(AiJobState value) {
        state = value;
    }

    public void setRetryCount(int value) {
        retryCount = value;
    }

    public void setLastError(String value) {
        lastError = value;
    }

    public void setNextRetryAt(LocalDateTime value) {
        nextRetryAt = value;
    }

    public void setProcessingToken(String value) {
        processingToken = value;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
