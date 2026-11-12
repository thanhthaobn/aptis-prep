package com.aptisprep.mvc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lesson_check_attempts")
public class LessonCheckAttempt {
    public LessonCheckAttempt() {}

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<LessonCheckAnswer> answers = new ArrayList<>();

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<LessonCheckFeedback> feedback = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "context_snapshot_id")
    private LessonContextSnapshot contextSnapshotEntity;

    public void snapshot(Lesson lesson) {
        contextSnapshotEntity = LessonContextSnapshot.capture(lesson);
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

    public LessonCheckAttempt(
            List<LessonCheckAnswer> answers,
            List<LessonCheckFeedback> feedback,
            LessonContextSnapshot contextSnapshotEntity,
            Integer id,
            Integer userId,
            Integer lessonId,
            int score,
            int total,
            LocalDateTime submittedAt) {
        this.answers = answers;
        this.feedback = feedback;
        this.contextSnapshotEntity = contextSnapshotEntity;
        this.id = id;
        this.userId = userId;
        this.lessonId = lessonId;
        this.score = score;
        this.total = total;
        this.submittedAt = submittedAt;
    }

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

    public List<LessonCheckAnswer> getAnswers() {
        return answers;
    }

    public List<LessonCheckFeedback> getFeedback() {
        return feedback;
    }

    public void addAnswer(LessonCheckAnswer value) {
        answers.add(value);
        value.setAttempt(this);
    }

    public void addFeedback(LessonCheckFeedback value) {
        feedback.add(value);
        value.setAttempt(this);
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setAnswers(List<LessonCheckAnswer> value) {
        this.answers = value;
    }

    public void setFeedback(List<LessonCheckFeedback> value) {
        this.feedback = value;
    }

    public LessonContextSnapshot getContextSnapshotEntity() {
        return contextSnapshotEntity;
    }

    public void setContextSnapshotEntity(LessonContextSnapshot value) {
        this.contextSnapshotEntity = value;
    }

    public void setId(Integer value) {
        this.id = value;
    }

    public void setSubmittedAt(LocalDateTime value) {
        this.submittedAt = value;
    }
}
