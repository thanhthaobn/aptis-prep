package com.aptisprep.dto.view;

import com.aptisprep.enums.UserStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class AdminStudentView {
    private AdminStudentView() {
    }

    public record ListItem(Integer id, String fullName, String email, String initials,
                           LocalDateTime joinedAt, UserStatus status, String targetLevel) {
        public String statusLabel() {
            return labels(status);
        }

        public String statusClass() {
            return switch (status) {
                case ACTIVE -> "student-status-active";
                case LOCKED -> "student-status-locked";
                case INACTIVE -> "student-status-inactive";
            };
        }

        public String joinedDate() {
            return joinedAt == null ? "" : joinedAt.toLocalDate().toString();
        }
    }

    public record Progress(Integer pathId, String skill, String pathSource, String pathStatus,
                           int completedLessons, int totalLessons, Integer progressPercent,
                           int completedRequiredLessons, int totalRequiredLessons,
                           String nextLessonTitle, String checkpointState) {
        public String percentLabel() {
            return progressPercent == null ? "Chưa có dữ liệu" : progressPercent + "%";
        }
    }

    public record Skill(String name, String level, String target) {
    }

    public record Attempt(String title, String date, String status, String score) {
    }

    public record Detail(Integer id, String fullName, String email, String initials,
                         LocalDateTime joinedAt, UserStatus status, AccountSnapshot.GoalView goal,
                         List<Skill> skills, List<Progress> progress, List<Attempt> attempts,
                         int attemptPage, int attemptSize, long attemptTotal) {
        public String statusLabel() {
            return labels(status);
        }

        public String statusClass() {
            return switch (status) {
                case ACTIVE -> "student-status-active";
                case LOCKED -> "student-status-locked";
                case INACTIVE -> "student-status-inactive";
            };
        }

        public String joinedDate() {
            return joinedAt == null ? "" : joinedAt.toLocalDate().toString();
        }
    }

    public record Page(List<ListItem> items, int page, int size, long totalItems) {
        public int totalPages() {
            return totalItems == 0 ? 0 : (int) ((totalItems + size - 1) / size);
        }

        public boolean hasPrevious() {
            return page > 0;
        }

        public boolean hasNext() {
            return page + 1 < totalPages();
        }

        public int previousPage() {
            return Math.max(0, page - 1);
        }

        public int nextPage() {
            return page + 1;
        }
    }

    private static String labels(UserStatus status) {
        return switch (status) {
            case ACTIVE -> "Đang hoạt động";
            case LOCKED -> "Đã khóa";
            case INACTIVE -> "Ngừng hoạt động";
        };
    }
}
