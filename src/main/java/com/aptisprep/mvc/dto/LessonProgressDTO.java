package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class LessonProgressDTO {
    private Integer id;
    private User user;
    private Lesson lesson;
    private String status;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime completedAt;

    public LessonProgressDTO() {}

    public LessonProgressDTO(
            Integer id,
            User user,
            Lesson lesson,
            String status,
            LocalDateTime lastAccessedAt,
            LocalDateTime completedAt) {
        this.id = id;
        this.user = user;
        this.lesson = lesson;
        this.status = status;
        this.lastAccessedAt = lastAccessedAt;
        this.completedAt = completedAt;
    }

    public static LessonProgressDTO convertEntityToDto(LessonProgress entity) {
        if (entity == null) return null;
        return new LessonProgressDTO(
                entity.getId(),
                entity.getUser(),
                entity.getLesson(),
                entity.getStatus(),
                entity.getLastAccessedAt(),
                entity.getCompletedAt());
    }

    public static LessonProgress convertDtoToEntity(LessonProgressDTO dto) {
        if (dto == null) return null;
        return new LessonProgress(
                dto.getId(),
                dto.getUser(),
                dto.getLesson(),
                dto.getStatus(),
                dto.getLastAccessedAt(),
                dto.getCompletedAt());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public void setLesson(Lesson lesson) {
        this.lesson = lesson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}











