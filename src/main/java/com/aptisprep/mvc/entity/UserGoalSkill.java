package com.aptisprep.mvc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
        name = "user_goal_skills",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_goal_skills_goal_skill",
                        columnNames = {"user_goal_id", "skill"}))
public class UserGoalSkill {
    public UserGoalSkill() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_goal_id", nullable = false)
    private UserGoal goal;

    @Column(nullable = false, length = 30)
    private String skill;

    @Column(name = "target_level", nullable = false, length = 10)
    private String targetLevel;

    @Column(name = "target_score", precision = 5, scale = 2)
    private BigDecimal targetScore;

    public UserGoalSkill(
            Integer id, UserGoal goal, String skill, String targetLevel, BigDecimal targetScore) {
        this.id = id;
        this.goal = goal;
        this.skill = skill;
        this.targetLevel = targetLevel;
        this.targetScore = targetScore;
    }

    public Integer getId() {
        return id;
    }

    public UserGoal getGoal() {
        return goal;
    }

    public String getSkill() {
        return skill;
    }

    public String getTargetLevel() {
        return targetLevel;
    }

    public BigDecimal getTargetScore() {
        return targetScore;
    }

    public void setGoal(UserGoal goal) {
        this.goal = goal;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public void setTargetLevel(String targetLevel) {
        this.targetLevel = targetLevel;
    }

    public void setTargetScore(BigDecimal targetScore) {
        this.targetScore = targetScore;
    }

    public void setId(Integer value) {
        this.id = value;
    }
}
