package com.aptisprep.mvc.service.admin;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminDashboardService {
    public ContentSummary contentSummary();

    record ContentSummary(
            long lessonCount,
            long questionCount,
            long publishedExamCount,
            List<ContentUpdate> recentContent) {}

    record ContentUpdate(String title, String kind, String url, LocalDateTime updatedAt) {}
}







