package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class UserMediaActivityDTO {
    private Integer id;
    private Integer attemptId;
    private Integer mediaId;
    private int playCount;
    private LocalDateTime lastPlayedAt;
    private String playTokenHash;
    private LocalDateTime playTokenExpiresAt;

    public UserMediaActivityDTO() {}

    public UserMediaActivityDTO(
            Integer id,
            Integer attemptId,
            Integer mediaId,
            int playCount,
            LocalDateTime lastPlayedAt,
            String playTokenHash,
            LocalDateTime playTokenExpiresAt) {
        this.id = id;
        this.attemptId = attemptId;
        this.mediaId = mediaId;
        this.playCount = playCount;
        this.lastPlayedAt = lastPlayedAt;
        this.playTokenHash = playTokenHash;
        this.playTokenExpiresAt = playTokenExpiresAt;
    }

    public static UserMediaActivityDTO convertEntityToDto(UserMediaActivity entity) {
        if (entity == null) return null;
        return new UserMediaActivityDTO(
                entity.getId(),
                entity.getAttemptId(),
                entity.getMediaId(),
                entity.getPlayCount(),
                entity.getLastPlayedAt(),
                entity.getPlayTokenHash(),
                entity.getPlayTokenExpiresAt());
    }

    public static UserMediaActivity convertDtoToEntity(UserMediaActivityDTO dto) {
        if (dto == null) return null;
        return new UserMediaActivity(
                dto.getId(),
                dto.getAttemptId(),
                dto.getMediaId(),
                dto.getPlayCount(),
                dto.getLastPlayedAt(),
                dto.getPlayTokenHash(),
                dto.getPlayTokenExpiresAt());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public Integer getMediaId() {
        return mediaId;
    }

    public void setMediaId(Integer mediaId) {
        this.mediaId = mediaId;
    }

    public int getPlayCount() {
        return playCount;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
    }

    public LocalDateTime getLastPlayedAt() {
        return lastPlayedAt;
    }

    public void setLastPlayedAt(LocalDateTime lastPlayedAt) {
        this.lastPlayedAt = lastPlayedAt;
    }

    public String getPlayTokenHash() {
        return playTokenHash;
    }

    public void setPlayTokenHash(String playTokenHash) {
        this.playTokenHash = playTokenHash;
    }

    public LocalDateTime getPlayTokenExpiresAt() {
        return playTokenExpiresAt;
    }

    public void setPlayTokenExpiresAt(LocalDateTime playTokenExpiresAt) {
        this.playTokenExpiresAt = playTokenExpiresAt;
    }
}











