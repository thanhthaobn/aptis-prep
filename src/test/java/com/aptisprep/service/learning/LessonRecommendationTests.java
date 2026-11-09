package com.aptisprep.service.learning;

import static org.junit.jupiter.api.Assertions.*;

import com.aptisprep.entity.*;
import com.aptisprep.repository.LessonProgressRepository;
import com.aptisprep.service.contract.AccountAccessContract;
import com.aptisprep.service.contract.IntegrationModels.*;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LessonRecommendationTests {
    List<LessonProgress> history = List.of();
    LearnerContext context;
    final LessonProgressRepository progress =
            (LessonProgressRepository)
                    java.lang.reflect.Proxy.newProxyInstance(
                            getClass().getClassLoader(),
                            new Class<?>[] {LessonProgressRepository.class},
                            (proxy, method, args) -> {
                                if (method.getName().equals("findByUserId")) return history;
                                throw new UnsupportedOperationException(method.getName());
                            });
    final AccountAccessContract accounts =
            new AccountAccessContract() {
                public Integer requireActiveStudent(String email) {
                    return 1;
                }

                public LearnerContext context(Integer id) {
                    return context;
                }

                public void applyAssessment(Integer userId, Integer attemptId) {
                    throw new UnsupportedOperationException();
                }
            };
      final LessonRecommendationService service = new com.aptisprep.service.learning.impl.LessonRecommendationServiceImpl(progress, accounts);

    PathProgress path(int id, String skill, int done) {
        return new PathProgress(
                id,
                skill,
                "GOAL",
                "ACTIVE",
                done,
                4,
                done * 25,
                done,
                4,
                id,
                skill + " lesson",
                "locked");
    }

    LessonProgress opened(int id, String skill, String status, int minutesAgo) {
        Lesson lesson = new Lesson();
        ReflectionTestUtils.setField(lesson, "id", id);
        lesson.setTitle(skill + " lesson");
        lesson.setSkill(skill);
        lesson.setStatus("PUBLISHED");
        LessonProgress value = new LessonProgress();
        value.setLesson(lesson);
        value.setStatus(status);
        value.setLastAccessedAt(LocalDateTime.now().minusMinutes(minutesAgo));
        return value;
    }

    @Test
    void resumesLatestUnfinishedEvenOutsideCurrentPaths() {
        var hidden = opened(8, "Reading", "IN_PROGRESS", 0);
        hidden.getLesson().setStatus("ARCHIVED");
        history =
                List.of(
                        opened(2, "Speaking", "IN_PROGRESS", 5),
                        opened(3, "Writing", "IN_PROGRESS", 1),
                        opened(4, "Reading", "COMPLETED", 0),
                        hidden);
        var result = service.recommend(1, List.of(path(1, "Grammar", 0)));
        assertEquals(3, result.lessonId());
        assertNull(result.path());
    }

    @Test
    void goalAndMeasuredGapComeBeforeStageProgress() {
        context =
                new LearnerContext(
                        1,
                        List.of(new SkillTarget("WRITING", "B2"), new SkillTarget("READING", "B2")),
                        List.of(
                                new SkillLevel("WRITING", "A1", 1),
                                new SkillLevel("READING", "B1", 1)));
        assertEquals(
                "Writing",
                service.recommend(
                                1,
                                List.of(
                                        path(1, "Grammar", 0),
                                        path(2, "Reading", 0),
                                        path(3, "Writing", 3)))
                        .skill());
    }

    @Test
    void withoutAssessmentUsesRealProgressThenLeastRecentlyVisited() {
        history = List.of(opened(8, "Reading", "COMPLETED", 1));
        assertEquals(
                "Writing",
                service.recommend(
                                1,
                                List.of(
                                        path(1, "Grammar", 3),
                                        path(2, "Reading", 1),
                                        path(3, "Writing", 1)))
                        .skill());
    }

    @Test
    void returnsNoLessonWhenNothingRemains() {
        assertNull(service.recommend(1, List.of()));
    }
}
