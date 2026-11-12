package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class LessonWorkSubmissionDTO {
    private LessonContextSnapshotDTO contextSnapshotEntity;
    private Integer id;
    private Integer userId;
    private Integer lessonId;
    private String skill;
    private String prompt;
    private String text;
    private String audioPath;
    private LocalDateTime submittedAt;

    public LessonWorkSubmissionDTO() {}

    public LessonWorkSubmissionDTO(
            LessonContextSnapshotDTO contextSnapshotEntity,
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

    public static LessonWorkSubmissionDTO convertEntityToDto(LessonWorkSubmission entity) {
        if (entity == null) return null;
        return new LessonWorkSubmissionDTO(
                LessonContextSnapshotDTO.convertEntityToDto(entity.getContextSnapshotEntity()),
                entity.getId(),
                entity.getUserId(),
                entity.getLessonId(),
                entity.getSkill(),
                entity.getPrompt(),
                entity.getText(),
                entity.getAudioPath(),
                entity.getSubmittedAt());
    }

    public static LessonWorkSubmission convertDtoToEntity(LessonWorkSubmissionDTO dto) {
        if (dto == null) return null;
        return new LessonWorkSubmission(
                LessonContextSnapshotDTO.convertDtoToEntity(dto.getContextSnapshotEntity()),
                dto.getId(),
                dto.getUserId(),
                dto.getLessonId(),
                dto.getSkill(),
                dto.getPrompt(),
                dto.getText(),
                dto.getAudioPath(),
                dto.getSubmittedAt());
    }

    public LessonContextSnapshotDTO getContextSnapshotEntity() {
        return contextSnapshotEntity;
    }

    public void setContextSnapshotEntity(LessonContextSnapshotDTO contextSnapshotEntity) {
        this.contextSnapshotEntity = contextSnapshotEntity;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getLessonId() {
        return lessonId;
    }

    public void setLessonId(Integer lessonId) {
        this.lessonId = lessonId;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
