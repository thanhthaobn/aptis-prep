package com.aptisprep.service.scoring;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.*;
import com.aptisprep.service.exam.ExamStartValidator;
import com.aptisprep.service.student.StudentExamCatalogService;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.util.*;

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
        var catalog = new StudentExamCatalogService(exams, sections, questions, validator);
        var result = catalog.browse("", "", "", "", "PRACTICE", "level", PageRequest.of(0, 9));
        assertThat(result.page().getTotalElements()).isZero();
        assertThat(result.categories()).allMatch(category -> category.count() == 0);
        assertThatThrownBy(() -> catalog.entry("exams", 99))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(exams, never()).delete(any());
    }
}
