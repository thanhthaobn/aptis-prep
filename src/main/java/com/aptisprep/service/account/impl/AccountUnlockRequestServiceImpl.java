package com.aptisprep.service.account.impl;

import com.aptisprep.dto.AccountUnlockRequestDTO;
import com.aptisprep.entity.AccountUnlockRequest;
import com.aptisprep.entity.User;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UnlockRequestStatus;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.exception.AccessDeniedException;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.AccountUnlockRequestRepository;
import com.aptisprep.service.account.AccountUnlockRequestService;
import com.aptisprep.service.account.UserService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountUnlockRequestServiceImpl implements AccountUnlockRequestService {

    private final AccountUnlockRequestRepository accountUnlockRequestRepository;
    private final UserService userService;

    public AccountUnlockRequestServiceImpl(
            AccountUnlockRequestRepository accountUnlockRequestRepository,
            UserService userService) {
        this.accountUnlockRequestRepository = accountUnlockRequestRepository;
        this.userService = userService;
    }

    @Override
    @Transactional
    public AccountUnlockRequestDTO submit(String email, String password, String message) {
        var normalizedMessage = message == null ? "" : message.trim();
        if (normalizedMessage.isBlank()) {
            throw new BusinessRuleException("Vui lòng nhập nội dung yêu cầu");
        }
        if (normalizedMessage.length() > 1000) {
            throw new BusinessRuleException("Nội dung yêu cầu tối đa 1000 ký tự");
        }

        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        User user = userService.getUserByEmailAndPassword(normalizedEmail, password);
        if (user == null) {
            throw new BusinessRuleException("Email hoặc mật khẩu không đúng.");
        }

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "Tài khoản đang hoạt động bình thường, không cần gửi yêu cầu.");
        }

        if (accountUnlockRequestRepository.existsByUserIdAndStatus(
                user.getId(), UnlockRequestStatus.PENDING)) {
            throw new BusinessRuleException("Bạn đã có yêu cầu đang chờ xử lý.");
        }

        AccountUnlockRequest request = new AccountUnlockRequest();
        request.setUser(user);
        request.setMessage(normalizedMessage);
        request.setStatus(UnlockRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        AccountUnlockRequest saved = accountUnlockRequestRepository.save(request);
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountUnlockRequestDTO> findLatestByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return accountUnlockRequestRepository
                .findFirstByUser_EmailIgnoreCaseOrderByCreatedAtDesc(
                        email.trim().toLowerCase(Locale.ROOT))
                .map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountUnlockRequestDTO> list(String statusFilter) {
        if (statusFilter == null
                || statusFilter.isBlank()
                || "ALL".equalsIgnoreCase(statusFilter.trim())) {
            return accountUnlockRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                    .map(this::toDTO)
                    .toList();
        }
        try {
            UnlockRequestStatus status =
                    UnlockRequestStatus.valueOf(statusFilter.trim().toUpperCase(Locale.ROOT));
            return accountUnlockRequestRepository.findByStatusOrderByCreatedAtDesc(status).stream()
                    .map(this::toDTO)
                    .toList();
        } catch (IllegalArgumentException e) {
            return accountUnlockRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                    .map(this::toDTO)
                    .toList();
        }
    }

    @Override
    @Transactional
    public AccountUnlockRequestDTO approve(String adminEmail, Integer requestId, String adminNote) {
        User admin = requireAdmin(adminEmail);
        AccountUnlockRequest request =
                accountUnlockRequestRepository
                        .findById(requestId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy yêu cầu mở khóa"));

        User target =
                userService
                        .findByIdForUpdate(request.getUser().getId())
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy học viên"));

        if (request.getStatus() != UnlockRequestStatus.PENDING) {
            throw new BusinessRuleException("Yêu cầu này đã được xử lý");
        }

        var normalizedNote = adminNote == null || adminNote.isBlank() ? null : adminNote.trim();
        if (normalizedNote != null && normalizedNote.length() > 1000) {
            throw new BusinessRuleException("Ghi chú của quản trị viên tối đa 1000 ký tự");
        }

        target.setStatus(UserStatus.ACTIVE);
        target.setLockReason(null);
        target.setRemovalReason(null);
        userService.save(target);

        request.setStatus(UnlockRequestStatus.APPROVED);
        request.setAdminNote(normalizedNote);
        request.setHandledBy(admin);
        request.setHandledAt(LocalDateTime.now());
        AccountUnlockRequest saved = accountUnlockRequestRepository.save(request);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public AccountUnlockRequestDTO reject(String adminEmail, Integer requestId, String adminNote) {
        var normalizedNote = adminNote == null ? "" : adminNote.trim();
        if (normalizedNote.isBlank()) {
            throw new BusinessRuleException("Vui lòng nhập lý do từ chối");
        }
        if (normalizedNote.length() > 1000) {
            throw new BusinessRuleException("Lý do từ chối tối đa 1000 ký tự");
        }

        User admin = requireAdmin(adminEmail);
        AccountUnlockRequest request =
                accountUnlockRequestRepository
                        .findById(requestId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy yêu cầu mở khóa"));

        User target =
                userService
                        .findByIdForUpdate(request.getUser().getId())
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy học viên"));

        if (request.getStatus() != UnlockRequestStatus.PENDING) {
            throw new BusinessRuleException("Yêu cầu này đã được xử lý");
        }

        request.setStatus(UnlockRequestStatus.REJECTED);
        request.setAdminNote(normalizedNote);
        request.setHandledBy(admin);
        request.setHandledAt(LocalDateTime.now());
        AccountUnlockRequest saved = accountUnlockRequestRepository.save(request);
        return toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPending() {
        return accountUnlockRequestRepository.countByStatus(UnlockRequestStatus.PENDING);
    }

    private User requireAdmin(String adminEmail) {
        User admin =
                userService
                        .findByEmailIgnoreCase(adminEmail)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Không có quyền thực hiện thao tác"));
        if (admin.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Chỉ quản trị viên mới được quản lý học viên");
        }
        return admin;
    }

    private AccountUnlockRequestDTO toDTO(AccountUnlockRequest request) {
        User user = request.getUser();
        User handledBy = request.getHandledBy();
        return new AccountUnlockRequestDTO(
                request.getId(),
                user == null ? null : user.getId(),
                user == null ? null : user.getEmail(),
                user == null ? null : user.getFullName(),
                user == null ? null : user.getStatus().name(),
                request.getMessage(),
                request.getStatus(),
                request.getAdminNote(),
                handledBy == null ? null : handledBy.getEmail(),
                request.getCreatedAt(),
                request.getHandledAt());
    }
}
