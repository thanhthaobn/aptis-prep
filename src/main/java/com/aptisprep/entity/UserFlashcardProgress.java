package com.aptisprep.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_flashcard_progress", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "flashcard_id"}))
public class UserFlashcardProgress {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "flashcard_id") private Flashcard flashcard;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "mastery_level") private Integer masteryLevel;
    @Column(name = "review_count", nullable = false) private Integer reviewCount;
    @Column(name = "last_reviewed_at") private LocalDateTime lastReviewedAt;
    public Integer getId() { return id; }
    public void setUser(User v) { user = v; } public void setFlashcard(Flashcard v) { flashcard = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public Integer getMasteryLevel() { return masteryLevel; } public void setMasteryLevel(Integer v) { masteryLevel = v; }
    public Integer getReviewCount() { return reviewCount; } public void setReviewCount(Integer v) { reviewCount = v; }
    public LocalDateTime getLastReviewedAt() { return lastReviewedAt; } public void setLastReviewedAt(LocalDateTime v) { lastReviewedAt = v; }
}
