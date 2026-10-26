package com.aptisprep.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_flashcard_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "flashcard_id"}))
public class UserFlashcardProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flashcard_id")
    private Flashcard flashcard;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "mastery_level")
    private Integer masteryLevel;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @Column(name = "next_review_at")
    private LocalDateTime nextReviewAt;

    @Column(name = "interval_days", nullable = false)
    private Integer intervalDays = 0;

    @Column(name = "ease_factor", nullable = false, precision = 4, scale = 2)
    private BigDecimal easeFactor = new BigDecimal("2.50");

    public Integer getId() {
        return id;
    }

    public void setUser(User v) {
        user = v;
    }

    public void setFlashcard(Flashcard v) {
        flashcard = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public Integer getMasteryLevel() {
        return masteryLevel;
    }

    public void setMasteryLevel(Integer v) {
        masteryLevel = v;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer v) {
        reviewCount = v;
    }

    public LocalDateTime getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setLastReviewedAt(LocalDateTime v) {
        lastReviewedAt = v;
    }

    public LocalDateTime getNextReviewAt() {
        return nextReviewAt;
    }

    public void setNextReviewAt(LocalDateTime v) {
        nextReviewAt = v;
    }

    public Integer getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(Integer v) {
        intervalDays = v;
    }

    public BigDecimal getEaseFactor() {
        return easeFactor;
    }

    public void setEaseFactor(BigDecimal v) {
        easeFactor = v;
    }
}
