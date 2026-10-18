package com.aptisprep.entity;

import com.aptisprep.enums.AiJobState;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_assessment_jobs", uniqueConstraints = @UniqueConstraint(name = "uq_ai_job_assessment_key", columnNames = "assessment_key"))
public class AiAssessmentJob {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "assessment_key", nullable = false, unique = true, length = 255) private String assessmentKey;
    @Column(name = "user_id", nullable = false) private Integer userId;
    @Column(name = "attempt_id", nullable = false) private Integer attemptId;
    @Column(name = "answer_id", nullable = false) private Integer answerId;
    @Column(name = "answer_version", nullable = false) private Integer answerVersion;
    @Column(nullable = false, length = 30) private String skill;
    @Column(name = "task_type", nullable = false, length = 30) private String taskType;
    @Lob @Column(name = "task_content", nullable = false) private String taskContent;
    @Lob @Column(name = "locked_content", nullable = false) private String lockedContent;
    @Column(name = "rubric_version", nullable = false, length = 100) private String rubricVersion;
    @Column(name = "policy_version", nullable = false, length = 100) private String policyVersion;
    @Column(name = "max_score", nullable = false, precision = 5, scale = 2) private BigDecimal maxScore;
    @Lob private String transcript;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private AiJobState state;
    @Column(name = "retry_count", nullable = false) private int retryCount;
    @Column(name = "last_error", length = 1000) private String lastError;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @Column(name = "next_retry_at") private LocalDateTime nextRetryAt;
    @PrePersist void onCreate() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public Integer getId() { return id; }
    public String getAssessmentKey() { return assessmentKey; }
    public Integer getUserId() { return userId; }
    public Integer getAttemptId() { return attemptId; }
    public Integer getAnswerId() { return answerId; }
    public Integer getAnswerVersion() { return answerVersion; }
    public String getSkill() { return skill; }
    public String getTaskType() { return taskType; }
    public String getTaskContent() { return taskContent; }
    public String getLockedContent() { return lockedContent; }
    public String getRubricVersion() { return rubricVersion; }
    public String getPolicyVersion() { return policyVersion; }
    public BigDecimal getMaxScore() { return maxScore; }
    public String getTranscript() { return transcript; }
    public AiJobState getState() { return state; }
    public int getRetryCount() { return retryCount; }
    public String getLastError() { return lastError; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    public void setAssessmentKey(String value) { assessmentKey = value; }
    public void setUserId(Integer value) { userId = value; }
    public void setAttemptId(Integer value) { attemptId = value; }
    public void setAnswerId(Integer value) { answerId = value; }
    public void setAnswerVersion(Integer value) { answerVersion = value; }
    public void setSkill(String value) { skill = value; }
    public void setTaskType(String value) { taskType = value; }
    public void setTaskContent(String value) { taskContent = value; }
    public void setLockedContent(String value) { lockedContent = value; }
    public void setRubricVersion(String value) { rubricVersion = value; }
    public void setPolicyVersion(String value) { policyVersion = value; }
    public void setMaxScore(BigDecimal value) { maxScore = value; }
    public void setTranscript(String value) { transcript = value; }
    public void setState(AiJobState value) { state = value; }
    public void setRetryCount(int value) { retryCount = value; }
    public void setLastError(String value) { lastError = value; }
    public void setNextRetryAt(LocalDateTime value) { nextRetryAt = value; }
}
