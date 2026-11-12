package com.aptisprep.mvc.service.account.impl;

import com.aptisprep.mvc.dto.AccountUnlockRequestDTO;
import com.aptisprep.mvc.entity.AccountUnlockRequest;
import com.aptisprep.mvc.entity.User;
import com.aptisprep.mvc.enums.Role;
import com.aptisprep.mvc.enums.UnlockRequestStatus;
import com.aptisprep.mvc.enums.UserStatus;
import com.aptisprep.mvc.exception.AccessDeniedException;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.exception.ResourceNotFoundException;
import com.aptisprep.mvc.repository.AccountUnlockRequestRepository;
import com.aptisprep.mvc.repository.UserRepository;
import com.aptisprep.mvc.service.account.AccountUnlockRequestService;
import com.aptisprep.mvc.service.auth.UserService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountUnlockRequestServiceImpl implements AccountUnlockRequestService {
    private final AccountUnlockRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public AccountUnlockRequestServiceImpl(
            AccountUnlockRequestRepository requestRepository,
            UserRepository userRepository,
            UserService userService) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    @Transactional
    public AccountUnlockRequestDTO submit(String email, String password, String message) {
        String normalizedMessage = message == null ? "" : message.trim();
        if (normalizedMessage.isBlank())
            throw new BusinessRuleException("Vui lòng nhập nội dung yêu cầu");
        if (normalizedMessage.length() > 1000)
            throw new BusinessRuleException("Nội dung yêu cầu tối đa 1000 ký tự");

        User user = userService.getUserByEmailAndPassword(email, password);
        if (user == null) throw new BusinessRuleException("Email hoặc mật khẩu không đúng.");
        if (user.getRole() != Role.STUDENT)
            throw new BusinessRuleException("Chỉ học viên mới có thể gửi yêu cầu.");
        if (user.getStatus() == UserStatus.ACTIVE)
            throw new BusinessRuleException("Tài khoản đang hoạt động bình thường.");
        if (requestRepository.existsByUserIdAndStatus(user.getId(), UnlockRequestStatus.PENDING)) {
            throw new BusinessRuleException("Bạn đã có yêu cầu đang chờ xử lý.");
        }

        AccountUnlockRequest request = new AccountUnlockRequest();
        request.setUser(user);
        request.setMessage(normalizedMessage);
        request.setStatus(UnlockRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        return toDto(requestRepository.saveAndFlush(request));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountUnlockRequestDTO> findLatestByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        return requestRepository
                .findFirstByUser_EmailIgnoreCaseOrderByCreatedAtDesc(email.trim())
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountUnlockRequestDTO> list(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return requestRepository.findAllByOrderByCreatedAtDesc().stream()
                    .map(this::toDto)
                    .toList();
        }
        try {
            return requestRepository
                    .findByStatusOrderByCreatedAtDesc(
                            UnlockRequestStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)))
                    .stream()
                    .map(this::toDto)
                    .toList();
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("Trạng thái yêu cầu không hợp lệ");
        }
    }

    @Override
    @Transactional
    public AccountUnlockRequestDTO approve(String adminEmail, Integer requestId, String adminNote) {
        User admin = requireAdmin(adminEmail);
        AccountUnlockRequest request =
                requestRepository
                        .findById(requestId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy yêu cầu mở khóa"));
        User target =
                userRepository
                        .findByIdForUpdate(request.getUser().getId())
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy học viên"));
        ensurePending(request);
        String note = normalizeNote(adminNote, false);
        target.setStatus(UserStatus.ACTIVE);
        target.setLockReason(null);
        request.setStatus(UnlockRequestStatus.APPROVED);
        request.setAdminNote(note);
        request.setHandledBy(admin);
        request.setHandledAt(LocalDateTime.now());
        return toDto(requestRepository.save(request));
    }

    @Override
    @Transactional
    public AccountUnlockRequestDTO reject(String adminEmail, Integer requestId, String adminNote) {
        User admin = requireAdmin(adminEmail);
        AccountUnlockRequest request =
                requestRepository
                        .findById(requestId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy yêu cầu mở khóa"));
        userRepository
                .findByIdForUpdate(request.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học viên"));
        ensurePending(request);
        String note = normalizeNote(adminNote, true);
        request.setStatus(UnlockRequestStatus.REJECTED);
        request.setAdminNote(note);
        request.setHandledBy(admin);
        request.setHandledAt(LocalDateTime.now());
        return toDto(requestRepository.save(request));
    }

    @Override
    @Transactional(readOnly = true)
    public long countPending() {
        return requestRepository.countByStatus(UnlockRequestStatus.PENDING);
    }

    private User requireAdmin(String email) {
        User admin =
                userRepository
                        .findByEmailIgnoreCase(email)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Không có quyền thực hiện thao tác"));
        if (admin.getRole() != Role.ADMIN)
            throw new AccessDeniedException("Chỉ quản trị viên mới được xử lý yêu cầu");
        return admin;
    }

    private void ensurePending(AccountUnlockRequest request) {
        if (request.getStatus() != UnlockRequestStatus.PENDING)
            throw new BusinessRuleException("Yêu cầu này đã được xử lý");
    }

    private String normalizeNote(String note, boolean required) {
        String value = note == null ? "" : note.trim();
        if (required && value.isBlank())
            throw new BusinessRuleException("Vui lòng nhập lý do từ chối");
        if (value.length() > 1000) throw new BusinessRuleException("Ghi chú tối đa 1000 ký tự");
        return value.isBlank() ? null : value;
    }

    private AccountUnlockRequestDTO toDto(AccountUnlockRequest request) {
        User user = request.getUser();
        User handler = request.getHandledBy();
        return new AccountUnlockRequestDTO(
                request.getId(),
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getStatus().name(),
                request.getMessage(),
                request.getStatus(),
                request.getAdminNote(),
                handler == null ? null : handler.getEmail(),
                request.getCreatedAt(),
                request.getHandledAt());
    }
}
