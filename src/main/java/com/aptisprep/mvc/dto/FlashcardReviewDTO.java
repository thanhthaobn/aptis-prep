package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class FlashcardReviewDTO {
    private String id;
    private Integer userId;
    private Integer flashcardId;
    private String state;
    private LocalDateTime reviewedAt;

    public FlashcardReviewDTO() {}

    public FlashcardReviewDTO(
            String id,
            Integer userId,
            Integer flashcardId,
            String state,
            LocalDateTime reviewedAt) {
        this.id = id;
        this.userId = userId;
        this.flashcardId = flashcardId;
        this.state = state;
        this.reviewedAt = reviewedAt;
    }

    public static FlashcardReviewDTO convertEntityToDto(FlashcardReview entity) {
        if (entity == null) return null;
        return new FlashcardReviewDTO(
                entity.getId(),
                entity.getUserId(),
                entity.getFlashcardId(),
                entity.getState(),
                entity.getReviewedAt());
    }

    public static FlashcardReview convertDtoToEntity(FlashcardReviewDTO dto) {
        if (dto == null) return null;
        return new FlashcardReview(
                dto.getId(),
                dto.getUserId(),
                dto.getFlashcardId(),
                dto.getState(),
                dto.getReviewedAt());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getFlashcardId() {
        return flashcardId;
    }

    public void setFlashcardId(Integer flashcardId) {
        this.flashcardId = flashcardId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}











