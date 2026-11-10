package com.aptisprep.service.account;

import static org.junit.jupiter.api.Assertions.*;

import com.aptisprep.mvc.auth.entity.User;
import com.aptisprep.mvc.auth.repository.UserRepository;
import com.aptisprep.mvc.common.enums.Role;
import com.aptisprep.mvc.common.enums.UserStatus;
import com.aptisprep.mvc.common.event.GoalChangedEvent;
import com.aptisprep.mvc.learning.entity.LearningPath;
import com.aptisprep.mvc.learning.entity.Lesson;
import com.aptisprep.mvc.learning.repository.LearningPathRepository;
import com.aptisprep.mvc.learning.repository.LessonRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
        properties =
                "spring.datasource.url=jdbc:h2:mem:goal_listener;MODE=MSSQLServer;DB_CLOSE_DELAY=-1")
class GoalPathListenerTests {
    @Autowired UserRepository userRepository;
    @Autowired LessonRepository lessonRepository;
    @Autowired LearningPathRepository learningPathRepository;
    @Autowired ApplicationEventPublisher events;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void afterCommitCreatesAndPersistsPathsAndRepeatedDeliveryKeepsTheSameStage() {
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        Integer userId =
                transactions.execute(
                        status -> {
                            User user = new User();
                            user.setEmail("goal-listener-" + UUID.randomUUID() + "@test.invalid");
                            user.setFullName("Goal listener regression");
                            user.setPasswordHash("unused");
                            user.setRole(Role.STUDENT);
                            user.setStatus(UserStatus.ACTIVE);
                            user = userRepository.saveAndFlush(user);
                            Lesson lesson = new Lesson();
                            lesson.setTitle("Goal listener reading");
                            lesson.setSkill("Reading");
                            lesson.setCefrLevel("A1");
                            lesson.setDisplayOrder(1);
                            lesson.setStatus("PUBLISHED");
                            lessonRepository.saveAndFlush(lesson);
                            events.publishEvent(new GoalChangedEvent(user.getId()));
                            return user.getId();
                        });
        LearningPath first =
                learningPathRepository
                        .findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(
                                userId, "Reading", "ACTIVE")
                        .orElseThrow();
        assertEquals("A1", first.getTargetLevel());
        transactions.executeWithoutResult(
                status -> events.publishEvent(new GoalChangedEvent(userId)));
        LearningPath repeated =
                learningPathRepository
                        .findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(
                                userId, "Reading", "ACTIVE")
                        .orElseThrow();
        assertEquals(first.getId(), repeated.getId());
    }
}
