package com.aptisprep.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "exam_attempt_snapshots",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_exam_attempt_snapshot_attempt",
                        columnNames = "exam_attempt_id"))
public class ExamAttemptSnapshotEntity {
    public ExamAttemptSnapshotEntity() {}

    public ExamAttemptSnapshotEntity(
            Integer attemptId,
            Integer examId,
            Integer certificateId,
            Integer scoringSchemeId,
            String examType,
            String skill,
            String targetLevel,
            String title,
            Integer durationMinutes,
            Integer answerVersion,
            String policyVersion,
            Long sectionAdvanceMillis,
            List<ExamQuestionSnapshotEntity> questions,
            LocalDateTime createdAt) {
        this.attemptId = attemptId;
        this.examId = examId;
        this.certificateId = certificateId;
        this.scoringSchemeId = scoringSchemeId;
        this.examType = examType;
        this.skill = skill;
        this.targetLevel = targetLevel;
        this.title = title;
        this.durationMinutes = durationMinutes;
        this.answerVersion = answerVersion;
        this.policyVersion = policyVersion;
        this.sectionAdvanceMillis = sectionAdvanceMillis;
        this.questions = questions;
        this.createdAt = createdAt;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "exam_attempt_id", nullable = false)
    private Integer attemptId;

    @Column(name = "mock_exam_id", nullable = false)
    private Integer examId;

    @Column(name = "certificate_id", nullable = false)
    private Integer certificateId;

    @Column(name = "scoring_scheme_id")
    private Integer scoringSchemeId;

    @Column(name = "exam_type", nullable = false, length = 30)
    private String examType;

    @Column(length = 30)
    private String skill;

    @Column(name = "target_level", length = 10)
    private String targetLevel;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "answer_version", nullable = false)
    private Integer answerVersion;

    @Column(name = "policy_version", nullable = false, length = 100)
    private String policyVersion;

    @Column(name = "section_advance_millis", nullable = false)
    private Long sectionAdvanceMillis = 0L;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<ExamQuestionSnapshotEntity> questions = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public Integer getExamId() {
        return examId;
    }

    public Integer getCertificateId() {
        return certificateId;
    }

    public Integer getScoringSchemeId() {
        return scoringSchemeId;
    }

    public String getExamType() {
        return examType;
    }

    public String getSkill() {
        return skill;
    }

    public String getTargetLevel() {
        return targetLevel;
    }

    public String getTitle() {
        return title;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public Integer getAnswerVersion() {
        return answerVersion;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getSectionAdvanceMillis() {
        return sectionAdvanceMillis;
    }

    public List<ExamQuestionSnapshotEntity> getQuestions() {
        return questions;
    }

    public void setAttemptId(Integer value) {
        attemptId = value;
    }

    public void setExamId(Integer value) {
        examId = value;
    }

    public void setCertificateId(Integer value) {
        certificateId = value;
    }

    public void setScoringSchemeId(Integer value) {
        scoringSchemeId = value;
    }

    public void setExamType(String value) {
        examType = value;
    }

    public void setSkill(String value) {
        skill = value;
    }

    public void setTargetLevel(String value) {
        targetLevel = value;
    }

    public void setTitle(String value) {
        title = value;
    }

    public void setDurationMinutes(Integer value) {
        durationMinutes = value;
    }

    public void setAnswerVersion(Integer value) {
        answerVersion = value;
    }

    public void setPolicyVersion(String value) {
        policyVersion = value;
    }

    public void setSectionAdvanceMillis(Long value) {
        sectionAdvanceMillis = value == null ? 0L : value;
    }

    public void addQuestion(ExamQuestionSnapshotEntity question) {
        questions.add(question);
        question.setSnapshot(this);
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setQuestions(List<ExamQuestionSnapshotEntity> questions) {
        this.questions = questions;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
