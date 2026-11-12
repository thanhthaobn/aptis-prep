package com.aptisprep.mvc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "learning_path_items",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"learning_path_id", "position"}),
            @UniqueConstraint(columnNames = {"learning_path_id", "lesson_id"})
        })
public class LearningPathItem {
    public LearningPathItem() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "learning_path_id")
    private LearningPath learningPath;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public LearningPathItem(
            Integer id,
            LearningPath learningPath,
            Lesson lesson,
            Integer position,
            boolean required,
            String status,
            LocalDateTime completedAt) {
        this.id = id;
        this.learningPath = learningPath;
        this.lesson = lesson;
        this.position = position;
        this.required = required;
        this.status = status;
        this.completedAt = completedAt;
    }


    public Integer getId() {
        return id;
    }

    public LearningPath getLearningPath() {
        return learningPath;
    }

    public void setLearningPath(LearningPath v) {
        learningPath = v;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public void setLesson(Lesson v) {
        lesson = v;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer v) {
        position = v;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean v) {
        required = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime v) {
        completedAt = v;
    }


    public void setId(Integer value) {
        this.id = value;
    }
}







