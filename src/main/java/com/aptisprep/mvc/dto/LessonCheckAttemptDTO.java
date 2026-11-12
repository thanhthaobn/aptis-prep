package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class LessonCheckAttemptDTO {
    private LessonContextSnapshotDTO contextSnapshotEntity;
    private Integer id;
    private Integer userId;
    private Integer lessonId;
    private int score;
    private int total;
    private LocalDateTime submittedAt;
    private List<LessonCheckAnswer> answers;
    private List<LessonCheckFeedback> feedback;

    public LessonCheckAttemptDTO() {}

    public LessonCheckAttemptDTO(
            LessonContextSnapshotDTO contextSnapshotEntity,
            Integer id,
            Integer userId,
            Integer lessonId,
            int score,
            int total,
            LocalDateTime submittedAt,
            List<LessonCheckAnswer> answers,
            List<LessonCheckFeedback> feedback) {
        this.contextSnapshotEntity = contextSnapshotEntity;
        this.id = id;
        this.userId = userId;
        this.lessonId = lessonId;
        this.score = score;
        this.total = total;
        this.submittedAt = submittedAt;
        this.answers = answers;
        this.feedback = feedback;
    }
    public static LessonCheckAttemptDTO convertEntityToDto(LessonCheckAttempt entity) {
        if (entity == null) return null;
        return new LessonCheckAttemptDTO(
                LessonContextSnapshotDTO.convertEntityToDto(entity.getContextSnapshotEntity()),
                entity.getId(),
                entity.getUserId(),
                entity.getLessonId(),
                entity.getScore(),
                entity.getTotal(),
                entity.getSubmittedAt(),
                entity.getAnswers(),
                entity.getFeedback());
    }

    public static LessonCheckAttempt convertDtoToEntity(LessonCheckAttemptDTO dto) {
        if (dto == null) return null;
        return new LessonCheckAttempt(
                dto.getAnswers(),
                dto.getFeedback(),
                LessonContextSnapshotDTO.convertDtoToEntity(dto.getContextSnapshotEntity()),
                dto.getId(),
                dto.getUserId(),
                dto.getLessonId(),
                dto.getScore(),
                dto.getTotal(),
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

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public List<LessonCheckAnswer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<LessonCheckAnswer> answers) {
        this.answers = answers;
    }

    public List<LessonCheckFeedback> getFeedback() {
        return feedback;
    }

    public void setFeedback(List<LessonCheckFeedback> feedback) {
        this.feedback = feedback;
    }
}











