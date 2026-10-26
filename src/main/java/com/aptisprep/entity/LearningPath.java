package com.aptisprep.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "learning_paths")
public class LearningPath {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "goal_skill_id")
    private Integer goalSkillId;

    @Column(name = "source_attempt_id")
    private Integer sourceAttemptId;

    @Column(nullable = false, length = 30)
    private String skill;

    @Column(name = "from_level", length = 10)
    private String fromLevel;

    @Column(name = "target_level", length = 10)
    private String targetLevel;

    @Column(name = "path_source", nullable = false, length = 30)
    private String pathSource;

    @Column(name = "checkpoint_exam_id")
    private Integer checkpointExamId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    void create() {
        createdAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User v) {
        user = v;
    }

    public Integer getGoalSkillId() {
        return goalSkillId;
    }

    public void setGoalSkillId(Integer v) {
        goalSkillId = v;
    }

    public Integer getSourceAttemptId() {
        return sourceAttemptId;
    }

    public void setSourceAttemptId(Integer v) {
        sourceAttemptId = v;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String v) {
        skill = v;
    }

    public String getFromLevel() {
        return fromLevel;
    }

    public void setFromLevel(String v) {
        fromLevel = v;
    }

    public String getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(String v) {
        targetLevel = v;
    }

    public String getPathSource() {
        return pathSource;
    }

    public void setPathSource(String v) {
        pathSource = v;
    }

    public Integer getCheckpointExamId() {
        return checkpointExamId;
    }

    public void setCheckpointExamId(Integer v) {
        checkpointExamId = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime v) {
        completedAt = v;
    }
}
