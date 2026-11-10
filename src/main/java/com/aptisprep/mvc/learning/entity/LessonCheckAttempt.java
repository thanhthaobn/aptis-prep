package com.aptisprep.mvc.learning.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lesson_check_attempts")
public class LessonCheckAttempt {
    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<LessonCheckAnswerEntity> answers = new ArrayList<>();

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<LessonCheckFeedbackEntity> feedback = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "context_snapshot_id")
    private LessonContextSnapshotEntity contextSnapshotEntity;

    public LessonContextSnapshot getContext() {
        return contextSnapshotEntity == null ? null : contextSnapshotEntity.toRecord();
    }

    public void snapshot(Lesson lesson) {
        contextSnapshotEntity = LessonContextSnapshotEntity.capture(lesson);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "lesson_id", nullable = false)
    private Integer lessonId;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int total;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @PrePersist
    void create() {
        if (submittedAt == null) submittedAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer v) {
        userId = v;
    }

    public Integer getLessonId() {
        return lessonId;
    }

    public void setLessonId(Integer v) {
        lessonId = v;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int v) {
        score = v;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int v) {
        total = v;
    }

    public List<LessonCheckAnswerEntity> getAnswers() {
        return answers;
    }

    public List<LessonCheckFeedbackEntity> getFeedback() {
        return feedback;
    }

    public void addAnswer(LessonCheckAnswerEntity value) {
        answers.add(value);
        value.setAttempt(this);
    }

    public void addFeedback(LessonCheckFeedbackEntity value) {
        feedback.add(value);
        value.setAttempt(this);
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
}
