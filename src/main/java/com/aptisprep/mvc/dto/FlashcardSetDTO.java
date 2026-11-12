package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class FlashcardSetDTO {
    private Lesson lesson;
    private User owner;
    private Integer id;
    private String visibility;

    @NotBlank(message = "Tên bộ thẻ không được để trống")
    @Size(max = 255, message = "Tên bộ thẻ tối đa 255 ký tự")
    private String title;

    @Size(max = 2000, message = "Mô tả bộ thẻ tối đa 2000 ký tự")
    private String description;

    @NotBlank(message = "Cấp độ không được để trống")
    private String cefrLevel = "A1";

    @NotBlank(message = "Trạng thái không được để trống")
    private String status = "DRAFT";

    private Integer lessonId;
    private List<@Valid FlashcardDTO> flashcards = new ArrayList<>();
    private Integer count;
    private boolean personal;
    private String lessonTitle;
    private String lessonSkill;

    public FlashcardSetDTO() {}

    public FlashcardSetDTO(
            Lesson lesson,
            User owner,
            Integer id,
            String visibility,
            String title,
            String description,
            Integer lessonId,
            List<FlashcardDTO> flashcards,
            String cefrLevel,
            String status) {
        this.lesson = lesson;
        this.owner = owner;
        this.id = id;
        this.visibility = visibility;
        this.title = title;
        this.description = description;
        this.lessonId = lessonId;
        this.flashcards = flashcards == null ? new ArrayList<>() : flashcards;
        this.cefrLevel = cefrLevel;
        this.status = status;
    }

    public FlashcardSetDTO(
            Lesson lesson,
            User owner,
            Integer id,
            String visibility,
            String title,
            String description,
            Integer lessonId,
            List<FlashcardDTO> flashcards,
            String cefrLevel,
            String status,
            Integer count,
            boolean personal,
            String lessonTitle,
            String lessonSkill) {
        this(
                lesson,
                owner,
                id,
                visibility,
                title,
                description,
                lessonId,
                flashcards,
                cefrLevel,
                status);
        this.count = count;
        this.personal = personal;
        this.lessonTitle = lessonTitle;
        this.lessonSkill = lessonSkill;
    }

    public static FlashcardSetDTO convertEntityToDto(FlashcardSet entity) {
        if (entity == null) return null;
        return new FlashcardSetDTO(
                entity.getLesson(),
                entity.getOwner(),
                entity.getId(),
                entity.getVisibility(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getLesson() == null ? null : entity.getLesson().getId(),
                null,
                entity.getCefrLevel(),
                entity.getStatus());
    }

    public static FlashcardSet convertDtoToEntity(FlashcardSetDTO dto) {
        if (dto == null) return null;
        return new FlashcardSet(
                dto.getId(),
                dto.getTitle(),
                dto.getDescription(),
                dto.getCefrLevel(),
                dto.getStatus(),
                dto.getLesson(),
                dto.getOwner(),
                dto.getVisibility());
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String v) {
        title = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        description = v;
    }

    public String getCefrLevel() {
        return cefrLevel;
    }

    public void setCefrLevel(String v) {
        cefrLevel = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public Integer getLessonId() {
        return lessonId;
    }

    public void setLessonId(Integer v) {
        lessonId = v;
    }

    public List<FlashcardDTO> getFlashcards() {
        return flashcards;
    }

    public void setFlashcards(List<FlashcardDTO> value) {
        flashcards = value == null ? new ArrayList<>() : value;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public void setLesson(Lesson value) {
        this.lesson = value;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User value) {
        this.owner = value;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer value) {
        this.id = value;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String value) {
        this.visibility = value;
    }

    public String getLevel() {
        return cefrLevel;
    }

    public void setLevel(String level) {
        this.cefrLevel = level;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public boolean isPersonal() {
        return personal;
    }

    public void setPersonal(boolean personal) {
        this.personal = personal;
    }

    public String getLessonTitle() {
        return lessonTitle;
    }

    public void setLessonTitle(String lessonTitle) {
        this.lessonTitle = lessonTitle;
    }

    public String getLessonSkill() {
        return lessonSkill;
    }

    public void setLessonSkill(String lessonSkill) {
        this.lessonSkill = lessonSkill;
    }
}
