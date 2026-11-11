package com.aptisprep.service.exam.impl;

import com.aptisprep.dto.PlacementDTO.PlacementExam;
import com.aptisprep.dto.PlacementDTO.Section;
import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.enums.Skill;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.admin.ExamCatalogService;
import com.aptisprep.service.exam.PlacementExamCatalogService;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Read model for Placement only. It deliberately does not expose the admin catalog model. */
@Service
public class PlacementExamCatalogServiceImpl implements PlacementExamCatalogService {
    private final ExamCatalogService catalogService;
    private final ExamStartValidator startValidator;

    public PlacementExamCatalogServiceImpl(
            ExamCatalogService catalogService, ExamStartValidator startValidator) {
        this.catalogService = catalogService;
        this.startValidator = startValidator;
    }

    @Override
    public List<PlacementExam> findPublished() {
        return findPublished("FULL_MOCK");
    }

    @Override
    public List<PlacementExam> findPublished(String skill) {
        String normalized = skill == null ? "" : skill.trim().toUpperCase(Locale.ROOT);
        if (!"FULL_MOCK".equals(normalized) && !Skill.codes().contains(normalized))
            throw new BusinessRuleException("Kỹ năng Placement không hợp lệ");
        return catalogService.findPublishedExamsBySkill(normalized).stream()
                .filter(exam -> !exam.isDemoOnly())
                .filter(exam -> "PLACEMENT".equals(exam.getExamType()))
                .filter(this::isReady)
                // FULL_MOCK is the canonical combined exam. The fallback keeps
                // older databases with one legacy Placement exam usable during migration.
                .sorted(Comparator.comparing(exam -> !"FULL_MOCK".equals(exam.getSkill())))
                .map(this::describe)
                .toList();
    }

    private PlacementExam describe(MockExamEntity exam) {
        List<Section> schedule =
                catalogService.findExamSections(exam.getId()).stream()
                        .sorted(Comparator.comparingInt(section -> section.getDisplayOrder()))
                        .map(
                                section ->
                                        new Section(
                                                section.getSkill(), section.getDurationMinutes()))
                        .toList();
        return new PlacementExam(
                exam.getId(),
                exam.getTitle(),
                exam.getDescription(),
                exam.getDurationMinutes(),
                schedule);
    }

    private boolean isReady(MockExamEntity exam) {
        try {
            startValidator.validate(exam, catalogService.findExamQuestions(exam.getId()));
            return true;
        } catch (BusinessRuleException exception) {
            return false;
        }
    }
}
