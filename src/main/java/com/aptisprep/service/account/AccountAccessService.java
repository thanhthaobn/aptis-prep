package com.aptisprep.service.account;

import com.aptisprep.entity.*;
import com.aptisprep.enums.*;
import com.aptisprep.exception.AccessDeniedException;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.*;
import com.aptisprep.service.contract.AccountAccessContract;
import com.aptisprep.service.contract.ExamAttemptContract;
import com.aptisprep.service.contract.IntegrationModels.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountAccessService implements AccountAccessContract {
    private final UserRepository users;
    private final UserGoalRepository goals;
    private final UserGoalSkillRepository goalSkills;
    private final UserSkillProfileRepository profiles;
    private final ProfileApplicationRepository applications;
    private final ObjectProvider<ExamAttemptContract> attempts;

    public AccountAccessService(
            UserRepository users,
            UserGoalRepository goals,
            UserGoalSkillRepository goalSkills,
            UserSkillProfileRepository profiles,
            ProfileApplicationRepository applications,
            ObjectProvider<ExamAttemptContract> attempts) {
        this.users = users;
        this.goals = goals;
        this.goalSkills = goalSkills;
        this.profiles = profiles;
        this.applications = applications;
        this.attempts = attempts;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer requireActiveStudent(String principalEmail) {
        User user =
                users.findByEmailIgnoreCase(normalizeEmail(principalEmail))
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (user.getRole() != Role.STUDENT || user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("Tài khoản không còn được phép truy cập");
        }
        return user.getId();
    }

    @Transactional(readOnly = true)
    public User requireActiveStudentById(Integer userId) {
        return activeStudent(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public LearnerContext context(Integer userId) {
        User user = activeStudent(userId);
        List<SkillTarget> targets =
                goals.findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                                user.getId(), GoalStatus.ACTIVE)
                        .map(
                                goal ->
                                        goalSkills.findByGoalId(goal.getId()).stream()
                                                .map(
                                                        skill ->
                                                                new SkillTarget(
                                                                        skill.getSkill(),
                                                                        skill.getTargetLevel()))
                                                .toList())
                        .orElseGet(List::of);
        List<SkillLevel> levels =
                profiles.findByUserIdOrderBySkillAsc(user.getId()).stream()
                        .map(
                                profile ->
                                        new SkillLevel(
                                                profile.getSkill(),
                                                profile.getCurrentLevel(),
                                                profile.getSourceAttemptId()))
                        .toList();
        return new LearnerContext(user.getId(), targets, levels);
    }

    @Override
    @Transactional
    public void applyAssessment(Integer userId, Integer attemptId) {
        User user = activeStudentForUpdate(userId);
        ExamAttemptContract attemptContract = attempts.getIfAvailable();
        if (attemptContract == null) {
            throw new BusinessRuleException("Chưa có dịch vụ kết quả bài thi để xác nhận hồ sơ");
        }
        AttemptResult result = attemptContract.result(userId, attemptId);
        if (result == null
                || !Objects.equals(result.userId(), userId)
                || !Objects.equals(result.attemptId(), attemptId)) {
            throw new AccessDeniedException("Không thể xác nhận kết quả bài thi");
        }
        if (!"COMPLETED".equalsIgnoreCase(result.state())
                && !"EXPIRED".equalsIgnoreCase(result.state())) {
            throw new BusinessRuleException("Kết quả bài thi chưa hoàn tất");
        }
        String type = Optional.ofNullable(result.examType()).orElse("").toUpperCase(Locale.ROOT);
        if (!type.equals("PLACEMENT") && !type.equals("CHECKPOINT")) {
            throw new BusinessRuleException(
                    "Chỉ Placement hoặc Checkpoint mới được cập nhật hồ sơ kỹ năng");
        }
        if (result.skills() == null || result.skills().isEmpty()) {
            throw new BusinessRuleException("Kết quả chưa có kỹ năng đủ điều kiện");
        }
        for (SkillResult skillResult : result.skills()) {
            applySkillResult(user, attemptId, type, result.submittedAt(), skillResult);
        }
    }

    private void applySkillResult(
            User user, Integer attemptId, String type, Instant submittedAt, SkillResult result) {
        String skill = normalizeSkill(result.skill());
        CefrLevel level = CefrLevel.parse(result.estimatedLevel());
        if (skill == null
                || level == null
                || result.scaleScore() == null
                || result.scaleScore().compareTo(BigDecimal.ZERO) < 0
                || result.scaleScore().compareTo(BigDecimal.valueOf(50)) > 0
                || !"COMPLETED".equalsIgnoreCase(result.gradingState())) {
            throw new BusinessRuleException("Kết quả kỹ năng chưa đủ điều kiện cập nhật hồ sơ");
        }
        if (applications.existsByUserIdAndAttemptIdAndSkill(user.getId(), attemptId, skill)) {
            return;
        }
        UserSkillProfile profile =
                profiles.findByUserIdAndSkill(user.getId(), skill)
                        .orElseGet(
                                () -> {
                                    UserSkillProfile created = new UserSkillProfile();
                                    created.setUser(user);
                                    created.setSkill(skill);
                                    return created;
                                });
        LocalDateTime submittedAtUtc =
                submittedAt == null ? null : LocalDateTime.ofInstant(submittedAt, ZoneOffset.UTC);
        if (type.equals("CHECKPOINT")) {
            boolean newerCheckpoint =
                    isNewer(
                            submittedAt,
                            attemptId,
                            profile.getCheckpointLastSubmittedAt(),
                            profile.getCheckpointLastAttemptId());
            if (newerCheckpoint) {
                profile.setCheckpointLastAttemptId(attemptId);
                profile.setCheckpointLastSubmittedAt(submittedAtUtc);
            }
            if (result.passed()) {
                CefrLevel floor = CefrLevel.parse(profile.getCheckpointFloorLevel());
                if (floor == null
                        || (level.atLeast(floor) && (level.higherThan(floor) || newerCheckpoint))) {
                    profile.setCheckpointFloorLevel(level.name());
                    profile.setCheckpointSourceAttemptId(attemptId);
                }
            }
        } else {
            if (isNewer(
                    submittedAt,
                    attemptId,
                    profile.getPlacementSubmittedAt(),
                    profile.getSourceAttemptId())) {
                profile.setPlacementLevel(level.name());
                profile.setPlacementSubmittedAt(submittedAtUtc);
                profile.setSourceAttemptId(attemptId);
            }
        }
        if (type.equals("CHECKPOINT") && !result.passed()) {
            // A failed first checkpoint is evidence of an attempt, not a CEFR
            // profile. user_skill_profiles.current_level is NOT NULL, so do
            // not persist a newly-created empty profile or invent A1.
            if (profile.getId() != null && profile.getCurrentLevel() != null) {
                profiles.save(profile);
            }
            saveApplication(user, attemptId, skill);
            return;
        }
        CefrLevel floor = CefrLevel.parse(profile.getCheckpointFloorLevel());
        CefrLevel placement = CefrLevel.parse(profile.getPlacementLevel());
        CefrLevel effective =
                floor != null && (placement == null || floor.atLeast(placement))
                        ? floor
                        : placement;
        if (effective == null) effective = level;
        profile.setCurrentLevel(effective.name());
        if (placement == null && profile.getCheckpointSourceAttemptId() != null) {
            profile.setSourceAttemptId(profile.getCheckpointSourceAttemptId());
        }
        profiles.save(profile);
        saveApplication(user, attemptId, skill);
    }

    private void saveApplication(User user, Integer attemptId, String skill) {
        ProfileApplication application = new ProfileApplication();
        application.setUserId(user.getId());
        application.setAttemptId(attemptId);
        application.setSkill(skill);
        applications.save(application);
    }

    private boolean isNewer(
            Instant incoming,
            Integer incomingAttemptId,
            LocalDateTime current,
            Integer currentAttemptId) {
        if (current == null) return true;
        if (incoming == null)
            return incomingAttemptId != null
                    && (currentAttemptId == null || incomingAttemptId > currentAttemptId);
        LocalDateTime incomingUtc = LocalDateTime.ofInstant(incoming, ZoneOffset.UTC);
        int time = incomingUtc.compareTo(current);
        return time > 0
                || (time == 0
                        && incomingAttemptId != null
                        && (currentAttemptId == null || incomingAttemptId > currentAttemptId));
    }

    private User activeStudent(Integer userId) {
        User user =
                users.findById(userId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (user.getRole() != Role.STUDENT || user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("Tài khoản không còn được phép truy cập");
        }
        return user;
    }

    private User activeStudentForUpdate(Integer userId) {
        User user =
                users.findByIdForUpdate(userId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (user.getRole() != Role.STUDENT || user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("Tài khoản không còn được phép truy cập");
        }
        return user;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeSkill(String value) {
        if (value == null) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        return normalized.equals("GRAMMAR_VOCABULARY")
                ? "GRAMMAR_VOCABULARY"
                : Arrays.stream(Skill.values()).map(Enum::name).anyMatch(normalized::equals)
                        ? normalized
                        : null;
    }
}
