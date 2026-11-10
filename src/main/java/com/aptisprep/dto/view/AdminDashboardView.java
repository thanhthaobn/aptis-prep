package com.aptisprep.dto.view;

import com.aptisprep.service.contract.IntegrationModels.AdminHistoryItem;
import java.time.LocalDateTime;
import java.util.List;

/** Read-only model for the admin dashboard page. */
public record AdminDashboardView(
        String email,
        long studentCount,
        long lessonCount,
        long questionCount,
        long publishedExamCount,
        List<ContentUpdate> recentContent,
        List<AdminHistoryItem> recentAttempts) {
    public record ContentUpdate(String title, String kind, String url, LocalDateTime updatedAt) {}
}
