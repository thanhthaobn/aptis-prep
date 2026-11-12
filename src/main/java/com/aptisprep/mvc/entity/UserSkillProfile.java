package com.aptisprep.mvc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_skill_profiles",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_skill_profiles_user_skill",
                        columnNames = {"user_id", "skill"}))
public class UserSkillProfile {
    public UserSkillProfile() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String skill;

    @Column(name = "current_level", nullable = false, length = 10)
    private String currentLevel;

    @Column(name = "source_attempt_id")
    private Integer sourceAttemptId;

    @Column(name = "checkpoint_floor_level", length = 10)
    private String checkpointFloorLevel;

    @Column(name = "checkpoint_source_attempt_id")
    private Integer checkpointSourceAttemptId;

    @Column(name = "checkpoint_last_attempt_id")
    private Integer checkpointLastAttemptId;

    @Column(name = "checkpoint_last_submitted_at")
    private LocalDateTime checkpointLastSubmittedAt;

    @Column(name = "placement_level", length = 10)
    private String placementLevel;

    @Column(name = "placement_submitted_at")
    private LocalDateTime placementSubmittedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UserSkillProfile(
            Integer id,
            User user,
            String skill,
            String currentLevel,
            Integer sourceAttemptId,
            String checkpointFloorLevel,
            Integer checkpointSourceAttemptId,
            Integer checkpointLastAttemptId,
            LocalDateTime checkpointLastSubmittedAt,
            String placementLevel,
            LocalDateTime placementSubmittedAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.skill = skill;
        this.currentLevel = currentLevel;
        this.sourceAttemptId = sourceAttemptId;
        this.checkpointFloorLevel = checkpointFloorLevel;
        this.checkpointSourceAttemptId = checkpointSourceAttemptId;
        this.checkpointLastAttemptId = checkpointLastAttemptId;
        this.checkpointLastSubmittedAt = checkpointLastSubmittedAt;
        this.placementLevel = placementLevel;
        this.placementSubmittedAt = placementSubmittedAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getSkill() {
        return skill;
    }

    public String getCurrentLevel() {
        return currentLevel;
    }

    public Integer getSourceAttemptId() {
        return sourceAttemptId;
    }

    public String getCheckpointFloorLevel() {
        return checkpointFloorLevel;
    }

    public Integer getCheckpointSourceAttemptId() {
        return checkpointSourceAttemptId;
    }

    public Integer getCheckpointLastAttemptId() {
        return checkpointLastAttemptId;
    }

    public LocalDateTime getCheckpointLastSubmittedAt() {
        return checkpointLastSubmittedAt;
    }

    public String getPlacementLevel() {
        return placementLevel;
    }

    public LocalDateTime getPlacementSubmittedAt() {
        return placementSubmittedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public void setCurrentLevel(String currentLevel) {
        this.currentLevel = currentLevel;
    }

    public void setSourceAttemptId(Integer sourceAttemptId) {
        this.sourceAttemptId = sourceAttemptId;
    }

    public void setCheckpointFloorLevel(String value) {
        checkpointFloorLevel = value;
    }

    public void setCheckpointSourceAttemptId(Integer value) {
        checkpointSourceAttemptId = value;
    }

    public void setCheckpointLastAttemptId(Integer value) {
        checkpointLastAttemptId = value;
    }

    public void setCheckpointLastSubmittedAt(LocalDateTime value) {
        checkpointLastSubmittedAt = value;
    }

    public void setPlacementLevel(String value) {
        placementLevel = value;
    }

    public void setPlacementSubmittedAt(LocalDateTime value) {
        placementSubmittedAt = value;
    }

    public void setId(Integer value) {
        this.id = value;
    }

    public void setUpdatedAt(LocalDateTime value) {
        this.updatedAt = value;
    }
}
