package com.aptisprep.service.admin;

import com.aptisprep.dto.form.ExamForm;
import com.aptisprep.entity.QuestionEntity;
import com.aptisprep.enums.CefrLevel;
import com.aptisprep.enums.Skill;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.repository.CertificateRepository;
import com.aptisprep.repository.QuestionRepository;
import com.aptisprep.repository.ScoringSchemeRepository;
import com.aptisprep.service.contract.CertificateSchemeContract;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Read-only validation of exam policy, sections and question selection. */
@Component
final class ExamDefinitionValidator {
    static final List<String> TYPES = List.of("PLACEMENT", "PRACTICE", "CHECKPOINT", "FULL_MOCK");
    private static final List<String> SKILLS = Skill.codes();
    private static final List<String> LEVELS = CefrLevel.codes();
    private final CertificateRepository certificates;
    private final ScoringSchemeRepository schemes;
    private final QuestionRepository questions;
    private final CertificateSchemeContract certificateSchemes;

    ExamDefinitionValidator(
            CertificateRepository certificates,
            ScoringSchemeRepository schemes,
            QuestionRepository questions,
            CertificateSchemeContract certificateSchemes) {
        this.certificates = certificates;
        this.schemes = schemes;
        this.questions = questions;
        this.certificateSchemes = certificateSchemes;
    }

    void validateBasics(ExamForm f) {
        if (f.getTitle() == null || f.getTitle().isBlank() || f.getTitle().length() > 255)
            throw new BusinessRuleException("Tên đề thi là bắt buộc");
        if (!TYPES.contains(f.getExamType())
                || (f.getTargetLevel() != null
                        && !f.getTargetLevel().isBlank()
                        && !LEVELS.contains(f.getTargetLevel()))
                || ("CHECKPOINT".equals(f.getExamType())
                        && (f.getTargetLevel() == null || !LEVELS.contains(f.getTargetLevel()))))
            throw new BusinessRuleException("Loại đề hoặc cấp độ không hợp lệ");
        if (f.getSections() == null || f.getSections().isEmpty())
            throw new BusinessRuleException("Đề phải có ít nhất một section");
        if (f.getSections().stream().anyMatch(section -> !SKILLS.contains(section.getSkill())))
            throw new BusinessRuleException("Kỹ năng section không hợp lệ");
        if ("FULL_MOCK".equals(f.getExamType())
                && !f.getSections().stream()
                        .map(ExamForm.SectionForm::getSkill)
                        .collect(Collectors.toSet())
                        .containsAll(SKILLS))
            throw new BusinessRuleException("Full Mock phải có đủ 5 kỹ năng");
        if (!"FULL_MOCK".equals(f.getExamType())
                && f.getSections().stream().map(ExamForm.SectionForm::getSkill).distinct().count()
                        != 1)
            throw new BusinessRuleException("Đề theo kỹ năng chỉ được chứa một kỹ năng");
        if (f.getDurationMinutes() == null
                || f.getDurationMinutes() <= 0
                || f.getSectionDurationMinutes() == null
                || f.getSectionDurationMinutes() <= 0)
            throw new BusinessRuleException("Thời lượng phải lớn hơn 0");
        if (f.getSections().stream()
                .anyMatch(
                        section ->
                                section.getDurationMinutes() == null
                                        || section.getDurationMinutes() <= 0))
            throw new BusinessRuleException("Thời lượng section phải lớn hơn 0");
        if (f.getCertificateId() == null || !certificates.existsById(f.getCertificateId()))
            throw new BusinessRuleException("Chứng chỉ không hợp lệ");
        if ("CHECKPOINT".equals(f.getExamType()) && f.getScoringSchemeId() == null)
            throw new BusinessRuleException("Checkpoint cần bộ chấm điểm để xác nhận CEFR");
        if (f.getScoringSchemeId() != null && !schemes.existsById(f.getScoringSchemeId()))
            throw new BusinessRuleException("Bộ chấm điểm không hợp lệ");
        if (f.getScoringSchemeId() != null)
            for (ExamForm.SectionForm section : f.getSections())
                certificateSchemes.requireAvailableScheme(
                        f.getCertificateId(), f.getScoringSchemeId(), section.getSkill());
    }

    void validateQuestionSelection(List<Integer> ids, String skill) {
        Set<Integer> unique = new HashSet<>(ids);
        if (unique.size() != ids.size())
            throw new BusinessRuleException("Một câu hỏi không được chọn lặp trong cùng đề");
        for (Integer id : ids) {
            QuestionEntity q =
                    questions
                            .findById(id)
                            .orElseThrow(
                                    () ->
                                            new BusinessRuleException(
                                                    "Câu hỏi không tồn tại: " + id));
            if (!"PUBLISHED".equals(q.getStatus()))
                throw new BusinessRuleException("Chỉ được đưa câu hỏi đã công bố vào đề");
            if (!Objects.equals(q.getSkill(), skill))
                throw new BusinessRuleException("Kỹ năng section không khớp với câu hỏi");
        }
    }
}
