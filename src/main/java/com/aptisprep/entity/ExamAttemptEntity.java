package com.aptisprep.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_attempts")
public class ExamAttemptEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "user_id", nullable = false) private Integer userId;
    @Column(name = "mock_exam_id", nullable = false) private Integer mockExamId;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "grading_status", length = 30) private String gradingStatus;
    @Column(name = "started_at", nullable = false) private LocalDateTime startedAt;
    @Column(name = "submitted_at") private LocalDateTime submittedAt;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    @Column(name = "final_scale_score", precision = 6, scale = 2) private BigDecimal finalScaleScore;
    @Column(name = "estimated_overall_cefr", length = 10) private String estimatedOverallCefr;
    @Column(name = "request_key", length = 255) private String requestKey;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    @PrePersist void onCreate() {
        if (startedAt == null) startedAt = LocalDateTime.now();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public Integer getUserId() { return userId; }
    public Integer getMockExamId() { return mockExamId; }
    public String getStatus() { return status; }
    public String getGradingStatus() { return gradingStatus; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public BigDecimal getFinalScaleScore() { return finalScaleScore; }
    public String getEstimatedOverallCefr() { return estimatedOverallCefr; }
    public String getRequestKey() { return requestKey; }
    public void setUserId(Integer value) { userId = value; }
    public void setMockExamId(Integer value) { mockExamId = value; }
    public void setStatus(String value) { status = value; }
    public void setGradingStatus(String value) { gradingStatus = value; }
    public void setStartedAt(LocalDateTime value) { startedAt = value; }
    public void setSubmittedAt(LocalDateTime value) { submittedAt = value; }
    public void setCompletedAt(LocalDateTime value) { completedAt = value; }
    public void setFinalScaleScore(BigDecimal value) { finalScaleScore = value; }
    public void setEstimatedOverallCefr(String value) { estimatedOverallCefr = value; }
    public void setRequestKey(String value) { requestKey = value; }
}
