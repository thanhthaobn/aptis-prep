package com.aptisprep.service.learning;

import static com.aptisprep.mvc.common.security.SessionMvcRequests.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aptisprep.mvc.account.entity.UserGoal;
import com.aptisprep.mvc.account.entity.UserGoalSkill;
import com.aptisprep.mvc.account.entity.UserSkillProfile;
import com.aptisprep.mvc.account.repository.UserGoalRepository;
import com.aptisprep.mvc.account.repository.UserSkillProfileRepository;
import com.aptisprep.mvc.auth.entity.User;
import com.aptisprep.mvc.auth.repository.UserRepository;
import com.aptisprep.mvc.catalog.entity.*;
import com.aptisprep.mvc.catalog.repository.*;
import com.aptisprep.mvc.common.enums.*;
import com.aptisprep.mvc.common.exception.*;
import com.aptisprep.mvc.exam.service.ExamAttemptService;
import com.aptisprep.mvc.learning.dto.*;
import com.aptisprep.mvc.learning.dto.LearningPathViewsDTO.StageLessonDTO;
import com.aptisprep.mvc.learning.entity.*;
import com.aptisprep.mvc.learning.repository.*;
import com.aptisprep.mvc.learning.service.*;
import com.aptisprep.mvc.learning.service.impl.*;
import com.aptisprep.service.contract.IntegrationModels.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Real paths, progress and Profile services; the attempt engine is an explicit boundary stub. */
@SpringBootTest(
        properties =
                "spring.datasource.url=jdbc:h2:mem:thao_checkpoint;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
class LearningPathCheckpointTests {
    @Autowired LearningPathService service;
    @Autowired UserRepository users;
    @Autowired UserGoalRepository goals;
    @Autowired UserSkillProfileRepository profiles;
    @Autowired LessonRepository lessons;
    @Autowired LearningPathRepository paths;
    @Autowired LearningPathItemRepository items;
    @Autowired MockExamRepository exams;
    @Autowired MockExamQuestionRepository links;
    @Autowired QuestionRepository questions;
    @Autowired MockMvc mvc;
    @Autowired org.springframework.context.ApplicationEventPublisher events;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @MockitoBean ExamAttemptService engine;

    @BeforeEach
    void curriculum() {
        for (String level : List.of("A1", "A2", "B1", "B2", "C1"))
            if (lessons.findAll().stream()
                    .noneMatch(lesson -> lesson.getTitle().equals("Checkpoint " + level))) {
                var lesson = new Lesson();
                lesson.setTitle("Checkpoint " + level);
                lesson.setSkill("Reading");
                lesson.setCefrLevel(level);
                lesson.setDisplayOrder(800 + List.of("A1", "A2", "B1", "B2", "C1").indexOf(level));
                lesson.setStatus("PUBLISHED");
                lessons.saveAndFlush(lesson);
            }
    }

    @Test
    void placementB1AndGoalB2AssignOnlyB2WithGoalLinkEvenWithoutSourceAttempt() {
        var user = learner("B1");
        var goal = goal(user, "B2");
        service.ensurePaths(user.getId());
        var path = current(user);
        var stage = service.stages(user.getEmail(), "Reading").getFirst();
        assertThat(path.getFromLevel()).isEqualTo("B1");
        assertThat(path.getTargetLevel()).isEqualTo("B2");
        assertThat(path.getGoalSkillId()).isEqualTo(goal.getSkills().getFirst().getId());
        assertThat(stage.lessons()).extracting(StageLessonDTO::level).containsOnly("B2");
        assertThat(path.getSourceAttemptId()).isNull();
        assertThat(
                        profiles.findByUserIdAndSkill(user.getId(), "READING")
                                .orElseThrow()
                                .getCurrentLevel())
                .isEqualTo("B1");
    }

    @Test
    void stagesAndPathPageSupportLessonsWithoutMaterial() throws Exception {
        var user = learner("B1");
        goal(user, "B2");
        service.ensurePaths(user.getId());
        var stage = service.stages(user.getEmail(), "Reading").getFirst();
        assertThat(stage.lessons())
                .anySatisfy(
                        lesson -> {
                            assertThat(lesson.title()).isEqualTo("Checkpoint B2");
                            assertThat(lesson.topic()).isNull();
                            assertThat(lesson.available()).isTrue();
                        });
        mvc.perform(get("/student/learning-path").with(user(user.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk());
    }

    @Test
    void noGoalStartsAtA1AndCanPassCheckpointToReachA2() {
        var user = learner(null);
        service.ensurePaths(user.getId());
        var foundation = current(user);
        assertThat(foundation.getTargetLevel()).isEqualTo("A1");
        assertThat(service.stages(user.getEmail(), "Reading").getFirst().goalLevel()).isNull();
        assertThat(service.stages(user.getEmail(), "Reading").getFirst().lessons())
                .extracting(StageLessonDTO::level)
                .containsOnly("A1");
        assertThat(service.summary(user.getId(), foundation.getId()).checkpointState())
                .isEqualTo("locked");
        assertThatThrownBy(
                        () -> service.startCheckpoint(user.getId(), foundation.getId(), "ignored"))
                .isInstanceOf(BusinessRuleException.class);
        complete(user, foundation);
        var exam = checkpoint("A1");
        assertThat(service.summary(user.getId(), foundation.getId()).checkpointState())
                .isEqualTo("available");
        start(user, foundation, exam, 1010, "IN_PROGRESS");
        when(engine.result(user.getId(), 1010))
                .thenReturn(result(user, exam, 1010, "COMPLETED", "A1", true));
        service.applyCheckpoint(user.getId(), 1010);
        assertThat(paths.findById(foundation.getId()).orElseThrow().getStatus())
                .isEqualTo("COMPLETED");
        assertThat(current(user).getTargetLevel()).isEqualTo("A2");
        assertThat(
                        profiles.findByUserIdAndSkill(user.getId(), "READING")
                                .orElseThrow()
                                .getCurrentLevel())
                .isEqualTo("A1");
        assertThat(
                        goals.findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                                user.getId(), GoalStatus.ACTIVE))
                .isEmpty();
    }

    @Test
    void failedFirstCheckpointWithoutGoalDoesNotCreateAnAssessedLevel() {
        var user = learner(null);
        service.ensurePaths(user.getId());
        var path = current(user);
        complete(user, path);
        var exam = checkpoint("A1");
        start(user, path, exam, 1011, "IN_PROGRESS");
        var failed = result(user, exam, 1011, "COMPLETED", "A1", false);
        when(engine.result(user.getId(), 1011)).thenReturn(failed);
        when(engine.history(eq(user.getId()), eq(0), eq(100), any()))
                .thenReturn(new HistoryPage(List.of(failed), 0, 100, 1));
        service.applyCheckpoint(user.getId(), 1011);
        assertThat(current(user).getId()).isEqualTo(path.getId());
        assertThat(profiles.findByUserIdAndSkill(user.getId(), "READING")).isEmpty();
        assertThat(service.summary(user.getId(), path.getId()).checkpointState())
                .isEqualTo("failed");
        start(user, path, exam, 1012, "IN_PROGRESS");
    }

    @Test
    void passedCheckpointClosesStageAndOpensNextOnlyOnceUsingProductionTransaction() {
        var user = learner(null);
        goal(user, "B1");
        service.ensurePaths(user.getId());
        var first = current(user);
        complete(user, first);
        var exam = checkpoint("A1");
        start(user, first, exam, 1001, "IN_PROGRESS");
        when(engine.result(user.getId(), 1001))
                .thenReturn(result(user, exam, 1001, "COMPLETED", "A1", true));
        var tx =
                new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        tx.executeWithoutResult(
                status ->
                        events.publishEvent(
                                new com.aptisprep.mvc.common.event.AttemptCompletedEvent(
                                        user.getId(), 1001, "CHECKPOINT")));
        tx.executeWithoutResult(
                status ->
                        events.publishEvent(
                                new com.aptisprep.mvc.common.event.AttemptCompletedEvent(
                                        user.getId(), 1001, "CHECKPOINT")));
        var closed = paths.findById(first.getId()).orElseThrow();
        assertThat(closed.getStatus()).isEqualTo("COMPLETED");
        assertThat(closed.getCompletedAt()).isNotNull();
        assertThat(closed.getSourceAttemptId()).isEqualTo(1001);
        assertThat(current(user).getFromLevel()).isEqualTo("A1");
        assertThat(current(user).getTargetLevel()).isEqualTo("A2");
        assertThat(
                        profiles.findByUserIdAndSkill(user.getId(), "READING")
                                .orElseThrow()
                                .getCurrentLevel())
                .isEqualTo("A1");
        verify(engine, times(2))
                .result(user.getId(), 1001); // validation + Profile service, no second application
        service.ensurePaths(user.getId());
        assertThat(paths.findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(user.getId(), "ACTIVE"))
                .hasSize(1);
    }

    @Test
    void goalReachedKeepsCompletedStageVisibleWithoutOpeningAnotherStage() {
        var user = learner(null);
        goal(user, "A1");
        service.ensurePaths(user.getId());
        var path = current(user);
        complete(user, path);
        var exam = checkpoint("A1");
        start(user, path, exam, 1002, "IN_PROGRESS");
        when(engine.result(user.getId(), 1002))
                .thenReturn(result(user, exam, 1002, "COMPLETED", "A1", true));
        service.applyCheckpoint(user.getId(), 1002);
        service.ensurePaths(user.getId());
        assertThat(paths.findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(user.getId(), "ACTIVE"))
                .isEmpty();
        assertThat(service.summaries(user.getId()))
                .singleElement()
                .satisfies(
                        summary -> {
                            assertThat(summary.pathStatus()).isEqualTo("COMPLETED");
                            assertThat(summary.checkpointState()).isEqualTo("passed");
                        });
    }

    @Test
    void failedCheckpointPreservesLevelAndCompletionAndCanStartANewAttempt() {
        var user = learner("A1");
        goal(user, "B1");
        service.ensurePaths(user.getId());
        var path = current(user);
        complete(user, path);
        var exam = checkpoint("A2");
        start(user, path, exam, 1003, "IN_PROGRESS");
        var failed = result(user, exam, 1003, "COMPLETED", "A1", false);
        when(engine.result(user.getId(), 1003)).thenReturn(failed);
        service.applyCheckpoint(user.getId(), 1003);
        assertThat(paths.findById(path.getId()).orElseThrow().getStatus()).isEqualTo("ACTIVE");
        assertThat(
                        profiles.findByUserIdAndSkill(user.getId(), "READING")
                                .orElseThrow()
                                .getCurrentLevel())
                .isEqualTo("A1");
        assertThat(service.summary(user.getId(), path.getId()).completedLessons()).isEqualTo(1);
        when(engine.history(eq(user.getId()), eq(0), eq(100), any()))
                .thenReturn(new HistoryPage(List.of(failed), 0, 100, 1));
        when(engine.startCheckpoint(
                        eq(user.getId()), eq(exam.getId()), eq(path.getId()), anyString()))
                .thenReturn(new AttemptHandle(1004, "IN_PROGRESS", null));
        assertThat(service.startCheckpoint(user.getId(), path.getId(), "client-key"))
                .isEqualTo(1004);
        verify(engine)
                .startCheckpoint(
                        user.getId(),
                        exam.getId(),
                        path.getId(),
                        "checkpoint:" + user.getId() + ":" + path.getId() + ":after:1003");
    }

    @Test
    void pendingAndWrongSkillResultsCannotAdvanceAPath() {
        var user = learner("A1");
        goal(user, "B1");
        service.ensurePaths(user.getId());
        var path = current(user);
        complete(user, path);
        var exam = checkpoint("A2");
        start(user, path, exam, 1005, "IN_PROGRESS");
        when(engine.result(user.getId(), 1005))
                .thenReturn(result(user, exam, 1005, "WAITING_GRADING", null, false));
        assertThatThrownBy(() -> service.applyCheckpoint(user.getId(), 1005))
                .isInstanceOf(BusinessRuleException.class);
        when(engine.result(user.getId(), 1005))
                .thenReturn(
                        new AttemptResult(
                                1005,
                                user.getId(),
                                exam.getId(),
                                "CHECKPOINT",
                                "Wrong skill",
                                1,
                                "COMPLETED",
                                List.of(
                                        new SkillResult(
                                                "LISTENING",
                                                BigDecimal.TEN,
                                                BigDecimal.TEN,
                                                "A2",
                                                "COMPLETED",
                                                null,
                                                true)),
                                null,
                                null,
                                afterStageCreated(user)));
        assertThatThrownBy(() -> service.applyCheckpoint(user.getId(), 1005))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(current(user).getId()).isEqualTo(path.getId());
        assertThat(
                        profiles.findByUserIdAndSkill(user.getId(), "READING")
                                .orElseThrow()
                                .getCurrentLevel())
                .isEqualTo("A1");
    }

    @Test
    void checkpointRedirectResumesUnsubmittedAttemptAndShowsPendingResult() throws Exception {
        var user = learner("A1");
        goal(user, "B1");
        service.ensurePaths(user.getId());
        var path = current(user);
        complete(user, path);
        var exam = checkpoint("A2");
        start(user, path, exam, 1006, "IN_PROGRESS");
        mvc.perform(
                        post("/student/learning-path/" + path.getId() + "/checkpoint/start")
                                .with(user(user.getEmail()).roles("STUDENT"))
                                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/attempts/1006"));
        var pending = result(user, exam, 1006, "WAITING_GRADING", null, false);
        when(engine.history(eq(user.getId()), eq(0), eq(100), any()))
                .thenReturn(new HistoryPage(List.of(pending), 0, 100, 1));
        when(engine.result(user.getId(), 1006)).thenReturn(pending);
        mvc.perform(
                        post("/student/learning-path/" + path.getId() + "/checkpoint/start")
                                .with(user(user.getEmail()).roles("STUDENT"))
                                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/attempts/1006/result"));
        verify(engine, times(2))
                .startCheckpoint(
                        anyInt(),
                        anyInt(),
                        anyInt(),
                        anyString()); // helper + first POST; pending does not start again
        mvc.perform(
                        get("/student/learning-path/Reading")
                                .with(user(user.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("đang chờ chấm")));
    }

    @Test
    void emptyRequiredSetAndOtherOwnersCannotStartCheckpoint() {
        var user = learner("A1");
        var other = learner(null);
        goal(user, "B1");
        service.ensurePaths(user.getId());
        var path = current(user);
        assertThatThrownBy(() -> service.startCheckpoint(other.getId(), path.getId(), "x"))
                .isInstanceOf(ResourceNotFoundException.class);
        var empty = new LearningPath();
        empty.setUser(user);
        empty.setSkill("Writing");
        empty.setTargetLevel("A2");
        empty.setStatus("ACTIVE");
        empty.setPathSource("GOAL");
        empty = paths.saveAndFlush(empty);
        int id = empty.getId();
        assertThatThrownBy(() -> service.startCheckpoint(user.getId(), id, "x"))
                .isInstanceOf(BusinessRuleException.class);
        verify(engine, never()).startCheckpoint(anyInt(), anyInt(), anyInt(), anyString());
    }

    @Test
    void simultaneousPathCreationStillHasOneActiveStage() throws Exception {
        var user = learner(null);
        goal(user, "B1");
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var tasks =
                    pool.invokeAll(
                            List.<java.util.concurrent.Callable<Void>>of(
                                    () -> {
                                        service.ensurePaths(user.getId());
                                        return null;
                                    },
                                    () -> {
                                        service.ensurePaths(user.getId());
                                        return null;
                                    }));
            for (var task : tasks) task.get();
            assertThat(
                            paths.findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(
                                    user.getId(), "ACTIVE"))
                    .hasSize(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void c1AndLegacyC2ProfilesDoNotCreateC2StagesOrLoseTheirLevel() {
        for (String level : List.of("C1", "C2")) {
            var user = learner(level);
            goal(user, "C2");
            service.ensurePaths(user.getId());
            assertThat(
                            paths.findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(
                                    user.getId(), "Reading", "ACTIVE"))
                    .isEmpty();
            assertThat(
                            profiles.findByUserIdAndSkill(user.getId(), "READING")
                                    .orElseThrow()
                                    .getCurrentLevel())
                    .isEqualTo(level);
        }
    }

    @Test
    void b2ProfileWithLegacyC2GoalStillGetsOnlyC1Content() {
        var user = learner("B2");
        goal(user, "C2");
        service.ensurePaths(user.getId());
        assertThat(current(user).getTargetLevel()).isEqualTo("C1");
        assertThat(service.stages(user.getEmail(), "Reading").getFirst().lessons())
                .extracting(StageLessonDTO::level)
                .containsOnly("C1");
    }

    private User learner(String level) {
        var user = new User();
        user.setFullName("Checkpoint learner");
        user.setEmail("cp-" + UUID.randomUUID() + "@test.local");
        user.setPasswordHash("unused");
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        user = users.saveAndFlush(user);
        if (level != null) {
            var profile = new UserSkillProfile();
            profile.setUser(user);
            profile.setSkill("READING");
            profile.setCurrentLevel(level);
            profile.setPlacementLevel(level);
            profiles.saveAndFlush(profile);
        }
        return user;
    }

    private UserGoal goal(User user, String level) {
        var goal = new UserGoal();
        goal.setUser(user);
        goal.setStatus(GoalStatus.ACTIVE);
        var target = new UserGoalSkill();
        target.setSkill("READING");
        target.setTargetLevel(level);
        goal.addSkill(target);
        return goals.saveAndFlush(goal);
    }

    private LearningPath current(User user) {
        return paths.findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(
                        user.getId(), "Reading", "ACTIVE")
                .orElseThrow();
    }

    private void complete(User user, LearningPath path) {
        service.stages(user.getEmail(), "Reading").stream()
                .filter(stage -> stage.pathId().equals(path.getId()))
                .flatMap(stage -> stage.lessons().stream())
                .forEach(lesson -> service.completeLesson(user.getId(), lesson.id()));
    }

    private MockExamEntity checkpoint(String target) {
        var exam = new MockExamEntity();
        exam.setTitle("Checkpoint " + UUID.randomUUID());
        exam.setExamType("CHECKPOINT");
        exam.setSkill("READING");
        exam.setTargetLevel(target);
        exam.setCertificateId(1);
        exam.setScoringSchemeId(1);
        exam.setDurationMinutes(10);
        exam.setStatus("PUBLISHED");
        exam = exams.saveAndFlush(exam);
        var question = new QuestionEntity();
        question.setContent("Checkpoint");
        question.setQuestionType("SINGLE_CHOICE");
        question.setSkill("READING");
        question.setStatus("PUBLISHED");
        question = questions.saveAndFlush(question);
        var link = new MockExamQuestionEntity();
        link.setMockExamId(exam.getId());
        link.setQuestionId(question.getId());
        link.setPosition(1);
        links.saveAndFlush(link);
        return exam;
    }

    private void start(User user, LearningPath path, MockExamEntity exam, int id, String state) {
        when(engine.startCheckpoint(
                        eq(user.getId()), eq(exam.getId()), eq(path.getId()), anyString()))
                .thenReturn(new AttemptHandle(id, state, null));
        when(engine.result(user.getId(), id))
                .thenReturn(
                        new AttemptResult(
                                id, user.getId(), exam.getId(), "CHECKPOINT", 1, state, List.of()));
        assertThat(service.startCheckpoint(user.getId(), path.getId(), "ignored")).isEqualTo(id);
    }

    private AttemptResult result(
            User user, MockExamEntity exam, int id, String state, String level, boolean passed) {
        var skill =
                new SkillResult(
                        "READING",
                        level == null ? null : BigDecimal.valueOf(40),
                        level == null ? null : BigDecimal.valueOf(40),
                        level,
                        level == null ? "PENDING" : "COMPLETED",
                        null,
                        passed);
        return new AttemptResult(
                id,
                user.getId(),
                exam.getId(),
                "CHECKPOINT",
                exam.getTitle(),
                1,
                state,
                List.of(skill),
                null,
                null,
                afterStageCreated(user));
    }

    private Instant afterStageCreated(User user) {
        // The stub must represent a submission after this persisted stage, regardless of
        // OS clock adjustments or database timestamp precision during a fast test.
        return current(user)
                .getCreatedAt()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .plusSeconds(1);
    }
}
