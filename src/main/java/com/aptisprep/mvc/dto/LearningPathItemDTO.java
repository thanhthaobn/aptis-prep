package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class LearningPathItemDTO {
    private Integer id;
    private LearningPath learningPath;
    private Lesson lesson;
    private Integer position;
    private boolean required;
    private String status;
    private LocalDateTime completedAt;

    public LearningPathItemDTO() {}

    public LearningPathItemDTO(
            Integer id,
            LearningPath learningPath,
            Lesson lesson,
            Integer position,
            boolean required,
            String status,
            LocalDateTime completedAt) {
        this.id = id;
        this.learningPath = learningPath;
        this.lesson = lesson;
        this.position = position;
        this.required = required;
        this.status = status;
        this.completedAt = completedAt;
    }

    public static LearningPathItemDTO convertEntityToDto(LearningPathItem entity) {
        if (entity == null) return null;
        return new LearningPathItemDTO(
                entity.getId(),
                entity.getLearningPath(),
                entity.getLesson(),
                entity.getPosition(),
                entity.isRequired(),
                entity.getStatus(),
                entity.getCompletedAt());
    }

    public static LearningPathItem convertDtoToEntity(LearningPathItemDTO dto) {
        if (dto == null) return null;
        return new LearningPathItem(
                dto.getId(),
                dto.getLearningPath(),
                dto.getLesson(),
                dto.getPosition(),
                dto.isRequired(),
                dto.getStatus(),
                dto.getCompletedAt());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LearningPath getLearningPath() {
        return learningPath;
    }

    public void setLearningPath(LearningPath learningPath) {
        this.learningPath = learningPath;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public void setLesson(Lesson lesson) {
        this.lesson = lesson;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

}











