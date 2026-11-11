package com.aptisprep.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AdminDashboardDTO {
    private String email;
    private long studentCount;
    private long lessonCount;
    private long questionCount;
    private long publishedExamCount;
    private long pendingUnlockRequestsCount;
    private List<ContentUpdate> recentContent;
    private List<AdminHistoryItemDTO> recentAttempts;

    public AdminDashboardDTO() {}

    public AdminDashboardDTO(
            String email,
            long studentCount,
            long lessonCount,
            long questionCount,
            long publishedExamCount,
            long pendingUnlockRequestsCount,
            List<ContentUpdate> recentContent,
            List<AdminHistoryItemDTO> recentAttempts) {
        this.email = email;
        this.studentCount = studentCount;
        this.lessonCount = lessonCount;
        this.questionCount = questionCount;
        this.publishedExamCount = publishedExamCount;
        this.pendingUnlockRequestsCount = pendingUnlockRequestsCount;
        this.recentContent = recentContent;
        this.recentAttempts = recentAttempts;
    }

    public AdminDashboardDTO(
            String email,
            long studentCount,
            long lessonCount,
            long questionCount,
            long publishedExamCount,
            List<ContentUpdate> recentContent,
            List<AdminHistoryItemDTO> recentAttempts) {
        this(
                email,
                studentCount,
                lessonCount,
                questionCount,
                publishedExamCount,
                0L,
                recentContent,
                recentAttempts);
    }

    public static class ContentUpdate {
        private String title;
        private String kind;
        private String url;
        private LocalDateTime updatedAt;

        public ContentUpdate() {}

        public ContentUpdate(String title, String kind, String url, LocalDateTime updatedAt) {
            this.title = title;
            this.kind = kind;
            this.url = url;
            this.updatedAt = updatedAt;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public String kind() {
            return kind;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String url() {
            return url;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }

        public LocalDateTime updatedAt() {
            return updatedAt;
        }
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String email() {
        return email;
    }

    public long getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(long studentCount) {
        this.studentCount = studentCount;
    }

    public long studentCount() {
        return studentCount;
    }

    public long getLessonCount() {
        return lessonCount;
    }

    public void setLessonCount(long lessonCount) {
        this.lessonCount = lessonCount;
    }

    public long lessonCount() {
        return lessonCount;
    }

    public long getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(long questionCount) {
        this.questionCount = questionCount;
    }

    public long questionCount() {
        return questionCount;
    }

    public long getPublishedExamCount() {
        return publishedExamCount;
    }

    public void setPublishedExamCount(long publishedExamCount) {
        this.publishedExamCount = publishedExamCount;
    }

    public long publishedExamCount() {
        return publishedExamCount;
    }

    public long getPendingUnlockRequestsCount() {
        return pendingUnlockRequestsCount;
    }

    public void setPendingUnlockRequestsCount(long pendingUnlockRequestsCount) {
        this.pendingUnlockRequestsCount = pendingUnlockRequestsCount;
    }

    public long pendingUnlockRequestsCount() {
        return pendingUnlockRequestsCount;
    }

    public List<ContentUpdate> getRecentContent() {
        return recentContent;
    }

    public void setRecentContent(List<ContentUpdate> recentContent) {
        this.recentContent = recentContent;
    }

    public List<ContentUpdate> recentContent() {
        return recentContent;
    }

    public List<AdminHistoryItemDTO> getRecentAttempts() {
        return recentAttempts;
    }

    public void setRecentAttempts(List<AdminHistoryItemDTO> recentAttempts) {
        this.recentAttempts = recentAttempts;
    }

    public List<AdminHistoryItemDTO> recentAttempts() {
        return recentAttempts;
    }
}
