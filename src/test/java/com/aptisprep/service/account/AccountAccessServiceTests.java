package com.aptisprep.service.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.aptisprep.entity.User;
import com.aptisprep.entity.UserSkillProfile;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.repository.*;
import com.aptisprep.service.contract.ExamAttemptContract;
import com.aptisprep.service.contract.IntegrationModels.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class AccountAccessServiceTests {
    @Mock UserRepository users;
    @Mock UserGoalRepository goals;
    @Mock UserGoalSkillRepository goalSkills;
    @Mock UserSkillProfileRepository profiles;
    @Mock ProfileApplicationRepository applications;
    @Mock ObjectProvider<ExamAttemptContract> attemptProvider;
    @Mock ExamAttemptContract attempts;

    private User student;
    private UserSkillProfile profile;
    private AccountAccessService service;

    @BeforeEach
    void setUp() {
        student = new User();
        student.setId(1);
        student.setRole(Role.STUDENT);
        student.setStatus(UserStatus.ACTIVE);
        profile = new UserSkillProfile();
        profile.setUser(student);
        profile.setSkill("READING");
        service =
                  new com.aptisprep.service.account.impl.AccountAccessServiceImpl(
                        users, goals, goalSkills, profiles, applications, attemptProvider);
        lenient().when(users.findById(1)).thenReturn(Optional.of(student));
        when(users.findByIdForUpdate(1)).thenReturn(Optional.of(student));
        when(attemptProvider.getIfAvailable()).thenReturn(attempts);
        lenient()
                .when(profiles.findByUserIdAndSkill(1, "READING"))
                .thenReturn(Optional.of(profile));
        lenient()
                .when(
                        applications.existsByUserIdAndAttemptIdAndSkill(
                                anyInt(), anyInt(), anyString()))
                .thenReturn(false);
        lenient()
                .when(profiles.save(any(UserSkillProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient()
                .when(applications.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void olderPlacementCannotOverwriteNewerPlacement() {
        when(attempts.result(eq(1), eq(20)))
                .thenReturn(
                        result(
                                20,
                                "PLACEMENT",
                                "B2",
                                Instant.parse("2026-01-02T00:00:00Z"),
                                false));
        when(attempts.result(eq(1), eq(10)))
                .thenReturn(
                        result(
                                10,
                                "PLACEMENT",
                                "B1",
                                Instant.parse("2026-01-01T00:00:00Z"),
                                false));

        service.applyAssessment(1, 20);
        service.applyAssessment(1, 10);

        assertEquals("B2", profile.getCurrentLevel());
        assertEquals("B2", profile.getPlacementLevel());
        assertEquals(20, profile.getSourceAttemptId());
    }

    @Test
    void checkpointFloorProtectsLaterPlacementAndFailedCheckpointIsClaimed() {
        when(attempts.result(eq(1), eq(30)))
                .thenReturn(
                        result(
                                30,
                                "CHECKPOINT",
                                "B2",
                                Instant.parse("2026-01-02T00:00:00Z"),
                                true));
        when(attempts.result(eq(1), eq(40)))
                .thenReturn(
                        result(
                                40,
                                "CHECKPOINT",
                                "C1",
                                Instant.parse("2026-01-03T00:00:00Z"),
                                false));
        when(attempts.result(eq(1), eq(50)))
                .thenReturn(
                        result(
                                50,
                                "PLACEMENT",
                                "B1",
                                Instant.parse("2026-01-04T00:00:00Z"),
                                false));

        service.applyAssessment(1, 30);
        service.applyAssessment(1, 40);
        service.applyAssessment(1, 50);

        assertEquals("B2", profile.getCheckpointFloorLevel());
        assertEquals(30, profile.getCheckpointSourceAttemptId());
        assertEquals("B1", profile.getPlacementLevel());
        assertEquals(50, profile.getSourceAttemptId());
        assertEquals("B2", profile.getCurrentLevel());
        verify(applications, times(3)).save(any());
    }

    @Test
    void failedFirstCheckpointDoesNotCreateProfileWithNullCurrentLevel() {
        when(profiles.findByUserIdAndSkill(1, "READING")).thenReturn(Optional.empty());
        when(attempts.result(eq(1), eq(45)))
                .thenReturn(
                        result(
                                45,
                                "CHECKPOINT",
                                "B1",
                                Instant.parse("2026-01-05T00:00:00Z"),
                                false));

        service.applyAssessment(1, 45);

        verify(profiles, never()).save(any(UserSkillProfile.class));
        verify(applications).save(any());
    }

    @Test
    void alreadyClaimedAttemptDoesNotWriteProfileAgain() {
        when(applications.existsByUserIdAndAttemptIdAndSkill(1, 20, "READING")).thenReturn(true);
        when(attempts.result(eq(1), eq(20)))
                .thenReturn(
                        result(
                                20,
                                "PLACEMENT",
                                "B2",
                                Instant.parse("2026-01-02T00:00:00Z"),
                                false));

        service.applyAssessment(1, 20);

        verify(profiles, never()).save(any());
        verify(applications, never()).save(any());
    }

    @Test
    void olderHigherCheckpointRaisesMonotonicFloorWithoutLoweringPlacement() {
        when(attempts.result(eq(1), eq(60)))
                .thenReturn(
                        result(
                                60,
                                "CHECKPOINT",
                                "B1",
                                Instant.parse("2026-01-02T00:00:00Z"),
                                true));
        when(attempts.result(eq(1), eq(70)))
                .thenReturn(
                        result(
                                70,
                                "CHECKPOINT",
                                "B2",
                                Instant.parse("2026-01-01T00:00:00Z"),
                                true));

        service.applyAssessment(1, 60);
        service.applyAssessment(1, 70);

        assertEquals("B2", profile.getCheckpointFloorLevel());
        assertEquals(70, profile.getCheckpointSourceAttemptId());
        assertEquals("B2", profile.getCurrentLevel());
        assertEquals(60, profile.getCheckpointLastAttemptId());
        assertEquals(
                java.time.LocalDateTime.of(2026, 1, 2, 0, 0),
                profile.getCheckpointLastSubmittedAt());
    }

    private AttemptResult result(
            int attemptId, String type, String level, Instant submittedAt, boolean passed) {
        SkillResult skill =
                new SkillResult(
                        "READING",
                        new BigDecimal("30"),
                        new BigDecimal("30"),
                        level,
                        "COMPLETED",
                        "ok",
                        passed);
        return new AttemptResult(
                attemptId, 1, 100, type, 1, "COMPLETED", List.of(skill), submittedAt);
    }
}
