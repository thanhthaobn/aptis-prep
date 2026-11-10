package com.aptisprep.service.learning;

import static org.junit.jupiter.api.Assertions.*;

import com.aptisprep.mvc.account.dto.LearnerContextDTO;
import com.aptisprep.mvc.account.dto.SkillLevelDTO;
import com.aptisprep.mvc.account.dto.SkillTargetDTO;
import com.aptisprep.mvc.account.service.AccountAccessService;
import com.aptisprep.mvc.learning.dto.*;
import com.aptisprep.mvc.learning.dto.PathProgressDTO;
import com.aptisprep.mvc.learning.entity.*;
import com.aptisprep.mvc.learning.entity.Lesson;
import com.aptisprep.mvc.learning.entity.LessonProgress;
import com.aptisprep.mvc.learning.repository.*;
import com.aptisprep.mvc.learning.repository.LessonProgressRepository;
import com.aptisprep.mvc.learning.service.*;
import com.aptisprep.mvc.learning.service.LessonRecommendationService;
import com.aptisprep.mvc.learning.service.impl.*;
import com.aptisprep.service.contract.IntegrationModels.*;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LessonRecommendationTests {
    List<LessonProgress> history = List.of();
    LearnerContextDTO context;
    final LessonProgressRepository progress =
            (LessonProgressRepository)
                    java.lang.reflect.Proxy.newProxyInstance(
                            getClass().getClassLoader(),
                            new Class<?>[] {LessonProgressRepository.class},
                            (proxy, method, args) -> {
                                if (method.getName().equals("findByUserId")) return history;
                                throw new UnsupportedOperationException(method.getName());
                            });
    final AccountAccessService accounts =
            new AccountAccessService() {
                public Integer requireActiveStudent(String email) {
                    return 1;
                }

                public LearnerContextDTO context(Integer id) {
                    return context;
                }

                public void applyAssessment(Integer userId, Integer attemptId) {
                    throw new UnsupportedOperationException();
                }

                public com.aptisprep.mvc.auth.entity.User requireActiveStudentById(Integer userId) {
                    return null;
                }
            };
    final LessonRecommendationService service =
            new com.aptisprep.mvc.learning.service.impl.LessonRecommendationServiceImpl(
                    progress, accounts);

    PathProgressDTO path(int id, String skill, int done) {
        return new PathProgressDTO(
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
                new LearnerContextDTO(
                        1,
                        List.of(
                                new SkillTargetDTO("WRITING", "B2"),
                                new SkillTargetDTO("READING", "B2")),
                        List.of(
                                new SkillLevelDTO("WRITING", "A1", 1),
                                new SkillLevelDTO("READING", "B1", 1)));
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
