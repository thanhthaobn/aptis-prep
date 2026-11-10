package com.aptisprep.service.scoring;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.entity.MockExamQuestionEntity;
import com.aptisprep.entity.MockExamSectionEntity;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.*;
import com.aptisprep.service.exam.ExamStartValidator;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

class UnavailableExamCatalogTests {
    @Test
    void invalidLegacyExamIsExcludedFromCountsAndCannotBeOpenedDirectly() {
        var exams = mock(MockExamRepository.class);
        var sections = mock(MockExamSectionRepository.class);
        var questions = mock(MockExamQuestionRepository.class);
        var validator = mock(ExamStartValidator.class);
        var old = new MockExamEntity();
        org.springframework.test.util.ReflectionTestUtils.setField(old, "id", 99);
        old.setStatus("PUBLISHED");
        old.setTitle("Legacy exam");
        old.setExamType("PRACTICE");
        old.setSkill("READING");
        when(exams.findByStatusOrderByUpdatedAtDesc("PUBLISHED")).thenReturn(List.of(old));
        when(exams.findById(99)).thenReturn(Optional.of(old));
        when(questions.findByMockExamIdOrderByPosition(99)).thenReturn(List.of());
        doThrow(new BusinessRuleException("Missing questions"))
                .when(validator)
                .validate(eq(old), anyList());
        var catalog =
                new com.aptisprep.service.student.impl.StudentExamCatalogServiceImpl(
                        exams, sections, questions, validator);
        var result = catalog.browse("", "", "", "", "PRACTICE", "level", PageRequest.of(0, 9));
        assertThat(result.page().getTotalElements()).isZero();
        assertThat(result.categories()).allMatch(category -> category.count() == 0);
        assertThatThrownBy(() -> catalog.entry("exams", 99))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(exams, never()).delete(any());
    }

    @Test
    void publishedCompositeExamKeepsAllSectionsVisibleWhenReadinessIsBlocked() {
        var exams = mock(MockExamRepository.class);
        var sections = mock(MockExamSectionRepository.class);
        var questions = mock(MockExamQuestionRepository.class);
        var validator = mock(ExamStartValidator.class);

        var exam = new MockExamEntity();
        ReflectionTestUtils.setField(exam, "id", 100);
        exam.setTitle("Published full mock");
        exam.setExamType("FULL_MOCK");
        exam.setSkill("FULL_MOCK");
        exam.setTargetLevel("B1");
        exam.setDurationMinutes(120);
        exam.setStatus("PUBLISHED");

        List<MockExamSectionEntity> examSections = new ArrayList<>();
        List<MockExamQuestionEntity> links = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            var section = mock(MockExamSectionEntity.class);
            when(section.getId()).thenReturn(index + 1);
            when(section.getDisplayOrder()).thenReturn(index + 1);
            when(section.getSkill())
                    .thenReturn(
                            List.of(
                                            "GRAMMAR_VOCABULARY",
                                            "READING",
                                            "LISTENING",
                                            "WRITING",
                                            "SPEAKING")
                                    .get(index));
            when(section.getDurationMinutes()).thenReturn(20);
            examSections.add(section);

            var link = mock(MockExamQuestionEntity.class);
            when(link.getMockExamSectionId()).thenReturn(index + 1);
            links.add(link);
        }
        when(exams.findByStatusOrderByUpdatedAtDesc("PUBLISHED")).thenReturn(List.of(exam));
        when(exams.findById(100)).thenReturn(Optional.of(exam));
        when(sections.findByMockExamIdOrderById(100)).thenReturn(examSections);
        when(questions.findByMockExamIdOrderByPosition(100)).thenReturn(links);
        doThrow(new BusinessRuleException("Listening audio chưa sẵn sàng"))
                .when(validator)
                .validate(eq(exam), eq(links));

        var catalog =
                new com.aptisprep.service.student.impl.StudentExamCatalogServiceImpl(
                        exams, sections, questions, validator);

        var page = catalog.browse("", "", "", "", "FULL_MOCK", "level", PageRequest.of(0, 9));

        assertThat(page.page().getTotalElements()).isEqualTo(1);
        var entry = page.page().getContent().getFirst();
        assertThat(entry.blocks())
                .extracting(block -> block.title())
                .containsExactly(
                        "Grammar & Vocabulary", "Reading", "Listening", "Writing", "Speaking");
        assertThat(entry.startable()).isFalse();
        assertThat(entry.readinessMessage()).contains("Listening audio");
    }
}
