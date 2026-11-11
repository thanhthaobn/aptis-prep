package com.aptisprep.service.account.impl;

import com.aptisprep.dto.view.AttemptResultDTO;
import com.aptisprep.dto.view.GoalSkillDTO;
import com.aptisprep.dto.view.LearnerContextDTO;
import com.aptisprep.dto.view.SkillLevelDTO;
import com.aptisprep.dto.view.SkillResultDTO;
import com.aptisprep.dto.view.SkillTargetDTO;
import com.aptisprep.entity.ProfileApplication;
import com.aptisprep.entity.User;
import com.aptisprep.entity.UserSkillProfile;
import com.aptisprep.enums.*;
import com.aptisprep.exception.AccessDeniedException;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.ProfileApplicationRepository;
import com.aptisprep.repository.UserGoalRepository;
import com.aptisprep.repository.UserGoalSkillRepository;
import com.aptisprep.repository.UserSkillProfileRepository;
import com.aptisprep.service.account.AccountAccessService;
import com.aptisprep.service.account.UserService;
import com.aptisprep.service.exam.ExamAttemptService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountAccessServiceImpl implements AccountAccessService {
    private final UserService userRepository;
    private final UserGoalRepository userGoalRepository;
    private final UserGoalSkillRepository userGoalSkillRepository;
    private final UserSkillProfileRepository userSkillProfileRepository;
    private final ProfileApplicationRepository profileApplicationRepository;
    private final ObjectProvider<ExamAttemptService> attempts;

    public AccountAccessServiceImpl(
            UserService userRepository,
            UserGoalRepository userGoalRepository,
            UserGoalSkillRepository userGoalSkillRepository,
            UserSkillProfileRepository userSkillProfileRepository,
            ProfileApplicationRepository profileApplicationRepository,
            ObjectProvider<ExamAttemptService> attempts) {
        this.userRepository = userRepository;
        this.userGoalRepository = userGoalRepository;
        this.userGoalSkillRepository = userGoalSkillRepository;
        this.userSkillProfileRepository = userSkillProfileRepository;
        this.profileApplicationRepository = profileApplicationRepository;
        this.attempts = attempts;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer requireActiveStudent(String principalEmail) {
        User user =
                userRepository
                        .findByEmailIgnoreCase(normalizeEmail(principalEmail))
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
    public LearnerContextDTO context(Integer userId) {
        User user = activeStudent(userId);
        List<SkillTargetDTO> targets =
                userGoalRepository
                        .findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                                user.getId(), GoalStatus.ACTIVE)
                        .map(
                                goal ->
                                        userGoalSkillRepository.findByGoalId(goal.getId()).stream()
                                                .map(
                                                        skill ->
                                                                new SkillTargetDTO(
                                                                        skill.getSkill(),
                                                                        skill.getTargetLevel()))
                                                .toList())
                        .orElseGet(List::of);
        List<SkillLevelDTO> levels =
                userSkillProfileRepository.findByUserIdOrderBySkillAsc(user.getId()).stream()
                        .map(
                                profile ->
                                        new SkillLevelDTO(
                                                profile.getSkill(),
                                                profile.getCurrentLevel(),
                                                profile.getSourceAttemptId()))
                        .toList();
        return new LearnerContextDTO(user.getId(), targets, levels);
    }

    @Override
    @Transactional
    public void applyAssessment(Integer userId, Integer attemptId) {
        User user = activeStudentForUpdate(userId);
        ExamAttemptService attemptContract = attempts.getIfAvailable();
        if (attemptContract == null) {
            throw new BusinessRuleException("Chưa có dịch vụ kết quả bài thi để xác nhận hồ sơ");
        }
        AttemptResultDTO result = attemptContract.result(userId, attemptId);
        if (result == null
                || !Objects.equals(result.userId(), userId)
                || !Objects.equals(result.attemptId(), attemptId)) {
            throw new AccessDeniedException("Không thể xác nhận kết quả bài thi");
        }
        if (!"COMPLETED".equalsIgnoreCase(result.state())
                && !"EXPIRED".equalsIgnoreCase(result.state())
                && !"ERROR".equalsIgnoreCase(result.state())) {
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
        boolean applied = false;
        for (SkillResultDTO skillResult : result.skills()) {
            if (!"COMPLETED".equalsIgnoreCase(skillResult.gradingState())
                    || skillResult.scaleScore() == null
                    || skillResult.estimatedLevel() == null) continue;
            applied |= applySkillResult(user, attemptId, type, result.submittedAt(), skillResult);
        }
        if (!applied && !"ERROR".equalsIgnoreCase(result.state()))
            throw new BusinessRuleException("Kết quả chưa có kỹ năng đủ điều kiện");
    }

    private boolean applySkillResult(
            User user, Integer attemptId, String type, Instant submittedAt, SkillResultDTO result) {
        String skill = normalizeSkill(result.skill());
        boolean belowA1 = CefrLevel.BELOW_A1.equals(result.estimatedLevel());
        CefrLevel level = CefrLevel.parse(result.estimatedLevel());
        if (skill == null
                || (!belowA1 && level == null)
                || result.scaleScore() == null
                || result.scaleScore().compareTo(BigDecimal.ZERO) < 0
                || !"COMPLETED".equalsIgnoreCase(result.gradingState())) {
            throw new BusinessRuleException("Kết quả kỹ năng chưa đủ điều kiện cập nhật hồ sơ");
        }
        boolean alreadyApplied =
                profileApplicationRepository.existsByUserIdAndAttemptIdAndSkill(
                        user.getId(), attemptId, skill);
        Optional<UserSkillProfile> existingProfile =
                userSkillProfileRepository.findByUserIdAndSkill(user.getId(), skill);
        if (alreadyApplied && existingProfile.isPresent()) {
            return true;
        }
        UserSkillProfile profile =
                existingProfile.orElseGet(
                        () -> {
                            UserSkillProfile created = new UserSkillProfile();
                            created.setUser(user);
                            created.setSkill(skill);
                            return created;
                        });
        if (type.equals("PLACEMENT") && profile.getPlacementLevel() != null) {
            if (!alreadyApplied) saveApplication(user, attemptId, skill);
            return true;
        }
        if (type.equals("PLACEMENT") && belowA1) {
            LocalDateTime submittedAtUtc =
                    submittedAt == null
                            ? null
                            : LocalDateTime.ofInstant(submittedAt, ZoneOffset.UTC);
            if (isNewer(
                    submittedAt,
                    attemptId,
                    profile.getPlacementSubmittedAt(),
                    profile.getSourceAttemptId())) {
                profile.setPlacementLevel(CefrLevel.BELOW_A1);
                profile.setPlacementSubmittedAt(submittedAtUtc);
                profile.setSourceAttemptId(attemptId);
            }
            CefrLevel floor = CefrLevel.parse(profile.getCheckpointFloorLevel());
            profile.setCurrentLevel(floor == null ? CefrLevel.BELOW_A1 : floor.name());
            userSkillProfileRepository.save(profile);
            if (!alreadyApplied) saveApplication(user, attemptId, skill);
            return true;
        }
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
            if (profile.getId() != null && profile.getCurrentLevel() != null) {
                userSkillProfileRepository.save(profile);
            }
            if (!alreadyApplied) saveApplication(user, attemptId, skill);
            return true;
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
        userSkillProfileRepository.save(profile);
        if (!alreadyApplied) saveApplication(user, attemptId, skill);
        return true;
    }

    private void saveApplication(User user, Integer attemptId, String skill) {
        ProfileApplication application = new ProfileApplication();
        application.setUserId(user.getId());
        application.setAttemptId(attemptId);
        application.setSkill(skill);
        profileApplicationRepository.save(application);
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
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (user.getRole() != Role.STUDENT || user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("Tài khoản không còn được phép truy cập");
        }
        return user;
    }

    private User activeStudentForUpdate(Integer userId) {
        User user =
                userRepository
                        .findByIdForUpdate(userId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (user.getRole() != Role.STUDENT || user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("Tài khoản không còn được phép truy cập");
        }
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public com.aptisprep.dto.view.AccountSnapshotDTO.GoalViewDTO goalView(Integer userId) {
        com.aptisprep.entity.UserGoal goal =
                userGoalRepository
                        .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, GoalStatus.ACTIVE)
                        .orElse(null);
        if (goal == null) return null;
        var certificate = goal.getCertificate();
        Map<String, String> targets = new HashMap<>();
        for (var skill : userGoalSkillRepository.findByGoalId(goal.getId())) {
            targets.put(skill.getSkill(), Objects.requireNonNull(skill.getTargetLevel()));
        }
        return new com.aptisprep.dto.view.AccountSnapshotDTO.GoalViewDTO(
                certificate == null ? null : certificate.getId(),
                certificate == null ? null : certificate.getName(),
                goal.getTargetDate(),
                targets);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, SkillLevelDTO> skillProfileMap(Integer userId) {
        return userSkillProfileRepository.findByUserIdOrderBySkillAsc(userId).stream()
                .collect(
                        java.util.stream.Collectors.toMap(
                                UserSkillProfile::getSkill,
                                p ->
                                        new SkillLevelDTO(
                                                p.getSkill(),
                                                p.getCurrentLevel(),
                                                p.getSourceAttemptId()),
                                (a, b) -> a));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> activeGoalTargets(Integer userId) {
        return userGoalRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, GoalStatus.ACTIVE)
                .map(
                        goal ->
                                userGoalSkillRepository.findByGoalId(goal.getId()).stream()
                                        .collect(
                                                java.util.stream.Collectors.toMap(
                                                        com.aptisprep.entity.UserGoalSkill
                                                                ::getSkill,
                                                        com.aptisprep.entity.UserGoalSkill
                                                                ::getTargetLevel,
                                                        (a, b) -> a)))
                .orElseGet(Map::of);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, String> goalSkillTargetsById(Integer userId) {
        return userGoalRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, GoalStatus.ACTIVE)
                .map(
                        goal ->
                                userGoalSkillRepository.findByGoalId(goal.getId()).stream()
                                        .collect(
                                                java.util.stream.Collectors.toMap(
                                                        com.aptisprep.entity.UserGoalSkill::getId,
                                                        com.aptisprep.entity.UserGoalSkill
                                                                ::getTargetLevel,
                                                        (a, b) -> a)))
                .orElseGet(Map::of);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, GoalSkillDTO> activeGoalSkillsBySkill(Integer userId) {
        return userGoalRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, GoalStatus.ACTIVE)
                .map(
                        goal ->
                                userGoalSkillRepository.findByGoalId(goal.getId()).stream()
                                        .collect(
                                                java.util.stream.Collectors.toMap(
                                                        s ->
                                                                s.getSkill() == null
                                                                        ? ""
                                                                        : s.getSkill()
                                                                                .trim()
                                                                                .toUpperCase(
                                                                                        Locale.ROOT)
                                                                                .replaceAll(
                                                                                        "[^A-Z0-9]+",
                                                                                        "_"),
                                                        s ->
                                                                new GoalSkillDTO(
                                                                        s.getId(),
                                                                        s.getSkill(),
                                                                        s.getTargetLevel()),
                                                        (a, b) -> a)))
                .orElseGet(Map::of);
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
