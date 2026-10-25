package com.aptisprep.service.student;

import com.aptisprep.dto.view.StudentCatalogView.*;
import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.MockExamQuestionRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.repository.MockExamSectionRepository;
import java.text.Normalizer;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Published database content only. Missing data never falls back to fabricated exams. */
@Service
@Transactional(readOnly = true)
public class StudentExamCatalogService {
    private final MockExamRepository exams;
    private final MockExamSectionRepository sections;
    private final MockExamQuestionRepository questions;

    public StudentExamCatalogService(
            MockExamRepository exams,
            MockExamSectionRepository sections,
            MockExamQuestionRepository questions) {
        this.exams = exams;
        this.sections = sections;
        this.questions = questions;
    }

    public List<Entry> find(String module, String query, String skill, String level, String state) {
        if (!"exams".equals(module)) return List.of();
        String term = normalize(query);
        return exams.findByStatusOrderByUpdatedAtDesc("PUBLISHED").stream()
                .filter(exam -> !exam.isDemoOnly())
                .filter(
                        exam ->
                                normalize(
                                                exam.getTitle()
                                                        + " "
                                                        + Objects.toString(
                                                                exam.getDescription(), ""))
                                        .contains(term))
                .filter(
                        exam ->
                                normalize(skill).isBlank()
                                        || normalizeSkill(skill)
                                                .equals(normalizeSkill(exam.getSkill())))
                .filter(
                        exam ->
                                level == null
                                        || level.isBlank()
                                        || level.equalsIgnoreCase(exam.getTargetLevel()))
                .filter(exam -> state == null || state.isBlank() || state.equals(exam.getStatus()))
                .map(this::entry)
                .toList();
    }

    public Entry entry(String module, int id) {
        if (!"exams".equals(module)) throw new ResourceNotFoundException("Không tìm thấy nội dung");
        return entry(
                exams.findById(id)
                        .filter(exam -> "PUBLISHED".equals(exam.getStatus()) && !exam.isDemoOnly())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy đề thi đã công bố")));
    }

    private Entry entry(MockExamEntity exam) {
        List<Block> blocks = new ArrayList<>();
        var links = questions.findByMockExamIdOrderByPosition(exam.getId());
        var examSections = sections.findByMockExamIdOrderById(exam.getId());
        examSections.stream()
                .sorted(Comparator.comparingInt(section -> section.getDisplayOrder()))
                .forEach(
                        section ->
                                blocks.add(
                                        new Block(
                                                displaySkill(section.getSkill()),
                                                links.stream()
                                                                .filter(
                                                                        link ->
                                                                                section.getId()
                                                                                        .equals(
                                                                                                link
                                                                                                        .getMockExamSectionId()))
                                                                .count()
                                                        + " câu hỏi",
                                                section.getDurationMinutes() + " phút")));
        if (blocks.isEmpty())
            blocks.add(
                    new Block(
                            displaySkill(exam.getSkill()),
                            links.size() + " câu hỏi",
                            exam.getDurationMinutes() + " phút"));
        return new Entry(
                exam.getId(),
                exam.getTitle(),
                displaySkill(exam.getSkill()),
                exam.getTargetLevel(),
                examType(exam.getExamType()),
                "PUBLISHED",
                "Đã công bố",
                Objects.toString(exam.getDescription(), ""),
                List.of(
                        new Fact("Tổng thời gian", exam.getDurationMinutes() + " phút"),
                        new Fact("Số phần thi", String.valueOf(Math.max(1, examSections.size())))),
                blocks);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(Objects.toString(value, "").trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd');
    }

    private static String normalizeSkill(String value) {
        return Objects.toString(value, "")
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace(" & ", "_")
                .replace(' ', '_');
    }

    private static String displaySkill(String value) {
        return switch (normalizeSkill(value)) {
            case "GRAMMAR_VOCABULARY" -> "Grammar & Vocabulary";
            case "READING" -> "Reading";
            case "LISTENING" -> "Listening";
            case "WRITING" -> "Writing";
            case "SPEAKING" -> "Speaking";
            default -> "Tổng hợp";
        };
    }

    private static String examType(String value) {
        return switch (Objects.toString(value, "")) {
            case "PLACEMENT" -> "Kiểm tra đầu vào";
            case "CHECKPOINT" -> "Kiểm tra cuối chặng";
            case "FULL_MOCK" -> "Thi thử tổng hợp";
            default -> "Luyện tập";
        };
    }
}
