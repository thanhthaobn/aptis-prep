package com.aptisprep.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Immutable learner submission; autosave remains in lesson_responses. */
@Entity
@Table(name = "lesson_work_submissions")
public class LessonWorkSubmission {
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

    @Column(name = "content_snapshot", columnDefinition = "nvarchar(max)")
    private String contentSnapshot;

    public LessonContextSnapshot getContext() {
        return LessonContextSnapshot.read(contentSnapshot);
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
        contentSnapshot = LessonContextSnapshot.serialize(lesson);
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
}
