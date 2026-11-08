package com.aptisprep.service.admin;

import com.aptisprep.dto.form.ExamForm;
import com.aptisprep.dto.view.AdminCatalogView.Entry;
import com.aptisprep.dto.view.AdminCatalogView.Field;
import com.aptisprep.dto.view.AdminCatalogView.Row;
import com.aptisprep.entity.*;
import com.aptisprep.enums.RecordStatus;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.*;
import com.aptisprep.service.exam.ExamSnapshotService;
import com.aptisprep.service.exam.ExamStartValidator;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Exam authoring only; catalog facade retains its public controller contract. */
@Service
public class AdminExamService {
    private final MockExamRepository mockExamRepository;
    private final MockExamSectionRepository mockExamSectionRepository;
    private final MockExamQuestionRepository mockExamQuestionRepository;
    private final CertificateRepository certificateRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamDefinitionValidator examValidator;
    private final ExamSnapshotService examSnapshots;
    private final ExamStartValidator readiness;

    public AdminExamService(
            MockExamRepository mockExamRepository,
            MockExamSectionRepository mockExamSectionRepository,
            MockExamQuestionRepository mockExamQuestionRepository,
            CertificateRepository certificateRepository,
            ExamAttemptRepository examAttemptRepository,
            ExamDefinitionValidator examValidator,
            ExamSnapshotService examSnapshots,
            ExamStartValidator readiness) {
        this.mockExamRepository = mockExamRepository;
        this.mockExamSectionRepository = mockExamSectionRepository;
        this.mockExamQuestionRepository = mockExamQuestionRepository;
        this.certificateRepository = certificateRepository;
        this.examAttemptRepository = examAttemptRepository;
        this.examValidator = examValidator;
        this.examSnapshots = examSnapshots;
        this.readiness = readiness;
    }

    public ExamForm examForm(Integer id) {
        ExamForm form = new ExamForm();
        certificateRepository.findByStatusOrderByName(RecordStatus.ACTIVE).stream()
                .findFirst()
                .ifPresent(x -> form.setCertificateId(x.getId()));
        if (id == null) {
            ExamForm.SectionForm section = new ExamForm.SectionForm();
            section.setSkill(form.getSectionSkill());
            section.setDurationMinutes(form.getSectionDurationMinutes());
            section.setQuestionIds("");
            form.setSections(new ArrayList<>(List.of(section)));
            return form;
        }
        MockExamEntity exam =
                mockExamRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
        form.setTitle(exam.getTitle());
        form.setDescription(exam.getDescription());
        form.setDemoOnly(exam.isDemoOnly());
        form.setExamType(exam.getExamType());
        form.setTargetLevel(exam.getTargetLevel());
        form.setSkill(exam.getSkill());
        form.setSectionSkill(exam.getSkill());
        form.setDurationMinutes(exam.getDurationMinutes());
        form.setCertificateId(exam.getCertificateId());
        form.setScoringSchemeId(exam.getScoringSchemeId());
        form.setStatus(exam.getStatus());
        form.setQuestionIds(
                mockExamQuestionRepository.findByMockExamIdOrderByPosition(id).stream()
                        .map(x -> "" + x.getQuestionId())
                        .collect(Collectors.joining(",")));
        List<ExamForm.SectionForm> sectionForms = new ArrayList<>();
        for (MockExamSectionEntity section :
                mockExamSectionRepository.findByMockExamIdOrderById(id)) {
            ExamForm.SectionForm sectionForm = new ExamForm.SectionForm();
            sectionForm.setSkill(section.getSkill());
            sectionForm.setDurationMinutes(section.getDurationMinutes());
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
    public Integer saveExam(Integer id, ExamForm form) {
        boolean explicitSections = form.getSections() != null && !form.getSections().isEmpty();
        normalizeSections(form);
        if (id == null
                && "PLACEMENT".equals(form.getExamType())
                && form.getSections().stream()
                                .map(ExamForm.SectionForm::getSkill)
                                .distinct()
                                .count()
                        != 5)
            throw new BusinessRuleException(
                    "Đề đầu vào mới cần đủ 4 kỹ năng và Grammar & Vocabulary");
        examValidator.validateBasics(form);
        MockExamEntity exam =
                id == null
                        ? new MockExamEntity()
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
        applyExamFields(exam, form);
        exam = mockExamRepository.save(exam);
        replaceExamSections(exam.getId(), form, explicitSections, id != null);
        return exam.getId();
    }

    @Transactional
    public void publishExam(Integer id) {
        MockExamEntity exam =
                mockExamRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
        ExamForm form = examForm(id);
        normalizeSections(form);
        examValidator.validateBasics(form);
        if ("FULL_MOCK".equals(form.getExamType()) && form.getScoringSchemeId() == null)
            throw new BusinessRuleException("Đề tổng hợp cần bộ chấm điểm trước khi công bố");
        Set<Integer> all = new HashSet<>();
        for (ExamForm.SectionForm section : form.getSections()) {
            List<Integer> ids = parseIds(section.getQuestionIds());
            if (ids.isEmpty()) throw new BusinessRuleException("Không thể công bố section trống");
            examValidator.validateQuestionSelection(ids, section.getSkill());
            if (ids.stream().anyMatch(questionId -> !all.add(questionId)))
                throw new BusinessRuleException("Một câu hỏi không được lặp giữa các section");
        }
        if (all.isEmpty()) throw new BusinessRuleException("Không thể công bố đề trống");
        exam.setDurationMinutes(form.getDurationMinutes());
        readiness.validateForPublication(
                exam, mockExamQuestionRepository.findByMockExamIdOrderByPosition(id));
        exam.setStatus("PUBLISHED");
        mockExamRepository.save(exam);
    }

    @Transactional
    public void unpublishExam(Integer id) {
        MockExamEntity exam =
                mockExamRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
        if ("PUBLISHED".equals(exam.getStatus())) {
            exam.setStatus("ARCHIVED");
            mockExamRepository.save(exam);
        }
    }

    void normalizeSections(ExamForm form) {
        if (form.getSections() != null && !form.getSections().isEmpty()) return;
        ExamForm.SectionForm section = new ExamForm.SectionForm();
        section.setSkill(form.getSectionSkill());
        section.setDurationMinutes(form.getSectionDurationMinutes());
        section.setQuestionIds(form.getQuestionIds());
        form.setSections(new ArrayList<>(List.of(section)));
    }

    private List<Integer> parseIds(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        try {
            return Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(x -> !x.isBlank())
                    .map(Integer::valueOf)
                    .toList();
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("Danh sách câu hỏi không hợp lệ");
        }
    }

    Entry entry(MockExamEntity e) {
        List<Row> rows =
                mockExamSectionRepository.findByMockExamIdOrderById(e.getId()).stream()
                        .map(s -> new Row(s.getSkill(), "Các câu hỏi đã chọn", "Section"))
                        .toList();
        return new Entry(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getSkill() == null ? "" : e.getSkill(),
                e.getTargetLevel(),
                e.getExamType(),
                e.getStatus(),
                statusLabel(e.getStatus()),
                e.getUpdatedAt().toLocalDate().toString(),
                List.of(
                        new Field("Thời lượng", e.getDurationMinutes() + " phút"),
                        new Field("Bộ chấm điểm", String.valueOf(e.getScoringSchemeId()))),
                rows);
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "PUBLISHED" -> "Đã công bố";
            case "ARCHIVED" -> "Đã ngừng công bố";
            default -> "Bản nháp";
        };
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void applyExamFields(MockExamEntity exam, ExamForm form) {
        exam.setTitle(form.getTitle().trim());
        exam.setDescription(blankToNull(form.getDescription()));
        exam.setDemoOnly(form.isDemoOnly());
        exam.setExamType(form.getExamType());
        exam.setTargetLevel(blankToNull(form.getTargetLevel()));
        exam.setSkill(
                "FULL_MOCK".equals(form.getExamType())
                                || ("PLACEMENT".equals(form.getExamType())
                                        && form.getSections().stream()
                                                        .map(ExamForm.SectionForm::getSkill)
                                                        .distinct()
                                                        .count()
                                                > 1)
                        ? "FULL_MOCK"
                        : form.getSections().getFirst().getSkill());
        exam.setDurationMinutes(form.getDurationMinutes());
        exam.setCertificateId(form.getCertificateId());
        exam.setScoringSchemeId(form.getScoringSchemeId());
        exam.setStatus("PUBLISHED".equals(form.getStatus()) ? "DRAFT" : form.getStatus());
    }

    private void replaceExamSections(
            Integer examId, ExamForm form, boolean explicitSections, boolean editing) {
        if (editing) {
            mockExamQuestionRepository.deleteByMockExamId(examId);
            mockExamSectionRepository.deleteByMockExamId(examId);
        }
        int position = 1;
        int sectionOrder = 1;
        Set<Integer> allQuestionIds = new HashSet<>();
        for (ExamForm.SectionForm sectionForm : form.getSections()) {
            List<Integer> questionIds = parseIds(sectionForm.getQuestionIds());
            examValidator.validateQuestionSelection(questionIds, sectionForm.getSkill());
            for (Integer questionId : questionIds) {
                if (!allQuestionIds.add(questionId))
                    throw new BusinessRuleException("Một câu hỏi không được lặp giữa các section");
            }
            MockExamSectionEntity section = new MockExamSectionEntity();
            section.setMockExamId(examId);
            section.setSkill(sectionForm.getSkill());
            section.setDisplayOrder(sectionOrder++);
            section.setDurationMinutes(sectionForm.getDurationMinutes());
            section.setStatus("ACTIVE");
            if (explicitSections || "FULL_MOCK".equals(form.getExamType()))
                section = mockExamSectionRepository.save(section);
            for (Integer questionId : questionIds) {
                MockExamQuestionEntity link = new MockExamQuestionEntity();
                link.setMockExamId(examId);
                link.setMockExamSectionId(section.getId());
                link.setQuestionId(questionId);
                link.setPosition(position++);
                mockExamQuestionRepository.save(link);
            }
        }
    }
}
