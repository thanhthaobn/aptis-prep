package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class AttemptOutboxDTO {
    private Long id;
    private String eventKey;
    private String eventType;
    private Integer aggregateId;
    private String payload;
    private String status;
    private int attempts;
    private LocalDateTime nextAttemptAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    public AttemptOutboxDTO() {}

    public AttemptOutboxDTO(
            Long id,
            String eventKey,
            String eventType,
            Integer aggregateId,
            String payload,
            String status,
            int attempts,
            LocalDateTime nextAttemptAt,
            String lastError,
            LocalDateTime createdAt,
            LocalDateTime processedAt) {
        this.id = id;
        this.eventKey = eventKey;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.status = status;
        this.attempts = attempts;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = lastError;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
    }

    public static AttemptOutboxDTO convertEntityToDto(AttemptOutbox entity) {
        if (entity == null) return null;
        return new AttemptOutboxDTO(
                entity.getId(),
                entity.getEventKey(),
                entity.getEventType(),
                entity.getAggregateId(),
                entity.getPayload(),
                entity.getStatus(),
                entity.getAttempts(),
                entity.getNextAttemptAt(),
                entity.getLastError(),
                entity.getCreatedAt(),
                entity.getProcessedAt());
    }

    public static AttemptOutbox convertDtoToEntity(AttemptOutboxDTO dto) {
        if (dto == null) return null;
        return new AttemptOutbox(
                dto.getId(),
                dto.getEventKey(),
                dto.getEventType(),
                dto.getAggregateId(),
                dto.getPayload(),
                dto.getStatus(),
                dto.getAttempts(),
                dto.getNextAttemptAt(),
                dto.getLastError(),
                dto.getCreatedAt(),
                dto.getProcessedAt());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public LocalDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(LocalDateTime nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
