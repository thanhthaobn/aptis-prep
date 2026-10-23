package com.aptisprep;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aptisprep.dto.form.ExamForm;
import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.entity.MockExamQuestionEntity;
import com.aptisprep.entity.MockExamSectionEntity;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.MockExamQuestionRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.repository.MockExamSectionRepository;
import com.aptisprep.service.student.StudentExamCatalogService;

import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

class IntegrationMergeRegressionTests {
    @Test
    void migrationVersionsAreUniqueIncludingFlywayNumericNormalization() throws Exception {
        var versions = new HashSet<MigrationVersion>();
        try (var paths = Files.list(Path.of("src/main/resources/db/migration"))) {
            for (Path path :
                    paths.filter(file -> file.getFileName().toString().startsWith("V")).toList()) {
                String name = path.getFileName().toString();
                var version = MigrationVersion.fromVersion(name.substring(1, name.indexOf("__")));
                assertTrue(versions.add(version), "Duplicate migration version: " + name);
            }
        }
        assertTrue(
                Files.exists(
                        Path.of(
                                "src/main/resources/db/migration/V026__seed_tram_exam_catalog.sql")));
        assertTrue(
                Files.exists(
                        Path.of(
                                "src/main/resources/db/migration/V028__add_reading_passages.sql")));
        assertTrue(
                Files.exists(
                        Path.of(
                                "src/main/resources/db/migration/V032__lesson_submission_history.sql")));
        assertTrue(
                Files.exists(
                        Path.of(
                                "src/main/resources/db/migration/V034__simple_lesson_release_rules.sql")));
        assertTrue(Files.exists(Path.of(
                "src/main/resources/db/migration/V039__lesson_playback_verification_and_six_options.sql")));
        assertTrue(Files.exists(Path.of(
                "src/main/resources/db/migration/V040__protect_ai_processing_claims.sql")));
    }

    @Test
    void examFormKeepsLegacyFieldsAndNewSectionsWithNullSafety() {
        var form = new ExamForm();
        form.setSectionSkill("READING");
        form.setSectionDurationMinutes(35);
        form.setQuestionIds("1,2");
        var section = new ExamForm.SectionForm();
        section.setSkill("LISTENING");
        section.setDurationMinutes(40);
        section.setQuestionIds("3,4");
        form.setSections(new ArrayList<>(List.of(section)));

        assertEquals("READING", form.getSectionSkill());
        assertEquals(35, form.getSectionDurationMinutes());
        assertEquals("1,2", form.getQuestionIds());
        assertEquals("LISTENING", form.getSections().getFirst().getSkill());
        assertEquals(40, form.getSections().getFirst().getDurationMinutes());
        assertEquals("3,4", form.getSections().getFirst().getQuestionIds());
        form.setSections(null);
        assertTrue(form.getSections().isEmpty());
    }

    @Test
    void sectionAndQuestionExposeFieldsUsedByExamCatalog() {
        var section = new MockExamSectionEntity();
        section.setMockExamId(12);
        section.setDisplayOrder(2);
        section.setDurationMinutes(40);
        var question = new MockExamQuestionEntity();
        question.setMockExamId(12);
        question.setMockExamSectionId(24);

        assertEquals(12, section.getMockExamId());
        assertEquals(2, section.getDisplayOrder());
        assertEquals(40, section.getDurationMinutes());
        assertEquals(12, question.getMockExamId());
        assertEquals(24, question.getMockExamSectionId());
    }

    @Test
    void previewPrefersPublishedDatabaseExamsAndRejectsDrafts() {
        var exams = mock(MockExamRepository.class);
        var sections = mock(MockExamSectionRepository.class);
        var questions = mock(MockExamQuestionRepository.class);
        var service = new StudentExamCatalogService(exams, sections, questions);
        var published = new MockExamEntity();
        ReflectionTestUtils.setField(published, "id", 12);
        published.setTitle("Published reading exam");
        published.setSkill("READING");
        published.setTargetLevel("A1");
        published.setExamType("PRACTICE");
        published.setStatus("PUBLISHED");
        published.setDurationMinutes(35);
        when(exams.count()).thenReturn(1L);
        when(exams.findByStatusOrderByUpdatedAtDesc("PUBLISHED")).thenReturn(List.of(published));
        when(exams.findById(12)).thenReturn(Optional.of(published));

        assertEquals(
                "Published reading exam",
                service.find("exams", "", "Reading", "A1", "").getFirst().title());
        assertEquals("Published reading exam", service.entry("exams", 12).title());
        published.setStatus("DRAFT");
        assertThrows(ResourceNotFoundException.class, () -> service.entry("exams", 12));
    }

    @Test
    void emptyDatabaseFallbackDoesNotRestoreFakeLessonsOrResults() {
        var service =
                new StudentExamCatalogService(
                        mock(MockExamRepository.class),
                        mock(MockExamSectionRepository.class),
                        mock(MockExamQuestionRepository.class));
        assertTrue(service.find("exams", "", "", "", "").isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> service.entry("exams", 7002));
        assertTrue(service.find("lessons", "", "", "", "").isEmpty());
        assertTrue(service.find("results", "", "", "", "").isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> service.entry("lessons", 6001));
        assertThrows(ResourceNotFoundException.class, () -> service.entry("results", 8001));
    }
}
