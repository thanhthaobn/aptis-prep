package com.aptisprep.service.placement;

import com.aptisprep.mvc.catalog.entity.*;
import com.aptisprep.mvc.catalog.entity.MockExamEntity;
import com.aptisprep.mvc.catalog.repository.*;
import com.aptisprep.mvc.catalog.repository.MockExamQuestionRepository;
import com.aptisprep.mvc.catalog.repository.MockExamRepository;
import com.aptisprep.mvc.catalog.repository.MockExamSectionRepository;
import com.aptisprep.mvc.catalog.service.*;
import com.aptisprep.mvc.common.enums.Skill;
import com.aptisprep.mvc.common.exception.BusinessRuleException;
import com.aptisprep.mvc.exam.service.impl.ExamStartValidator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Read model for Placement only. It deliberately does not expose the admin catalog model. */
@Service
public class PlacementExamCatalogService {
    private final MockExamRepository mockExamRepository;
    private final MockExamQuestionRepository mockExamQuestionRepository;
    private final MockExamSectionRepository sections;
    private final ExamStartValidator startValidator;

    public PlacementExamCatalogService(
            MockExamRepository mockExamRepository,
            MockExamQuestionRepository mockExamQuestionRepository,
            MockExamSectionRepository sections,
            ExamStartValidator startValidator) {
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
                .filter(this::isReady)
                // FULL_MOCK is the canonical combined exam. The fallback keeps
                // older databases with one legacy Placement exam usable during migration.
                .sorted(Comparator.comparing(exam -> !"FULL_MOCK".equals(exam.getSkill())))
                .map(this::describe)
                .toList();
    }

    /** Returns the single combined Placement exam used by the student flow. */
    public List<PlacementExam> findPublished() {
        return mockExamRepository.findByStatusOrderByUpdatedAtDesc("PUBLISHED").stream()
                .filter(exam -> !exam.isDemoOnly())
                .filter(exam -> "PLACEMENT".equals(exam.getExamType()))
                .filter(this::isReady)
                // Prefer the canonical combined exam even when a newer legacy
                // skill-specific Placement row exists in the catalog.
                .sorted(Comparator.comparing(exam -> !"FULL_MOCK".equals(exam.getSkill())))
                .map(this::describe)
                .limit(1)
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

    public record PlacementExam(
            Integer id,
            String title,
            String subtitle,
            int durationMinutes,
            List<Section> sections) {}

    public record Section(String skill, int durationMinutes) {}
}
