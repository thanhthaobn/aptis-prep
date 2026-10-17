package com.aptisprep.dto.view;

import com.aptisprep.enums.UserStatus;
import java.util.List;

/** UI fixture only. Replace with a read model from the student management service. */
public record StudentPreview(
        int id, String fullName, String email, String initials, String joinedAt,
        UserStatus status, String targetLevel, String targetDate,
        int completedLessons, int totalLessons, int reviewedCards,
        List<Skill> skills, List<Attempt> attempts) {
    public String statusLabel() {
        return switch (status) {
            case ACTIVE -> "Đang hoạt động";
            case LOCKED -> "Đã khóa";
            case INACTIVE -> "Ngừng hoạt động";
        };
    }
    public String statusClass() {
        return switch (status) {
            case ACTIVE -> "student-status-active";
            case LOCKED -> "student-status-locked";
            case INACTIVE -> "student-status-inactive";
        };
    }
    public int progressPercent() {
        return totalLessons == 0 ? 0 : completedLessons * 100 / totalLessons;
    }
    public record Skill(String name, String level, String target) {}
    public record Attempt(String title, String date, String status, String score) {}
}
