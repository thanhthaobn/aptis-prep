package com.aptisprep.mvc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mock_exams")
public class MockExam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "certificate_id", nullable = false)
    private Integer certificateId;

    @Column(name = "scoring_scheme_id")
    private Integer scoringSchemeId;

    @Column(nullable = false, length = 255, columnDefinition = "nvarchar(255)")
    private String title;

    @Column(columnDefinition = "nvarchar(max)")
    private String description;

    @Column(name = "exam_type", nullable = false, length = 30)
    private String examType;

    @Column(name = "target_level", length = 10)
    private String targetLevel;

    @Column(length = 30)
    private String skill;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(name = "demo_only", nullable = false)
    private boolean demoOnly;

    public boolean isDemoOnly() {
        return demoOnly;
    }

    public void setDemoOnly(boolean value) {
        demoOnly = value;
    }

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public MockExam(
            Integer id,
            Integer certificateId,
            Integer scoringSchemeId,
            String title,
            String description,
            String examType,
            String targetLevel,
            String skill,
            Integer durationMinutes,
            String status,
            boolean demoOnly,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.certificateId = certificateId;
        this.scoringSchemeId = scoringSchemeId;
        this.title = title;
        this.description = description;
        this.examType = examType;
        this.targetLevel = targetLevel;
        this.skill = skill;
        this.durationMinutes = durationMinutes;
        this.status = status;
        this.demoOnly = demoOnly;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


    public MockExam() {}

    public static MockExam fromSnapshot(ExamAttemptSnapshot snapshot) {
        MockExam exam = new MockExam();
        exam.id = snapshot.getExamId();
        exam.certificateId = snapshot.getCertificateId();
        exam.scoringSchemeId = snapshot.getScoringSchemeId();
        exam.title = snapshot.getTitle();
        exam.examType = snapshot.getExamType();
        exam.skill = snapshot.getSkill();
        exam.targetLevel = snapshot.getTargetLevel();
        exam.durationMinutes = snapshot.getDurationMinutes();
        return exam;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public Integer getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(Integer v) {
        certificateId = v;
    }

    public Integer getScoringSchemeId() {
        return scoringSchemeId;
    }

    public void setScoringSchemeId(Integer v) {
        scoringSchemeId = v;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String v) {
        title = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        description = v;
    }

    public String getExamType() {
        return examType;
    }

    public void setExamType(String v) {
        examType = v;
    }

    public String getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(String v) {
        targetLevel = v;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String v) {
        skill = v;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer v) {
        durationMinutes = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
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

    public void setUpdatedAt(LocalDateTime value) {
        this.updatedAt = value;
    }
}







