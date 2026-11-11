package com.aptisprep.mvc.learning.dto;

public record LessonProgressDTO(Integer id, String status) {
    public String getStatus() {
        return status;
    }
}
