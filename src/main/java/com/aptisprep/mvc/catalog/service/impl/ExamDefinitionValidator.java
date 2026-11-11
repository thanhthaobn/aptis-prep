package com.aptisprep.mvc.catalog.service.impl;

import com.aptisprep.mvc.catalog.dto.ExamFormDTO;
import com.aptisprep.mvc.catalog.entity.QuestionEntity;
import com.aptisprep.mvc.catalog.repository.QuestionRepository;
import com.aptisprep.mvc.common.enums.CefrLevel;
import com.aptisprep.mvc.common.enums.Skill;
import com.aptisprep.mvc.common.exception.BusinessRuleException;
import com.aptisprep.mvc.scoring.entity.ScoringScheme;
import com.aptisprep.mvc.scoring.service.CertificateSchemeService;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Read-only validation of exam policy, sections and question selection. */
@Component
final class ExamDefinitionValidator {
    public static final List<String> TYPES =
            List.of("PLACEMENT", "PRACTICE", "CHECKPOINT", "FULL_MOCK");
    private static final List<String> SKILLS = Skill.codes();
    private static final List<String> LEVELS = CefrLevel.codes();
    private final QuestionRepository questionRepository;
    private final CertificateSchemeService certificateSchemes;

    public ExamDefinitionValidator(
            QuestionRepository questionRepository, CertificateSchemeService certificateSchemes) {
        this.questionRepository = questionRepository;
        this.certificateSchemes = certificateSchemes;
    }

    public void validateBasics(ExamFormDTO f) {
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
        ScoringScheme scheme =
                f.getScoringSchemeId() == null
                        ? null
                        : certificateSchemes
                                .findSchemeById(f.getScoringSchemeId())
                                .orElseThrow(
                                        () ->
                                                new BusinessRuleException(
                                                        "Bộ chấm điểm không hợp lệ"));
        boolean combined =
                "FULL_MOCK".equals(f.getExamType())
                        || ("PLACEMENT".equals(f.getExamType())
                                && (f.getSections().stream()
                                                        .map(ExamFormDTO.SectionForm::getSkill)
                                                        .distinct()
                                                        .count()
                                                > 1
                                        || (scheme != null
                                                && "PLACEMENT".equals(scheme.getExamType()))));
        if (combined
                && !f.getSections().stream()
                        .map(ExamFormDTO.SectionForm::getSkill)
                        .collect(Collectors.toSet())
                        .containsAll(SKILLS))
            throw new BusinessRuleException("Đề tổng hợp cần đủ 4 kỹ năng và Grammar & Vocabulary");
        if (!combined
                && f.getSections().stream()
                                .map(ExamFormDTO.SectionForm::getSkill)
                                .distinct()
                                .count()
                        != 1)
            throw new BusinessRuleException("Đề theo kỹ năng chỉ được chứa một kỹ năng");
        if (f.getSections().stream()
                .anyMatch(
                        section ->
                                section.getDurationMinutes() == null
                                        || section.getDurationMinutes() <= 0))
            throw new BusinessRuleException("Thời lượng section phải lớn hơn 0");
        long duration =
                f.getSections().stream().mapToLong(section -> section.getDurationMinutes()).sum();
        if (duration > Integer.MAX_VALUE)
            throw new BusinessRuleException("Tổng thời gian vượt giới hạn");
        f.setDurationMinutes((int) duration);
        com.aptisprep.mvc.scoring.entity.Certificate certificate =
                f.getCertificateId() == null
                        ? null
                        : certificateSchemes.findById(f.getCertificateId()).orElse(null);
        if (certificate == null) throw new BusinessRuleException("Chứng chỉ không hợp lệ");
        if (certificate.isAptisGeneral() && CefrLevel.C2.name().equals(f.getTargetLevel()))
            throw new BusinessRuleException("Aptis ESOL General chỉ có mức tối đa C1");
        if (Set.of("CHECKPOINT", "PLACEMENT").contains(f.getExamType())
                && f.getScoringSchemeId() == null)
            throw new BusinessRuleException(
                    "Placement và Checkpoint cần bộ chấm điểm để xác nhận CEFR");
        if (scheme != null
                && scheme.getExamType() != null
                && (!scheme.getExamType().equals(f.getExamType())
                        || (scheme.getScopeSkill() != null
                                && !scheme.getScopeSkill()
                                        .equals(f.getSections().getFirst().getSkill()))))
            throw new BusinessRuleException("Bộ chấm điểm không phù hợp với loại đề hoặc kỹ năng");
        if (f.getScoringSchemeId() != null)
            for (ExamFormDTO.SectionForm section : f.getSections())
                certificateSchemes.requireAvailableScheme(
                        f.getCertificateId(), f.getScoringSchemeId(), section.getSkill());
    }

    public void validateQuestionSelection(List<Integer> ids, String skill) {
        Set<Integer> unique = new HashSet<>(ids);
        if (unique.size() != ids.size())
            throw new BusinessRuleException("Một câu hỏi không được chọn lặp trong cùng đề");
        for (Integer id : ids) {
            QuestionEntity q =
                    questionRepository
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
