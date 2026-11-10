package com.aptisprep.mvc.exam.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_media_activities",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_media_activities_attempt_media",
                        columnNames = {"exam_attempt_id", "question_media_id"}))
public class UserMediaActivityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "exam_attempt_id", nullable = false)
    private Integer attemptId;

    @Column(name = "question_media_id", nullable = false)
    private Integer mediaId;

    @Column(name = "play_count", nullable = false)
    private int playCount;

    @Column(name = "last_played_at")
    private LocalDateTime lastPlayedAt;

    @Column(name = "play_token_hash", length = 64)
    private String playTokenHash;

    @Column(name = "play_token_expires_at")
    private LocalDateTime playTokenExpiresAt;

    public Integer getId() {
        return id;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public Integer getMediaId() {
        return mediaId;
    }

    public int getPlayCount() {
        return playCount;
    }

    public LocalDateTime getLastPlayedAt() {
        return lastPlayedAt;
    }

    public String getPlayTokenHash() {
        return playTokenHash;
    }

    public LocalDateTime getPlayTokenExpiresAt() {
        return playTokenExpiresAt;
    }

    public void setAttemptId(Integer value) {
        attemptId = value;
    }

    public void setMediaId(Integer value) {
        mediaId = value;
    }

    public void setPlayCount(int value) {
        playCount = value;
    }

    public void setLastPlayedAt(LocalDateTime value) {
        lastPlayedAt = value;
    }

    public void setPlayTokenHash(String value) {
        playTokenHash = value;
    }

    public void setPlayTokenExpiresAt(LocalDateTime value) {
        playTokenExpiresAt = value;
    }
}
