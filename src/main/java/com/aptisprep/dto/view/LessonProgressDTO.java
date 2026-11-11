package com.aptisprep.dto.view;

public record LessonProgressDTO(Integer id, String status) {
    public String getStatus() {
        return status;
    }
}
