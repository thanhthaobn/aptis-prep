package com.aptisprep.dto;

import com.aptisprep.entity.QuestionMediaEntity;
import java.time.LocalDateTime;

public class QuestionMediaDTO {
    private Integer id;
    private Integer questionId;
    private String mediaType;
    private String mediaUrl;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private Integer maxPlayCount;
    private Integer position;
    private String altText;
    private String transcript;
    private LocalDateTime createdAt;

    public QuestionMediaDTO() {}

    public QuestionMediaDTO(
            Integer id,
            String mediaType,
            String originalFilename,
            String contentType,
            Long fileSize,
            Integer position,
            String altText,
            String mediaUrl,
            String transcript,
            Integer maxPlayCount) {
        this.id = id;
        this.mediaType = mediaType;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.position = position;
        this.altText = altText;
        this.mediaUrl = mediaUrl;
        this.transcript = transcript;
        this.maxPlayCount = maxPlayCount;
    }

    public static QuestionMediaDTO convertEntityToDto(QuestionMediaEntity entity) {
        if (entity == null) return null;
        QuestionMediaDTO dto =
                new QuestionMediaDTO(
                        entity.getId(),
                        entity.getMediaType(),
                        entity.getOriginalFilename(),
                        entity.getContentType(),
                        entity.getFileSize(),
                        entity.getPosition(),
                        entity.getAltText(),
                        entity.getMediaUrl(),
                        entity.getTranscript(),
                        entity.getMaxPlayCount());
        dto.setQuestionId(entity.getQuestionId());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public QuestionMediaEntity convertDtoToEntity() {
        QuestionMediaEntity entity =
                new QuestionMediaEntity(
                        this.questionId,
                        this.mediaType,
                        this.mediaUrl,
                        this.originalFilename,
                        this.contentType,
                        this.fileSize,
                        this.maxPlayCount,
                        this.position != null ? this.position : 0,
                        this.altText,
                        this.transcript,
                        this.createdAt != null ? this.createdAt : LocalDateTime.now());
        if (this.id != null) entity.setId(this.id);
        return entity;
    }

    public void updateEntityFromDto(QuestionMediaEntity entity) {
        if (entity == null) return;
        entity.setMediaType(this.mediaType);
        entity.setMediaUrl(this.mediaUrl);
        entity.setOriginalFilename(this.originalFilename);
        entity.setContentType(this.contentType);
        entity.setFileSize(this.fileSize);
        entity.setMaxPlayCount(this.maxPlayCount);
        if (this.position != null) entity.setPosition(this.position);
        entity.setAltText(this.altText);
        entity.setTranscript(this.transcript);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getMaxPlayCount() {
        return maxPlayCount;
    }

    public void setMaxPlayCount(Integer maxPlayCount) {
        this.maxPlayCount = maxPlayCount;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer id() {
        return getId();
    }

    public String mediaType() {
        return getMediaType();
    }

    public String originalFilename() {
        return getOriginalFilename();
    }

    public String contentType() {
        return getContentType();
    }

    public Long fileSize() {
        return getFileSize();
    }

    public Integer position() {
        return getPosition();
    }

    public String altText() {
        return getAltText();
    }

    public String mediaUrl() {
        return getMediaUrl();
    }

    public String transcript() {
        return getTranscript();
    }

    public Integer maxPlayCount() {
        return getMaxPlayCount();
    }
}
