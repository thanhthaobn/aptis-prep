package com.aptisprep.service.account.impl;

import com.aptisprep.dto.form.GoalFormDTO;
import com.aptisprep.dto.form.ProfileFormDTO;
import com.aptisprep.dto.view.AccountSnapshotDTO;
import com.aptisprep.dto.view.AccountSnapshotDTO.GoalViewDTO;
import com.aptisprep.dto.view.AccountSnapshotDTO.SkillViewDTO;
import com.aptisprep.dto.view.UserGoalDTO;
import com.aptisprep.entity.Certificate;
import com.aptisprep.entity.User;
import com.aptisprep.entity.UserGoal;
import com.aptisprep.entity.UserGoalSkill;
import com.aptisprep.entity.UserSkillProfile;
import com.aptisprep.enums.*;
import com.aptisprep.enums.Skill;
import com.aptisprep.event.GoalChangedEvent;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.UserGoalRepository;
import com.aptisprep.repository.UserGoalSkillRepository;
import com.aptisprep.repository.UserSkillProfileRepository;
import com.aptisprep.service.account.AccountAccessService;
import com.aptisprep.service.account.AccountProfileService;
import com.aptisprep.service.account.UserService;
import com.aptisprep.service.scoring.CertificateSchemeService;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountProfileServiceImpl implements AccountProfileService {
    private static final List<String> SKILLS = Skill.codes();
    private final UserService userRepository;
    private final CertificateSchemeService certificateRepository;
    private final UserGoalRepository userGoalRepository;
    private final UserGoalSkillRepository userGoalSkillRepository;
    private final UserSkillProfileRepository userSkillProfileRepository;
    private final AccountAccessService access;
    private final ApplicationEventPublisher events;

    public AccountProfileServiceImpl(
            UserService userRepository,
            CertificateSchemeService certificateRepository,
            UserGoalRepository userGoalRepository,
            UserGoalSkillRepository userGoalSkillRepository,
            UserSkillProfileRepository userSkillProfileRepository,
            AccountAccessService access,
            ApplicationEventPublisher events) {
        this.userRepository = userRepository;
        this.certificateRepository = certificateRepository;
        this.userGoalRepository = userGoalRepository;
        this.userGoalSkillRepository = userGoalSkillRepository;
        this.userSkillProfileRepository = userSkillProfileRepository;
        this.access = access;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public AccountSnapshotDTO snapshot(String principalEmail) {
        Integer userId = access.requireActiveStudent(principalEmail);
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        UserGoal activeGoal =
                userGoalRepository
                        .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, GoalStatus.ACTIVE)
                        .orElse(null);
        GoalViewDTO goal = toGoalView(activeGoal);
        List<SkillViewDTO> currentProfiles = loadSkillViews(userId);
        return new AccountSnapshotDTO(user.getFullName(), user.getEmail(), goal, currentProfiles);
    }

    private GoalViewDTO toGoalView(UserGoal goal) {
        if (goal == null) return null;

        Certificate certificate = goal.getCertificate();
        Integer certificateId = certificate == null ? null : certificate.getId();
        String certificateName = certificate == null ? null : certificate.getName();
        Map<String, String> targets = new LinkedHashMap<>();
        Map<String, BigDecimal> targetScores = new HashMap<>();
        List<UserGoalSkill> goalSkills = userGoalSkillRepository.findByGoalId(goal.getId());
        for (UserGoalSkill goalSkill : goalSkills) {
            targets.put(goalSkill.getSkill(), goalSkill.getTargetLevel());
            if (goalSkill.getTargetScore() != null) {
                targetScores.put(goalSkill.getSkill(), goalSkill.getTargetScore());
            }
        }
        return new GoalViewDTO(
                certificateId, certificateName, goal.getTargetDate(), targets, targetScores);
    }

    private List<SkillViewDTO> loadSkillViews(Integer userId) {
        List<SkillViewDTO> result = new ArrayList<>();
        List<UserSkillProfile> profiles =
                userSkillProfileRepository.findByUserIdOrderBySkillAsc(userId);
        for (UserSkillProfile profile : profiles) {
            result.add(
                    new SkillViewDTO(
                            profile.getSkill(),
                            levelLabel(profile.getCurrentLevel()),
                            profile.getSourceAttemptId()));
        }
        return List.copyOf(result);
    }

    @Transactional
    public void updateProfile(String principalEmail, ProfileFormDTO form) {
        Integer userId = access.requireActiveStudent(principalEmail);
        User user =
                userRepository
                        .findByIdForUpdate(userId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        User mapped = new User();
        mapped.setFullName(form.getFullName());
        String name = mapped.getFullName() == null ? "" : mapped.getFullName().trim();
        if (name.isBlank()) throw new BusinessRuleException("Họ và tên không được để trống");
        user.setFullName(name);
    }

    @Transactional
    public void saveGoal(String principalEmail, GoalFormDTO form) {
        Integer userId = access.requireActiveStudent(principalEmail);
        User user =
                userRepository
                        .findByIdForUpdate(userId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        Map<String, String> targets = new LinkedHashMap<>();
        addTarget(targets, "GRAMMAR_VOCABULARY", form.getGrammarVocabulary());
        addTarget(targets, "READING", form.getReading());
        addTarget(targets, "LISTENING", form.getListening());
        addTarget(targets, "WRITING", form.getWriting());
        addTarget(targets, "SPEAKING", form.getSpeaking());
        if (targets.isEmpty())
            throw new BusinessRuleException("Hãy chọn ít nhất một kỹ năng hoặc bỏ qua mục tiêu");
        form.getTargetScores()
                .forEach(
                        (skill, score) -> {
                            if (!SKILLS.contains(skill)
                                    || (score != null
                                            && (!targets.containsKey(skill)
                                                    || score.signum() < 0
                                                    || score.compareTo(BigDecimal.valueOf(50))
                                                            > 0)))
                                throw new BusinessRuleException(
                                        "Điểm mục tiêu phải thuộc kỹ năng đã chọn và nằm trong"
                                                + " 0–50");
                        });
        Certificate certificate = null;
        if (form.getCertificateId() != null) {
            certificate =
                    certificateRepository
                            .findById(form.getCertificateId())
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    "Không tìm thấy chứng chỉ"));
            if (certificate.getStatus() != RecordStatus.ACTIVE)
                throw new BusinessRuleException("Chứng chỉ đã ngừng sử dụng");
        } else {
            certificate =
                    userGoalRepository
                            .findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                                    userId, GoalStatus.ACTIVE)
                            .map(UserGoal::getCertificate)
                            .orElseGet(
                                    () ->
                                            certificateRepository
                                                    .findByCodeIgnoreCase("APTIS_GENERAL")
                                                    .filter(
                                                            value ->
                                                                    value.getStatus()
                                                                            == RecordStatus.ACTIVE)
                                                    .orElse(null));
        }
        userGoalRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, GoalStatus.ACTIVE)
                .ifPresent(old -> old.setStatus(GoalStatus.ARCHIVED));
        UserGoal goal = UserGoalDTO.convertDtoToEntity(form, user, certificate);
        targets.forEach(
                (skill, level) -> {
                    UserGoalSkill item = new UserGoalSkill();
                    item.setSkill(skill);
                    item.setTargetLevel(level);
                    item.setTargetScore(form.getTargetScores().get(skill));
                    goal.addSkill(item);
                });
        userGoalRepository.save(goal);
        events.publishEvent(new GoalChangedEvent(userId));
    }

    public GoalFormDTO formFrom(AccountSnapshotDTO snapshot) {
        GoalFormDTO form = new GoalFormDTO();
        if (snapshot.goal() == null) return form;
        form.setCertificateId(snapshot.goal().certificateId());
        form.setTargetDate(snapshot.goal().targetDate());
        form.setTargetScores(new LinkedHashMap<>(snapshot.goal().targetScores()));
        form.setGrammarVocabulary(snapshot.goal().targets().getOrDefault("GRAMMAR_VOCABULARY", ""));
        form.setReading(snapshot.goal().targets().getOrDefault("READING", ""));
        form.setListening(snapshot.goal().targets().getOrDefault("LISTENING", ""));
        form.setWriting(snapshot.goal().targets().getOrDefault("WRITING", ""));
        form.setSpeaking(snapshot.goal().targets().getOrDefault("SPEAKING", ""));
        return form;
    }

    @Transactional(readOnly = true)
    public List<Certificate> activeCertificates() {
        return certificateRepository.findAllActiveCertificates();
    }

    private void addTarget(Map<String, String> targets, String skill, String value) {
        if (value != null && !value.isBlank()) {
            CefrLevel level = CefrLevel.parse(value);
            if (level == null || level == CefrLevel.C2)
                throw new BusinessRuleException("Mục tiêu học tập chỉ hỗ trợ từ A1 đến C1");
            targets.put(skill, level.name());
        }
    }

    private String levelLabel(String value) {
        return value == null || value.isBlank() ? "Chưa đánh giá" : value;
    }
}
