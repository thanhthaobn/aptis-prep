package com.aptisprep.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "lesson_progress", uniqueConstraints = @UniqueConstraint(name = "uq_lesson_progress_user_lesson", columnNames = {"user_id", "lesson_id"}))
public class LessonProgressEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "user_id", nullable = false) private Integer userId;
    @Column(name = "lesson_id", nullable = false) private Integer lessonId;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "last_accessed_at") private LocalDateTime lastAccessedAt;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    public Integer getId() { return id; }
    public Integer getUserId() { return userId; }
    public Integer getLessonId() { return lessonId; }
    public String getStatus() { return status; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setUserId(Integer value) { userId = value; }
    public void setLessonId(Integer value) { lessonId = value; }
    public void setStatus(String value) { status = value; }
    public void setLastAccessedAt(LocalDateTime value) { lastAccessedAt = value; }
    public void setCompletedAt(LocalDateTime value) { completedAt = value; }
}
