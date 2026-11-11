package com.aptisprep.entity;

import com.aptisprep.dto.view.LessonContextSnapshotEntityDTO;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Immutable learner submission; autosave remains in lesson_responses. */
@Entity
@Table(name = "lesson_work_submissions")
public class LessonWorkSubmission {
    public LessonWorkSubmission(
            LessonContextSnapshotEntity contextSnapshotEntity,
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

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "context_snapshot_id")
    private LessonContextSnapshotEntity contextSnapshotEntity;

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

    public LessonContextSnapshotEntityDTO getContext() {
        return contextSnapshotEntity == null ? null : contextSnapshotEntity.toRecord();
    }

    @Column(name = "audio_path", length = 500)
    private String audioPath;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    public LessonWorkSubmission() {}

    public LessonWorkSubmission(Integer userId, Lesson lesson, LessonResponse work) {
        this.userId = userId;
        lessonId = lesson.getId();
        skill = lesson.getSkill();
        prompt =
                lesson.getMaterial() == null
                        ? lesson.getTitle()
                        : lesson.getMaterial().getPracticePrompt();
        contextSnapshotEntity = LessonContextSnapshotEntity.capture(lesson);
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

    public LessonContextSnapshotEntity getContextSnapshotEntity() {
        return contextSnapshotEntity;
    }

    public void setContextSnapshotEntity(LessonContextSnapshotEntity contextSnapshotEntity) {
        this.contextSnapshotEntity = contextSnapshotEntity;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public void setLessonId(Integer lessonId) {
        this.lessonId = lessonId;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
