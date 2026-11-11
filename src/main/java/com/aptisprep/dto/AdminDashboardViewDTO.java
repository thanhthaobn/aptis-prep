package com.aptisprep.dto;

import java.util.List;

public class AdminDashboardViewDTO extends AdminDashboardDTO {
    public AdminDashboardViewDTO() {
        super();
    }

    public AdminDashboardViewDTO(
            String email,
            long studentCount,
            long lessonCount,
            long questionCount,
            long publishedExamCount,
            long pendingUnlockRequestsCount,
            List<ContentUpdate> recentContent,
            List<AdminHistoryItemDTO> recentAttempts) {
        super(
                email,
                studentCount,
                lessonCount,
                questionCount,
                publishedExamCount,
                pendingUnlockRequestsCount,
                recentContent,
                recentAttempts);
    }

    public AdminDashboardViewDTO(
            String email,
            long studentCount,
            long lessonCount,
            long questionCount,
            long publishedExamCount,
            List<ContentUpdate> recentContent,
            List<AdminHistoryItemDTO> recentAttempts) {
        super(
                email,
                studentCount,
                lessonCount,
                questionCount,
                publishedExamCount,
                recentContent,
                recentAttempts);
    }
}
