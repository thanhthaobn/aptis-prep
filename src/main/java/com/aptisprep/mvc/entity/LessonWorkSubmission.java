package com.aptisprep.mvc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Immutable learner submission; autosave remains in lesson_responses. */
@Entity
@Table(name = "lesson_work_submissions")
public class LessonWorkSubmission {
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "context_snapshot_id")
    private LessonContextSnapshot contextSnapshotEntity;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "lesson_id", nullable = false)
    private Integer lessonId;

    @Column(nullable = false, length = 30)
    private String skill;

    @Column(name = "prompt_snapshot", columnDefinition = "nvarchar(max)")
    private String prompt;

    @Column(name = "response_text", columnDefinition = "nvarchar(max)")
    private String text;

    @Column(name = "audio_path", length = 500)
    private String audioPath;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    public LessonWorkSubmission(
            LessonContextSnapshot contextSnapshotEntity,
            Integer id,
            Integer userId,
            Integer lessonId,
            String skill,
            String prompt,
            String text,
            String audioPath,
            LocalDateTime submittedAt) {
        this.contextSnapshotEntity = contextSnapshotEntity;
        this.id = id;
        this.userId = userId;
        this.lessonId = lessonId;
        this.skill = skill;
        this.prompt = prompt;
        this.text = text;
        this.audioPath = audioPath;
        this.submittedAt = submittedAt;
    }

    public LessonWorkSubmission() {}

    public LessonWorkSubmission(Integer userId, Lesson lesson, LessonResponse work) {
        this.userId = userId;
        lessonId = lesson.getId();
        skill = lesson.getSkill();
        prompt =
                lesson.getPracticePrompt() == null ? lesson.getTitle() : lesson.getPracticePrompt();
        contextSnapshotEntity = LessonContextSnapshot.capture(lesson);
        text = work.getText();
        audioPath = work.getAudioPath();
        submittedAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public Integer getUserId() {
        return userId;
    }

    public Integer getLessonId() {
        return lessonId;
    }

    public String getSkill() {
        return skill;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getText() {
        return text;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
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

    public void setUserId(Integer value) {
        this.userId = value;
    }

    public void setLessonId(Integer value) {
        this.lessonId = value;
    }

    public void setSkill(String value) {
        this.skill = value;
    }

    public void setPrompt(String value) {
        this.prompt = value;
    }

    public void setText(String value) {
        this.text = value;
    }

    public void setAudioPath(String value) {
        this.audioPath = value;
    }

    public void setSubmittedAt(LocalDateTime value) {
        this.submittedAt = value;
    }
}
