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
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Exam authoring only; catalog facade retains its public controller contract. */
@Service
public class AdminExamService {
    private final MockExamRepository exams;
    private final MockExamSectionRepository sections;
    private final MockExamQuestionRepository examQuestions;
    private final CertificateRepository certificates;
    private final ScoringSchemeRepository schemes;
    private final ExamAttemptRepository attempts;
    private final ExamDefinitionValidator examValidator;

    public AdminExamService(
            MockExamRepository exams,
            MockExamSectionRepository sections,
            MockExamQuestionRepository examQuestions,
            CertificateRepository certificates,
            ScoringSchemeRepository schemes,
            ExamAttemptRepository attempts,
            ExamDefinitionValidator examValidator) {
        this.exams = exams;
        this.sections = sections;
        this.examQuestions = examQuestions;
        this.certificates = certificates;
        this.schemes = schemes;
        this.attempts = attempts;
        this.examValidator = examValidator;
    }

    public ExamForm examForm(Integer id) {
        ExamForm form = new ExamForm();
        certificates.findByStatusOrderByName(RecordStatus.ACTIVE).stream()
                .findFirst()
                .ifPresent(x -> form.setCertificateId(x.getId()));
        if (form.getCertificateId() != null)
            schemes
                    .findByCertificateIdAndStatusOrderByVersionDesc(
                            form.getCertificateId(), RecordStatus.ACTIVE)
                    .stream()
                    .findFirst()
                    .ifPresent(x -> form.setScoringSchemeId(x.getId()));
        if (id == null) {
            ExamForm.SectionForm section = new ExamForm.SectionForm();
            section.setSkill(form.getSectionSkill());
            section.setDurationMinutes(form.getSectionDurationMinutes());
            section.setQuestionIds("");
            form.setSections(new ArrayList<>(List.of(section)));
            return form;
        }
        MockExamEntity exam =
                exams.findById(id)
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
                examQuestions.findByMockExamIdOrderByPosition(id).stream()
                        .map(x -> "" + x.getQuestionId())
                        .collect(Collectors.joining(",")));
        List<ExamForm.SectionForm> sectionForms = new ArrayList<>();
        for (MockExamSectionEntity section : sections.findByMockExamIdOrderById(id)) {
            ExamForm.SectionForm sectionForm = new ExamForm.SectionForm();
            sectionForm.setSkill(section.getSkill());
            sectionForm.setDurationMinutes(section.getDurationMinutes());
            sectionForm.setQuestionIds(
                    examQuestions.findByMockExamIdOrderByPosition(id).stream()
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
        }
        return form;
    }

    @Transactional
    public Integer saveExam(Integer id, ExamForm form) {
        boolean explicitSections = form.getSections() != null && !form.getSections().isEmpty();
        normalizeSections(form);
        examValidator.validateBasics(form);
        MockExamEntity exam =
                id == null
                        ? new MockExamEntity()
                        : exams.findById(id)
                                .orElseThrow(
                                        () ->
                                                new ResourceNotFoundException(
                                                        "Không tìm thấy đề thi"));
        if (id != null && attempts.existsByMockExamId(id)) {
            throw new BusinessRuleException(
                    "Đề thi đã có lịch sử làm bài. Hãy tạo phiên bản đề mới");
        }
        if ("PUBLISHED".equals(exam.getStatus()) && !"PUBLISHED".equals(form.getStatus()))
            throw new BusinessRuleException("Đề đã công bố không thể quay về bản nháp");
        exam.setTitle(form.getTitle().trim());
        exam.setDescription(blankToNull(form.getDescription()));
        exam.setDemoOnly(form.isDemoOnly());
        exam.setExamType(form.getExamType());
        exam.setTargetLevel(blankToNull(form.getTargetLevel()));
        exam.setSkill(
                "FULL_MOCK".equals(form.getExamType())
                        ? "FULL_MOCK"
                        : form.getSections().get(0).getSkill());
        exam.setDurationMinutes(form.getDurationMinutes());
        exam.setCertificateId(form.getCertificateId());
        exam.setScoringSchemeId(form.getScoringSchemeId());
        exam.setStatus("PUBLISHED".equals(form.getStatus()) ? "DRAFT" : form.getStatus());
        exam = exams.save(exam);
        if (id != null) {
            examQuestions.deleteByMockExamId(id);
            sections.deleteByMockExamId(id);
        }
        int pos = 1;
        int sectionOrder = 1;
        Set<Integer> allQuestionIds = new HashSet<>();
        for (ExamForm.SectionForm sectionForm : form.getSections()) {
            List<Integer> ids = parseIds(sectionForm.getQuestionIds());
            examValidator.validateQuestionSelection(ids, sectionForm.getSkill());
            if (ids.stream().anyMatch(questionId -> !allQuestionIds.add(questionId)))
                throw new BusinessRuleException("Một câu hỏi không được lặp giữa các section");
            MockExamSectionEntity section = new MockExamSectionEntity();
            section.setMockExamId(exam.getId());
            section.setSkill(sectionForm.getSkill());
            section.setDisplayOrder(sectionOrder++);
            section.setDurationMinutes(sectionForm.getDurationMinutes());
            section.setStatus("ACTIVE");
            if (explicitSections || "FULL_MOCK".equals(form.getExamType()))
                section = sections.save(section);
            for (Integer questionId : ids) {
                MockExamQuestionEntity link = new MockExamQuestionEntity();
                link.setMockExamId(exam.getId());
                link.setMockExamSectionId(section.getId());
                link.setQuestionId(questionId);
                link.setPosition(pos++);
                examQuestions.save(link);
            }
        }
        return exam.getId();
    }

    @Transactional
    public void publishExam(Integer id) {
        MockExamEntity exam =
                exams.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề thi"));
        ExamForm form = examForm(id);
        normalizeSections(form);
        examValidator.validateBasics(form);
        Set<Integer> all = new HashSet<>();
        for (ExamForm.SectionForm section : form.getSections()) {
            List<Integer> ids = parseIds(section.getQuestionIds());
            if (ids.isEmpty()) throw new BusinessRuleException("Không thể công bố section trống");
            examValidator.validateQuestionSelection(ids, section.getSkill());
            if (ids.stream().anyMatch(questionId -> !all.add(questionId)))
                throw new BusinessRuleException("Một câu hỏi không được lặp giữa các section");
        }
        if (all.isEmpty()) throw new BusinessRuleException("Không thể công bố đề trống");
        exam.setStatus("PUBLISHED");
        exams.save(exam);
    }

    private void normalizeSections(ExamForm form) {
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
                sections.findByMockExamIdOrderById(e.getId()).stream()
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
            case "ARCHIVED" -> "Đã lưu trữ";
            default -> "Bản nháp";
        };
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
