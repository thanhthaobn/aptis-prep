package com.aptisprep.mvc.flashcard.dto;

import com.aptisprep.mvc.auth.entity.User;
import com.aptisprep.mvc.flashcard.entity.FlashcardSet;
import com.aptisprep.mvc.learning.entity.Lesson;

/** DTO boundary for a flashcard set entity. */
public class FlashcardSetEntityDTO {
    private String title;
    private String description;
    private String cefrLevel;
    private String status;
    private Lesson lesson;
    private User owner;
    private String visibility;
    private Integer lessonId;

    public static FlashcardSetEntityDTO fromForm(
            FlashcardSetFormDTO form, Lesson lesson, User owner) {
        if (form == null) return null;
        FlashcardSetEntityDTO dto = new FlashcardSetEntityDTO();
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

    public static FlashcardSet convertDtoToEntity(FlashcardSetEntityDTO dto) {
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

    public static FlashcardSetEntityDTO convertEntityToDto(FlashcardSet entity) {
        if (entity == null) return null;
        FlashcardSetEntityDTO dto = new FlashcardSetEntityDTO();
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
}
