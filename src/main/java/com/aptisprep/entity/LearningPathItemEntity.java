package com.aptisprep.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "learning_path_items", uniqueConstraints = @UniqueConstraint(name = "uq_path_items_order", columnNames = {"learning_path_id", "position"}))
public class LearningPathItemEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "learning_path_id", nullable = false) private Integer pathId;
    @Column(name = "lesson_id", nullable = false) private Integer lessonId;
    @Column(nullable = false) private int position;
    @Column(name = "is_required", nullable = false) private boolean required;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    public Integer getId() { return id; }
    public Integer getPathId() { return pathId; }
    public Integer getLessonId() { return lessonId; }
    public int getPosition() { return position; }
    public boolean isRequired() { return required; }
    public String getStatus() { return status; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setPathId(Integer value) { pathId = value; }
    public void setLessonId(Integer value) { lessonId = value; }
    public void setPosition(int value) { position = value; }
    public void setRequired(boolean value) { required = value; }
    public void setStatus(String value) { status = value; }
    public void setCompletedAt(LocalDateTime value) { completedAt = value; }
}
