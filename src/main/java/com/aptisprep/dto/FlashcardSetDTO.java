package com.aptisprep.dto;

import com.aptisprep.entity.FlashcardSet;
import com.aptisprep.entity.Lesson;
import com.aptisprep.entity.User;
import com.aptisprep.presentation.form.FlashcardSetFormDTO;

/** DTO boundary for a flashcard set entity. */
public class FlashcardSetDTO {
    public FlashcardSetDTO() {}

    public FlashcardSetDTO(
            String title,
            String description,
            String cefrLevel,
            String status,
            Lesson lesson,
            User owner,
            String visibility,
            Integer lessonId) {
        this.title = title;
        this.description = description;
        this.cefrLevel = cefrLevel;
        this.status = status;
        this.lesson = lesson;
        this.owner = owner;
        this.visibility = visibility;
        this.lessonId = lessonId;
    }

    private String title;
    private String description;
    private String cefrLevel;
    private String status;
    private Lesson lesson;
    private User owner;
    private String visibility;
    private Integer lessonId;

    public static FlashcardSetDTO fromForm(FlashcardSetFormDTO form, Lesson lesson, User owner) {
        if (form == null) return null;
        FlashcardSetDTO dto = new FlashcardSetDTO();
        dto.title = form.getTitle();
        dto.description = form.getDescription();
        dto.cefrLevel = form.getCefrLevel();
        dto.status = form.getStatus();
        dto.lesson = lesson;
        dto.owner = owner;
        dto.visibility = owner == null ? "PUBLIC" : "PRIVATE";
        dto.lessonId = form.getLessonId();
        return dto;
    }

    public static FlashcardSet convertDtoToEntity(FlashcardSetDTO dto) {
        if (dto == null) return null;
        FlashcardSet entity = new FlashcardSet();
        entity.setTitle(trim(dto.title));
        entity.setDescription(blankToNull(dto.description));
        entity.setCefrLevel(dto.cefrLevel);
        entity.setStatus(dto.status);
        entity.setLesson(dto.lesson);
        entity.setOwner(dto.owner);
        entity.setVisibility(dto.visibility);
        return entity;
    }

    public static FlashcardSet convertDtoToEntity(
            FlashcardSetFormDTO form, Lesson lesson, User owner) {
        return convertDtoToEntity(fromForm(form, lesson, owner));
    }

    public static FlashcardSetDTO convertEntityToDto(FlashcardSet entity) {
        if (entity == null) return null;
        FlashcardSetDTO dto = new FlashcardSetDTO();
        dto.title = entity.getTitle();
        dto.description = entity.getDescription();
        dto.cefrLevel = entity.getCefrLevel();
        dto.status = entity.getStatus();
        dto.lesson = entity.getLesson();
        dto.owner = entity.getOwner();
        dto.visibility = entity.getVisibility();
        dto.lessonId = entity.getLesson() == null ? null : entity.getLesson().getId();
        return dto;
    }

    public FlashcardSetFormDTO toForm() {
        FlashcardSetFormDTO form = new FlashcardSetFormDTO();
        form.setTitle(title);
        form.setDescription(description);
        form.setCefrLevel(cefrLevel);
        form.setStatus(status);
        form.setLessonId(lessonId);
        return form;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String blankToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCefrLevel() {
        return cefrLevel;
    }

    public String getStatus() {
        return status;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public User getOwner() {
        return owner;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCefrLevel(String cefrLevel) {
        this.cefrLevel = cefrLevel;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLesson(Lesson lesson) {
        this.lesson = lesson;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public void setLessonId(Integer lessonId) {
        this.lessonId = lessonId;
    }
}
