package com.aptisprep.mvc.service.placement.impl;

import com.aptisprep.mvc.entity.MockExam;
import com.aptisprep.mvc.enums.Skill;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.repository.MockExamQuestionRepository;
import com.aptisprep.mvc.repository.MockExamRepository;
import com.aptisprep.mvc.repository.MockExamSectionRepository;
import com.aptisprep.mvc.service.exam.ExamReadinessService;
import com.aptisprep.mvc.service.placement.PlacementExamCatalogService;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Read model for Placement only. It deliberately does not expose the admin catalog model. */
@Service
public class PlacementExamCatalogServiceImpl implements PlacementExamCatalogService {
    private MockExamRepository mockExamRepository;
    private MockExamQuestionRepository mockExamQuestionRepository;
    private MockExamSectionRepository sections;
    private ExamReadinessService startValidator;

    public PlacementExamCatalogServiceImpl(
            MockExamRepository mockExamRepository,
            MockExamQuestionRepository mockExamQuestionRepository,
            MockExamSectionRepository sections,
            ExamReadinessService startValidator) {
        this.mockExamRepository = mockExamRepository;
        this.mockExamQuestionRepository = mockExamQuestionRepository;
        this.sections = sections;
        this.startValidator = startValidator;
    }

    public List<PlacementExam> findPublished(String skill) {
        String normalized = skill == null ? "" : skill.trim().toUpperCase(Locale.ROOT);
        if (!"FULL_MOCK".equals(normalized) && !Skill.codes().contains(normalized))
            throw new BusinessRuleException("Kỹ năng Placement không hợp lệ");
        return mockExamRepository
                .findBySkillAndStatusOrderByUpdatedAtDesc(normalized, "PUBLISHED")
                .stream()
                .filter(exam -> !exam.isDemoOnly())
                .filter(exam -> "PLACEMENT".equals(exam.getExamType()))
                .filter(streamValue -> isReady(streamValue))
                // FULL_MOCK is the canonical combined exam. The fallback keeps
                // older databases with one legacy Placement exam usable during migration.
                .sorted(Comparator.comparing(exam -> !"FULL_MOCK".equals(exam.getSkill())))
                .map(streamValue -> describe(streamValue))
                .toList();
    }

    /** Returns the single combined Placement exam used by the student flow. */
    public List<PlacementExam> findPublished() {
        return mockExamRepository.findByStatusOrderByUpdatedAtDesc("PUBLISHED").stream()
                .filter(exam -> !exam.isDemoOnly())
                .filter(exam -> "PLACEMENT".equals(exam.getExamType()))
                .filter(streamValue -> isReady(streamValue))
                // Prefer the canonical combined exam even when a newer legacy
                // skill-specific Placement row exists in the catalog.
                .sorted(Comparator.comparing(exam -> !"FULL_MOCK".equals(exam.getSkill())))
                .map(streamValue -> describe(streamValue))
                .limit(1)
                .toList();
    }

    private PlacementExam describe(MockExam exam) {
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

    private boolean isReady(MockExam exam) {
        try {
            startValidator.validate(
                    exam, mockExamQuestionRepository.findByMockExamIdOrderByPosition(exam.getId()));
            return true;
        } catch (BusinessRuleException exception) {
            return false;
        }
    }
}
