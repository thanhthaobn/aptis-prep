package com.aptisprep.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_skill_results", uniqueConstraints = @UniqueConstraint(name = "uq_skill_results_attempt_skill", columnNames = {"exam_attempt_id", "skill"}))
public class ExamSkillResultEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "exam_attempt_id", nullable = false) private Integer attemptId;
    @Column(nullable = false, length = 30) private String skill;
    @Column(name = "correct_count") private Integer correctCount;
    @Column(name = "total_items") private Integer totalItems;
    @Column(name = "raw_score", precision = 6, scale = 2) private BigDecimal rawScore;
    @Column(name = "scale_score", precision = 5, scale = 2) private BigDecimal scaleScore;
    @Column(name = "cefr_level", length = 10) private String cefrLevel;
    @Column(name = "grading_method", length = 30) private String gradingMethod;
    @Column(name = "grading_status", length = 30) private String gradingStatus;
    @Column(name = "objective_raw_score", precision = 6, scale = 2) private BigDecimal objectiveRawScore;
    @Column(name = "subjective_raw_score", precision = 6, scale = 2) private BigDecimal subjectiveRawScore;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public Integer getId() { return id; }
    public Integer getAttemptId() { return attemptId; }
    public String getSkill() { return skill; }
    public Integer getCorrectCount() { return correctCount; }
    public Integer getTotalItems() { return totalItems; }
    public BigDecimal getRawScore() { return rawScore; }
    public BigDecimal getScaleScore() { return scaleScore; }
    public String getCefrLevel() { return cefrLevel; }
    public String getGradingMethod() { return gradingMethod; }
    public String getGradingStatus() { return gradingStatus; }
    public BigDecimal getObjectiveRawScore() { return objectiveRawScore; }
    public BigDecimal getSubjectiveRawScore() { return subjectiveRawScore; }
    public void setAttemptId(Integer value) { attemptId = value; }
    public void setSkill(String value) { skill = value; }
    public void setCorrectCount(Integer value) { correctCount = value; }
    public void setTotalItems(Integer value) { totalItems = value; }
    public void setRawScore(BigDecimal value) { rawScore = value; }
    public void setScaleScore(BigDecimal value) { scaleScore = value; }
    public void setCefrLevel(String value) { cefrLevel = value; }
    public void setGradingMethod(String value) { gradingMethod = value; }
    public void setGradingStatus(String value) { gradingStatus = value; }
    public void setObjectiveRawScore(BigDecimal value) { objectiveRawScore = value; }
    public void setSubjectiveRawScore(BigDecimal value) { subjectiveRawScore = value; }
}
