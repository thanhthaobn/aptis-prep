package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class ExamMediaSnapshotDTO {
    private Integer id;
    private ExamQuestionSnapshot question;
    private Integer sourceMediaId;
    private Integer position;
    private String mediaType;
    private String mediaUrl;
    private String originalFilename;
    private String contentType;
    private String transcript;
    private Integer maxPlayCount;

    public ExamMediaSnapshotDTO() {}

    public ExamMediaSnapshotDTO(
            Integer id,
            ExamQuestionSnapshot question,
            Integer sourceMediaId,
            Integer position,
            String mediaType,
            String mediaUrl,
            String originalFilename,
            String contentType,
            String transcript,
            Integer maxPlayCount) {
        this.id = id;
        this.question = question;
        this.sourceMediaId = sourceMediaId;
        this.position = position;
        this.mediaType = mediaType;
        this.mediaUrl = mediaUrl;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.transcript = transcript;
        this.maxPlayCount = maxPlayCount;
    }

    public static ExamMediaSnapshotDTO convertEntityToDto(ExamMediaSnapshot entity) {
        if (entity == null) return null;
        return new ExamMediaSnapshotDTO(
                entity.getId(),
                entity.getQuestion(),
                entity.getSourceMediaId(),
                entity.getPosition(),
                entity.getMediaType(),
                entity.getMediaUrl(),
                entity.getOriginalFilename(),
                entity.getContentType(),
                entity.getTranscript(),
                entity.getMaxPlayCount());
    }

    public static ExamMediaSnapshot convertDtoToEntity(ExamMediaSnapshotDTO dto) {
        if (dto == null) return null;
        return new ExamMediaSnapshot(
                dto.getId(),
                dto.getQuestion(),
                dto.getSourceMediaId(),
                dto.getPosition(),
                dto.getMediaType(),
                dto.getMediaUrl(),
                dto.getOriginalFilename(),
                dto.getContentType(),
                dto.getTranscript(),
                dto.getMaxPlayCount());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ExamQuestionSnapshot getQuestion() {
        return question;
    }

    public void setQuestion(ExamQuestionSnapshot question) {
        this.question = question;
    }

    public Integer getSourceMediaId() {
        return sourceMediaId;
    }

    public void setSourceMediaId(Integer sourceMediaId) {
        this.sourceMediaId = sourceMediaId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
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

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public Integer getMaxPlayCount() {
        return maxPlayCount;
    }

    public void setMaxPlayCount(Integer maxPlayCount) {
        this.maxPlayCount = maxPlayCount;
    }
}
