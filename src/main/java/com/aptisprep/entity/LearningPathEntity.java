package com.aptisprep.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "learning_paths")
public class LearningPathEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "user_id", nullable = false) private Integer userId;
    @Column(name = "goal_skill_id") private Integer goalSkillId;
    @Column(name = "source_attempt_id") private Integer sourceAttemptId;
    @Column(nullable = false, length = 30) private String skill;
    @Column(name = "from_level", length = 10) private String fromLevel;
    @Column(name = "target_level", length = 10) private String targetLevel;
    @Column(name = "path_source", nullable = false, length = 30) private String pathSource;
    @Column(name = "checkpoint_exam_id") private Integer checkpointExamId;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    public Integer getId() { return id; }
    public Integer getUserId() { return userId; }
    public String getSkill() { return skill; }
    public String getFromLevel() { return fromLevel; }
    public String getTargetLevel() { return targetLevel; }
    public String getPathSource() { return pathSource; }
    public String getStatus() { return status; }
    public void setUserId(Integer value) { userId = value; }
    public void setGoalSkillId(Integer value) { goalSkillId = value; }
    public void setSourceAttemptId(Integer value) { sourceAttemptId = value; }
    public void setSkill(String value) { skill = value; }
    public void setFromLevel(String value) { fromLevel = value; }
    public void setTargetLevel(String value) { targetLevel = value; }
    public void setPathSource(String value) { pathSource = value; }
    public void setCheckpointExamId(Integer value) { checkpointExamId = value; }
    public void setStatus(String value) { status = value; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public void setCompletedAt(LocalDateTime value) { completedAt = value; }
}
