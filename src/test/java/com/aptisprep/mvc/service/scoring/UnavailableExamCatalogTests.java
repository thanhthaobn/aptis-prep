package com.aptisprep.mvc.service.scoring;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.aptisprep.mvc.entity.MockExam;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.exception.ResourceNotFoundException;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.service.exam.ExamReadinessService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class UnavailableExamCatalogTests {
    @Test
    void invalidLegacyExamIsExcludedFromCountsAndCannotBeOpenedDirectly() {
        var exams = mock(MockExamRepository.class);
        var sections = mock(MockExamSectionRepository.class);
        var questions = mock(MockExamQuestionRepository.class);
        var validator = mock(ExamReadinessService.class);
        var old = new MockExam();
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
                new com.aptisprep.mvc.service.student.impl.StudentExamCatalogServiceImpl(
                        exams, sections, questions, validator);
        var result = catalog.browse("", "", "", "", "PRACTICE", "level", PageRequest.of(0, 9));
        assertThat(result.getTotalElements()).isZero();
        assertThat(catalog.categoryCounts("", "", "", "").values()).allMatch(count -> count == 0L);
        assertThatThrownBy(() -> catalog.entry("exams", 99))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(exams, never()).delete(any());
    }
}
