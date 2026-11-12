package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class LessonResponseDTO {
    private Integer id;
    private Integer userId;
    private Integer lessonId;
    private String text;
    private String audioPath;
    private String audioName;
    private LocalDateTime updatedAt;
    private String assessmentKey;
    private Integer responseVersion;

    public LessonResponseDTO() {}

    public LessonResponseDTO(
            Integer id,
            Integer userId,
            Integer lessonId,
            String text,
            String audioPath,
            String audioName,
            LocalDateTime updatedAt,
            String assessmentKey,
            Integer responseVersion) {
        this.id = id;
        this.userId = userId;
        this.lessonId = lessonId;
        this.text = text;
        this.audioPath = audioPath;
        this.audioName = audioName;
        this.updatedAt = updatedAt;
        this.assessmentKey = assessmentKey;
        this.responseVersion = responseVersion;
    }

    public static LessonResponseDTO convertEntityToDto(LessonResponse entity) {
        if (entity == null) return null;
        return new LessonResponseDTO(
                entity.getId(),
                entity.getUserId(),
                entity.getLessonId(),
                entity.getText(),
                entity.getAudioPath(),
                entity.getAudioName(),
                entity.getUpdatedAt(),
                entity.getAssessmentKey(),
                entity.getResponseVersion());
    }

    public static LessonResponse convertDtoToEntity(LessonResponseDTO dto) {
        if (dto == null) return null;
        return new LessonResponse(
                dto.getId(),
                dto.getUserId(),
                dto.getLessonId(),
                dto.getText(),
                dto.getAudioPath(),
                dto.getAudioName(),
                dto.getUpdatedAt(),
                dto.getResponseVersion(),
                dto.getAssessmentKey());
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

    public String getAudioName() {
        return audioName;
    }

    public void setAudioName(String audioName) {
        this.audioName = audioName;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAssessmentKey() {
        return assessmentKey;
    }

    public void setAssessmentKey(String assessmentKey) {
        this.assessmentKey = assessmentKey;
    }

    public Integer getResponseVersion() {
        return responseVersion;
    }

    public void setResponseVersion(Integer responseVersion) {
        this.responseVersion = responseVersion;
    }
}











