package com.aptisprep.service.learning;

import com.aptisprep.entity.*;
import com.aptisprep.enums.*;
import com.aptisprep.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
class LearningPathRefreshTests {
    @Autowired LearningPathService service;
    @Autowired UserRepository users;
    @Autowired LessonRepository lessons;
    @Autowired LearningPathRepository paths;
    @Autowired LearningPathItemRepository items;
    @Autowired LessonProgressRepository progress;

    @Test void refreshEmptyPathThenPreserveCompletionAndHistory() {
        var user = new User();
        user.setFullName("Refresh learner"); user.setEmail("refresh@test.local");
        user.setPasswordHash("unused"); user.setRole(Role.STUDENT); user.setStatus(UserStatus.ACTIVE);
        users.saveAndFlush(user);
        service.ensurePaths(user.getId());
        var original = paths.findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(user.getId(), "Reading", "ACTIVE").orElseThrow();
        var first = lesson("First", 901);
        assertThat(service.refreshPaths(user.getEmail())).isEqualTo(1);
        assertThat(original.getStatus()).isEqualTo("ARCHIVED");
        service.completeLesson(user.getId(), first.getId());
        var completedAt = progress.findByUserIdAndLessonId(user.getId(), first.getId()).orElseThrow().getCompletedAt();
        var previous = paths.findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(user.getId(), "Reading", "ACTIVE").orElseThrow();
        lesson("Second", 902);
        assertThat(service.refreshPaths(user.getEmail())).isEqualTo(1);
        assertThat(previous.getStatus()).isEqualTo("ARCHIVED");
        assertThat(items.findByLearningPathIdOrderByPositionAsc(previous.getId())).hasSize(1);
        var summary = service.summaries(user.getId()).stream().filter(p -> p.skill().equals("Reading")).findFirst().orElseThrow();
        assertThat(summary.completedLessons()).isEqualTo(1);
        assertThat(summary.totalLessons()).isEqualTo(2);
        assertThat(summary.progressPercent()).isEqualTo(50);
        assertThat(progress.findByUserIdAndLessonId(user.getId(), first.getId()).orElseThrow().getCompletedAt()).isEqualTo(completedAt);
        assertThat(service.refreshPaths(user.getEmail())).isZero();
    }

    private Lesson lesson(String title, int order) {
        var lesson = new Lesson(); lesson.setTitle(title); lesson.setSkill("Reading");
        lesson.setCefrLevel("A1"); lesson.setDisplayOrder(order); lesson.setStatus("PUBLISHED");
        return lessons.saveAndFlush(lesson);
    }
}
