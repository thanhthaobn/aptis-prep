package com.aptisprep.service.student;

import com.aptisprep.dto.view.StudentCatalogView.*;
import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.entity.MockExamQuestionEntity;
import com.aptisprep.entity.MockExamSectionEntity;
import com.aptisprep.entity.QuestionEntity;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.MockExamQuestionRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.repository.MockExamSectionRepository;
import com.aptisprep.repository.QuestionRepository;
import com.aptisprep.service.contract.ScoringPolicyContract;
import java.text.Normalizer;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Published database content only. Missing data never falls back to fabricated exams. */
@Service
@Transactional(readOnly = true)
public class StudentExamCatalogService {
    public static final List<String> EXAM_TYPES =
            List.of("PRACTICE", "FULL_MOCK", "PLACEMENT", "CHECKPOINT");
    public static final List<String> SORT_OPTIONS = List.of("level", "newest", "duration");
    private static final Set<String> APTIS_SKILLS =
            Set.of("GRAMMAR_VOCABULARY", "READING", "LISTENING", "WRITING", "SPEAKING");
    private static final List<String> SKILL_ORDER =
            List.of(
                    "GRAMMAR_VOCABULARY",
                    "READING",
                    "LISTENING",
                    "WRITING",
                    "SPEAKING",
                    "FULL_MOCK");
    private static final List<String> LEVEL_ORDER = List.of("A1", "A2", "B1", "B2", "C1");

    public record Category(String code, String label, long count) {}

    public record Catalog(Page<Entry> page, List<Category> categories) {}

    private final MockExamRepository exams;
    private final MockExamSectionRepository sections;
    private final MockExamQuestionRepository questions;
    private final QuestionRepository questionCatalog;
    private final ScoringPolicyContract scoring;

    @Autowired
    public StudentExamCatalogService(
            MockExamRepository exams,
            MockExamSectionRepository sections,
            MockExamQuestionRepository questions,
            QuestionRepository questionCatalog,
            ScoringPolicyContract scoring) {
        this.exams = exams;
        this.sections = sections;
        this.questions = questions;
        this.questionCatalog = questionCatalog;
        this.scoring = scoring;
    }

    /** Kept for lightweight preview tests that do not need readiness validation. */
    public StudentExamCatalogService(
            MockExamRepository exams,
            MockExamSectionRepository sections,
            MockExamQuestionRepository questions) {
        this(exams, sections, questions, null, null);
    }

    public List<Entry> find(String module, String query, String skill, String level, String state) {
        if (!"exams".equals(module)) return List.of();
        return filteredExams(query, skill, level, state).stream().map(this::entry).toList();
    }

    public Catalog browse(
            String query,
            String skill,
            String level,
            String state,
            String examType,
            String sort,
            Pageable pageable) {
        var matches = filteredExams(query, skill, level, state);
        var categories =
                EXAM_TYPES.stream()
                        .map(
                                type ->
                                        new Category(
                                                type,
                                                categoryLabel(type),
                                                matches.stream()
                                                        .filter(
                                                                exam ->
                                                                        type.equals(
                                                                                exam.getExamType()))
                                                        .count()))
                        .toList();
        String selectedType = Objects.toString(examType, "");
        var selected =
                matches.stream()
                        .filter(
                                exam ->
                                        selectedType.isBlank()
                                                || selectedType.equals(exam.getExamType()))
                        .sorted(catalogOrder(sort))
                        .toList();
        int size = pageable.getPageSize();
        int lastPage = selected.isEmpty() ? 0 : (selected.size() - 1) / size;
        int page = Math.min(pageable.getPageNumber(), lastPage);
        int from = page * size;
        var entries =
                selected.subList(from, Math.min(from + size, selected.size())).stream()
                        .map(this::entry)
                        .toList();
        return new Catalog(
                new PageImpl<>(entries, PageRequest.of(page, size), selected.size()), categories);
    }

    public static String categoryLabel(String type) {
        return switch (type) {
            case "FULL_MOCK" -> "Thi thử tổng hợp";
            case "PLACEMENT" -> "Kiểm tra đầu vào";
            case "CHECKPOINT" -> "Kiểm tra cuối chặng";
            default -> "Luyện từng kỹ năng";
        };
    }

    private static Comparator<MockExamEntity> catalogOrder(String sort) {
        Comparator<MockExamEntity> recent =
                Comparator.comparing(
                        MockExamEntity::getUpdatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()));
        Comparator<MockExamEntity> selected =
                switch (Objects.toString(sort, "")) {
                    case "newest" -> recent;
                    case "duration" ->
                            Comparator.comparing(
                                    MockExamEntity::getDurationMinutes,
                                    Comparator.nullsLast(Comparator.naturalOrder()));
                    default ->
                            Comparator.comparingInt(
                                    exam -> rank(LEVEL_ORDER, exam.getTargetLevel()));
                };
        return Comparator.comparingInt(
                        (MockExamEntity exam) -> rank(SKILL_ORDER, normalizeSkill(exam.getSkill())))
                .thenComparing(selected)
                .thenComparing(recent)
                .thenComparing(MockExamEntity::getId, Comparator.reverseOrder());
    }

    private static int rank(List<String> order, String value) {
        if (value == null) return Integer.MAX_VALUE;
        int index = order.indexOf(value);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private List<MockExamEntity> filteredExams(
            String query, String skill, String level, String state) {
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
                .filter(exam -> matchesSkill(exam, skill))
                .filter(
                        exam ->
                                level == null
                                        || level.isBlank()
                                        || level.equalsIgnoreCase(exam.getTargetLevel()))
                .filter(exam -> state == null || state.isBlank() || state.equals(exam.getStatus()))
                .sorted(
                        Comparator.comparing(
                                        MockExamEntity::getUpdatedAt,
                                        Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(MockExamEntity::getId, Comparator.reverseOrder()))
                .toList();
    }

    private boolean matchesSkill(MockExamEntity exam, String skill) {
        if (normalize(skill).isBlank()) return true;
        String code = normalizeSkill(skill);
        if ("FULL_MOCK".equals(code) && "FULL_MOCK".equals(exam.getExamType())) return true;
        if (code.equals(normalizeSkill(exam.getSkill()))) return true;
        return "FULL_MOCK".equals(exam.getExamType())
                && sections.findByMockExamIdOrderById(exam.getId()).stream()
                        .anyMatch(section -> code.equals(normalizeSkill(section.getSkill())));
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
                            exam.getDurationMinutes() == null
                                    ? "Chưa có"
                                    : exam.getDurationMinutes() + " phút"));

        Readiness readiness =
                questionCatalog == null
                        ? new Readiness(true, "Đề đã sẵn sàng để làm bài.", List.of())
                        : checkReadiness(exam, links, examSections);
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
                        new Fact(
                                "Tổng thời gian",
                                exam.getDurationMinutes() == null
                                        ? "Chưa có"
                                        : exam.getDurationMinutes() + " phút"),
                        new Fact("Số phần thi", String.valueOf(Math.max(1, examSections.size()))),
                        new Fact("Số câu hỏi", String.valueOf(links.size()))),
                blocks,
                exam.getExamType(),
                normalizeSkill(exam.getSkill()),
                readiness.startable(),
                readiness.message(),
                readiness.checks());
    }

    private Readiness checkReadiness(
            MockExamEntity exam,
            List<MockExamQuestionEntity> links,
            List<MockExamSectionEntity> examSections) {
        List<String> checks = new ArrayList<>();
        checks.add("Đề đã được công bố");
        if (links.isEmpty())
            return blocked(
                    "Đề chưa có câu hỏi để làm.", checks, "Có ít nhất một câu hỏi đã công bố");

        Map<Integer, QuestionEntity> questionById = new LinkedHashMap<>();
        for (MockExamQuestionEntity link : links) {
            QuestionEntity question = questionCatalog.findById(link.getQuestionId()).orElse(null);
            if (question == null || !"PUBLISHED".equals(question.getStatus()))
                return blocked(
                        "Đề đang chứa câu hỏi chưa được công bố.",
                        checks,
                        "Mọi câu hỏi trong đề đều đã công bố");
            if (questionById.put(link.getQuestionId(), question) != null)
                return blocked(
                        "Đề đang chứa câu hỏi bị lặp.", checks, "Không có câu hỏi trùng trong đề");
        }
        checks.add("Mọi câu hỏi trong đề đều đã công bố");

        Map<Integer, String> sectionSkills = new HashMap<>();
        for (MockExamSectionEntity section : examSections)
            sectionSkills.put(section.getId(), section.getSkill());
        for (MockExamQuestionEntity link : links) {
            String sectionSkill =
                    link.getMockExamSectionId() == null
                            ? null
                            : sectionSkills.get(link.getMockExamSectionId());
            QuestionEntity question = questionById.get(link.getQuestionId());
            if (sectionSkill != null
                    && !normalizeSkill(sectionSkill).equals(normalizeSkill(question.getSkill())))
                return blocked(
                        "Một câu hỏi không thuộc đúng kỹ năng của section.",
                        checks,
                        "Câu hỏi khớp với kỹ năng section");
        }
        checks.add("Câu hỏi khớp với kỹ năng section");

        Set<String> skills =
                questionById.values().stream()
                        .map(QuestionEntity::getSkill)
                        .map(StudentExamCatalogService::normalizeSkill)
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if ("FULL_MOCK".equals(exam.getExamType())) {
            if (!skills.containsAll(APTIS_SKILLS))
                return blocked(
                        "Thi thử tổng hợp phải có đủ 5 kỹ năng Aptis.",
                        checks,
                        "Đủ Grammar, Reading, Listening, Writing và Speaking");
            checks.add("Đủ 5 kỹ năng Aptis");
        } else if (skills.size() != 1) {
            return blocked(
                    "Đề theo kỹ năng đang chứa nhiều kỹ năng khác nhau.",
                    checks,
                    "Đề theo kỹ năng chỉ có một kỹ năng");
        } else {
            checks.add("Đề theo kỹ năng chỉ có một kỹ năng");
        }

        if (exam.getScoringSchemeId() != null) {
            try {
                for (String skill : skills)
                    scoring.validatePolicy(exam.getScoringSchemeId(), skill);
            } catch (BusinessRuleException exception) {
                return blocked(
                        exception.getMessage(), checks, "Bộ chấm điểm đã phủ đủ miền điểm CEFR");
            }
            checks.add("Bộ chấm điểm đã phủ đủ miền điểm CEFR");
        } else if ("CHECKPOINT".equals(exam.getExamType())) {
            return blocked(
                    "Checkpoint chưa có bộ chấm điểm để xác nhận CEFR.",
                    checks,
                    "Checkpoint có bộ chấm điểm hợp lệ");
        } else {
            checks.add("Chấm điểm raw, không quy đổi CEFR");
        }

        if ("CHECKPOINT".equals(exam.getExamType()))
            return blocked(
                    "Checkpoint chỉ được bắt đầu từ lộ trình học.",
                    checks,
                    "Được mở từ đúng lộ trình học");
        return new Readiness(
                true, "Đề đã sẵn sàng. Bạn có thể bắt đầu làm bài.", List.copyOf(checks));
    }

    private Readiness blocked(String message, List<String> checks, String missing) {
        List<String> values = new ArrayList<>(checks);
        values.add(missing);
        return new Readiness(false, message, List.copyOf(values));
    }

    private record Readiness(boolean startable, String message, List<String> checks) {}

    private static String normalize(String value) {
        return Normalizer.normalize(Objects.toString(value, "").trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd');
    }

    private static String normalizeSkill(String value) {
        String code =
                Objects.toString(value, "")
                        .trim()
                        .toUpperCase(Locale.ROOT)
                        .replace(" & ", "_")
                        .replace(' ', '_');
        return "TỔNG_HỢP".equals(code) ? "FULL_MOCK" : code;
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
