package com.aptisprep.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_profile_applications", uniqueConstraints = @UniqueConstraint(name = "uq_profile_application_attempt_skill", columnNames = {"user_id", "attempt_id", "skill"}))
public class ProfileApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    @Column(name = "attempt_id", nullable = false)
    private Integer attemptId;
    @Column(nullable = false, length = 30)
    private String skill;
    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @PrePersist
    void onCreate() {
        if (appliedAt == null) appliedAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public String getSkill() {
        return skill;
    }

    public void setUserId(Integer value) {
        userId = value;
    }

    public void setAttemptId(Integer value) {
        attemptId = value;
    }

    public void setSkill(String value) {
        skill = value;
    }
}
