package com.aptisprep.mvc.service.account;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aptisprep.mvc.dto.UserGoalDTO;
import com.aptisprep.mvc.dto.UserSkillProfileDTO;
import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.GoalStatus;
import com.aptisprep.mvc.repository.*;
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
                new com.aptisprep.mvc.service.account.impl.AccountProfileServiceImpl(
                        userRepository,
                        certificateRepository,
                        userGoalRepository,
                        userGoalSkillRepository,
                        userSkillProfileRepository,
                        access,
                        events);
        when(access.requireActiveStudent("snapshot@test.local")).thenReturn(7);
        User user = new User();
        user.setFullName("Snapshot learner");
        user.setEmail("snapshot@test.local");
        lenient().when(userRepository.findById(7)).thenReturn(Optional.of(user));
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
        assertEquals("Snapshot learner", service.profile("snapshot@test.local").getFullName());
        assertEquals("snapshot@test.local", service.profile("snapshot@test.local").getEmail());
        UserGoalDTO goalDto = service.goal("snapshot@test.local");
        assertNull(goalDto.getCertificateId());
        assertEquals(LocalDate.of(2027, 1, 1), goalDto.getTargetDate());
        assertEquals("B2", goalDto.getReading());
        assertEquals("C1", goalDto.getWriting());
        assertEquals(Map.of("READING", new BigDecimal("35.50")), goalDto.getTargetScores());
        UserSkillProfileDTO profileDto = service.skillProfiles("snapshot@test.local").getFirst();
        assertEquals("READING", profileDto.getSkill());
        assertEquals("A2", profileDto.getCurrentLevel());
        assertEquals(23, profileDto.getSourceAttemptId());
        verify(userGoalSkillRepository, times(1)).findByGoalId(17);
    }

    @Test
    void snapshotWithoutGoalOrProfilesRetainsItsEmptyState() {
        when(userGoalRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                        7, GoalStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(userSkillProfileRepository.findByUserIdOrderBySkillAsc(7)).thenReturn(List.of());
        assertNull(service.goal("snapshot@test.local"));
        assertTrue(service.skillProfiles("snapshot@test.local").isEmpty());
        verifyNoInteractions(userGoalSkillRepository);
    }
}










