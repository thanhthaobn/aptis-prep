package com.aptisprep.mvc.student.dto;

import com.aptisprep.mvc.exam.dto.AdminHistoryItemDTO;
import java.time.LocalDateTime;
import java.util.List;

/** Read-only model for the admin dashboard page. */
public record AdminDashboardViewDTO(
        String email,
        long studentCount,
        long lessonCount,
        long questionCount,
        long publishedExamCount,
        List<ContentUpdate> recentContent,
        List<AdminHistoryItemDTO> recentAttempts) {
    public record ContentUpdate(String title, String kind, String url, LocalDateTime updatedAt) {}
}
