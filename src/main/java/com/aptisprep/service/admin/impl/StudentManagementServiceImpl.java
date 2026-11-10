package com.aptisprep.service.admin.impl;

import com.aptisprep.dto.view.AccountSnapshot;
import com.aptisprep.dto.view.AdminStudentView;
import com.aptisprep.entity.*;
import com.aptisprep.mvc.common.enums.*;
import com.aptisprep.mvc.common.enums.Skill;
import com.aptisprep.mvc.common.exception.AccessDeniedException;
import com.aptisprep.mvc.common.exception.BusinessRuleException;
import com.aptisprep.mvc.common.exception.ResourceNotFoundException;
import com.aptisprep.repository.*;
import com.aptisprep.service.admin.StudentManagementService;
import com.aptisprep.service.contract.ExamAttemptContract;
import com.aptisprep.service.contract.IntegrationModels.*;
import com.aptisprep.service.contract.LearningPathContract;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentManagementServiceImpl implements StudentManagementService {
    private static final List<String> SKILLS = Skill.codes();
    private final UserRepository userRepository;
    private final UserGoalRepository userGoalRepository;
    private final UserGoalSkillRepository userGoalSkillRepository;
    private final UserSkillProfileRepository userSkillProfileRepository;
    private final ObjectProvider<LearningPathContract> paths;
    private final ObjectProvider<ExamAttemptContract> attempts;

    public StudentManagementServiceImpl(
            UserRepository userRepository,
            UserGoalRepository userGoalRepository,
            UserGoalSkillRepository userGoalSkillRepository,
            UserSkillProfileRepository userSkillProfileRepository,
            ObjectProvider<LearningPathContract> paths,
            ObjectProvider<ExamAttemptContract> attempts) {
        this.userRepository = userRepository;
        this.userGoalRepository = userGoalRepository;
        this.userGoalSkillRepository = userGoalSkillRepository;
        this.userSkillProfileRepository = userSkillProfileRepository;
        this.paths = paths;
        this.attempts = attempts;
    }

    @Transactional(readOnly = true)
    public AdminStudentView.Page list(String query, UserStatus status, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        String term = query == null ? "" : query.trim();
        if (term.length() > 255)
            throw new BusinessRuleException("Từ khóa tìm kiếm tối đa 255 ký tự");
        String pattern =
                term.isBlank() ? "%" : "%" + escapeLike(term.toLowerCase(Locale.ROOT)) + "%";
        Page<User> result =
                userRepository.searchStudents(
                        pattern,
                        status == null ? null : status.name(),
                        PageRequest.of(safePage, safeSize));
        return new AdminStudentView.Page(
                result.getContent().stream().map(this::listItem).toList(),
                safePage,
                safeSize,
                result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AdminStudentView.Detail detail(Integer id, int attemptPage, int attemptSize) {
        User user = studentsOnly(id);
        int safePage = Math.max(0, attemptPage);
        int safeSize = Math.min(100, Math.max(1, attemptSize));
        AccountSnapshot.GoalView goal = goalView(user.getId());
        Map<String, String> targetMap = goal == null ? Map.of() : goal.targets();
        Map<String, UserSkillProfile> profileMap =
                userSkillProfileRepository.findByUserIdOrderBySkillAsc(user.getId()).stream()
                        .collect(
                                Collectors.toMap(
                                        UserSkillProfile::getSkill,
                                        value -> value,
                                        (left, right) -> right));
        List<AdminStudentView.Skill> skillViews =
                SKILLS.stream()
                        .map(
                                skill -> {
                                    UserSkillProfile profile = profileMap.get(skill);
                                    return new AdminStudentView.Skill(
                                            skillLabel(skill),
                                            profile == null
                                                    ? "Chưa đánh giá"
                                                    : levelLabel(profile.getCurrentLevel()),
                                            targetMap.getOrDefault(skill, "Chưa đặt"));
                                })
                        .toList();
        List<AdminStudentView.Progress> progress = progressFor(user.getId());
        HistoryPage history = historyFor(user.getId(), safePage, safeSize);
        List<AdminStudentView.Attempt> attempts =
                history.items().stream().map(this::attemptView).toList();
        return new AdminStudentView.Detail(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                initials(user.getFullName()),
                user.getCreatedAt(),
                user.getStatus(),
                goal,
                skillViews,
                progress,
                attempts,
                safePage,
                safeSize,
                history.totalItems());
    }

    @Transactional
    public void lock(String adminEmail, Integer id, String reason) {
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isBlank())
            throw new BusinessRuleException("Vui lòng nhập lý do khóa tài khoản");
        if (normalizedReason.length() > 1000)
            throw new BusinessRuleException("Lý do khóa tài khoản tối đa 1000 ký tự");
        User target = changeStatus(adminEmail, id, UserStatus.ACTIVE, UserStatus.LOCKED, "khóa");
        target.setLockReason(normalizedReason);
    }

    @Transactional
    public void unlock(String adminEmail, Integer id) {
        User target = changeStatus(adminEmail, id, UserStatus.LOCKED, UserStatus.ACTIVE, "mở khóa");
        target.setLockReason(null);
    }

    @Override
    @Transactional
    public void remove(String adminEmail, Integer id, String reason) {
        var normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isBlank())
            throw new BusinessRuleException("Vui lòng nhập lý do xóa tài khoản");
        if (normalizedReason.length() > 1000)
            throw new BusinessRuleException("Lý do xóa tài khoản tối đa 1000 ký tự");
        User admin =
                userRepository
                        .findByEmailIgnoreCase(adminEmail)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Không có quyền thực hiện thao tác"));
        if (admin.getRole() != Role.ADMIN)
            throw new AccessDeniedException("Chỉ quản trị viên mới được quản lý học viên");
        User target =
                userRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy học viên"));
        if (target.getRole() != Role.STUDENT)
            throw new ResourceNotFoundException("Không tìm thấy học viên");
        if (target.getStatus() != UserStatus.LOCKED)
            throw new BusinessRuleException("Chỉ có thể xóa tài khoản đã khóa");
        target.setStatus(UserStatus.INACTIVE);
        target.setLockReason(normalizedReason);
    }

    public long totalStudents() {
        return userRepository.countByRole(Role.STUDENT);
    }

    public long activeStudents() {
        return userRepository.countByRoleAndStatus(Role.STUDENT, UserStatus.ACTIVE);
    }

    public long lockedStudents() {
        return userRepository.countByRoleAndStatus(Role.STUDENT, UserStatus.LOCKED);
    }

    private User changeStatus(
            String adminEmail, Integer id, UserStatus expected, UserStatus next, String action) {
        User admin =
                userRepository
                        .findByEmailIgnoreCase(adminEmail)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Không có quyền thực hiện thao tác"));
        if (admin.getRole() != Role.ADMIN)
            throw new AccessDeniedException("Chỉ quản trị viên mới được quản lý học viên");
        User target =
                userRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy học viên"));
        if (Objects.equals(admin.getId(), target.getId()))
            throw new BusinessRuleException("Không thể khóa tài khoản của chính mình");
        if (target.getRole() != Role.STUDENT)
            throw new ResourceNotFoundException("Không tìm thấy học viên");
        if (target.getStatus() == UserStatus.INACTIVE)
            throw new BusinessRuleException("Tài khoản ngừng hoạt động không thể " + action);
        if (target.getStatus() != expected)
            throw new BusinessRuleException("Trạng thái tài khoản không phù hợp để " + action);
        target.setStatus(next);
        return target;
    }

    private AdminStudentView.ListItem listItem(User user) {
        AccountSnapshot.GoalView goal = goalView(user.getId());
        String target =
                goal == null || goal.targets().isEmpty()
                        ? "Chưa đặt mục tiêu"
                        : goal.targets().values().stream().collect(Collectors.joining(", "));
        return new AdminStudentView.ListItem(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                initials(user.getFullName()),
                user.getCreatedAt(),
                user.getStatus(),
                target);
    }

    private AccountSnapshot.GoalView goalView(Integer userId) {
        UserGoal goal =
                userGoalRepository
                        .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, GoalStatus.ACTIVE)
                        .orElse(null);
        if (goal == null) return null;
        Certificate certificate = goal.getCertificate();
        Map<String, String> targets = new LinkedHashMap<>();
        for (UserGoalSkill skill : userGoalSkillRepository.findByGoalId(goal.getId())) {
            targets.put(skill.getSkill(), Objects.requireNonNull(skill.getTargetLevel()));
        }
        return new AccountSnapshot.GoalView(
                certificate == null ? null : certificate.getId(),
                certificate == null ? null : certificate.getName(),
                goal.getTargetDate(),
                targets);
    }

    private List<AdminStudentView.Progress> progressFor(Integer userId) {
        LearningPathContract contract = paths.getIfAvailable();
        if (contract == null) return List.of();
        AccountSnapshot.GoalView goal = goalView(userId);
        Map<String, String> targetMap = goal == null ? Map.of() : goal.targets();
        List<PathProgress> values = contract.summaries(userId);
        return values == null
                ? List.of()
                : values.stream()
                        .map(
                                value ->
                                        new AdminStudentView.Progress(
                                                value.pathId(),
                                                skillLabel(value.skill()),
                                                value.pathSource(),
                                                value.pathStatus(),
                                                value.completedLessons(),
                                                value.totalLessons(),
                                                value.progressPercent(),
                                                value.completedRequiredLessons(),
                                                value.totalRequiredLessons(),
                                                value.nextLessonTitle(),
                                                checkpointLabel(value.checkpointState()),
                                                targetMap.get(value.skill())))
                        .toList();
    }

    private HistoryPage historyFor(Integer userId, int page, int size) {
        ExamAttemptContract contract = attempts.getIfAvailable();
        if (contract == null) return new HistoryPage(List.of(), page, size, 0);
        HistoryPage value = contract.history(userId, page, size);
        return value == null ? new HistoryPage(List.of(), page, size, 0) : value;
    }

    private AdminStudentView.Attempt attemptView(AttemptResult result) {
        String date = result.submittedAt() == null ? "Chưa nộp" : result.submittedAt().toString();
        String status = statusLabel(result.state());
        List<String> scores =
                result.skills() == null
                        ? List.of()
                        : result.skills().stream()
                                .filter(skill -> skill.scaleScore() != null)
                                .map(
                                        skill ->
                                                skillLabel(skill.skill())
                                                        + ": "
                                                        + skill.scaleScore()
                                                                .stripTrailingZeros()
                                                                .toPlainString())
                                .toList();
        return new AdminStudentView.Attempt(
                result.examType() == null ? "Lượt thi" : result.examType(),
                date,
                status,
                scores.isEmpty() ? "Chưa chấm" : String.join(" · ", scores));
    }

    private User studentsOnly(Integer id) {
        User user =
                userRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy học viên"));
        if (user.getRole() != Role.STUDENT)
            throw new ResourceNotFoundException("Không tìm thấy học viên");
        return user;
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private String initials(String value) {
        return Arrays.stream(Optional.ofNullable(value).orElse("").trim().split("\\s+"))
                .filter(part -> !part.isBlank())
                .map(part -> part.substring(0, 1).toUpperCase(Locale.ROOT))
                .limit(2)
                .collect(Collectors.joining());
    }

    private String skillLabel(String value) {
        if (value == null) return "Kỹ năng";
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "GRAMMAR_VOCABULARY" -> "Grammar & Vocabulary";
            case "READING" -> "Reading";
            case "LISTENING" -> "Listening";
            case "WRITING" -> "Writing";
            case "SPEAKING" -> "Speaking";
            default -> value;
        };
    }

    private String checkpointLabel(String value) {
        if (value == null) return "Chưa cấu hình";
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "AVAILABLE" -> "Có thể kiểm tra";
            case "LOCKED" -> "Chưa mở";
            case "GRADING" -> "Đang chờ chấm";
            case "PASSED" -> "Đã đạt";
            case "FAILED" -> "Chưa đạt";
            default -> "Chưa cấu hình";
        };
    }

    private String statusLabel(String value) {
        if (value == null) return "Chưa có trạng thái";
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "IN_PROGRESS" -> "Đang làm bài";
            case "SUBMITTED" -> "Đã nộp bài";
            case "WAITING_GRADING" -> "Đang chờ chấm";
            case "COMPLETED" -> "Đã hoàn thành";
            case "PENDING", "GRADING" -> "Đang chờ chấm";
            case "ERROR" -> "Lỗi chấm";
            case "GRADING_FAILED" -> "Chấm chưa thành công";
            case "EXPIRED" -> "Đã hết thời gian";
            default -> value;
        };
    }

    private String levelLabel(String value) {
        return value == null || value.isBlank() ? "Chưa đánh giá" : value;
    }
}
