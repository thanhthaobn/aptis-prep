package com.aptisprep.mvc.service.account.impl;

import com.aptisprep.mvc.dto.*;
import com.aptisprep.mvc.dto.UserGoalDTO;
import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import com.aptisprep.mvc.enums.Skill;
import com.aptisprep.mvc.event.GoalChangedEvent;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.exception.ResourceNotFoundException;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.service.account.AccountAccessService;
import com.aptisprep.mvc.service.account.AccountProfileService;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountProfileServiceImpl implements AccountProfileService {
    private List<String> SKILLS = Skill.codes();
    private UserRepository userRepository;
    private CertificateRepository certificateRepository;
    private UserGoalRepository userGoalRepository;
    private UserGoalSkillRepository userGoalSkillRepository;
    private UserSkillProfileRepository userSkillProfileRepository;
    private AccountAccessService access;
    private ApplicationEventPublisher events;

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
    public UserDTO profile(String principalEmail) {
        Integer userId = access.requireActiveStudent(principalEmail);
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        return UserDTO.convertEntityToDto(user);
    }

    @Transactional(readOnly = true)
    public UserGoalDTO goal(String principalEmail) {
        Integer userId = access.requireActiveStudent(principalEmail);
        UserGoal activeGoal =
                userGoalRepository
                        .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, GoalStatus.ACTIVE)
                        .orElse(null);
        return toGoalDto(activeGoal);
    }

    private UserGoalDTO toGoalDto(UserGoal goal) {
        if (goal == null) return null;
        UserGoalDTO dto = UserGoalDTO.convertEntityToDto(goal);
        Map<String, String> targets = new LinkedHashMap<>();
        Map<String, BigDecimal> targetScores = new HashMap<>();
        List<UserGoalSkill> goalSkills = userGoalSkillRepository.findByGoalId(goal.getId());
        for (UserGoalSkill goalSkill : goalSkills) {
            targets.put(goalSkill.getSkill(), goalSkill.getTargetLevel());
            if (goalSkill.getTargetScore() != null) {
                targetScores.put(goalSkill.getSkill(), goalSkill.getTargetScore());
            }
        }
        dto.setTargetScores(targetScores);
        dto.setGrammarVocabulary(targets.get("GRAMMAR_VOCABULARY"));
        dto.setReading(targets.get("READING"));
        dto.setListening(targets.get("LISTENING"));
        dto.setWriting(targets.get("WRITING"));
        dto.setSpeaking(targets.get("SPEAKING"));
        return dto;
    }

    @Transactional(readOnly = true)
    public List<UserSkillProfileDTO> skillProfiles(String principalEmail) {
        Integer userId = access.requireActiveStudent(principalEmail);
        List<UserSkillProfileDTO> result = new ArrayList<>();
        List<UserSkillProfile> profiles =
                userSkillProfileRepository.findByUserIdOrderBySkillAsc(userId);
        for (UserSkillProfile profile : profiles) {
            result.add(UserSkillProfileDTO.convertEntityToDto(profile));
        }
        return List.copyOf(result);
    }

    @Transactional
    public void updateProfile(String principalEmail, UserDTO form) {
        Integer userId = access.requireActiveStudent(principalEmail);
        User user =
                userRepository
                        .findByIdForUpdate(userId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        String name = form.getFullName() == null ? "" : form.getFullName().trim();
        if (name.isBlank()) throw new BusinessRuleException("Họ và tên không được để trống");
        user.setFullName(name);
    }

    @Transactional
    public void saveGoal(String principalEmail, UserGoalDTO form) {
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
                            .map(streamValue -> streamValue.getCertificate())
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
        UserGoal goal = UserGoalDTO.convertDtoToEntity(form);
        goal.setUser(user);
        goal.setCertificate(certificate);
        goal.setTargetDate(form.getTargetDate());
        goal.setStatus(GoalStatus.ACTIVE);
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
