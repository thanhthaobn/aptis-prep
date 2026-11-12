package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class UserFlashcardProgressDTO {
    private Integer id;
    private User user;
    private Flashcard flashcard;
    private String status;
    private Integer masteryLevel;
    private Integer reviewCount;
    private LocalDateTime lastReviewedAt;
    private LocalDateTime nextReviewAt;
    private Integer intervalDays;
    private BigDecimal easeFactor;

    public UserFlashcardProgressDTO() {}

    public UserFlashcardProgressDTO(
            Integer id,
            User user,
            Flashcard flashcard,
            String status,
            Integer masteryLevel,
            Integer reviewCount,
            LocalDateTime lastReviewedAt,
            LocalDateTime nextReviewAt,
            Integer intervalDays,
            BigDecimal easeFactor) {
        this.id = id;
        this.user = user;
        this.flashcard = flashcard;
        this.status = status;
        this.masteryLevel = masteryLevel;
        this.reviewCount = reviewCount;
        this.lastReviewedAt = lastReviewedAt;
        this.nextReviewAt = nextReviewAt;
        this.intervalDays = intervalDays;
        this.easeFactor = easeFactor;
    }

    public static UserFlashcardProgressDTO convertEntityToDto(UserFlashcardProgress entity) {
        if (entity == null) return null;
        return new UserFlashcardProgressDTO(
                entity.getId(),
                entity.getUser(),
                entity.getFlashcard(),
                entity.getStatus(),
                entity.getMasteryLevel(),
                entity.getReviewCount(),
                entity.getLastReviewedAt(),
                entity.getNextReviewAt(),
                entity.getIntervalDays(),
                entity.getEaseFactor());
    }

    public static UserFlashcardProgress convertDtoToEntity(UserFlashcardProgressDTO dto) {
        if (dto == null) return null;
        return new UserFlashcardProgress(
                dto.getId(),
                dto.getUser(),
                dto.getFlashcard(),
                dto.getStatus(),
                dto.getMasteryLevel(),
                dto.getReviewCount(),
                dto.getLastReviewedAt(),
                dto.getNextReviewAt(),
                dto.getIntervalDays(),
                dto.getEaseFactor());
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

    public Flashcard getFlashcard() {
        return flashcard;
    }

    public void setFlashcard(Flashcard flashcard) {
        this.flashcard = flashcard;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getMasteryLevel() {
        return masteryLevel;
    }

    public void setMasteryLevel(Integer masteryLevel) {
        this.masteryLevel = masteryLevel;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public LocalDateTime getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setLastReviewedAt(LocalDateTime lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
    }

    public LocalDateTime getNextReviewAt() {
        return nextReviewAt;
    }

    public void setNextReviewAt(LocalDateTime nextReviewAt) {
        this.nextReviewAt = nextReviewAt;
    }

    public Integer getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(Integer intervalDays) {
        this.intervalDays = intervalDays;
    }

    public BigDecimal getEaseFactor() {
        return easeFactor;
    }

    public void setEaseFactor(BigDecimal easeFactor) {
        this.easeFactor = easeFactor;
    }
}
