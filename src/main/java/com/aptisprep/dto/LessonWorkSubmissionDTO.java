package com.aptisprep.dto;

import java.time.LocalDateTime;

public class LessonWorkSubmissionDTO {
    private Integer id;
    private LocalDateTime submittedAt;
    private String text;
    private String skill;
    private String prompt;
    private LessonContextSnapshot context;
    private String audioPath;

    public LessonWorkSubmissionDTO() {}

    public LessonWorkSubmissionDTO(
            Integer id,
            LocalDateTime submittedAt,
            String text,
            String skill,
            String prompt,
            LessonContextSnapshot context,
            String audioPath) {
        this.id = id;
        this.submittedAt = submittedAt;
        this.text = text;
        this.skill = skill;
        this.prompt = prompt;
        this.context = context;
        this.audioPath = audioPath;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer id() {
        return id;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime submittedAt() {
        return submittedAt;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String skill() {
        return skill;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String prompt() {
        return prompt;
    }

    public LessonContextSnapshot getContext() {
        return context;
    }

    public void setContext(LessonContextSnapshot context) {
        this.context = context;
    }

    public LessonContextSnapshot context() {
        return context;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public String audioPath() {
        return audioPath;
    }
}
