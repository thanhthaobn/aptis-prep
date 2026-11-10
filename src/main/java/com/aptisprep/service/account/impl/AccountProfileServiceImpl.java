package com.aptisprep.service.account.impl;

import com.aptisprep.dto.UserGoalEntityDto;
import com.aptisprep.dto.form.GoalForm;
import com.aptisprep.dto.form.ProfileForm;
import com.aptisprep.dto.view.AccountSnapshot;
import com.aptisprep.dto.view.AccountSnapshot.GoalView;
import com.aptisprep.dto.view.AccountSnapshot.SkillView;
import com.aptisprep.entity.*;
import com.aptisprep.mvc.auth.dto.UserEntityDTO;
import com.aptisprep.mvc.auth.entity.User;
import com.aptisprep.mvc.auth.repository.UserRepository;
import com.aptisprep.mvc.scoring.entity.Certificate;
import com.aptisprep.mvc.scoring.repository.CertificateRepository;
import com.aptisprep.mvc.common.enums.*;
import com.aptisprep.mvc.common.enums.Skill;
import com.aptisprep.mvc.common.event.GoalChangedEvent;
import com.aptisprep.mvc.common.exception.BusinessRuleException;
import com.aptisprep.mvc.common.exception.ResourceNotFoundException;
import com.aptisprep.mvc.scoring.entity.Certificate;
import com.aptisprep.mvc.scoring.repository.CertificateRepository;
import com.aptisprep.repository.*;
import com.aptisprep.service.account.AccountAccessService;
import com.aptisprep.service.account.AccountProfileService;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountProfileServiceImpl implements AccountProfileService {
    private static final List<String> SKILLS = Skill.codes();
    private final UserRepository userRepository;
    private final CertificateRepository certificateRepository;
    private final UserGoalRepository userGoalRepository;
    private final UserGoalSkillRepository userGoalSkillRepository;
    private final UserSkillProfileRepository userSkillProfileRepository;
    private final AccountAccessService access;
    private final ApplicationEventPublisher events;

    public AccountProfileServiceImpl(
            UserRepository userRepository,
            CertificateRepository certificateRepository,
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
    public AccountSnapshot snapshot(String principalEmail) {
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
        GoalView goal = toGoalView(activeGoal);
        List<SkillView> currentProfiles = loadSkillViews(userId);
        return new AccountSnapshot(user.getFullName(), user.getEmail(), goal, currentProfiles);
    }

    private GoalView toGoalView(UserGoal goal) {
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
        return new GoalView(
                certificateId, certificateName, goal.getTargetDate(), targets, targetScores);
    }

    private List<SkillView> loadSkillViews(Integer userId) {
        List<SkillView> result = new ArrayList<>();
        List<UserSkillProfile> profiles =
                userSkillProfileRepository.findByUserIdOrderBySkillAsc(userId);
        for (UserSkillProfile profile : profiles) {
            result.add(
                    new SkillView(
                            profile.getSkill(),
                            levelLabel(profile.getCurrentLevel()),
                            profile.getSourceAttemptId()));
        }
        return List.copyOf(result);
    }

    @Transactional
    public void updateProfile(String principalEmail, ProfileForm form) {
        Integer userId = access.requireActiveStudent(principalEmail);
        User user =
                userRepository
                        .findByIdForUpdate(userId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        User mapped = UserEntityDTO.convertDtoToEntity(form);
        String name = mapped.getFullName() == null ? "" : mapped.getFullName().trim();
        if (name.isBlank()) throw new BusinessRuleException("Họ và tên không được để trống");
        user.setFullName(name);
    }

    @Transactional
    public void saveGoal(String principalEmail, GoalForm form) {
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
        UserGoal goal = UserGoalEntityDto.convertDtoToEntity(form, user, certificate);
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

    public GoalForm formFrom(AccountSnapshot snapshot) {
        GoalForm form = new GoalForm();
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
        return certificateRepository.findByStatusOrderByNameAsc(RecordStatus.ACTIVE);
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
