package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.*;
import java.time.*;
import java.util.*;

public class FlashcardDTO {
    private Integer id;
    private FlashcardSet set;

    @NotBlank(message = "Mặt trước của thẻ không được để trống")
    @Size(max = 200, message = "Mặt trước của thẻ tối đa 200 ký tự")
    private String frontContent;

    @NotBlank(message = "Mặt sau của thẻ không được để trống")
    @Size(max = 1000, message = "Mặt sau của thẻ tối đa 1000 ký tự")
    private String backContent;

    private String cefrLevel;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status;
    private String state;
    private LocalDateTime nextReviewAt;

    public FlashcardDTO() {}

    public FlashcardDTO(
            Integer id,
            FlashcardSet set,
            String frontContent,
            String backContent,
            String cefrLevel,
            Integer displayOrder,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String status) {
        this.id = id;
        this.set = set;
        this.frontContent = frontContent;
        this.backContent = backContent;
        this.cefrLevel = cefrLevel;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.status = status;
    }

    public FlashcardDTO(
            Integer id,
            FlashcardSet set,
            String frontContent,
            String backContent,
            String cefrLevel,
            Integer displayOrder,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String status,
            String state,
            LocalDateTime nextReviewAt) {
        this(
                id,
                set,
                frontContent,
                backContent,
                cefrLevel,
                displayOrder,
                createdAt,
                updatedAt,
                status);
        this.state = state;
        this.nextReviewAt = nextReviewAt;
    }

    public static FlashcardDTO convertEntityToDto(Flashcard entity) {
        if (entity == null) return null;
        return new FlashcardDTO(
                entity.getId(),
                entity.getSet(),
                entity.getFrontContent(),
                entity.getBackContent(),
                entity.getCefrLevel(),
                entity.getDisplayOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getStatus());
    }

    public static Flashcard convertDtoToEntity(FlashcardDTO dto) {
        if (dto == null) return null;
        return new Flashcard(
                dto.getId(),
                dto.getSet(),
                dto.getStatus(),
                dto.getFrontContent(),
                dto.getBackContent(),
                dto.getCefrLevel(),
                dto.getDisplayOrder(),
                dto.getCreatedAt(),
                dto.getUpdatedAt());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public FlashcardSet getSet() {
        return set;
    }

    public void setSet(FlashcardSet set) {
        this.set = set;
    }

    public String getFrontContent() {
        return frontContent;
    }

    public void setFrontContent(String frontContent) {
        this.frontContent = frontContent;
    }

    public String getBackContent() {
        return backContent;
    }

    public void setBackContent(String backContent) {
        this.backContent = backContent;
    }

    public String getCefrLevel() {
        return cefrLevel;
    }

    public void setCefrLevel(String cefrLevel) {
        this.cefrLevel = cefrLevel;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public LocalDateTime getNextReviewAt() {
        return nextReviewAt;
    }

    public void setNextReviewAt(LocalDateTime nextReviewAt) {
        this.nextReviewAt = nextReviewAt;
    }
}
