package com.aptisprep.presentation;

import java.time.LocalDateTime;

public record LessonWorkSubmissionDTO(
        Integer id,
        LocalDateTime submittedAt,
        String text,
        String skill,
        String prompt,
        LessonContextSnapshot context,
        String audioPath) {
    public Integer getId() {
        return id;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public String getText() {
        return text;
    }

    public String getSkill() {
        return skill;
    }

    public String getPrompt() {
        return prompt;
    }

    public LessonContextSnapshot getContext() {
        return context;
    }

    public String getAudioPath() {
        return audioPath;
    }
}
