package com.aptisprep.dto;

import com.aptisprep.entity.Flashcard;
import com.aptisprep.entity.User;
import com.aptisprep.entity.UserFlashcardProgress;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UserFlashcardProgressDTO {
    private Integer id;
    private User user;
    private Integer userId;
    private Flashcard flashcard;
    private Integer flashcardId;
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
        this.userId = user == null ? null : user.getId();
        this.flashcard = flashcard;
        this.flashcardId = flashcard == null ? null : flashcard.getId();
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
        UserFlashcardProgressDTO dto = new UserFlashcardProgressDTO();
        dto.id = entity.getId();
        dto.user = entity.getUser();
        dto.userId = entity.getUser() == null ? null : entity.getUser().getId();
        dto.flashcard = entity.getFlashcard();
        dto.flashcardId = entity.getFlashcard() == null ? null : entity.getFlashcard().getId();
        dto.status = entity.getStatus();
        dto.masteryLevel = entity.getMasteryLevel();
        dto.reviewCount = entity.getReviewCount();
        dto.lastReviewedAt = entity.getLastReviewedAt();
        dto.nextReviewAt = entity.getNextReviewAt();
        dto.intervalDays = entity.getIntervalDays();
        dto.easeFactor = entity.getEaseFactor();
        return dto;
    }

    public UserFlashcardProgress convertDtoToEntity() {
        UserFlashcardProgress entity =
                new UserFlashcardProgress(
                        this.user,
                        this.flashcard,
                        this.status != null ? this.status : "NEW",
                        this.masteryLevel != null ? this.masteryLevel : 0,
                        this.reviewCount != null ? this.reviewCount : 0,
                        this.lastReviewedAt,
                        this.nextReviewAt,
                        this.intervalDays != null ? this.intervalDays : 1,
                        this.easeFactor != null ? this.easeFactor : BigDecimal.valueOf(2.5));
        if (this.id != null) {
            entity.setId(this.id);
        }
        return entity;
    }

    public static UserFlashcardProgress convertDtoToEntity(UserFlashcardProgressDTO dto) {
        return dto == null ? null : dto.convertDtoToEntity();
    }

    public void updateEntityFromDto(UserFlashcardProgress entity) {
        if (entity == null) return;
        if (this.user != null) entity.setUser(this.user);
        if (this.flashcard != null) entity.setFlashcard(this.flashcard);
        if (this.status != null) entity.setStatus(this.status);
        if (this.masteryLevel != null) entity.setMasteryLevel(this.masteryLevel);
        if (this.reviewCount != null) entity.setReviewCount(this.reviewCount);
        entity.setLastReviewedAt(this.lastReviewedAt);
        entity.setNextReviewAt(this.nextReviewAt);
        if (this.intervalDays != null) entity.setIntervalDays(this.intervalDays);
        if (this.easeFactor != null) entity.setEaseFactor(this.easeFactor);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer id() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        this.userId = user == null ? null : user.getId();
    }

    public User user() {
        return user;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer userId() {
        return userId;
    }

    public Flashcard getFlashcard() {
        return flashcard;
    }

    public void setFlashcard(Flashcard flashcard) {
        this.flashcard = flashcard;
        this.flashcardId = flashcard == null ? null : flashcard.getId();
    }

    public Flashcard flashcard() {
        return flashcard;
    }

    public Integer getFlashcardId() {
        return flashcardId;
    }

    public void setFlashcardId(Integer flashcardId) {
        this.flashcardId = flashcardId;
    }

    public Integer flashcardId() {
        return flashcardId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String status() {
        return status;
    }

    public Integer getMasteryLevel() {
        return masteryLevel;
    }

    public void setMasteryLevel(Integer masteryLevel) {
        this.masteryLevel = masteryLevel;
    }

    public Integer masteryLevel() {
        return masteryLevel;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Integer reviewCount() {
        return reviewCount;
    }

    public LocalDateTime getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setLastReviewedAt(LocalDateTime lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
    }

    public LocalDateTime lastReviewedAt() {
        return lastReviewedAt;
    }

    public LocalDateTime getNextReviewAt() {
        return nextReviewAt;
    }

    public void setNextReviewAt(LocalDateTime nextReviewAt) {
        this.nextReviewAt = nextReviewAt;
    }

    public LocalDateTime nextReviewAt() {
        return nextReviewAt;
    }

    public Integer getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(Integer intervalDays) {
        this.intervalDays = intervalDays;
    }

    public Integer intervalDays() {
        return intervalDays;
    }

    public BigDecimal getEaseFactor() {
        return easeFactor;
    }

    public void setEaseFactor(BigDecimal easeFactor) {
        this.easeFactor = easeFactor;
    }

    public BigDecimal easeFactor() {
        return easeFactor;
    }
}
