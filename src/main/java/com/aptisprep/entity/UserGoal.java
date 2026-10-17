package com.aptisprep.entity;

import com.aptisprep.enums.GoalStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_goals")
public class UserGoal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "certificate_id") private Certificate certificate;
    @Column(name = "target_date") private LocalDate targetDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private GoalStatus status;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserGoalSkill> skills = new ArrayList<>();
    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public Integer getId() { return id; }
    public User getUser() { return user; }
    public Certificate getCertificate() { return certificate; }
    public LocalDate getTargetDate() { return targetDate; }
    public GoalStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<UserGoalSkill> getSkills() { return skills; }
    public void setUser(User user) { this.user = user; }
    public void setCertificate(Certificate certificate) { this.certificate = certificate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public void setStatus(GoalStatus status) { this.status = status; }
    public void addSkill(UserGoalSkill skill) { skills.add(skill); skill.setGoal(this); }
}
