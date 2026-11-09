package com.aptisprep.service.account;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aptisprep.dto.view.AccountSnapshot;
import com.aptisprep.entity.*;
import com.aptisprep.enums.GoalStatus;
import com.aptisprep.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AccountProfileServiceTests {
    @Mock UserRepository userRepository;
    @Mock CertificateRepository certificateRepository;
    @Mock UserGoalRepository userGoalRepository;
    @Mock UserGoalSkillRepository userGoalSkillRepository;
    @Mock UserSkillProfileRepository userSkillProfileRepository;
    @Mock AccountAccessService access;
    @Mock ApplicationEventPublisher events;
    AccountProfileService service;

    @BeforeEach
    void setup() {
        service =
                  new com.aptisprep.service.account.impl.AccountProfileServiceImpl(
                        userRepository,
                        certificateRepository,
                        userGoalRepository,
                        userGoalSkillRepository,
                        userSkillProfileRepository,
                        access,
                        events);
        User user = new User();
        user.setFullName("Snapshot learner");
        user.setEmail("snapshot@test.local");
        when(access.requireActiveStudent(user.getEmail())).thenReturn(7);
        when(userRepository.findById(7)).thenReturn(Optional.of(user));
    }

    @Test
    void snapshotPreservesTargetsScoresAndProfilesWithOneGoalSkillRead() {
        UserGoal goal = mock(UserGoal.class);
        when(goal.getId()).thenReturn(17);
        when(goal.getTargetDate()).thenReturn(LocalDate.of(2027, 1, 1));
        when(userGoalRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                        7, GoalStatus.ACTIVE))
                .thenReturn(Optional.of(goal));
        UserGoalSkill reading = new UserGoalSkill();
        reading.setSkill("READING");
        reading.setTargetLevel("B2");
        reading.setTargetScore(new BigDecimal("35.50"));
        UserGoalSkill writing = new UserGoalSkill();
        writing.setSkill("WRITING");
        writing.setTargetLevel("C1");
        when(userGoalSkillRepository.findByGoalId(17)).thenReturn(List.of(reading, writing));
        UserSkillProfile profile = new UserSkillProfile();
        profile.setSkill("READING");
        profile.setCurrentLevel("A2");
        profile.setSourceAttemptId(23);
        when(userSkillProfileRepository.findByUserIdOrderBySkillAsc(7))
                .thenReturn(List.of(profile));
        AccountSnapshot snapshot = service.snapshot("snapshot@test.local");
        assertEquals("Snapshot learner", snapshot.fullName());
        assertEquals("snapshot@test.local", snapshot.email());
        assertNull(snapshot.goal().certificateId());
        assertEquals(LocalDate.of(2027, 1, 1), snapshot.goal().targetDate());
        assertEquals(
                List.of("READING", "WRITING"), new ArrayList<>(snapshot.goal().targets().keySet()));
        assertEquals(Map.of("READING", "B2", "WRITING", "C1"), snapshot.goal().targets());
        assertEquals(Map.of("READING", new BigDecimal("35.50")), snapshot.goal().targetScores());
        assertEquals(
                new AccountSnapshot.SkillView("READING", "A2", 23), snapshot.profiles().getFirst());
        verify(userGoalSkillRepository, times(1)).findByGoalId(17);
    }

    @Test
    void snapshotWithoutGoalOrProfilesRetainsItsEmptyState() {
        when(userGoalRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                        7, GoalStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(userSkillProfileRepository.findByUserIdOrderBySkillAsc(7)).thenReturn(List.of());
        AccountSnapshot snapshot = service.snapshot("snapshot@test.local");
        assertNull(snapshot.goal());
        assertTrue(snapshot.profiles().isEmpty());
        verifyNoInteractions(userGoalSkillRepository);
    }
}
