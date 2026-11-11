package com.aptisprep.dto;

import java.time.LocalDateTime;

public class LessonActivityDTO {
    private LocalDateTime lastAccessedAt;
    private LocalDateTime completedAt;

    public LessonActivityDTO() {}

    public LessonActivityDTO(LocalDateTime lastAccessedAt, LocalDateTime completedAt) {
        this.lastAccessedAt = lastAccessedAt;
        this.completedAt = completedAt;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
