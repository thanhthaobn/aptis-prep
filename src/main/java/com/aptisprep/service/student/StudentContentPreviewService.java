package com.aptisprep.service.student;

import com.aptisprep.dto.view.StudentContentPreview.*;
import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.entity.MockExamSectionEntity;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.MockExamQuestionRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.repository.MockExamSectionRepository;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

/** Temporary exam catalog only. Lessons, flashcards and results use persisted data. */
@Service
public class StudentContentPreviewService {
    private final MockExamRepository exams;
    private final MockExamSectionRepository sections;
    private final MockExamQuestionRepository examQuestions;
    @Value("${app.demo.catalog-fallback:false}")
    private boolean catalogFallback;

    public StudentContentPreviewService(
            MockExamRepository exams,
            MockExamSectionRepository sections,
            MockExamQuestionRepository examQuestions) {
        this.exams = exams;
        this.sections = sections;
        this.examQuestions = examQuestions;
    }

    private final List<Entry> fallbackExams =
            List.of(
                    new Entry(
                            7001,
                            "Thi thử tổng hợp số 01",
                            "Tổng hợp",
                            "B2",
                            "Thi thử",
                            "AVAILABLE",
                            "Xem cấu trúc",
                            "Làm quen các phần thi Aptis.",
                            List.of(
                                    new Fact("Tổng thời gian", "165 phút"),
                                    new Fact("Lượt nghe tối đa", "2 lượt mỗi đoạn")),
                            List.of(
                                    new Block("Grammar & Vocabulary", "50 câu", "25 phút"),
                                    new Block("Reading", "4 phần", "35 phút"),
                                    new Block("Listening", "4 phần", "40 phút"),
                                    new Block("Writing", "4 phần", "50 phút"),
                                    new Block("Speaking", "4 phần", "15 phút"))),
                    new Entry(
                            7002,
                            "Luyện tập Reading",
                            "Reading",
                            "B1",
                            "Luyện tập",
                            "AVAILABLE",
                            "Xem cấu trúc",
                            "Luyện đọc hiểu và sắp xếp ý.",
                            List.of(new Fact("Thời gian", "35 phút")),
                            List.of(
                                    new Block("Đọc hiểu", "Chọn thông tin phù hợp", "Phần 1"),
                                    new Block("Liên kết ý", "Sắp xếp đoạn văn", "Phần 2"))),
                    new Entry(
                            7003,
                            "Luyện tập Writing",
                            "Writing",
                            "B2",
                            "Luyện tập",
                            "AVAILABLE",
                            "Xem cấu trúc",
                            "Luyện diễn đạt theo yêu cầu.",
                            List.of(new Fact("Thời gian", "50 phút")),
                            List.of(
                                    new Block(
                                            "Trả lời ngắn",
                                            "Viết về chủ đề quen thuộc",
                                            "Phần 1–3"),
                                    new Block(
                                            "Viết thư",
                                            "Điều chỉnh cách diễn đạt theo người nhận",
                                            "Phần 4"))));

    public List<Entry> find(String module, String q, String skill, String level, String state) {
        if ("exams".equals(module)) {
            if (exams.count() > 0) return realExams(q, skill, level);
            if (!catalogFallback) return List.of();
        }
        String term = normalize(q);
        return fallbackExams.stream()
                .filter(e -> normalize(e.title() + " " + e.summary()).contains(term))
                .filter(e -> skill.isBlank() || e.skill().equals(skill))
                .filter(e -> level.isBlank() || e.level().equals(level))
                .filter(e -> state.isBlank() || e.state().equals(state))
                .toList();
    }

    public Entry entry(String module, int id) {
        if ("exams".equals(module)) {
            if (exams.count() > 0) {
                var real = exams.findById(id).filter(exam -> "PUBLISHED".equals(exam.getStatus()));
                if (real.isEmpty()) throw new ResourceNotFoundException("Không tìm thấy đề thi đã công bố");
                return realExamEntry(real.get());
            }
            if (!catalogFallback) throw new ResourceNotFoundException("Chưa có đề thi đã công bố");
        }
        return fallbackExams.stream()
                .filter(e -> e.id() == id)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nội dung"));
    }

    private List<Entry> realExams(String q, String skill, String level) {
        String term = normalize(q);
        String requestedSkill = normalizeSkill(skill);
        List<MockExamEntity> publishedExams = exams.findByStatusOrderByUpdatedAtDesc("PUBLISHED");
        return publishedExams.stream()
                .filter(
                        exam ->
                                normalize(exam.getTitle() + " " + exam.getDescription())
                                        .contains(term))
                .filter(
                        exam ->
                                requestedSkill.isBlank()
                                        || requestedSkill.equals(normalizeSkill(exam.getSkill())))
                .filter(
                        exam ->
                                level == null
                                        || level.isBlank()
                                        || level.equalsIgnoreCase(exam.getTargetLevel()))
                .map(this::realExamEntry)
                .toList();
    }

    private Entry realExamEntry(MockExamEntity exam) {
        List<Block> blocks = new ArrayList<>();
        List<MockExamSectionEntity> realSections = sections.findByMockExamIdOrderById(exam.getId());
        if (!realSections.isEmpty()) {
            realSections.stream()
                    .sorted(Comparator.comparingInt(MockExamSectionEntity::getDisplayOrder))
                    .forEach(
                            section ->
                                    blocks.add(
                                            new Block(
                                                    displaySkill(section.getSkill()),
                                                    examQuestions
                                                                    .findByMockExamIdOrderByPosition(
                                                                            exam.getId())
                                                                    .stream()
                                                                    .filter(
                                                                            link ->
                                                                                    section.getId()
                                                                                            .equals(
                                                                                                    link
                                                                                                            .getMockExamSectionId()))
                                                                    .count()
                                                            + " câu hỏi",
                                                    section.getDurationMinutes() + " phút")));
        } else {
            blocks.add(
                    new Block(
                            displaySkill(exam.getSkill()),
                            examQuestions.findByMockExamIdOrderByPosition(exam.getId()).size()
                                    + " câu hỏi",
                            exam.getDurationMinutes() + " phút"));
        }
        return new Entry(
                exam.getId(),
                exam.getTitle(),
                displaySkill(exam.getSkill()),
                exam.getTargetLevel(),
                examTypeLabel(exam.getExamType()),
                "PUBLISHED",
                "Đã công bố",
                exam.getDescription() == null ? "Đề thi đã công bố" : exam.getDescription(),
                List.of(
                        new Fact(
                                "Tổng thời gian",
                                String.valueOf(exam.getDurationMinutes()) + " phút"),
                        new Fact("Số section", String.valueOf(Math.max(1, realSections.size())))),
                blocks);
    }

    private String normalizeSkill(String value) {
        if (value == null) return "";
        return value.trim().toUpperCase(Locale.ROOT).replace(" & ", "_").replace(' ', '_');
    }

    private String displaySkill(String value) {
        return switch (normalizeSkill(value)) {
            case "GRAMMAR_VOCABULARY" -> "Grammar & Vocabulary";
            case "READING" -> "Reading";
            case "LISTENING" -> "Listening";
            case "WRITING" -> "Writing";
            case "SPEAKING" -> "Speaking";
            default -> value == null ? "" : value;
        };
    }

    private String examTypeLabel(String value) {
        return switch (value == null ? "" : value) {
            case "PLACEMENT" -> "Kiểm tra đầu vào";
            case "CHECKPOINT" -> "Kiểm tra cuối chặng";
            case "FULL_MOCK" -> "Thi thử tổng hợp";
            default -> "Luyện tập";
        };
    }

    private String normalize(String value) {
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd');
    }
}
