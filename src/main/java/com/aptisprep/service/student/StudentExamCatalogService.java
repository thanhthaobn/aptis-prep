package com.aptisprep.service.student;

import com.aptisprep.dto.view.StudentCatalogView.*;
import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.entity.MockExamQuestionEntity;
import com.aptisprep.entity.MockExamSectionEntity;
import com.aptisprep.entity.QuestionEntity;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.repository.MockExamSectionRepository;
import com.aptisprep.repository.MockExamQuestionRepository;
import com.aptisprep.repository.QuestionRepository;
import com.aptisprep.service.contract.ScoringPolicyContract;
import java.text.Normalizer;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Published database content only. Missing data never falls back to fabricated exams. */
@Service
@Transactional(readOnly = true)
public class StudentExamCatalogService {
    private static final Set<String> APTIS_SKILLS = Set.of(
            "GRAMMAR_VOCABULARY", "READING", "LISTENING", "WRITING", "SPEAKING");
    private final MockExamRepository exams;
    private final MockExamSectionRepository sections;
    private final MockExamQuestionRepository questions;
    private final QuestionRepository questionCatalog;
    private final ScoringPolicyContract scoring;

    public StudentExamCatalogService(MockExamRepository exams, MockExamSectionRepository sections,
            MockExamQuestionRepository questions, QuestionRepository questionCatalog,
            ScoringPolicyContract scoring) {
        this.exams = exams; this.sections = sections; this.questions = questions;
        this.questionCatalog = questionCatalog; this.scoring = scoring;
    }

    public List<Entry> find(String module, String query, String skill, String level, String state) {
        if (!"exams".equals(module)) return List.of();
        String term = normalize(query);
        return exams.findByStatusOrderByUpdatedAtDesc("PUBLISHED").stream()
                .filter(exam -> normalize(exam.getTitle() + " " + Objects.toString(exam.getDescription(), "")).contains(term))
                .filter(exam -> normalize(skill).isBlank() || normalizeSkill(skill).equals(normalizeSkill(exam.getSkill())))
                .filter(exam -> level == null || level.isBlank() || level.equalsIgnoreCase(exam.getTargetLevel()))
                .filter(exam -> state == null || state.isBlank() || state.equals(exam.getStatus()))
                .map(this::entry).toList();
    }

    public Entry entry(String module, int id) {
        if (!"exams".equals(module)) throw new ResourceNotFoundException("Không tìm thấy nội dung");
        return entry(exams.findById(id).filter(exam -> "PUBLISHED".equals(exam.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi đã công bố")));
    }

    private Entry entry(MockExamEntity exam) {
        List<Block> blocks = new ArrayList<>();
        var links = questions.findByMockExamIdOrderByPosition(exam.getId());
        var examSections = sections.findByMockExamIdOrderById(exam.getId());
        examSections.stream().sorted(Comparator.comparingInt(section -> section.getDisplayOrder())).forEach(section ->
                blocks.add(new Block(displaySkill(section.getSkill()),
                        links.stream().filter(link -> section.getId().equals(link.getMockExamSectionId())).count() + " câu hỏi",
                        section.getDurationMinutes() + " phút")));
        if (blocks.isEmpty()) blocks.add(new Block(displaySkill(exam.getSkill()), links.size() + " câu hỏi", exam.getDurationMinutes() + " phút"));
        Readiness readiness = checkReadiness(exam, links, examSections);
        return new Entry(exam.getId(), exam.getTitle(), displaySkill(exam.getSkill()), exam.getTargetLevel(),
                examType(exam.getExamType()), "PUBLISHED", "Đã công bố", Objects.toString(exam.getDescription(), ""),
                List.of(new Fact("Tổng thời gian", exam.getDurationMinutes() + " phút"),
                        new Fact("Số phần thi", String.valueOf(Math.max(1, examSections.size()))),
                        new Fact("Số câu hỏi", String.valueOf(links.size()))), blocks,
                readiness.startable(), readiness.message(), readiness.checks());
    }

    private Readiness checkReadiness(MockExamEntity exam, List<MockExamQuestionEntity> links,
            List<MockExamSectionEntity> examSections) {
        List<String> checks = new ArrayList<>();
        checks.add("Đề đã được công bố");
        if (links.isEmpty()) return blocked("Đề chưa có câu hỏi để làm.", checks, "Có ít nhất một câu hỏi đã công bố");

        Map<Integer, QuestionEntity> questionById = new LinkedHashMap<>();
        for (MockExamQuestionEntity link : links) {
            QuestionEntity question = questionCatalog.findById(link.getQuestionId()).orElse(null);
            if (question == null || !"PUBLISHED".equals(question.getStatus())) {
                return blocked("Đề đang chứa câu hỏi chưa được công bố.", checks, "Mọi câu hỏi trong đề đều đã công bố");
            }
            if (questionById.put(link.getQuestionId(), question) != null) {
                return blocked("Đề đang chứa câu hỏi bị lặp.", checks, "Không có câu hỏi trùng trong đề");
            }
        }
        checks.add("Mọi câu hỏi trong đề đều đã công bố");

        Map<Integer, String> sectionSkills = new HashMap<>();
        for (MockExamSectionEntity section : examSections) sectionSkills.put(section.getId(), section.getSkill());
        for (MockExamQuestionEntity link : links) {
            String sectionSkill = link.getMockExamSectionId() == null ? null : sectionSkills.get(link.getMockExamSectionId());
            QuestionEntity question = questionById.get(link.getQuestionId());
            if (sectionSkill != null && !normalizeSkill(sectionSkill).equals(normalizeSkill(question.getSkill()))) {
                return blocked("Một câu hỏi không thuộc đúng kỹ năng của section.", checks, "Câu hỏi khớp với kỹ năng section");
            }
        }
        checks.add("Câu hỏi khớp với kỹ năng section");

        Set<String> skills = questionById.values().stream().map(QuestionEntity::getSkill)
                .map(StudentExamCatalogService::normalizeSkill)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if ("FULL_MOCK".equals(exam.getExamType())) {
            if (!skills.containsAll(APTIS_SKILLS)) {
                return blocked("Thi thử tổng hợp phải có đủ 5 kỹ năng Aptis.", checks, "Đủ Grammar, Reading, Listening, Writing và Speaking");
            }
            checks.add("Đủ 5 kỹ năng Aptis");
        } else if (skills.size() != 1) {
            return blocked("Đề theo kỹ năng đang chứa nhiều kỹ năng khác nhau.", checks, "Đề theo kỹ năng chỉ có một kỹ năng");
        } else {
            checks.add("Đề theo kỹ năng chỉ có một kỹ năng");
        }

        if (exam.getScoringSchemeId() != null) {
            try {
                for (String skill : skills) scoring.validatePolicy(exam.getScoringSchemeId(), skill);
            } catch (BusinessRuleException exception) {
                return blocked(exception.getMessage(), checks, "Bộ chấm điểm đã phủ đủ miền điểm CEFR");
            }
            checks.add("Bộ chấm điểm đã phủ đủ miền điểm CEFR");
        } else if ("CHECKPOINT".equals(exam.getExamType())) {
            return blocked("Checkpoint chưa có bộ chấm điểm để xác nhận CEFR.", checks, "Checkpoint có bộ chấm điểm hợp lệ");
        } else {
            checks.add("Chấm điểm raw, không quy đổi CEFR");
        }

        if ("CHECKPOINT".equals(exam.getExamType())) {
            return blocked("Checkpoint chỉ được bắt đầu từ lộ trình học.", checks, "Được mở từ đúng lộ trình học");
        }
        return new Readiness(true, "Đề đã sẵn sàng. Bạn có thể bắt đầu làm bài.", List.copyOf(checks));
    }

    private Readiness blocked(String message, List<String> checks, String missing) {
        List<String> values = new ArrayList<>(checks);
        values.add(missing);
        return new Readiness(false, message, List.copyOf(values));
    }

    private record Readiness(boolean startable, String message, List<String> checks) {}

    private static String normalize(String value) {
        return Normalizer.normalize(Objects.toString(value, "").trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replace('đ', 'd');
    }
    private static String normalizeSkill(String value) {
        return Objects.toString(value, "").trim().toUpperCase(Locale.ROOT).replace(" & ", "_").replace(' ', '_');
    }
    private static String displaySkill(String value) {
        return switch (normalizeSkill(value)) {
            case "GRAMMAR_VOCABULARY" -> "Grammar & Vocabulary";
            case "READING" -> "Reading"; case "LISTENING" -> "Listening";
            case "WRITING" -> "Writing"; case "SPEAKING" -> "Speaking";
            default -> "Tổng hợp";
        };
    }
    private static String examType(String value) {
        return switch (Objects.toString(value, "")) {
            case "PLACEMENT" -> "Kiểm tra đầu vào"; case "CHECKPOINT" -> "Kiểm tra cuối chặng";
            case "FULL_MOCK" -> "Thi thử tổng hợp"; default -> "Luyện tập";
        };
    }
}
