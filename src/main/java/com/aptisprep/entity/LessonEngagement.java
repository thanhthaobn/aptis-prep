package com.aptisprep.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "lesson_engagement",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lesson_id"}))
public class LessonEngagement {
    @Column(name = "playback_token_hash", length = 64)
    private String playbackTokenHash;

    @Column(name = "playback_audio_path", length = 500)
    private String playbackAudioPath;

    @Column(name = "playback_duration_seconds")
    private Double playbackDurationSeconds;

    @Column(name = "playback_position_seconds")
    private Double playbackPositionSeconds;

    @Column(name = "playback_last_ping_at")
    private LocalDateTime playbackLastPingAt;

    @Column(name = "playback_expires_at")
    private LocalDateTime playbackExpiresAt;

    @OneToMany(mappedBy = "engagement", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startSeconds ASC")
    private List<LessonPlaybackRangeEntity> playbackRanges = new ArrayList<>();

    @Column(name = "playback_sequence", nullable = false)
    private int playbackSequence;

    @Column(name = "playback_running", nullable = false)
    private boolean playbackRunning;

    @Column(name = "verified_audio_path", length = 500)
    private String verifiedAudioPath;

    public String getPlaybackTokenHash() {
        return playbackTokenHash;
    }

    public void setPlaybackTokenHash(String value) {
        playbackTokenHash = value;
    }

    public String getPlaybackAudioPath() {
        return playbackAudioPath;
    }

    public void setPlaybackAudioPath(String value) {
        playbackAudioPath = value;
    }

    public Double getPlaybackDurationSeconds() {
        return playbackDurationSeconds;
    }

    public void setPlaybackDurationSeconds(Double value) {
        playbackDurationSeconds = value;
    }

    public Double getPlaybackPositionSeconds() {
        return playbackPositionSeconds;
    }

    public void setPlaybackPositionSeconds(Double value) {
        playbackPositionSeconds = value;
    }

    public LocalDateTime getPlaybackLastPingAt() {
        return playbackLastPingAt;
    }

    public void setPlaybackLastPingAt(LocalDateTime value) {
        playbackLastPingAt = value;
    }

    public LocalDateTime getPlaybackExpiresAt() {
        return playbackExpiresAt;
    }

    public void setPlaybackExpiresAt(LocalDateTime value) {
        playbackExpiresAt = value;
    }

    public List<LessonPlaybackRangeEntity> getPlaybackRanges() {
        return playbackRanges;
    }

    public void replacePlaybackRanges(List<double[]> values) {
        playbackRanges.clear();
        for (double[] range : values) {
            LessonPlaybackRangeEntity entity = new LessonPlaybackRangeEntity();
            entity.setEngagement(this);
            entity.setStartSeconds(range[0]);
            entity.setEndSeconds(range[1]);
            playbackRanges.add(entity);
        }
    }

    public int getPlaybackSequence() {
        return playbackSequence;
    }

    public void setPlaybackSequence(int value) {
        playbackSequence = value;
    }

    public boolean isPlaybackRunning() {
        return playbackRunning;
    }

    public void setPlaybackRunning(boolean value) {
        playbackRunning = value;
    }

    public String getVerifiedAudioPath() {
        return verifiedAudioPath;
    }

    public void setVerifiedAudioPath(String value) {
        verifiedAudioPath = value;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "lesson_id", nullable = false)
    private Integer lessonId;

    @Column(name = "audio_played_at")
    private LocalDateTime audioPlayedAt;

    @Column(name = "last_section", length = 40)
    private String lastSection;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer v) {
        userId = v;
    }

    public Integer getLessonId() {
        return lessonId;
    }

    public void setLessonId(Integer v) {
        lessonId = v;
    }

    public LocalDateTime getAudioPlayedAt() {
        return audioPlayedAt;
    }

    public void setAudioPlayedAt(LocalDateTime v) {
        audioPlayedAt = v;
    }

    public String getLastSection() {
        return lastSection;
    }

    public void setLastSection(String v) {
        lastSection = v;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
