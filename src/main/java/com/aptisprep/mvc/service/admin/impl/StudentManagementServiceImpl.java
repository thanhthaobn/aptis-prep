package com.aptisprep.mvc.service.admin.impl;

import com.aptisprep.mvc.dto.UserDTO;
import com.aptisprep.mvc.dto.UserGoalDTO;
import com.aptisprep.mvc.dto.UserSkillProfileDTO;
import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import com.aptisprep.mvc.enums.Skill;
import com.aptisprep.mvc.exception.AccessDeniedException;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.exception.ResourceNotFoundException;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.service.admin.StudentManagementService;
import com.aptisprep.mvc.service.exam.ExamAttemptQueryService;
import com.aptisprep.mvc.support.contract.IntegrationModels.*;
import com.aptisprep.mvc.service.learning.LearningPathService;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentManagementServiceImpl implements StudentManagementService {
    private List<String> SKILLS = Skill.codes();
    private UserRepository userRepository;
    private UserGoalRepository userGoalRepository;
    private UserGoalSkillRepository userGoalSkillRepository;
    private UserSkillProfileRepository userSkillProfileRepository;
    private LearningPathService paths;
    private ExamAttemptQueryService attempts;

    public StudentManagementServiceImpl(
            UserRepository userRepository,
            UserGoalRepository userGoalRepository,
            UserGoalSkillRepository userGoalSkillRepository,
            UserSkillProfileRepository userSkillProfileRepository,
            LearningPathService paths,
            ExamAttemptQueryService attempts) {
        this.userRepository = userRepository;
        this.userGoalRepository = userGoalRepository;
        this.userGoalSkillRepository = userGoalSkillRepository;
        this.userSkillProfileRepository = userSkillProfileRepository;
        this.paths = paths;
        this.attempts = attempts;
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> list(String query, UserStatus status, int page, int size) {
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
        return result.map(streamValue -> UserDTO.convertEntityToDto(streamValue));
    }

    @Transactional(readOnly = true)
    public UserDTO detail(Integer id) {
        return UserDTO.convertEntityToDto(studentsOnly(id));
    }

    @Transactional(readOnly = true)
    public UserGoalDTO goal(Integer userId) {
        studentsOnly(userId);
        return goalDto(userId);
    }

    @Transactional(readOnly = true)
    public List<UserSkillProfileDTO> skills(Integer userId) {
        studentsOnly(userId);
        return userSkillProfileRepository.findByUserIdOrderBySkillAsc(userId).stream()
                .map(streamValue -> UserSkillProfileDTO.convertEntityToDto(streamValue))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PathProgress> progress(Integer userId) {
        studentsOnly(userId);
        List<PathProgress> values = paths.summaries(userId);
        return values == null ? List.of() : values;
    }

    @Transactional(readOnly = true)
    public HistoryPage attempts(Integer userId, int page, int size) {
        studentsOnly(userId);
        return historyFor(userId, Math.max(0, page), Math.min(100, Math.max(1, size)));
    }

    @Transactional(readOnly = true)
    public Map<Integer, String> targetLabels(Collection<Integer> userIds) {
        LinkedHashMap<Integer, String> result = new LinkedHashMap<>();
        for (Integer userId : userIds) {
            UserGoalDTO goal = goalDto(userId);
            List<String> targets = goalTargets(goal);
            result.put(userId, targets.isEmpty() ? "Chưa đặt mục tiêu" : String.join(", ", targets));
        }
        return result;
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

    @Override
    @Transactional
    public void restore(String adminEmail, Integer id) {
        User admin = userRepository.findByEmailIgnoreCase(adminEmail)
                .orElseThrow(() -> new AccessDeniedException("Không có quyền thực hiện thao tác"));
        if (admin.getRole() != Role.ADMIN)
            throw new AccessDeniedException("Chỉ quản trị viên mới được quản lý học viên");
        User target = userRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học viên"));
        if (target.getRole() != Role.STUDENT)
            throw new ResourceNotFoundException("Không tìm thấy học viên");
        if (target.getStatus() != UserStatus.INACTIVE)
            throw new BusinessRuleException("Chỉ có thể khôi phục tài khoản ngừng hoạt động");
        target.setStatus(UserStatus.ACTIVE);
        target.setLockReason(null);
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

    private UserGoalDTO goalDto(Integer userId) {
        UserGoal goal =
                userGoalRepository
                        .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, GoalStatus.ACTIVE)
                        .orElse(null);
        if (goal == null) return null;
        UserGoalDTO dto = UserGoalDTO.convertEntityToDto(goal);
        for (UserGoalSkill skill : userGoalSkillRepository.findByGoalId(goal.getId())) {
            String target = skill.getTargetLevel();
            switch (skill.getSkill()) {
                case "GRAMMAR_VOCABULARY" -> dto.setGrammarVocabulary(target);
                case "READING" -> dto.setReading(target);
                case "LISTENING" -> dto.setListening(target);
                case "WRITING" -> dto.setWriting(target);
                case "SPEAKING" -> dto.setSpeaking(target);
                default -> { }
            }
            if (skill.getTargetScore() != null)
                dto.getTargetScores().put(skill.getSkill(), skill.getTargetScore());
        }
        return dto;
    }

    private List<String> goalTargets(UserGoalDTO goal) {
        if (goal == null) return List.of();
        return SKILLS.stream()
                .map(streamValue -> goal.targetLevel(streamValue))
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    private HistoryPage historyFor(Integer userId, int page, int size) {
        HistoryPage value = attempts.history(userId, page, size, HistoryFilter.empty());
        return value == null ? new HistoryPage(List.of(), page, size, 0) : value;
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







