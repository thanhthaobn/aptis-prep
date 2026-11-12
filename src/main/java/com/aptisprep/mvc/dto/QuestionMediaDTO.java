package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class QuestionMediaDTO {
    private Integer id;
    private Integer questionId;
    private String mediaType;
    private String mediaUrl;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private Integer maxPlayCount;
    private int position;
    private String altText;
    private String transcript;
    private LocalDateTime createdAt;

    public QuestionMediaDTO() {}

    public QuestionMediaDTO(
            Integer id,
            Integer questionId,
            String mediaType,
            String mediaUrl,
            String originalFilename,
            String contentType,
            Long fileSize,
            Integer maxPlayCount,
            int position,
            String altText,
            String transcript,
            LocalDateTime createdAt) {
        this.id = id;
        this.questionId = questionId;
        this.mediaType = mediaType;
        this.mediaUrl = mediaUrl;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.maxPlayCount = maxPlayCount;
        this.position = position;
        this.altText = altText;
        this.transcript = transcript;
        this.createdAt = createdAt;
    }

    public static QuestionMediaDTO convertEntityToDto(QuestionMedia entity) {
        if (entity == null) return null;
        return new QuestionMediaDTO(
                entity.getId(),
                entity.getQuestionId(),
                entity.getMediaType(),
                entity.getMediaUrl(),
                entity.getOriginalFilename(),
                entity.getContentType(),
                entity.getFileSize(),
                entity.getMaxPlayCount(),
                entity.getPosition(),
                entity.getAltText(),
                entity.getTranscript(),
                entity.getCreatedAt());
    }

    public static QuestionMedia convertDtoToEntity(QuestionMediaDTO dto) {
        if (dto == null) return null;
        return new QuestionMedia(
                dto.getId(),
                dto.getQuestionId(),
                dto.getMediaType(),
                dto.getMediaUrl(),
                dto.getOriginalFilename(),
                dto.getContentType(),
                dto.getFileSize(),
                dto.getMaxPlayCount(),
                dto.getPosition(),
                dto.getAltText(),
                dto.getTranscript(),
                dto.getCreatedAt());
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

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
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
}











