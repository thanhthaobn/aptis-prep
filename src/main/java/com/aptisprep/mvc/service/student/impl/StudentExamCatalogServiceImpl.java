package com.aptisprep.mvc.service.student.impl;

import com.aptisprep.mvc.dto.MockExamDTO;
import com.aptisprep.mvc.dto.MockExamSectionDTO;
import com.aptisprep.mvc.entity.MockExam;
import com.aptisprep.mvc.entity.MockExamQuestion;
import com.aptisprep.mvc.entity.MockExamSection;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.exception.ResourceNotFoundException;
import com.aptisprep.mvc.repository.MockExamQuestionRepository;
import com.aptisprep.mvc.repository.MockExamRepository;
import com.aptisprep.mvc.repository.MockExamSectionRepository;
import com.aptisprep.mvc.service.exam.ExamReadinessService;
import com.aptisprep.mvc.service.student.StudentExamCatalogService;
import java.text.Normalizer;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Published database content only. Missing data never falls back to fabricated exams. */
@Service
@Transactional(readOnly = true)
public class StudentExamCatalogServiceImpl implements StudentExamCatalogService {
    private List<String> examTypes =
            List.of("PRACTICE", "FULL_MOCK", "PLACEMENT");
    private List<String> sortOptions = List.of("level", "newest", "duration");
    private List<String> SKILL_ORDER =
            List.of(
                    "GRAMMAR_VOCABULARY",
                    "READING",
                    "LISTENING",
                    "WRITING",
                    "SPEAKING",
                    "FULL_MOCK");
    private List<String> LEVEL_ORDER = List.of("A1", "A2", "B1", "B2", "C1");

    private MockExamRepository mockExamRepository;
    private MockExamSectionRepository mockExamSectionRepository;
    private MockExamQuestionRepository mockExamQuestionRepository;
    private ExamReadinessService startValidator;

    @Autowired
    public StudentExamCatalogServiceImpl(
            MockExamRepository mockExamRepository,
            MockExamSectionRepository mockExamSectionRepository,
            MockExamQuestionRepository mockExamQuestionRepository,
            ExamReadinessService startValidator) {
        this.mockExamRepository = mockExamRepository;
        this.mockExamSectionRepository = mockExamSectionRepository;
        this.mockExamQuestionRepository = mockExamQuestionRepository;
        this.startValidator = startValidator;
    }

    /** Kept for lightweight preview tests that do not need readiness validation. */
    public StudentExamCatalogServiceImpl(
            MockExamRepository mockExamRepository,
            MockExamSectionRepository mockExamSectionRepository,
            MockExamQuestionRepository mockExamQuestionRepository) {
        this(mockExamRepository, mockExamSectionRepository, mockExamQuestionRepository, null);
    }

    @Override
    public List<MockExamDTO> find(
            String module, String query, String skill, String level, String state) {
        if (!"exams".equals(module)) return List.of();
        return filteredExams(query, skill, level, state).stream()
                .filter(exam -> !"CHECKPOINT".equals(exam.getExamType()))
                .map(streamValue -> entry(streamValue))
                .toList();
    }

    public Page<MockExamDTO> browse(
            String query,
            String skill,
            String level,
            String state,
            String examType,
            String sort,
            Pageable pageable) {
        List<MockExam> matches = filteredExams(query, skill, level, state);
        String selectedType = Objects.toString(examType, "");
        List<MockExam> selected =
                matches.stream()
                        .filter(
                                exam ->
                                        selectedType.isBlank()
                                                || selectedType.equals(exam.getExamType()))
                        .filter(exam -> !"CHECKPOINT".equals(exam.getExamType()))
                        .sorted(catalogOrder(sort))
                        .toList();
        int size = pageable.getPageSize();
        int lastPage = selected.isEmpty() ? 0 : (selected.size() - 1) / size;
        int page = Math.min(pageable.getPageNumber(), lastPage);
        int from = page * size;
        List<MockExamDTO> entries =
                selected.subList(from, Math.min(from + size, selected.size())).stream()
                        .map(streamValue -> entry(streamValue))
                        .toList();
        return new PageImpl<>(entries, PageRequest.of(page, size), selected.size());
    }

    @Override
    public Map<String, Long> categoryCounts(
            String query, String skill, String level, String state) {
        List<MockExam> matches = filteredExams(query, skill, level, state);
        LinkedHashMap<String, Long> counts = new LinkedHashMap<>();
        for (String type : examTypes()) {
            counts.put(
                    type,
                    matches.stream().filter(exam -> type.equals(exam.getExamType())).count());
        }
        return counts;
    }

    @Override
    public String categoryLabel(String type) {
        return switch (type) {
            case "FULL_MOCK" -> "Thi thử tổng hợp";
            case "PLACEMENT" -> "Kiểm tra đầu vào";
            case "CHECKPOINT" -> "Kiểm tra cuối chặng";
            default -> "Luyện từng kỹ năng";
        };
    }

    @Override
    public List<String> examTypes() {
        return examTypes;
    }

    @Override
    public List<String> sortOptions() {
        return sortOptions;
    }

    private Comparator<MockExam> catalogOrder(String sort) {
        Comparator<MockExam> recent =
                Comparator.comparing(
                        streamValue -> streamValue.getUpdatedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder()));
        Comparator<MockExam> selected =
                switch (Objects.toString(sort, "")) {
                    case "newest" -> recent;
                    case "duration" ->
                            Comparator.comparing(
                                    streamValue -> streamValue.getDurationMinutes(),
                                    Comparator.nullsLast(Comparator.naturalOrder()));
                    default ->
                            Comparator.comparingInt(
                                    exam -> rank(LEVEL_ORDER, exam.getTargetLevel()));
                };
        return Comparator.comparingInt(
                        (MockExam exam) -> rank(SKILL_ORDER, normalizeSkill(exam.getSkill())))
                .thenComparing(selected)
                .thenComparing(recent)
                .thenComparing(streamValue -> streamValue.getId(), Comparator.reverseOrder());
    }

    private int rank(List<String> order, String value) {
        if (value == null) return Integer.MAX_VALUE;
        int index = order.indexOf(value);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private List<MockExam> filteredExams(
            String query, String skill, String level, String state) {
        String term = normalize(query);
        return mockExamRepository.findByStatusOrderByUpdatedAtDesc("PUBLISHED").stream()
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
                .filter(streamValue -> eligibleForCatalog(streamValue))
                .sorted(
                        Comparator.comparing(
                                        (MockExam streamValue) -> streamValue.getUpdatedAt(),
                                        Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing((MockExam streamValue) -> streamValue.getId(), Comparator.reverseOrder()))
                .toList();
    }

    private boolean eligibleForCatalog(MockExam exam) {
        if (startValidator == null) return true;
        try {
            startValidator.validate(
                    exam, mockExamQuestionRepository.findByMockExamIdOrderByPosition(exam.getId()));
            return true;
        } catch (BusinessRuleException exception) {
            return false;
        }
    }

    private boolean matchesSkill(MockExam exam, String skill) {
        if (normalize(skill).isBlank()) return true;
        String code = normalizeSkill(skill);
        if ("FULL_MOCK".equals(code) && "FULL_MOCK".equals(exam.getExamType())) return true;
        if (code.equals(normalizeSkill(exam.getSkill()))) return true;
        return "FULL_MOCK".equals(exam.getSkill())
                && mockExamSectionRepository.findByMockExamIdOrderById(exam.getId()).stream()
                        .anyMatch(section -> code.equals(normalizeSkill(section.getSkill())));
    }

    @Override
    public MockExamDTO entry(String module, int id) {
        if (!"exams".equals(module)) throw new ResourceNotFoundException("Không tìm thấy nội dung");
        return entry(
                mockExamRepository
                        .findById(id)
                        .filter(exam -> "PUBLISHED".equals(exam.getStatus()) && !exam.isDemoOnly())
                        .filter(streamValue -> eligibleForCatalog(streamValue))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy đề thi đã công bố")));
    }

    private MockExamDTO entry(MockExam exam) {
        List<MockExamQuestion> links =
                mockExamQuestionRepository.findByMockExamIdOrderByPosition(exam.getId());
        List<MockExamSection> examSections =
                mockExamSectionRepository.findByMockExamIdOrderById(exam.getId());
        List<MockExamSectionDTO> sections =
                examSections.stream()
                        .sorted(Comparator.comparingInt(section -> section.getDisplayOrder()))
                        .map(
                                section -> {
                                    MockExamSectionDTO sectionDTO =
                                            MockExamSectionDTO.convertEntityToDto(section);
                                    int questionCount =
                                            (int)
                                                    links.stream()
                                                            .filter(
                                                                    link ->
                                                                            section.getId()
                                                                                    .equals(link.getMockExamSectionId()))
                                                            .count();
                                    sectionDTO.setQuestionCount(questionCount);
                                    sectionDTO.setSkillLabel(displaySkill(section.getSkill()));
                                    return sectionDTO;
                                })
                        .toList();

        Readiness readiness =
                startValidator == null
                        ? new Readiness(true, "Đề đã sẵn sàng để làm bài.", List.of())
                        : checkReadiness(exam, links);
        MockExamDTO result = MockExamDTO.convertEntityToDto(exam);
        result.setSections(sections);
        result.setSkillLabel(displaySkill(exam.getSkill()));
        result.setKindLabel(examType(exam.getExamType()));
        result.setDurationLabel(
                exam.getDurationMinutes() == null
                        ? "Chưa có"
                        : exam.getDurationMinutes() + " phút");
        result.setSectionCount(Math.max(1, examSections.size()));
        result.setQuestionCount(links.size());
        result.setStartable(readiness.startable());
        result.setReadinessMessage(readiness.message());
        result.setReadinessChecks(readiness.checks());
        return result;
    }

    private Readiness checkReadiness(MockExam exam, List<MockExamQuestion> links) {
        List<String> checks = new ArrayList<>();
        try {
            startValidator.validate(exam, links);
        } catch (BusinessRuleException exception) {
            return blocked(
                    exception.getMessage(),
                    checks,
                    "Đề phải có nội dung, thời lượng và bộ điểm hợp lệ");
        }
        checks.add("Đề đã công bố; câu hỏi, section, thời lượng và audio hợp lệ");
        checks.add(
                exam.getScoringSchemeId() == null
                        ? "Chấm điểm raw, không quy đổi CEFR"
                        : "Bộ chấm điểm đã phủ đủ miền điểm CEFR");
        if ("CHECKPOINT".equals(exam.getExamType()))
            return blocked(
                    "Checkpoint chỉ được bắt đầu từ lộ trình học.",
                    checks,
                    "Được mở từ đúng lộ trình học");
        if ("PLACEMENT".equals(exam.getExamType())) checks.add("Bắt đầu từ trang kiểm tra đầu vào");
        return new Readiness(
                true, "Đề đã sẵn sàng. Bạn có thể bắt đầu làm bài.", List.copyOf(checks));
    }

    private Readiness blocked(String message, List<String> checks, String missing) {
        List<String> values = new ArrayList<>(checks);
        values.add(missing);
        return new Readiness(false, message, List.copyOf(values));
    }

    private record Readiness(boolean startable, String message, List<String> checks) {}

    private String normalize(String value) {
        return Normalizer.normalize(Objects.toString(value, "").trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd');
    }

    private String normalizeSkill(String value) {
        String code =
                Objects.toString(value, "")
                        .trim()
                        .toUpperCase(Locale.ROOT)
                        .replace(" & ", "_")
                        .replace(' ', '_');
        return "TỔNG_HỢP".equals(code) ? "FULL_MOCK" : code;
    }

    private String displaySkill(String value) {
        return switch (normalizeSkill(value)) {
            case "GRAMMAR_VOCABULARY" -> "Grammar & Vocabulary";
            case "READING" -> "Reading";
            case "LISTENING" -> "Listening";
            case "WRITING" -> "Writing";
            case "SPEAKING" -> "Speaking";
            default -> "Tổng hợp";
        };
    }

    private String examType(String value) {
        return switch (Objects.toString(value, "")) {
            case "PLACEMENT" -> "Kiểm tra đầu vào";
            case "CHECKPOINT" -> "Kiểm tra cuối chặng";
            case "FULL_MOCK" -> "Thi thử tổng hợp";
            default -> "Luyện tập";
        };
    }
}







