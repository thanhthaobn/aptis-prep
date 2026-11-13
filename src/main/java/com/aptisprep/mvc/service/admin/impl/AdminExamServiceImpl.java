package com.aptisprep.mvc.service.admin.impl;

import com.aptisprep.mvc.dto.MockExamDTO;
import com.aptisprep.mvc.dto.MockExamSectionDTO;
import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.RecordStatus;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.exception.ResourceNotFoundException;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.service.admin.AdminExamService;
import com.aptisprep.mvc.service.exam.ExamReadinessService;
import com.aptisprep.mvc.service.exam.ExamSnapshotService;
import com.aptisprep.mvc.service.scoring.CertificateSchemeService;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Exam authoring only; catalog facade retains its public controller contract. */
@Service
public class AdminExamServiceImpl implements AdminExamService {
    private List<String> SKILLS = com.aptisprep.mvc.enums.Skill.codes();
    private List<String> LEVELS = com.aptisprep.mvc.enums.CefrLevel.codes();
    private MockExamRepository mockExamRepository;
    private MockExamSectionRepository mockExamSectionRepository;
    private MockExamQuestionRepository mockExamQuestionRepository;
    private CertificateRepository certificateRepository;
    private ScoringSchemeRepository scoringSchemeRepository;
    private QuestionRepository questionRepository;
    private ExamAttemptRepository examAttemptRepository;
    private CertificateSchemeService certificateSchemes;
    private ExamSnapshotService examSnapshots;
    private ExamReadinessService readiness;

    public AdminExamServiceImpl(
            MockExamRepository mockExamRepository,
            MockExamSectionRepository mockExamSectionRepository,
            MockExamQuestionRepository mockExamQuestionRepository,
            CertificateRepository certificateRepository,
            ScoringSchemeRepository scoringSchemeRepository,
            QuestionRepository questionRepository,
            ExamAttemptRepository examAttemptRepository,
            CertificateSchemeService certificateSchemes,
            ExamSnapshotService examSnapshots,
            ExamReadinessService readiness) {
        this.mockExamRepository = mockExamRepository;
        this.mockExamSectionRepository = mockExamSectionRepository;
        this.mockExamQuestionRepository = mockExamQuestionRepository;
        this.certificateRepository = certificateRepository;
        this.scoringSchemeRepository = scoringSchemeRepository;
        this.questionRepository = questionRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.certificateSchemes = certificateSchemes;
        this.examSnapshots = examSnapshots;
        this.readiness = readiness;
    }

    public MockExamDTO examForm(Integer id) {
        MockExamDTO form = new MockExamDTO();
        Certificate activeCertificate =
                certificateRepository
                        .findByCodeIgnoreCase("APTIS_GENERAL")
                        .filter(certificate -> certificate.getStatus() == RecordStatus.ACTIVE)
                        .orElse(null);
        if (activeCertificate != null) form.setCertificateId(activeCertificate.getId());
        if (id == null) {
            MockExamSectionDTO section = new MockExamSectionDTO();
            section.setSkill(form.getSectionSkill());
            section.setDurationMinutes(form.getSectionDurationMinutes());
            section.setQuestionIds("");
            form.setSections(new ArrayList<>(List.of(section)));
            return form;
        }
        MockExam exam =
                mockExamRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
        form = MockExamDTO.convertEntityToDto(exam);
        form.setSectionSkill(exam.getSkill());
        form.setQuestionIds(
                mockExamQuestionRepository.findByMockExamIdOrderByPosition(id).stream()
                        .map(x -> "" + x.getQuestionId())
                        .collect(Collectors.joining(",")));
        List<MockExamSectionDTO> sectionForms = new ArrayList<>();
        for (MockExamSection section : mockExamSectionRepository.findByMockExamIdOrderById(id)) {
            MockExamSectionDTO sectionForm = MockExamSectionDTO.convertEntityToDto(section);
            sectionForm.setQuestionIds(
                    mockExamQuestionRepository.findByMockExamIdOrderByPosition(id).stream()
                            .filter(
                                    link ->
                                            Objects.equals(
                                                    link.getMockExamSectionId(), section.getId()))
                            .map(link -> String.valueOf(link.getQuestionId()))
                            .collect(Collectors.joining(",")));
            sectionForms.add(sectionForm);
        }
        if (!sectionForms.isEmpty()) {
            form.setSections(sectionForms);
            form.setSectionSkill(sectionForms.get(0).getSkill());
            form.setSectionDurationMinutes(sectionForms.get(0).getDurationMinutes());
        } else if (exam.getDurationMinutes() != null) {
            form.setSectionDurationMinutes(exam.getDurationMinutes());
        }
        return form;
    }

    @Transactional
    public Integer saveExam(Integer id, MockExamDTO form) {
        boolean explicitSections = form.getSections() != null && !form.getSections().isEmpty();
        normalizeSections(form);
        if (id == null
                && "PLACEMENT".equals(form.getExamType())
                && form.getSections().stream()
                                .map(streamValue -> streamValue.getSkill())
                                .distinct()
                                .count()
                        != 5)
            throw new BusinessRuleException(
                    "Đề đầu vào mới cần đủ 4 kỹ năng và Grammar & Vocabulary");
        validateExamDefinition(form);
        MockExam exam =
                id == null
                        ? new MockExam()
                        : mockExamRepository
                                .findByIdForUpdate(id)
                                .orElseThrow(
                                        () ->
                                                new ResourceNotFoundException(
                                                        "Không tìm thấy đề thi"));
        if ("PUBLISHED".equals(exam.getStatus())) {
            throw new BusinessRuleException("Hãy ngừng công bố trước khi chỉnh sửa đề");
        }
        if (id != null) {
            var legacyAttempts = examAttemptRepository.findWithoutSnapshot(id);
            if (!legacyAttempts.isEmpty()) {
                var links = mockExamQuestionRepository.findByMockExamIdOrderByPosition(id);
                for (var attempt : legacyAttempts) {
                    examSnapshots.preserveLegacyAttempt(attempt, exam, links);
                }
            }
        }
        exam = applyExamFields(exam, form);
        exam = mockExamRepository.save(exam);
        replaceExamSections(exam.getId(), form, explicitSections, id != null);
        return exam.getId();
    }

    @Transactional
    public void publishExam(Integer id) {
        MockExam exam =
                mockExamRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
        MockExamDTO form = examForm(id);
        normalizeSections(form);
        validateExamDefinition(form);
        if ("FULL_MOCK".equals(form.getExamType()) && form.getScoringSchemeId() == null)
            throw new BusinessRuleException("Đề tổng hợp cần bộ chấm điểm trước khi công bố");
        Set<Integer> all = new HashSet<>();
        for (MockExamSectionDTO section : form.getSections()) {
            List<Integer> ids = parseIds(section.getQuestionIds());
            if (ids.isEmpty()) throw new BusinessRuleException("Không thể công bố section trống");
            validateQuestionSelection(ids, section.getSkill());
            if (ids.stream().anyMatch(questionId -> !all.add(questionId)))
                throw new BusinessRuleException("Một câu hỏi không được lặp giữa các section");
        }
        if (all.isEmpty()) throw new BusinessRuleException("Không thể công bố đề trống");
        exam.setDurationMinutes(form.getDurationMinutes());
        readiness.validateForPublication(
                exam, mockExamQuestionRepository.findByMockExamIdOrderByPosition(id));
        archivePublishedExamConflict(exam);
        exam.setStatus("PUBLISHED");
        mockExamRepository.save(exam);
    }

    /**
     * Chỉ giữ một đề Placement công bố tại một thời điểm. Với Checkpoint, mỗi cặp kỹ năng và cấp
     * độ mục tiêu cũng chỉ có một đề đang công bố để lộ trình luôn trỏ tới đúng đề.
     */
    private void archivePublishedExamConflict(MockExam exam) {
        if (!Set.of("PLACEMENT", "CHECKPOINT").contains(exam.getExamType())) return;
        List<MockExam> published = mockExamRepository.findByStatusOrderByUpdatedAtDesc("PUBLISHED");
        List<MockExam> conflicts =
                published.stream()
                        .filter(value -> !Objects.equals(value.getId(), exam.getId()))
                        .filter(value -> Objects.equals(value.getExamType(), exam.getExamType()))
                        .filter(
                                value ->
                                        "PLACEMENT".equals(exam.getExamType())
                                                || (Objects.equals(
                                                                value.getSkill(), exam.getSkill())
                                                        && Objects.equals(
                                                                value.getTargetLevel(),
                                                                exam.getTargetLevel())))
                        .toList();
        conflicts.forEach(value -> value.setStatus("ARCHIVED"));
        if (!conflicts.isEmpty()) mockExamRepository.saveAll(conflicts);
    }

    @Transactional
    public void unpublishExam(Integer id) {
        MockExam exam =
                mockExamRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
        if ("PUBLISHED".equals(exam.getStatus())) {
            exam.setStatus("ARCHIVED");
            mockExamRepository.save(exam);
        }
    }

    @Transactional
    public void restoreExam(Integer id) {
        MockExam exam =
                mockExamRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
        if ("ARCHIVED".equals(exam.getStatus())) {
            exam.setStatus("DRAFT");
            mockExamRepository.save(exam);
        }
    }

    public void normalizeSections(MockExamDTO form) {
        if (form.getSections() != null && !form.getSections().isEmpty()) return;
        MockExamSectionDTO section = new MockExamSectionDTO();
        section.setSkill(form.getSectionSkill());
        section.setDurationMinutes(form.getSectionDurationMinutes());
        section.setQuestionIds(form.getQuestionIds());
        form.setSections(new ArrayList<>(List.of(section)));
    }

    private List<Integer> parseIds(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        try {
            return Arrays.stream(raw.split(","))
                    .map(streamValue -> streamValue.trim())
                    .filter(x -> !x.isBlank())
                    .map(streamValue -> Integer.valueOf(streamValue))
                    .toList();
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("Danh sách câu hỏi không hợp lệ");
        }
    }

    public MockExamDTO entry(MockExam e) {
        MockExamDTO result = MockExamDTO.convertEntityToDto(e);
        result.setSections(
                mockExamSectionRepository.findByMockExamIdOrderById(e.getId()).stream()
                        .map(streamValue -> MockExamSectionDTO.convertEntityToDto(streamValue))
                        .toList());
        result.setSkillLabel(e.getSkill());
        result.setKindLabel(e.getExamType());
        result.setDurationLabel(
                e.getDurationMinutes() == null ? "Chưa có" : e.getDurationMinutes() + " phút");
        result.setSectionCount(result.getSections().size());
        return result;
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "PUBLISHED" -> "Đã công bố";
            case "ARCHIVED" -> "Đã ngừng công bố";
            default -> "Bản nháp";
        };
    }

    /**
     * Validates rules that depend on persisted data. Shape validation is declared on
     * MockExamDTO and is executed by the MVC controller through @Valid.
     */
    private void validateExamDefinition(MockExamDTO form) {
        if (form.getTitle() == null || form.getTitle().isBlank() || form.getTitle().length() > 255)
            throw new BusinessRuleException("Tên đề thi là bắt buộc");
        if (!MockExamDTO.EXAM_TYPES.contains(form.getExamType())
                || (form.getTargetLevel() != null
                        && !form.getTargetLevel().isBlank()
                        && !LEVELS.contains(form.getTargetLevel()))
                || ("CHECKPOINT".equals(form.getExamType())
                        && (form.getTargetLevel() == null
                                || !LEVELS.contains(form.getTargetLevel()))))
            throw new BusinessRuleException("Loại đề hoặc cấp độ không hợp lệ");
        if (form.getSections() == null || form.getSections().isEmpty())
            throw new BusinessRuleException("Đề phải có ít nhất một section");
        if (form.getSections().stream().anyMatch(section -> !SKILLS.contains(section.getSkill())))
            throw new BusinessRuleException("Kỹ năng section không hợp lệ");

        ScoringScheme scheme =
                form.getScoringSchemeId() == null
                        ? null
                        : scoringSchemeRepository
                                .findById(form.getScoringSchemeId())
                                .orElseThrow(
                                        () ->
                                                new BusinessRuleException(
                                                        "Bộ chấm điểm không hợp lệ"));
        boolean combined =
                "FULL_MOCK".equals(form.getExamType())
                        || ("PLACEMENT".equals(form.getExamType())
                                && (form.getSections().stream()
                                                        .map(streamValue -> streamValue.getSkill())
                                                        .distinct()
                                                        .count()
                                                > 1
                                        || (scheme != null
                                                && "PLACEMENT".equals(scheme.getExamType()))));
        if (combined
                && !form.getSections().stream()
                        .map(streamValue -> streamValue.getSkill())
                        .collect(Collectors.toSet())
                        .containsAll(SKILLS))
            throw new BusinessRuleException("Đề tổng hợp cần đủ 4 kỹ năng và Grammar & Vocabulary");
        if (!combined
                && form.getSections().stream()
                                .map(streamValue -> streamValue.getSkill())
                                .distinct()
                                .count()
                        != 1)
            throw new BusinessRuleException("Đề theo kỹ năng chỉ được chứa một kỹ năng");
        if (form.getSections().stream()
                .anyMatch(
                        section ->
                                section.getDurationMinutes() == null
                                        || section.getDurationMinutes() <= 0))
            throw new BusinessRuleException("Thời lượng section phải lớn hơn 0");
        long duration =
                form.getSections().stream()
                        .mapToLong(section -> section.getDurationMinutes())
                        .sum();
        if (duration > Integer.MAX_VALUE)
            throw new BusinessRuleException("Tổng thời gian vượt giới hạn");
        form.setDurationMinutes((int) duration);

        var certificate =
                certificateRepository
                        .findByCodeIgnoreCase("APTIS_GENERAL")
                        .filter(value -> value.getStatus() == RecordStatus.ACTIVE)
                        .orElse(null);
        if (certificate == null) throw new BusinessRuleException("Chứng chỉ không hợp lệ");
        form.setCertificateId(certificate.getId());
        if (certificate.isAptisGeneral()
                && com.aptisprep.mvc.enums.CefrLevel.C2.name().equals(form.getTargetLevel()))
            throw new BusinessRuleException("Aptis ESOL General chỉ có mức tối đa C1");
        if (Set.of("CHECKPOINT", "PLACEMENT").contains(form.getExamType())
                && form.getScoringSchemeId() == null)
            throw new BusinessRuleException(
                    "Placement và Checkpoint cần bộ chấm điểm để xác nhận CEFR");
        if (scheme != null
                && scheme.getExamType() != null
                && (!scheme.getExamType().equals(form.getExamType())
                        || (scheme.getScopeSkill() != null
                                && !scheme.getScopeSkill()
                                        .equals(form.getSections().getFirst().getSkill()))))
            throw new BusinessRuleException("Bộ chấm điểm không phù hợp với loại đề hoặc kỹ năng");
        if (form.getScoringSchemeId() != null)
            for (MockExamSectionDTO section : form.getSections())
                certificateSchemes.requireAvailableScheme(
                        form.getCertificateId(), form.getScoringSchemeId(), section.getSkill());
    }

    private void validateQuestionSelection(List<Integer> ids, String skill) {
        Set<Integer> unique = new HashSet<>(ids);
        if (unique.size() != ids.size())
            throw new BusinessRuleException("Một câu hỏi không được chọn lặp trong cùng đề");
        for (Integer id : ids) {
            Question question =
                    questionRepository
                            .findById(id)
                            .orElseThrow(
                                    () ->
                                            new BusinessRuleException(
                                                    "Câu hỏi không tồn tại: " + id));
            if (!"PUBLISHED".equals(question.getStatus()))
                throw new BusinessRuleException("Chỉ được đưa câu hỏi đã công bố vào đề");
            if (!Objects.equals(question.getSkill(), skill))
                throw new BusinessRuleException("Kỹ năng section không khớp với câu hỏi");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private MockExam applyExamFields(MockExam exam, MockExamDTO form) {
        Integer examId = exam.getId();
        exam = MockExamDTO.convertDtoToEntity(form);
        exam.setId(examId);
        exam.setTitle(form.getTitle().trim());
        exam.setDescription(blankToNull(form.getDescription()));
        exam.setTargetLevel(blankToNull(form.getTargetLevel()));
        exam.setSkill(
                "FULL_MOCK".equals(form.getExamType())
                                || ("PLACEMENT".equals(form.getExamType())
                                        && form.getSections().stream()
                                                        .map(streamValue -> streamValue.getSkill())
                                                        .distinct()
                                                        .count()
                                                > 1)
                        ? "FULL_MOCK"
                        : form.getSections().getFirst().getSkill());
        exam.setStatus("PUBLISHED".equals(form.getStatus()) ? "DRAFT" : form.getStatus());
        return exam;
    }

    private void replaceExamSections(
            Integer examId, MockExamDTO form, boolean explicitSections, boolean editing) {
        if (editing) {
            mockExamQuestionRepository.deleteByMockExamId(examId);
            mockExamQuestionRepository.flush();
            mockExamSectionRepository.deleteByMockExamId(examId);
            mockExamSectionRepository.flush();
        }
        int position = 1;
        int sectionOrder = 1;
        Set<Integer> allQuestionIds = new HashSet<>();
        for (MockExamSectionDTO sectionForm : form.getSections()) {
            List<Integer> questionIds = parseIds(sectionForm.getQuestionIds());
            validateQuestionSelection(questionIds, sectionForm.getSkill());
            for (Integer questionId : questionIds) {
                if (!allQuestionIds.add(questionId))
                    throw new BusinessRuleException("Một câu hỏi không được lặp giữa các section");
            }
            sectionForm.setMockExamId(examId);
            sectionForm.setDisplayOrder(sectionOrder++);
            sectionForm.setStatus("ACTIVE");
            MockExamSection section = MockExamSectionDTO.convertDtoToEntity(sectionForm);
            if (explicitSections || "FULL_MOCK".equals(form.getExamType()))
                section = mockExamSectionRepository.save(section);
            for (Integer questionId : questionIds) {
                MockExamQuestion link = new MockExamQuestion();
                link.setMockExamId(examId);
                link.setMockExamSectionId(section.getId());
                link.setQuestionId(questionId);
                link.setPosition(position++);
                mockExamQuestionRepository.save(link);
            }
        }
    }
}
