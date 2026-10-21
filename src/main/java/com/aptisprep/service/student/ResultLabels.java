package com.aptisprep.service.student;

import org.springframework.stereotype.Component;

/** Presentation labels only; grading and attempt state remain owned by their services. */
@Component("resultLabels")
public class ResultLabels {
    public String state(String state) {
        return switch (state == null ? "" : state) {
            case "IN_PROGRESS" -> "Đang làm bài";
            case "SUBMITTED" -> "Đã nộp bài";
            case "WAITING_GRADING", "GRADING", "WAITING", "PENDING" -> "Đang chờ chấm";
            case "COMPLETED" -> "Đã hoàn thành";
            case "ERROR", "GRADING_FAILED" -> "Chấm chưa thành công";
            case "EXPIRED" -> "Đã hết thời gian";
            case "CANCELLED" -> "Đã hủy";
            default -> "Chưa có trạng thái";
        };
    }
    public String examType(String type) {
        return switch (type == null ? "" : type) {
            case "PLACEMENT" -> "Kiểm tra đầu vào";
            case "PRACTICE" -> "Luyện tập";
            case "CHECKPOINT" -> "Kiểm tra cuối chặng";
            case "FULL_MOCK" -> "Thi thử tổng hợp";
            default -> "Bài thi";
        };
    }
}
