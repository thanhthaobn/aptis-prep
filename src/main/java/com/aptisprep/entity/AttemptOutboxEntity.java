package com.aptisprep.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "attempt_outbox", uniqueConstraints = @UniqueConstraint(name = "uq_attempt_outbox_event_key", columnNames = "event_key"))
public class AttemptOutboxEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_key", nullable = false, length = 255)
    private String eventKey;
    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;
    @Column(name = "aggregate_id", nullable = false)
    private Integer aggregateId;
    @Column(nullable = false, columnDefinition = "nvarchar(max)")
    private String payload;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(nullable = false)
    private int attempts;
    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;
    @Column(name = "last_error", length = 1000)
    private String lastError;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (nextAttemptAt == null) nextAttemptAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getEventKey() {
        return eventKey;
    }

    public String getEventType() {
        return eventType;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public String getPayload() {
        return payload;
    }

    public String getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public LocalDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setEventKey(String value) {
        eventKey = value;
    }

    public void setEventType(String value) {
        eventType = value;
    }

    public void setAggregateId(Integer value) {
        aggregateId = value;
    }

    public void setPayload(String value) {
        payload = value;
    }

    public void setStatus(String value) {
        status = value;
    }

    public void setAttempts(int value) {
        attempts = value;
    }

    public void setNextAttemptAt(LocalDateTime value) {
        nextAttemptAt = value;
    }

    public void setLastError(String value) {
        lastError = value;
    }

    public void setProcessedAt(LocalDateTime value) {
        processedAt = value;
    }
}
