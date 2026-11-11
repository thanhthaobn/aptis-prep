package com.aptisprep.mvc.learning.service;

import static org.assertj.core.api.Assertions.*;

import com.aptisprep.mvc.account.entity.UserGoal;
import com.aptisprep.mvc.account.entity.UserGoalSkill;
import com.aptisprep.mvc.account.repository.UserGoalRepository;
import com.aptisprep.mvc.auth.entity.User;
import com.aptisprep.mvc.auth.repository.UserRepository;
import com.aptisprep.mvc.common.enums.*;
import com.aptisprep.mvc.learning.dto.*;
import com.aptisprep.mvc.learning.dto.LearningPathViewsDTO.StageLessonDTO;
import com.aptisprep.mvc.learning.entity.*;
import com.aptisprep.mvc.learning.repository.*;
import com.aptisprep.mvc.learning.service.impl.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    TransactionalTestExecutionListener.class
})
class LearningPathRefreshTests {
    @Autowired LearningPathService service;
    @Autowired UserRepository users;
    @Autowired LessonRepository lessons;
    @Autowired LearningPathRepository paths;
    @Autowired LearningPathItemRepository items;
    @Autowired LessonProgressRepository progress;
    @Autowired UserGoalRepository goals;

    @Test
    void refreshEmptyPathThenPreserveCompletionAndHistory() {
        var user = new User();
        user.setFullName("Refresh learner");
        user.setEmail("refresh@test.local");
        user.setPasswordHash("unused");
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        users.saveAndFlush(user);
        service.ensurePaths(user.getId());
        assertThat(
                        paths.findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(
                                user.getId(), "Reading", "ACTIVE"))
                .isEmpty();
        var first = lesson("First", 901);
        assertThat(service.refreshPaths(user.getEmail())).isEqualTo(1);
        service.completeLesson(user.getId(), first.getId());
        var completedAt =
                progress.findByUserIdAndLessonId(user.getId(), first.getId())
                        .orElseThrow()
                        .getCompletedAt();
        var previous =
                paths.findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(
                                user.getId(), "Reading", "ACTIVE")
                        .orElseThrow();
        lesson("Second", 902);
        service.ensureAndSummaries(user.getEmail());
        assertThat(items.findByLearningPathIdOrderByPositionAsc(previous.getId())).hasSize(1);
        assertThat(
                        paths.findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(
                                        user.getId(), "Reading", "ACTIVE")
                                .orElseThrow()
                                .getId())
                .isNotEqualTo(previous.getId());
        assertThat(service.refreshPaths(user.getEmail())).isZero();
        assertThat(previous.getStatus()).isEqualTo("ARCHIVED");
        assertThat(items.findByLearningPathIdOrderByPositionAsc(previous.getId())).hasSize(1);
        var summary =
                service.summaries(user.getId()).stream()
                        .filter(p -> p.skill().equals("Reading"))
                        .findFirst()
                        .orElseThrow();
        assertThat(summary.completedLessons()).isEqualTo(1);
        assertThat(summary.totalLessons()).isEqualTo(2);
        assertThat(summary.progressPercent()).isEqualTo(50);
        assertThat(
                        progress.findByUserIdAndLessonId(user.getId(), first.getId())
                                .orElseThrow()
                                .getCompletedAt())
                .isEqualTo(completedAt);
        assertThat(service.refreshPaths(user.getEmail())).isZero();
    }

    @Test
    void openingPathReplacesArchivedAndWrongLevelAssignmentsWithoutLosingCompletion() {
        var user = new User();
        user.setFullName("Stale path learner");
        user.setEmail("stale-path@test.local");
        user.setPasswordHash("unused");
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        users.saveAndFlush(user);
        var kept = lesson("Keep A1", 921);
        var archived = lesson("Archived A1", 922);
        var moved = lesson("Moved to A2", 923);
        service.ensurePaths(user.getId());
        service.completeLesson(user.getId(), kept.getId());
        var old =
                paths.findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(
                                user.getId(), "Reading", "ACTIVE")
                        .orElseThrow();
        archived.setStatus("ARCHIVED");
        lessons.saveAndFlush(archived);
        moved.setCefrLevel("A2");
        lessons.saveAndFlush(moved);

        var summary =
                service.ensureAndSummaries(user.getEmail()).stream()
                        .filter(p -> p.skill().equals("Reading"))
                        .findFirst()
                        .orElseThrow();

        assertThat(old.getStatus()).isEqualTo("ARCHIVED");
        assertThat(items.findByLearningPathIdOrderByPositionAsc(old.getId())).hasSize(3);
        assertThat(summary.totalLessons()).isEqualTo(1);
        assertThat(summary.completedLessons()).isEqualTo(1);
        assertThat(service.stages(user.getEmail(), "Reading").getFirst().lessons())
                .extracting(StageLessonDTO::id)
                .containsExactly(kept.getId());
        assertThat(
                        progress.findByUserIdAndLessonId(user.getId(), kept.getId())
                                .orElseThrow()
                                .getCompletedAt())
                .isNotNull();
    }

    @Test
    void activeGoalCreatesGoalLinkedPathWithoutEmptyBranches() {
        var user = new User();
        user.setFullName("Goal learner");
        user.setEmail("goal-path@test.local");
        user.setPasswordHash("unused");
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        users.saveAndFlush(user);
        lesson("Goal A1", 911);
        var goal = new UserGoal();
        goal.setUser(user);
        goal.setStatus(GoalStatus.ACTIVE);
        var target = new UserGoalSkill();
        target.setSkill("READING");
        target.setTargetLevel("A1");
        goal.addSkill(target);
        goals.saveAndFlush(goal);

        service.ensurePaths(user.getId());

        var path =
                paths.findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(
                                user.getId(), "Reading", "ACTIVE")
                        .orElseThrow();
        assertThat(path.getPathSource()).isEqualTo("GOAL");
        assertThat(path.getGoalSkillId()).isEqualTo(target.getId());
        assertThat(path.getTargetLevel()).isEqualTo("A1");
        assertThat(items.findByLearningPathIdOrderByPositionAsc(path.getId()))
                .isNotEmpty()
                .allMatch(item -> item.getLesson().getCefrLevel().equals("A1"));
    }

    private Lesson lesson(String title, int order) {
        var lesson = new Lesson();
        lesson.setTitle(title);
        lesson.setSkill("Reading");
        lesson.setCefrLevel("A1");
        lesson.setDisplayOrder(order);
        lesson.setStatus("PUBLISHED");
        return lessons.saveAndFlush(lesson);
    }
}
