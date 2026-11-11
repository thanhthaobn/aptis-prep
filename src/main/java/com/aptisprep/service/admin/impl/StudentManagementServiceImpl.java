package com.aptisprep.service.admin.impl;

import com.aptisprep.dto.AccountSnapshotDTO;
import com.aptisprep.dto.AdminStudentViewDTO;
import com.aptisprep.dto.AttemptResultDTO;
import com.aptisprep.dto.HistoryPageDTO;
import com.aptisprep.dto.PathProgressDTO;
import com.aptisprep.dto.SkillLevelDTO;
import com.aptisprep.entity.User;
import com.aptisprep.enums.*;
import com.aptisprep.enums.Skill;
import com.aptisprep.exception.AccessDeniedException;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.service.account.AccountAccessService;
import com.aptisprep.service.account.UserService;
import com.aptisprep.service.admin.StudentManagementService;
import com.aptisprep.service.exam.ExamAttemptService;
import com.aptisprep.service.learning.LearningPathService;
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
    private final UserService userService;
    private final AccountAccessService accounts;
    private final ObjectProvider<LearningPathService> paths;
    private final ObjectProvider<ExamAttemptService> attempts;
    private final com.aptisprep.service.account.AccountUnlockRequestService
            accountUnlockRequestService;

    public StudentManagementServiceImpl(
            UserService userService,
            AccountAccessService accounts,
            ObjectProvider<LearningPathService> paths,
            ObjectProvider<ExamAttemptService> attempts,
            com.aptisprep.service.account.AccountUnlockRequestService accountUnlockRequestService) {
        this.userService = userService;
        this.accounts = accounts;
        this.paths = paths;
        this.attempts = attempts;
        this.accountUnlockRequestService = accountUnlockRequestService;
    }

    @Transactional(readOnly = true)
    public AdminStudentViewDTO.Page list(String query, UserStatus status, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        String term = query == null ? "" : query.trim();
        if (term.length() > 255)
            throw new BusinessRuleException("Từ khóa tìm kiếm tối đa 255 ký tự");
        String pattern =
                term.isBlank() ? "%" : "%" + escapeLike(term.toLowerCase(Locale.ROOT)) + "%";
        Page<User> result =
                userService.searchStudents(
                        pattern,
                        status == null ? null : status.name(),
                        PageRequest.of(safePage, safeSize));
        return new AdminStudentViewDTO.Page(
                result.getContent().stream().map(this::listItem).toList(),
                safePage,
                safeSize,
                result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public AdminStudentViewDTO.Detail detail(Integer id, int attemptPage, int attemptSize) {
        User user = studentsOnly(id);
        int safePage = Math.max(0, attemptPage);
        int safeSize = Math.min(100, Math.max(1, attemptSize));
        AccountSnapshotDTO.GoalViewDTO goal = accounts.goalView(user.getId());
        Map<String, String> targetMap = goal == null ? Map.of() : goal.targets();
        Map<String, SkillLevelDTO> profileMap = accounts.skillProfileMap(user.getId());
        List<AdminStudentViewDTO.Skill> skillViews =
                SKILLS.stream()
                        .map(
                                skill -> {
                                    SkillLevelDTO profile = profileMap.get(skill);
                                    return new AdminStudentViewDTO.Skill(
                                            skillLabel(skill),
                                            profile == null
                                                    ? "Chưa đánh giá"
                                                    : levelLabel(profile.estimatedLevel()),
                                            targetMap.getOrDefault(skill, "Chưa đặt"));
                                })
                        .toList();
        List<AdminStudentViewDTO.Progress> progress = progressFor(user.getId());
        HistoryPageDTO history = historyFor(user.getId(), safePage, safeSize);
        return new AdminStudentViewDTO.Detail(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                initials(user.getFullName()),
                user.getCreatedAt(),
                user.getStatus(),
                goal,
                skillViews,
                progress,
                history.items().stream().map(this::attemptView).toList(),
                history.page(),
                history.size(),
                history.totalItems());
    }

    @Transactional
    public void lock(String adminEmail, Integer id, String reason) {
        var normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isBlank())
            throw new BusinessRuleException("Vui lòng nhập lý do khóa tài khoản");
        if (normalizedReason.length() > 1000)
            throw new BusinessRuleException("Lý do khóa tài khoản tối đa 1000 ký tự");
        User user =
                changeStatus(
                        adminEmail, id, UserStatus.ACTIVE, UserStatus.LOCKED, "khóa tài khoản");
        user.setLockReason(normalizedReason);
    }

    @Transactional
    public void unlock(String adminEmail, Integer id) {
        User user =
                changeStatus(
                        adminEmail, id, UserStatus.LOCKED, UserStatus.ACTIVE, "mở khóa tài khoản");
        user.setLockReason(null);
    }

    @Transactional
    public void remove(String adminEmail, Integer id, String reason) {
        var normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isBlank())
            throw new BusinessRuleException("Vui lòng nhập lý do xóa tài khoản");
        if (normalizedReason.length() > 1000)
            throw new BusinessRuleException("Lý do xóa tài khoản tối đa 1000 ký tự");
        User admin =
                userService
                        .findByEmailIgnoreCase(adminEmail)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Không có quyền thực hiện thao tác"));
        if (admin.getRole() != Role.ADMIN)
            throw new AccessDeniedException("Chỉ quản trị viên mới được quản lý học viên");
        User target =
                userService
                        .findByIdForUpdate(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy học viên"));
        if (target.getRole() != Role.STUDENT)
            throw new ResourceNotFoundException("Không tìm thấy học viên");
        if (target.getStatus() != UserStatus.LOCKED)
            throw new BusinessRuleException("Chỉ có thể xóa tài khoản đã khóa");
        target.setStatus(UserStatus.INACTIVE);
        target.setRemovalReason(normalizedReason);
    }

    @Transactional
    public void restore(String adminEmail, Integer id) {
        User admin =
                userService
                        .findByEmailIgnoreCase(adminEmail)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Không có quyền thực hiện thao tác"));
        if (admin.getRole() != Role.ADMIN)
            throw new AccessDeniedException("Chỉ quản trị viên mới được quản lý học viên");
        User target =
                userService
                        .findByIdForUpdate(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy học viên"));
        if (target.getRole() != Role.STUDENT)
            throw new ResourceNotFoundException("Không tìm thấy học viên");
        if (target.getStatus() != UserStatus.INACTIVE)
            throw new BusinessRuleException("Chỉ có thể khôi phục tài khoản ngừng hoạt động");
        target.setStatus(UserStatus.ACTIVE);
        target.setRemovalReason(null);
        target.setLockReason(null);
    }

    @Transactional(readOnly = true)
    public List<com.aptisprep.dto.AccountUnlockRequestDTO> listUnlockRequests(String statusFilter) {
        return accountUnlockRequestService.list(statusFilter);
    }

    @Transactional
    public void approveUnlockRequest(String adminEmail, Integer requestId, String note) {
        accountUnlockRequestService.approve(adminEmail, requestId, note);
    }

    @Transactional
    public void rejectUnlockRequest(String adminEmail, Integer requestId, String note) {
        accountUnlockRequestService.reject(adminEmail, requestId, note);
    }

    @Transactional(readOnly = true)
    public long pendingUnlockRequests() {
        return accountUnlockRequestService.countPending();
    }

    public long totalStudents() {
        return userService.countByRole(Role.STUDENT);
    }

    public long activeStudents() {
        return userService.countByRoleAndStatus(Role.STUDENT, UserStatus.ACTIVE);
    }

    public long lockedStudents() {
        return userService.countByRoleAndStatus(Role.STUDENT, UserStatus.LOCKED);
    }

    private User changeStatus(
            String adminEmail, Integer id, UserStatus expected, UserStatus next, String action) {
        User admin =
                userService
                        .findByEmailIgnoreCase(adminEmail)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Không có quyền thực hiện thao tác"));
        if (admin.getRole() != Role.ADMIN)
            throw new AccessDeniedException("Chỉ quản trị viên mới được quản lý học viên");
        User target =
                userService
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
            throw new BusinessRuleException("Trạng thái tài khoản không hợp lệ để " + action);
        target.setStatus(next);
        return target;
    }

    private AdminStudentViewDTO.ListItem listItem(User user) {
        AccountSnapshotDTO.GoalViewDTO goal = accounts.goalView(user.getId());
        String target =
                goal == null || goal.targets().isEmpty()
                        ? "Chưa đặt mục tiêu"
                        : goal.targets().entrySet().stream()
                                .map(entry -> skillLabel(entry.getKey()) + ": " + entry.getValue())
                                .collect(Collectors.joining(" · "));
        return new AdminStudentViewDTO.ListItem(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                initials(user.getFullName()),
                user.getCreatedAt(),
                user.getStatus(),
                target);
    }

    private List<AdminStudentViewDTO.Progress> progressFor(Integer userId) {
        LearningPathService contract = paths.getIfAvailable();
        if (contract == null) return List.of();
        AccountSnapshotDTO.GoalViewDTO goal = accounts.goalView(userId);
        Map<String, String> targetMap = goal == null ? Map.of() : goal.targets();
        List<PathProgressDTO> values = contract.summaries(userId);
        return values == null
                ? List.of()
                : values.stream()
                        .map(
                                value ->
                                        new AdminStudentViewDTO.Progress(
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

    private HistoryPageDTO historyFor(Integer userId, int page, int size) {
        ExamAttemptService contract = attempts.getIfAvailable();
        if (contract == null) return new HistoryPageDTO(List.of(), page, size, 0);
        HistoryPageDTO value = contract.history(userId, page, size);
        return value == null ? new HistoryPageDTO(List.of(), page, size, 0) : value;
    }

    private AdminStudentViewDTO.Attempt attemptView(AttemptResultDTO result) {
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
        return new AdminStudentViewDTO.Attempt(
                result.examType() == null ? "Lượt thi" : result.examType(),
                date,
                status,
                scores.isEmpty() ? "Chưa chấm" : String.join(" · ", scores));
    }

    private User studentsOnly(Integer id) {
        User user =
                userService
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
