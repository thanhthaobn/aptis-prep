package com.aptisprep.presentation;

public record LessonProgressDTO(Integer id, String status) {
    public String getStatus() {
        return status;
    }
}
