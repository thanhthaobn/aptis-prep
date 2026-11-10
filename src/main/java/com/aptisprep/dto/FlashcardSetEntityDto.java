package com.aptisprep.dto;

import com.aptisprep.dto.form.FlashcardSetForm;
import com.aptisprep.entity.FlashcardSet;
import com.aptisprep.entity.Lesson;
import com.aptisprep.entity.User;

/** DTO boundary for a flashcard set entity. */
public class FlashcardSetEntityDto {
    private String title;
    private String description;
    private String cefrLevel;
    private String status;
    private Lesson lesson;
    private User owner;
    private String visibility;
    private Integer lessonId;

    public static FlashcardSetEntityDto fromForm(
            FlashcardSetForm form, Lesson lesson, User owner) {
        if (form == null) return null;
        FlashcardSetEntityDto dto = new FlashcardSetEntityDto();
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

    public static FlashcardSet convertDtoToEntity(FlashcardSetEntityDto dto) {
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
            FlashcardSetForm form, Lesson lesson, User owner) {
        return convertDtoToEntity(fromForm(form, lesson, owner));
    }

    public static FlashcardSetEntityDto convertEntityToDto(FlashcardSet entity) {
        if (entity == null) return null;
        FlashcardSetEntityDto dto = new FlashcardSetEntityDto();
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

    public FlashcardSetForm toForm() {
        FlashcardSetForm form = new FlashcardSetForm();
        form.setTitle(title);
        form.setDescription(description);
        form.setCefrLevel(cefrLevel);
        form.setStatus(status);
        form.setLessonId(lessonId);
        return form;
    }

    private static String trim(String value) { return value == null ? null : value.trim(); }

    private static String blankToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCefrLevel() { return cefrLevel; }
    public String getStatus() { return status; }
    public Lesson getLesson() { return lesson; }
    public User getOwner() { return owner; }
    public String getVisibility() { return visibility; }
}
