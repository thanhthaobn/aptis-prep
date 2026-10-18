package com.aptisprep.service.account;

import com.aptisprep.dto.form.GoalForm;
import com.aptisprep.dto.form.ProfileForm;
import com.aptisprep.dto.view.AccountSnapshot;
import com.aptisprep.entity.*;
import com.aptisprep.enums.*;
import com.aptisprep.event.GoalChangedEvent;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.*;
import com.aptisprep.service.contract.LearningPathContract;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountProfileService {
    private static final List<String> SKILLS = List.of("GRAMMAR_VOCABULARY", "READING", "LISTENING", "WRITING", "SPEAKING");
    private final UserRepository users;
    private final CertificateRepository certificates;
    private final UserGoalRepository goals;
    private final UserGoalSkillRepository goalSkills;
    private final UserSkillProfileRepository profiles;
    private final AccountAccessService access;
    private final ApplicationEventPublisher events;

    public AccountProfileService(UserRepository users, CertificateRepository certificates,
            UserGoalRepository goals, UserGoalSkillRepository goalSkills, UserSkillProfileRepository profiles,
            AccountAccessService access, ApplicationEventPublisher events) {
        this.users = users; this.certificates = certificates; this.goals = goals; this.goalSkills = goalSkills;
        this.profiles = profiles; this.access = access; this.events = events;
    }

    @Transactional(readOnly = true)
    public AccountSnapshot snapshot(String principalEmail) {
        Integer userId = access.requireActiveStudent(principalEmail);
        User user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        var goal = goals.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, GoalStatus.ACTIVE)
                .map(current -> new AccountSnapshot.GoalView(
                        current.getCertificate() == null ? null : current.getCertificate().getId(),
                        current.getCertificate() == null ? null : current.getCertificate().getName(),
                        current.getTargetDate(),
                        goalSkills.findByGoalUserId(userId).stream().collect(Collectors.toMap(
                                UserGoalSkill::getSkill, UserGoalSkill::getTargetLevel, (left, right) -> right,
                                LinkedHashMap::new))))
                .orElse(null);
        var currentProfiles = profiles.findByUserIdOrderBySkillAsc(userId).stream()
                .map(profile -> new AccountSnapshot.SkillView(profile.getSkill(), levelLabel(profile.getCurrentLevel()), profile.getSourceAttemptId()))
                .toList();
        return new AccountSnapshot(user.getFullName(), user.getEmail(), goal, currentProfiles);
    }

    @Transactional
    public void updateProfile(String principalEmail, ProfileForm form) {
        Integer userId = access.requireActiveStudent(principalEmail);
        User user = users.findByIdForUpdate(userId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        String name = form.getFullName() == null ? "" : form.getFullName().trim();
        if (name.isBlank()) throw new BusinessRuleException("Họ và tên không được để trống");
        user.setFullName(name);
    }

    @Transactional
    public void saveGoal(String principalEmail, GoalForm form) {
        Integer userId = access.requireActiveStudent(principalEmail);
        User user = users.findByIdForUpdate(userId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        Map<String, String> targets = new LinkedHashMap<>();
        addTarget(targets, "GRAMMAR_VOCABULARY", form.getGrammarVocabulary());
        addTarget(targets, "READING", form.getReading());
        addTarget(targets, "LISTENING", form.getListening());
        addTarget(targets, "WRITING", form.getWriting());
        addTarget(targets, "SPEAKING", form.getSpeaking());
        if (targets.isEmpty()) throw new BusinessRuleException("Hãy chọn ít nhất một kỹ năng hoặc bỏ qua mục tiêu");
        Certificate certificate = null;
        if (form.getCertificateId() != null) {
            certificate = certificates.findById(form.getCertificateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chứng chỉ"));
            if (certificate.getStatus() != RecordStatus.ACTIVE) throw new BusinessRuleException("Chứng chỉ đã ngừng sử dụng");
        }
        goals.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, GoalStatus.ACTIVE)
                .ifPresent(old -> old.setStatus(GoalStatus.ARCHIVED));
        UserGoal goal = new UserGoal(); goal.setUser(user); goal.setCertificate(certificate);
        goal.setTargetDate(form.getTargetDate()); goal.setStatus(GoalStatus.ACTIVE);
        targets.forEach((skill, level) -> { UserGoalSkill item = new UserGoalSkill(); item.setSkill(skill); item.setTargetLevel(level); goal.addSkill(item); });
        goals.save(goal);
        events.publishEvent(new GoalChangedEvent(userId));
    }

    public GoalForm formFrom(AccountSnapshot snapshot) {
        GoalForm form = new GoalForm();
        if (snapshot.goal() == null) return form;
        form.setCertificateId(snapshot.goal().certificateId()); form.setTargetDate(snapshot.goal().targetDate());
        form.setGrammarVocabulary(snapshot.goal().targets().getOrDefault("GRAMMAR_VOCABULARY", ""));
        form.setReading(snapshot.goal().targets().getOrDefault("READING", ""));
        form.setListening(snapshot.goal().targets().getOrDefault("LISTENING", ""));
        form.setWriting(snapshot.goal().targets().getOrDefault("WRITING", ""));
        form.setSpeaking(snapshot.goal().targets().getOrDefault("SPEAKING", ""));
        return form;
    }

    @Transactional(readOnly = true)
    public List<Certificate> activeCertificates() { return certificates.findByStatusOrderByNameAsc(RecordStatus.ACTIVE); }
    private void addTarget(Map<String, String> targets, String skill, String value) {
        if (value != null && !value.isBlank()) {
            CefrLevel level = CefrLevel.parse(value);
            if (level == null) throw new BusinessRuleException("CEFR không hợp lệ");
            targets.put(skill, level.name());
        }
    }
    private String levelLabel(String value) { return value == null || value.isBlank() ? "Chưa đánh giá" : value; }
}
