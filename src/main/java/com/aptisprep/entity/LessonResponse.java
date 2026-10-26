package com.aptisprep.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "lesson_responses",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lesson_id"}))
public class LessonResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "lesson_id", nullable = false)
    private Integer lessonId;

    @Column(name = "response_text", columnDefinition = "nvarchar(max)")
    private String text;

    @Column(name = "audio_path", length = 500)
    private String audioPath;

    @Column(name = "audio_name", length = 255)
    private String audioName;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "response_version", nullable = false)
    private Integer responseVersion = 1;

    @Column(name = "assessment_key", length = 255)
    private String assessmentKey;

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

    public String getText() {
        return text;
    }

    public void setText(String v) {
        text = v;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String v) {
        audioPath = v;
    }

    public String getAudioName() {
        return audioName;
    }

    public void setAudioName(String v) {
        audioName = v;
    }

    public Integer getResponseVersion() {
        return responseVersion;
    }

    public void setResponseVersion(Integer v) {
        responseVersion = v;
    }

    public String getAssessmentKey() {
        return assessmentKey;
    }

    public void setAssessmentKey(String v) {
        assessmentKey = v;
    }

    public void setUpdatedAt(LocalDateTime v) {
        updatedAt = v;
    }
}
