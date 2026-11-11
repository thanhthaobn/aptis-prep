package com.aptisprep;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aptisprep.mvc.catalog.dto.*;
import com.aptisprep.mvc.catalog.dto.ExamFormDTO;
import com.aptisprep.mvc.catalog.entity.*;
import com.aptisprep.mvc.catalog.entity.MockExamEntity;
import com.aptisprep.mvc.catalog.entity.MockExamQuestionEntity;
import com.aptisprep.mvc.catalog.entity.MockExamSectionEntity;
import com.aptisprep.mvc.catalog.repository.*;
import com.aptisprep.mvc.catalog.repository.MockExamQuestionRepository;
import com.aptisprep.mvc.catalog.repository.MockExamRepository;
import com.aptisprep.mvc.catalog.repository.MockExamSectionRepository;
import com.aptisprep.mvc.catalog.service.*;
import com.aptisprep.mvc.common.exception.ResourceNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class IntegrationMergeRegressionTests {
    @Test
    void currentSchemaIsExplicitAndContainsNoSeedRowsOrMigrationHistory() throws Exception {
        String ddl = Files.readString(Path.of("database/schema-current.sql"));
        assertEquals(
                39,
                java.util.regex.Pattern.compile(
                                "CREATE TABLE", java.util.regex.Pattern.CASE_INSENSITIVE)
                        .matcher(ddl)
                        .results()
                        .count());
        assertTrue(ddl.contains("Schema installation requires an empty database"));
        assertTrue(ddl.contains("BEGIN TRANSACTION"));
        assertFalse(ddl.toUpperCase().contains("INSERT INTO"));
        assertFalse(ddl.contains("flyway_schema_history"));
        assertFalse(ddl.contains("CREATE DATABASE"));
        Properties settings = new Properties();
        Path settingsPath = Path.of("src/main/resources/application.properties");
        try (var reader = Files.newBufferedReader(settingsPath)) {
            settings.load(reader);
        }
        assertEquals("validate", settings.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("never", settings.getProperty("spring.sql.init.mode"));
        String passwordSetting = settings.getProperty("spring.datasource.password", "");
        assertTrue(
                passwordSetting.matches("\\$\\{DB_PASSWORD:[^}]+\\}"),
                "The datasource password must support a DB_PASSWORD override and a default");
        assertFalse(settings.containsKey("app.demo-data.enabled"));
    }

    @Test
    void examFormKeepsLegacyFieldsAndNewSectionsWithNullSafety() {
        var form = new ExamFormDTO();
        form.setSectionSkill("READING");
        form.setSectionDurationMinutes(35);
        form.setQuestionIds("1,2");
        var section = new ExamFormDTO.SectionForm();
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
        var catalog =
                new com.aptisprep.mvc.catalog.service.impl.ExamCatalogServiceImpl(
                        exams, questions, sections, null, null, null, null);
        var service =
                new com.aptisprep.mvc.student.service.impl.StudentExamCatalogServiceImpl(
                        catalog, null);
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
        var catalog =
                new com.aptisprep.mvc.catalog.service.impl.ExamCatalogServiceImpl(
                        mock(MockExamRepository.class),
                        mock(MockExamQuestionRepository.class),
                        mock(MockExamSectionRepository.class),
                        null,
                        null,
                        null,
                        null);
        var service =
                new com.aptisprep.mvc.student.service.impl.StudentExamCatalogServiceImpl(
                        catalog, null);
        assertTrue(service.find("exams", "", "", "", "").isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> service.entry("exams", 7002));
        assertTrue(service.find("lessons", "", "", "", "").isEmpty());
        assertTrue(service.find("results", "", "", "", "").isEmpty());
        assertThrows(ResourceNotFoundException.class, () -> service.entry("lessons", 6001));
        assertThrows(ResourceNotFoundException.class, () -> service.entry("results", 8001));
    }
}
