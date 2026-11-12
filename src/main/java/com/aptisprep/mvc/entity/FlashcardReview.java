package com.aptisprep.mvc.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Durable review action key: retrying a request must not count a second review. */
@Entity
@Table(name = "flashcard_reviews")
public class FlashcardReview {
    public FlashcardReview() {}

    @Id
    @Column(length = 160)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "flashcard_id", nullable = false)
    private Integer flashcardId;

    @Column(nullable = false, length = 30)
    private String state;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

    public FlashcardReview(
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

    public String getState() {
        return state;
    }


    public String getId() {
        return id;
    }

    public void setId(String value) {
        this.id = value;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer value) {
        this.userId = value;
    }

    public Integer getFlashcardId() {
        return flashcardId;
    }

    public void setFlashcardId(Integer value) {
        this.flashcardId = value;
    }

    public void setState(String value) {
        this.state = value;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime value) {
        this.reviewedAt = value;
    }
}







