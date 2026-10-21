package com.aptisprep.service.learning;

import com.aptisprep.entity.*;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.repository.*;
import com.aptisprep.service.contract.*;
import com.aptisprep.service.contract.IntegrationModels.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LearningPathCheckpointTests {
    @Mock LearningPathRepository paths;
    @Mock LearningPathItemRepository items;
    @Mock LessonRepository lessons;
    @Mock LessonProgressRepository progress;
    @Mock UserRepository users;
    @Mock UserGoalRepository goals;
    @Mock ObjectProvider<ExamAttemptContract> attemptProvider;
    @Mock ExamAttemptContract attempts;
    @Mock ExamCatalogContract catalog;
    @Mock AccountAccessContract accounts;
    @Mock EntityManager entityManager;

    @Test
    void passedCheckpointOpensNextCefrStageOnlyOnce() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(12);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(users.findById(12)).thenReturn(Optional.of(user));
        when(attemptProvider.getIfAvailable()).thenReturn(attempts);
        SkillResult reading = new SkillResult("READING", BigDecimal.valueOf(40), BigDecimal.valueOf(40), "A1", "COMPLETED", null, true);
        AttemptResult result = new AttemptResult(99, 12, 77, "CHECKPOINT", "Checkpoint Reading A1", 1,
                "COMPLETED", List.of(reading), BigDecimal.valueOf(40), "A1", java.time.Instant.now());
        when(attempts.result(12, 99)).thenReturn(result);

        LearningPath current = new LearningPath(); current.setUser(user); current.setSkill("Reading");
        current.setTargetLevel("A1"); current.setCheckpointExamId(77); current.setStatus("ACTIVE"); current.setPathSource("ORIENTATION");
        when(paths.findByUserIdAndSourceAttemptId(12, 99)).thenReturn(Optional.empty(), Optional.of(current));
        when(paths.findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(12, "ACTIVE")).thenReturn(List.of(current));
        Lesson a2 = mock(Lesson.class);
        when(a2.getSkill()).thenReturn("Reading"); when(a2.getCefrLevel()).thenReturn("A2"); when(a2.getId()).thenReturn(501);
        when(lessons.findByStatusOrderBySkillAscCefrLevelAscDisplayOrderAsc("PUBLISHED")).thenReturn(List.of(a2));
        when(progress.findByUserIdAndLessonId(12, 501)).thenReturn(Optional.empty());
        when(paths.save(any(LearningPath.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LearningPathService service = new LearningPathService(paths, items, lessons, progress, users, goals,
                attemptProvider, catalog, accounts);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        service.applyCheckpoint(12, 99);
        service.applyCheckpoint(12, 99);

        ArgumentCaptor<LearningPath> created = ArgumentCaptor.forClass(LearningPath.class);
        verify(paths, atLeastOnce()).save(created.capture());
        LearningPath next = created.getAllValues().stream().filter(path -> path != current).findFirst().orElseThrow();
        assertThat(current.getStatus()).isEqualTo("COMPLETED");
        assertThat(current.getSourceAttemptId()).isEqualTo(99);
        assertThat(next.getStatus()).isEqualTo("ACTIVE");
        assertThat(next.getFromLevel()).isEqualTo("A1");
        assertThat(next.getTargetLevel()).isEqualTo("A2");
        verify(accounts, times(1)).applyAssessment(12, 99);
        verify(items, times(1)).save(any(LearningPathItem.class));
    }
}
