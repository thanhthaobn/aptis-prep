package com.aptisprep.service.student;

import com.aptisprep.dto.view.StudentContentPreview.*;
import com.aptisprep.exception.ResourceNotFoundException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Temporary exam catalog only. Lessons, flashcards and results use persisted data. */
@Service
public class StudentContentPreviewService {
    private final List<Entry> exams = List.of(
        new Entry(7001, "Thi thử tổng hợp số 01", "Tổng hợp", "B2", "Thi thử", "AVAILABLE", "Xem cấu trúc", "Làm quen các phần thi Aptis.",
            List.of(new Fact("Tổng thời gian", "165 phút"), new Fact("Lượt nghe tối đa", "2 lượt mỗi đoạn")),
            List.of(new Block("Grammar & Vocabulary", "50 câu", "25 phút"), new Block("Reading", "4 phần", "35 phút"), new Block("Listening", "4 phần", "40 phút"), new Block("Writing", "4 phần", "50 phút"), new Block("Speaking", "4 phần", "15 phút"))),
        new Entry(7002, "Luyện tập Reading", "Reading", "B1", "Luyện tập", "AVAILABLE", "Xem cấu trúc", "Luyện đọc hiểu và sắp xếp ý.",
            List.of(new Fact("Thời gian", "35 phút")), List.of(new Block("Đọc hiểu", "Chọn thông tin phù hợp", "Phần 1"), new Block("Liên kết ý", "Sắp xếp đoạn văn", "Phần 2"))),
        new Entry(7003, "Luyện tập Writing", "Writing", "B2", "Luyện tập", "AVAILABLE", "Xem cấu trúc", "Luyện diễn đạt theo yêu cầu.",
            List.of(new Fact("Thời gian", "50 phút")), List.of(new Block("Trả lời ngắn", "Viết về chủ đề quen thuộc", "Phần 1–3"), new Block("Viết thư", "Điều chỉnh cách diễn đạt theo người nhận", "Phần 4")))
    );

    public List<Entry> find(String module, String q, String skill, String level, String state) {
        if (!"exams".equals(module)) return List.of();
        String term = normalize(q);
        return exams.stream()
            .filter(e -> normalize(e.title() + " " + e.summary()).contains(term))
            .filter(e -> skill.isBlank() || e.skill().equals(skill))
            .filter(e -> level.isBlank() || e.level().equals(level))
            .filter(e -> state.isBlank() || e.state().equals(state)).toList();
    }

    public Entry entry(String module, int id) {
        if (!"exams".equals(module)) throw new ResourceNotFoundException("Không tìm thấy nội dung");
        return exams.stream().filter(e -> e.id() == id).findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nội dung"));
    }

    private String normalize(String value) {
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT).replace('đ', 'd');
    }
}
