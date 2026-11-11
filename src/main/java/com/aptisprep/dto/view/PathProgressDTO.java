package com.aptisprep.dto.view;

public record PathProgressDTO(
        Integer pathId,
        String skill,
        String pathSource,
        String pathStatus,
        int completedLessons,
        int totalLessons,
        Integer progressPercent,
        int completedRequiredLessons,
        int totalRequiredLessons,
        Integer nextLessonId,
        String nextLessonTitle,
        String checkpointState) {
    public String checkpointActionLabel() {
        return switch (checkpointState == null ? "" : checkpointState) {
            case "in_progress" -> "Tiếp tục bài đang làm";
            case "grading" -> "Xem trạng thái chấm";
            case "failed" -> "Làm lại kiểm tra cuối chặng";
            default -> "Bắt đầu bài kiểm tra cuối chặng";
        };
    }
}
