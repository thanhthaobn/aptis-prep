package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class LessonEngagementDTO {
    private String playbackTokenHash;
    private String playbackAudioPath;
    private Double playbackDurationSeconds;
    private Double playbackPositionSeconds;
    private LocalDateTime playbackLastPingAt;
    private LocalDateTime playbackExpiresAt;
    private int playbackSequence;
    private boolean playbackRunning;
    private String verifiedAudioPath;
    private Integer id;
    private Integer userId;
    private Integer lessonId;
    private LocalDateTime audioPlayedAt;
    private String lastSection;
    private LocalDateTime updatedAt;
    private List<LessonPlaybackRange> playbackRanges;

    public LessonEngagementDTO() {}

    public LessonEngagementDTO(
            String playbackTokenHash,
            String playbackAudioPath,
            Double playbackDurationSeconds,
            Double playbackPositionSeconds,
            LocalDateTime playbackLastPingAt,
            LocalDateTime playbackExpiresAt,
            int playbackSequence,
            boolean playbackRunning,
            String verifiedAudioPath,
            Integer id,
            Integer userId,
            Integer lessonId,
            LocalDateTime audioPlayedAt,
            String lastSection,
            LocalDateTime updatedAt,
            List<LessonPlaybackRange> playbackRanges) {
        this.playbackTokenHash = playbackTokenHash;
        this.playbackAudioPath = playbackAudioPath;
        this.playbackDurationSeconds = playbackDurationSeconds;
        this.playbackPositionSeconds = playbackPositionSeconds;
        this.playbackLastPingAt = playbackLastPingAt;
        this.playbackExpiresAt = playbackExpiresAt;
        this.playbackSequence = playbackSequence;
        this.playbackRunning = playbackRunning;
        this.verifiedAudioPath = verifiedAudioPath;
        this.id = id;
        this.userId = userId;
        this.lessonId = lessonId;
        this.audioPlayedAt = audioPlayedAt;
        this.lastSection = lastSection;
        this.updatedAt = updatedAt;
        this.playbackRanges = playbackRanges;
    }

    public static LessonEngagementDTO convertEntityToDto(LessonEngagement entity) {
        if (entity == null) return null;
        return new LessonEngagementDTO(
                entity.getPlaybackTokenHash(),
                entity.getPlaybackAudioPath(),
                entity.getPlaybackDurationSeconds(),
                entity.getPlaybackPositionSeconds(),
                entity.getPlaybackLastPingAt(),
                entity.getPlaybackExpiresAt(),
                entity.getPlaybackSequence(),
                entity.isPlaybackRunning(),
                entity.getVerifiedAudioPath(),
                entity.getId(),
                entity.getUserId(),
                entity.getLessonId(),
                entity.getAudioPlayedAt(),
                entity.getLastSection(),
                entity.getUpdatedAt(),
                entity.getPlaybackRanges());
    }

    public static LessonEngagement convertDtoToEntity(LessonEngagementDTO dto) {
        if (dto == null) return null;
        return new LessonEngagement(
                dto.getPlaybackTokenHash(),
                dto.getPlaybackAudioPath(),
                dto.getPlaybackDurationSeconds(),
                dto.getPlaybackPositionSeconds(),
                dto.getPlaybackLastPingAt(),
                dto.getPlaybackExpiresAt(),
                dto.getPlaybackRanges(),
                dto.getPlaybackSequence(),
                dto.isPlaybackRunning(),
                dto.getVerifiedAudioPath(),
                dto.getId(),
                dto.getUserId(),
                dto.getLessonId(),
                dto.getAudioPlayedAt(),
                dto.getLastSection(),
                dto.getUpdatedAt());
    }

    public String getPlaybackTokenHash() {
        return playbackTokenHash;
    }

    public void setPlaybackTokenHash(String playbackTokenHash) {
        this.playbackTokenHash = playbackTokenHash;
    }

    public String getPlaybackAudioPath() {
        return playbackAudioPath;
    }

    public void setPlaybackAudioPath(String playbackAudioPath) {
        this.playbackAudioPath = playbackAudioPath;
    }

    public Double getPlaybackDurationSeconds() {
        return playbackDurationSeconds;
    }

    public void setPlaybackDurationSeconds(Double playbackDurationSeconds) {
        this.playbackDurationSeconds = playbackDurationSeconds;
    }

    public Double getPlaybackPositionSeconds() {
        return playbackPositionSeconds;
    }

    public void setPlaybackPositionSeconds(Double playbackPositionSeconds) {
        this.playbackPositionSeconds = playbackPositionSeconds;
    }

    public LocalDateTime getPlaybackLastPingAt() {
        return playbackLastPingAt;
    }

    public void setPlaybackLastPingAt(LocalDateTime playbackLastPingAt) {
        this.playbackLastPingAt = playbackLastPingAt;
    }

    public LocalDateTime getPlaybackExpiresAt() {
        return playbackExpiresAt;
    }

    public void setPlaybackExpiresAt(LocalDateTime playbackExpiresAt) {
        this.playbackExpiresAt = playbackExpiresAt;
    }

    public int getPlaybackSequence() {
        return playbackSequence;
    }

    public void setPlaybackSequence(int playbackSequence) {
        this.playbackSequence = playbackSequence;
    }

    public boolean isPlaybackRunning() {
        return playbackRunning;
    }

    public void setPlaybackRunning(boolean playbackRunning) {
        this.playbackRunning = playbackRunning;
    }

    public String getVerifiedAudioPath() {
        return verifiedAudioPath;
    }

    public void setVerifiedAudioPath(String verifiedAudioPath) {
        this.verifiedAudioPath = verifiedAudioPath;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getLessonId() {
        return lessonId;
    }

    public void setLessonId(Integer lessonId) {
        this.lessonId = lessonId;
    }

    public LocalDateTime getAudioPlayedAt() {
        return audioPlayedAt;
    }

    public void setAudioPlayedAt(LocalDateTime audioPlayedAt) {
        this.audioPlayedAt = audioPlayedAt;
    }

    public String getLastSection() {
        return lastSection;
    }

    public void setLastSection(String lastSection) {
        this.lastSection = lastSection;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<LessonPlaybackRange> getPlaybackRanges() {
        return playbackRanges;
    }

    public void setPlaybackRanges(List<LessonPlaybackRange> playbackRanges) {
        this.playbackRanges = playbackRanges;
    }
}
