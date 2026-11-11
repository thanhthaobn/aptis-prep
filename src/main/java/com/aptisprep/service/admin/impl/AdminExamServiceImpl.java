package com.aptisprep.service.admin.impl;

import com.aptisprep.dto.AdminCatalogViewDTO.Entry;
import com.aptisprep.dto.AdminCatalogViewDTO.Field;
import com.aptisprep.dto.AdminCatalogViewDTO.Row;
import com.aptisprep.dto.ExamFormDTO;
import com.aptisprep.dto.MockExamEntityDTO;
import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.entity.MockExamQuestionEntity;
import com.aptisprep.entity.MockExamSectionEntity;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.MockExamQuestionRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.repository.MockExamSectionRepository;
import com.aptisprep.service.admin.AdminExamService;
import com.aptisprep.service.exam.ExamAttemptService;
import com.aptisprep.service.scoring.CertificateSchemeService;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Exam authoring only; catalog facade retains its public controller contract. */
@Service
public class AdminExamServiceImpl implements AdminExamService {
    private final MockExamRepository mockExamRepository;
    private final MockExamSectionRepository mockExamSectionRepository;
    private final MockExamQuestionRepository mockExamQuestionRepository;
    private final CertificateSchemeService certificateSchemes;
    private final ExamAttemptService examAttemptService;
    private final ExamDefinitionValidator examValidator;

    public AdminExamServiceImpl(
            MockExamRepository mockExamRepository,
            MockExamSectionRepository mockExamSectionRepository,
            MockExamQuestionRepository mockExamQuestionRepository,
            CertificateSchemeService certificateSchemes,
            ExamAttemptService examAttemptService,
            ExamDefinitionValidator examValidator) {
        this.mockExamRepository = mockExamRepository;
        this.mockExamSectionRepository = mockExamSectionRepository;
        this.mockExamQuestionRepository = mockExamQuestionRepository;
        this.certificateSchemes = certificateSchemes;
        this.examAttemptService = examAttemptService;
        this.examValidator = examValidator;
    }

    public ExamFormDTO examForm(Integer id) {
        if (id == null) {
            ExamFormDTO form = new ExamFormDTO();
            certificateSchemes.findAllActiveCertificates().stream()
                    .findFirst()
                    .ifPresent(x -> form.setCertificateId(x.getId()));
            ExamFormDTO.SectionForm section = new ExamFormDTO.SectionForm();
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
        ExamFormDTO form = MockExamEntityDTO.convertEntityToDto(exam).toForm();
        form.setSectionSkill(exam.getSkill());
        form.setQuestionIds(
                mockExamQuestionRepository.findByMockExamIdOrderByPosition(id).stream()
                        .map(x -> "" + x.getQuestionId())
                        .collect(Collectors.joining(",")));
        List<ExamFormDTO.SectionForm> sectionForms = new ArrayList<>();
        for (MockExamSectionEntity section :
                mockExamSectionRepository.findByMockExamIdOrderById(id)) {
            ExamFormDTO.SectionForm sectionForm = new ExamFormDTO.SectionForm();
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
    public Integer saveExam(Integer id, ExamFormDTO form) {
        boolean explicitSections = form.getSections() != null && !form.getSections().isEmpty();
        normalizeSections(form);
        if (id == null
                && "PLACEMENT".equals(form.getExamType())
                && form.getSections().stream()
                                .map(ExamFormDTO.SectionForm::getSkill)
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
            examAttemptService.preserveLegacyAttempts(id);
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
        ExamFormDTO form = examForm(id);
        normalizeSections(form);
        examValidator.validateBasics(form);
        if ("FULL_MOCK".equals(form.getExamType()) && form.getScoringSchemeId() == null)
            throw new BusinessRuleException("Đề tổng hợp cần bộ chấm điểm trước khi công bố");
        Set<Integer> all = new HashSet<>();
        for (ExamFormDTO.SectionForm section : form.getSections()) {
            List<Integer> ids = parseIds(section.getQuestionIds());
            if (ids.isEmpty()) throw new BusinessRuleException("Không thể công bố section trống");
            examValidator.validateQuestionSelection(ids, section.getSkill());
            if (ids.stream().anyMatch(questionId -> !all.add(questionId)))
                throw new BusinessRuleException("Một câu hỏi không được lặp giữa các section");
        }
        if (all.isEmpty()) throw new BusinessRuleException("Không thể công bố đề trống");
        exam.setDurationMinutes(form.getDurationMinutes());
        examAttemptService.validateExamForPublication(
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

    public void normalizeSections(ExamFormDTO form) {
        if (form.getSections() != null && !form.getSections().isEmpty()) return;
        ExamFormDTO.SectionForm section = new ExamFormDTO.SectionForm();
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

    public Entry entry(MockExamEntity e) {
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

    private void applyExamFields(MockExamEntity exam, ExamFormDTO form) {
        MockExamEntity mapped = MockExamEntityDTO.convertDtoToEntity(form);
        exam.setTitle(mapped.getTitle());
        exam.setDescription(mapped.getDescription());
        exam.setDemoOnly(mapped.isDemoOnly());
        exam.setExamType(mapped.getExamType());
        exam.setTargetLevel(mapped.getTargetLevel());
        exam.setSkill(
                "FULL_MOCK".equals(form.getExamType())
                                || ("PLACEMENT".equals(form.getExamType())
                                        && form.getSections().stream()
                                                        .map(ExamFormDTO.SectionForm::getSkill)
                                                        .distinct()
                                                        .count()
                                                > 1)
                        ? "FULL_MOCK"
                        : form.getSections().getFirst().getSkill());
        exam.setDurationMinutes(mapped.getDurationMinutes());
        exam.setCertificateId(mapped.getCertificateId());
        exam.setScoringSchemeId(mapped.getScoringSchemeId());
        exam.setStatus("PUBLISHED".equals(form.getStatus()) ? "DRAFT" : mapped.getStatus());
    }

    private void replaceExamSections(
            Integer examId, ExamFormDTO form, boolean explicitSections, boolean editing) {
        if (editing) {
            mockExamQuestionRepository.deleteByMockExamId(examId);
            mockExamSectionRepository.deleteByMockExamId(examId);
        }
        int position = 1;
        int sectionOrder = 1;
        Set<Integer> allQuestionIds = new HashSet<>();
        for (ExamFormDTO.SectionForm sectionForm : form.getSections()) {
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
