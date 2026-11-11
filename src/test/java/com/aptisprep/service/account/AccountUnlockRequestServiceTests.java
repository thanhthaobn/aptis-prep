package com.aptisprep.service.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.aptisprep.dto.AccountUnlockRequestDTO;
import com.aptisprep.entity.AccountUnlockRequest;
import com.aptisprep.entity.User;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UnlockRequestStatus;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.exception.AccessDeniedException;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.repository.AccountUnlockRequestRepository;
import com.aptisprep.service.account.impl.AccountUnlockRequestServiceImpl;
import com.aptisprep.service.admin.StudentManagementService;
import com.aptisprep.service.admin.impl.StudentManagementServiceImpl;
import com.aptisprep.service.exam.ExamAttemptService;
import com.aptisprep.service.learning.LearningPathService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class AccountUnlockRequestServiceTests {

    private AccountUnlockRequestRepository requestRepository;
    private UserService userService;
    private AccountUnlockRequestService unlockService;

    @BeforeEach
    void setUp() {
        requestRepository = mock(AccountUnlockRequestRepository.class);
        userService = mock(UserService.class);
        unlockService = new AccountUnlockRequestServiceImpl(requestRepository, userService);
    }

    @Test
    void submitValidRequestForLockedUserCreatesPendingRequest() {
        User user = new User();
        user.setId(10);
        user.setEmail("locked@example.com");
        user.setStatus(UserStatus.LOCKED);
        user.setRole(Role.STUDENT);

        when(userService.getUserByEmailAndPassword("locked@example.com", "Password-123"))
                .thenReturn(user);
        when(requestRepository.existsByUserIdAndStatus(10, UnlockRequestStatus.PENDING))
                .thenReturn(false);
        when(requestRepository.save(any(AccountUnlockRequest.class)))
                .thenAnswer(
                        inv -> {
                            AccountUnlockRequest r = inv.getArgument(0);
                            r.setId(100);
                            return r;
                        });

        AccountUnlockRequestDTO result =
                unlockService.submit("locked@example.com", "Password-123", "Tôi bị khóa nhầm");

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(100);
        assertThat(result.userId()).isEqualTo(10);
        assertThat(result.status()).isEqualTo(UnlockRequestStatus.PENDING);
        assertThat(result.message()).isEqualTo("Tôi bị khóa nhầm");
    }

    @Test
    void submitValidRequestForInactiveUserCreatesPendingRequest() {
        User user = new User();
        user.setId(11);
        user.setEmail("inactive@example.com");
        user.setStatus(UserStatus.INACTIVE);
        user.setRole(Role.STUDENT);

        when(userService.getUserByEmailAndPassword("inactive@example.com", "Password-123"))
                .thenReturn(user);
        when(requestRepository.existsByUserIdAndStatus(11, UnlockRequestStatus.PENDING))
                .thenReturn(false);
        when(requestRepository.save(any(AccountUnlockRequest.class)))
                .thenAnswer(
                        inv -> {
                            AccountUnlockRequest r = inv.getArgument(0);
                            r.setId(101);
                            return r;
                        });

        AccountUnlockRequestDTO result =
                unlockService.submit(
                        "inactive@example.com", "Password-123", "Xin khôi phục tài khoản");

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(101);
        assertThat(result.status()).isEqualTo(UnlockRequestStatus.PENDING);
    }

    @Test
    void duplicatePendingRequestIsBlocked() {
        User user = new User();
        user.setId(10);
        user.setEmail("locked@example.com");
        user.setStatus(UserStatus.LOCKED);

        when(userService.getUserByEmailAndPassword("locked@example.com", "Password-123"))
                .thenReturn(user);
        when(requestRepository.existsByUserIdAndStatus(10, UnlockRequestStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(
                        () ->
                                unlockService.submit(
                                        "locked@example.com", "Password-123", "Gửi lần 2"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Bạn đã có yêu cầu đang chờ xử lý");

        verify(requestRepository, never()).save(any());
    }

    @Test
    void wrongPasswordReturnsGenericAuthenticationError() {
        when(userService.getUserByEmailAndPassword("locked@example.com", "Wrong-Password"))
                .thenReturn(null);

        assertThatThrownBy(
                        () ->
                                unlockService.submit(
                                        "locked@example.com", "Wrong-Password", "Mở khóa giúp tôi"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Email hoặc mật khẩu không đúng.");

        verify(requestRepository, never()).save(any());
    }

    @Test
    void activeAccountSubmissionIsRejected() {
        User user = new User();
        user.setId(12);
        user.setEmail("active@example.com");
        user.setStatus(UserStatus.ACTIVE);

        when(userService.getUserByEmailAndPassword("active@example.com", "Password-123"))
                .thenReturn(user);

        assertThatThrownBy(
                        () ->
                                unlockService.submit(
                                        "active@example.com", "Password-123", "Mở khóa giúp tôi"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Tài khoản đang hoạt động bình thường");

        verify(requestRepository, never()).save(any());
    }

    @Test
    void blankMessageOrExcessiveLengthIsRejected() {
        assertThatThrownBy(() -> unlockService.submit("a@b.com", "pass", "   "))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Vui lòng nhập nội dung yêu cầu");

        String longMsg = "a".repeat(1001);
        assertThatThrownBy(() -> unlockService.submit("a@b.com", "pass", longMsg))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("tối đa 1000 ký tự");
    }

    @Test
    void approvePendingRequestUpdatesUserToActiveAndClearsReasons() {
        User admin = new User();
        admin.setId(1);
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);
        when(userService.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));

        User student = new User();
        student.setId(20);
        student.setEmail("student@example.com");
        student.setStatus(UserStatus.LOCKED);
        student.setLockReason("Vi phạm quy chế");
        student.setRemovalReason("Lý do xóa cũ");

        AccountUnlockRequest req = new AccountUnlockRequest();
        req.setId(50);
        req.setUser(student);
        req.setStatus(UnlockRequestStatus.PENDING);
        req.setMessage("Cho em xin mở khóa");
        req.setCreatedAt(LocalDateTime.now().minusHours(1));

        when(requestRepository.findById(50)).thenReturn(Optional.of(req));
        when(userService.findByIdForUpdate(20)).thenReturn(Optional.of(student));
        when(requestRepository.save(any(AccountUnlockRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AccountUnlockRequestDTO approved =
                unlockService.approve("admin@example.com", 50, "Đã xem xét và mở khóa");

        assertThat(approved.status()).isEqualTo(UnlockRequestStatus.APPROVED);
        assertThat(approved.adminNote()).isEqualTo("Đã xem xét và mở khóa");
        assertThat(approved.handledByEmail()).isEqualTo("admin@example.com");
        assertThat(student.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(student.getLockReason()).isNull();
        assertThat(student.getRemovalReason()).isNull();
        verify(userService).save(student);
    }

    @Test
    void rejectPendingRequestRequiresAdminNoteAndPreservesUserStatus() {
        User admin = new User();
        admin.setId(1);
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);
        when(userService.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));

        User student = new User();
        student.setId(20);
        student.setStatus(UserStatus.LOCKED);
        student.setLockReason("Gian lận");

        AccountUnlockRequest req = new AccountUnlockRequest();
        req.setId(51);
        req.setUser(student);
        req.setStatus(UnlockRequestStatus.PENDING);
        req.setMessage("Mở khóa giúp em");

        when(requestRepository.findById(51)).thenReturn(Optional.of(req));
        when(userService.findByIdForUpdate(20)).thenReturn(Optional.of(student));
        when(requestRepository.save(any(AccountUnlockRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Rejection with blank note must fail
        assertThatThrownBy(() -> unlockService.reject("admin@example.com", 51, "   "))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Vui lòng nhập lý do từ chối");

        AccountUnlockRequestDTO rejected =
                unlockService.reject("admin@example.com", 51, "Không đủ căn cứ mở khóa");

        assertThat(rejected.status()).isEqualTo(UnlockRequestStatus.REJECTED);
        assertThat(rejected.adminNote()).isEqualTo("Không đủ căn cứ mở khóa");
        assertThat(student.getStatus()).isEqualTo(UserStatus.LOCKED);
        assertThat(student.getLockReason()).isEqualTo("Gian lận");
    }

    @Test
    void concurrentHandlingByTwoAdminsThrowsExceptionForSecondAdmin() {
        User admin = new User();
        admin.setId(1);
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);
        when(userService.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));

        User student = new User();
        student.setId(20);
        student.setStatus(UserStatus.LOCKED);

        AccountUnlockRequest req = new AccountUnlockRequest();
        req.setId(52);
        req.setUser(student);
        // Already processed by another admin
        req.setStatus(UnlockRequestStatus.APPROVED);

        when(requestRepository.findById(52)).thenReturn(Optional.of(req));
        when(userService.findByIdForUpdate(20)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> unlockService.approve("admin@example.com", 52, "Duyệt đồng thời"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Yêu cầu này đã được xử lý");

        assertThatThrownBy(() -> unlockService.reject("admin@example.com", 52, "Từ chối đồng thời"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Yêu cầu này đã được xử lý");
    }

    @Test
    void nonAdminCallerIsRejectedForApproveAndReject() {
        User nonAdmin = new User();
        nonAdmin.setId(2);
        nonAdmin.setEmail("student@example.com");
        nonAdmin.setRole(Role.STUDENT);
        when(userService.findByEmailIgnoreCase("student@example.com"))
                .thenReturn(Optional.of(nonAdmin));

        assertThatThrownBy(() -> unlockService.approve("student@example.com", 1, "note"))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> unlockService.reject("student@example.com", 1, "note"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void removeDoesNotOverwriteLockReasonAndSetsRemovalReason() {
        var accounts = mock(AccountAccessService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<LearningPathService> paths = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<ExamAttemptService> attempts = mock(ObjectProvider.class);
        StudentManagementService studentManagementService =
                new StudentManagementServiceImpl(
                        userService, accounts, paths, attempts, unlockService);

        User admin = new User();
        admin.setId(1);
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);
        when(userService.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));

        User target = new User();
        target.setId(30);
        target.setRole(Role.STUDENT);
        target.setStatus(UserStatus.LOCKED);
        target.setLockReason("Lý do khóa ban đầu");

        when(userService.findByIdForUpdate(30)).thenReturn(Optional.of(target));

        studentManagementService.remove("admin@example.com", 30, "Lý do xóa mềm tài khoản");

        assertThat(target.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(target.getLockReason()).isEqualTo("Lý do khóa ban đầu");
        assertThat(target.getRemovalReason()).isEqualTo("Lý do xóa mềm tài khoản");
    }

    @Test
    void restoreInactiveUserRestoresActiveStatusAndClearsReasons() {
        var accounts = mock(AccountAccessService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<LearningPathService> paths = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<ExamAttemptService> attempts = mock(ObjectProvider.class);
        StudentManagementService studentManagementService =
                new StudentManagementServiceImpl(
                        userService, accounts, paths, attempts, unlockService);

        User admin = new User();
        admin.setId(1);
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);
        when(userService.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));

        User target = new User();
        target.setId(31);
        target.setRole(Role.STUDENT);
        target.setStatus(UserStatus.INACTIVE);
        target.setLockReason("Lý do khóa");
        target.setRemovalReason("Lý do xóa");

        when(userService.findByIdForUpdate(31)).thenReturn(Optional.of(target));

        studentManagementService.restore("admin@example.com", 31);

        assertThat(target.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(target.getLockReason()).isNull();
        assertThat(target.getRemovalReason()).isNull();
    }

    @Test
    void restoreActiveOrLockedUserIsRejected() {
        var accounts = mock(AccountAccessService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<LearningPathService> paths = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<ExamAttemptService> attempts = mock(ObjectProvider.class);
        StudentManagementService studentManagementService =
                new StudentManagementServiceImpl(
                        userService, accounts, paths, attempts, unlockService);

        User admin = new User();
        admin.setId(1);
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);
        when(userService.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));

        User lockedStudent = new User();
        lockedStudent.setId(32);
        lockedStudent.setRole(Role.STUDENT);
        lockedStudent.setStatus(UserStatus.LOCKED);

        when(userService.findByIdForUpdate(32)).thenReturn(Optional.of(lockedStudent));

        assertThatThrownBy(() -> studentManagementService.restore("admin@example.com", 32))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Chỉ có thể khôi phục tài khoản ngừng hoạt động");
    }
}
