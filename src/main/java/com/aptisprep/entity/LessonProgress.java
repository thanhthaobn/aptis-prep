package com.aptisprep.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "lesson_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lesson_id"}))
public class LessonProgress {
    public LessonProgress() {}

    public LessonProgress(
            User user,
            Lesson lesson,
            String status,
            LocalDateTime lastAccessedAt,
            LocalDateTime completedAt) {
        this.user = user;
        this.lesson = lesson;
        this.status = status;
        this.lastAccessedAt = lastAccessedAt;
        this.completedAt = completedAt;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public Integer getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User v) {
        user = v;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public void setLesson(Lesson v) {
        lesson = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime v) {
        lastAccessedAt = v;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime v) {
        completedAt = v;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
