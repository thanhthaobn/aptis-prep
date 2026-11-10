package com.aptisprep.mvc.placement.service.impl;

import com.aptisprep.mvc.catalog.entity.MockExamEntity;
import com.aptisprep.mvc.catalog.repository.MockExamQuestionRepository;
import com.aptisprep.mvc.catalog.repository.MockExamRepository;
import com.aptisprep.mvc.catalog.repository.MockExamSectionRepository;
import com.aptisprep.mvc.common.enums.Skill;
import com.aptisprep.mvc.common.exception.BusinessRuleException;
import com.aptisprep.mvc.exam.service.impl.ExamStartValidator;
import com.aptisprep.mvc.placement.dto.PlacementDTO.PlacementExam;
import com.aptisprep.mvc.placement.dto.PlacementDTO.Section;
import com.aptisprep.mvc.placement.service.PlacementExamCatalogService;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Read model for Placement only. It deliberately does not expose the admin catalog model. */
@Service
public class PlacementExamCatalogServiceImpl implements PlacementExamCatalogService {
    private final MockExamRepository mockExamRepository;
    private final MockExamQuestionRepository mockExamQuestionRepository;
    private final MockExamSectionRepository sections;
    private final ExamStartValidator startValidator;

    public PlacementExamCatalogServiceImpl(
            MockExamRepository mockExamRepository,
            MockExamQuestionRepository mockExamQuestionRepository,
            MockExamSectionRepository sections,
            ExamStartValidator startValidator) {
        this.mockExamRepository = mockExamRepository;
        this.mockExamQuestionRepository = mockExamQuestionRepository;
        this.sections = sections;
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
        return mockExamRepository
                .findBySkillAndStatusOrderByUpdatedAtDesc(normalized, "PUBLISHED")
                .stream()
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
                sections.findByMockExamIdOrderById(exam.getId()).stream()
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
            startValidator.validate(
                    exam, mockExamQuestionRepository.findByMockExamIdOrderByPosition(exam.getId()));
            return true;
        } catch (BusinessRuleException exception) {
            return false;
        }
    }
}
