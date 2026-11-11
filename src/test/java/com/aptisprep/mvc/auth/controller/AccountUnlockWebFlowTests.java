package com.aptisprep.mvc.auth.controller;

import static com.aptisprep.mvc.common.security.SessionMvcRequests.csrf;
import static com.aptisprep.mvc.common.security.SessionMvcRequests.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aptisprep.mvc.auth.entity.AccountUnlockRequest;
import com.aptisprep.mvc.auth.entity.User;
import com.aptisprep.mvc.auth.repository.AccountUnlockRequestRepository;
import com.aptisprep.mvc.auth.repository.UserRepository;
import com.aptisprep.mvc.auth.service.impl.PasswordHasher;
import com.aptisprep.mvc.common.enums.Role;
import com.aptisprep.mvc.common.enums.UnlockRequestStatus;
import com.aptisprep.mvc.common.enums.UserStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AccountUnlockWebFlowTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository users;
    @Autowired private AccountUnlockRequestRepository unlockRequests;
    @Autowired private PasswordHasher passwords;

    private User lockedUser;
    private User inactiveUser;
    private User activeUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        unlockRequests.deleteAll();

        adminUser =
                users.findByEmailIgnoreCase("admin-unlock@example.com")
                        .orElseGet(
                                () -> {
                                    User u = new User();
                                    u.setEmail("admin-unlock@example.com");
                                    u.setFullName("Admin Unlock");
                                    u.setRole(Role.ADMIN);
                                    u.setStatus(UserStatus.ACTIVE);
                                    u.setPasswordHash(passwords.encode("AdminPass-123"));
                                    return users.saveAndFlush(u);
                                });

        lockedUser =
                users.findByEmailIgnoreCase("locked-student@example.com")
                        .orElseGet(
                                () -> {
                                    User u = new User();
                                    u.setEmail("locked-student@example.com");
                                    u.setFullName("Locked Student");
                                    u.setRole(Role.STUDENT);
                                    u.setStatus(UserStatus.LOCKED);
                                    u.setLockReason("Vi phạm quy chế thi cử");
                                    u.setPasswordHash(passwords.encode("LockedPass-123"));
                                    return users.saveAndFlush(u);
                                });
        lockedUser.setStatus(UserStatus.LOCKED);
        lockedUser.setLockReason("Vi phạm quy chế thi cử");
        lockedUser.setRemovalReason(null);
        users.saveAndFlush(lockedUser);

        inactiveUser =
                users.findByEmailIgnoreCase("inactive-student@example.com")
                        .orElseGet(
                                () -> {
                                    User u = new User();
                                    u.setEmail("inactive-student@example.com");
                                    u.setFullName("Inactive Student");
                                    u.setRole(Role.STUDENT);
                                    u.setStatus(UserStatus.INACTIVE);
                                    u.setRemovalReason("Ngừng hoạt động theo yêu cầu");
                                    u.setPasswordHash(passwords.encode("InactivePass-123"));
                                    return users.saveAndFlush(u);
                                });
        inactiveUser.setStatus(UserStatus.INACTIVE);
        inactiveUser.setRemovalReason("Ngừng hoạt động theo yêu cầu");
        users.saveAndFlush(inactiveUser);

        activeUser =
                users.findByEmailIgnoreCase("active-student@example.com")
                        .orElseGet(
                                () -> {
                                    User u = new User();
                                    u.setEmail("active-student@example.com");
                                    u.setFullName("Active Student");
                                    u.setRole(Role.STUDENT);
                                    u.setStatus(UserStatus.ACTIVE);
                                    u.setPasswordHash(passwords.encode("ActivePass-123"));
                                    return users.saveAndFlush(u);
                                });
        activeUser.setStatus(UserStatus.ACTIVE);
        users.saveAndFlush(activeUser);
    }

    @Test
    void postWithoutCsrfIsRejectedWithForbidden() throws Exception {
        mockMvc.perform(
                        post("/account-unlock-request")
                                .param("email", "locked-student@example.com")
                                .param("password", "LockedPass-123")
                                .param("message", "Mở khóa giúp tôi"))
                .andExpect(status().isForbidden());
    }

    @Test
    void submitUnlockRequestWithCsrfSucceedsAndCreatesPendingRequest() throws Exception {
        mockMvc.perform(
                        post("/account-unlock-request")
                                .with(csrf())
                                .param("email", "locked-student@example.com")
                                .param("password", "LockedPass-123")
                                .param("message", "Em xin mở khóa tài khoản vì đã hiểu rõ lỗi."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account-locked"))
                .andExpect(flash().attributeExists("successMessage"));

        var requests = unlockRequests.findByStatusOrderByCreatedAtDesc(UnlockRequestStatus.PENDING);
        assertThat(requests).isNotEmpty();
        assertThat(requests.getFirst().getUser().getEmail())
                .isEqualTo("locked-student@example.com");
    }

    @Test
    void accountLockedPageRendersCorrectlyForLockedAccount() throws Exception {
        mockMvc.perform(
                        get("/account-locked")
                                .flashAttr("status", "LOCKED")
                                .flashAttr("lockReason", "Vi phạm quy chế thi cử")
                                .flashAttr("email", "locked-student@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/account-locked"))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "Tài khoản đã bị khóa")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "Vi phạm quy chế thi cử")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "Gửi yêu cầu mở khóa")));
    }

    @Test
    void accountLockedPageRendersCorrectlyForInactiveAccount() throws Exception {
        mockMvc.perform(
                        get("/account-locked")
                                .flashAttr("status", "INACTIVE")
                                .flashAttr("removalReason", "Ngừng hoạt động theo yêu cầu")
                                .flashAttr("email", "inactive-student@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/account-locked"))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "Tài khoản ngừng hoạt động")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "Ngừng hoạt động theo yêu cầu")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "Gửi yêu cầu khôi phục")));
    }

    @Test
    void loginWithLockedAccountRedirectsToAccountLocked() throws Exception {
        mockMvc.perform(
                        post("/login")
                                .with(csrf())
                                .param("email", "locked-student@example.com")
                                .param("password", "LockedPass-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account-locked"))
                .andExpect(flash().attribute("status", "LOCKED"))
                .andExpect(flash().attribute("lockReason", "Vi phạm quy chế thi cử"))
                .andExpect(flash().attribute("email", "locked-student@example.com"));
    }

    @Test
    void loginWithInactiveAccountRedirectsToAccountLockedWithRemovalReason() throws Exception {
        mockMvc.perform(
                        post("/login")
                                .with(csrf())
                                .param("email", "inactive-student@example.com")
                                .param("password", "InactivePass-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account-locked"))
                .andExpect(flash().attribute("status", "INACTIVE"))
                .andExpect(flash().attribute("removalReason", "Ngừng hoạt động theo yêu cầu"))
                .andExpect(flash().attribute("email", "inactive-student@example.com"));
    }

    @Test
    void duplicateRegistrationEmailDoesNotLeakAccountStatus() throws Exception {
        String expectedMessage =
                "Email đã được sử dụng. Nếu đây là tài khoản của bạn, hãy đăng nhập để xem trạng"
                        + " thái hoặc gửi yêu cầu mở khóa.";

        // Test with ACTIVE user email
        mockMvc.perform(
                        post("/register")
                                .with(csrf())
                                .param("fullName", "Nguyen Van A")
                                .param("email", "active-student@example.com")
                                .param("password", "Pass-Valid-123")
                                .param("confirmPassword", "Pass-Valid-123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(
                        model().attributeHasFieldErrorCode(
                                        "registerForm", "email", "email.duplicate"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(expectedMessage)))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString("ACTIVE"))));

        // Test with LOCKED user email
        mockMvc.perform(
                        post("/register")
                                .with(csrf())
                                .param("fullName", "Nguyen Van B")
                                .param("email", "locked-student@example.com")
                                .param("password", "Pass-Valid-123")
                                .param("confirmPassword", "Pass-Valid-123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(
                        model().attributeHasFieldErrorCode(
                                        "registerForm", "email", "email.duplicate"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(expectedMessage)))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString("LOCKED"))));

        // Test with INACTIVE user email
        mockMvc.perform(
                        post("/register")
                                .with(csrf())
                                .param("fullName", "Nguyen Van C")
                                .param("email", "inactive-student@example.com")
                                .param("password", "Pass-Valid-123")
                                .param("confirmPassword", "Pass-Valid-123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(
                        model().attributeHasFieldErrorCode(
                                        "registerForm", "email", "email.duplicate"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(expectedMessage)))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString("INACTIVE"))));
    }

    @Test
    void adminCanApproveUnlockRequest() throws Exception {
        AccountUnlockRequest req = new AccountUnlockRequest();
        req.setUser(lockedUser);
        req.setMessage("Xin mở khóa");
        req.setStatus(UnlockRequestStatus.PENDING);
        req.setCreatedAt(LocalDateTime.now());
        req = unlockRequests.saveAndFlush(req);

        mockMvc.perform(
                        post("/admin/unlock-requests/" + req.getId() + "/approve")
                                .with(user("admin-unlock@example.com").roles("ADMIN"))
                                .with(csrf())
                                .param("adminNote", "Đã phê duyệt"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/unlock-requests"))
                .andExpect(flash().attributeExists("successMessage"));

        User refreshedUser = users.findById(lockedUser.getId()).orElseThrow();
        assertThat(refreshedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(refreshedUser.getLockReason()).isNull();

        AccountUnlockRequest refreshedReq = unlockRequests.findById(req.getId()).orElseThrow();
        assertThat(refreshedReq.getStatus()).isEqualTo(UnlockRequestStatus.APPROVED);
        assertThat(refreshedReq.getAdminNote()).isEqualTo("Đã phê duyệt");
    }

    @Test
    void adminCanRejectUnlockRequestWithMandatoryNote() throws Exception {
        AccountUnlockRequest req = new AccountUnlockRequest();
        req.setUser(lockedUser);
        req.setMessage("Xin mở khóa");
        req.setStatus(UnlockRequestStatus.PENDING);
        req.setCreatedAt(LocalDateTime.now());
        req = unlockRequests.saveAndFlush(req);

        mockMvc.perform(
                        post("/admin/unlock-requests/" + req.getId() + "/reject")
                                .with(user("admin-unlock@example.com").roles("ADMIN"))
                                .with(csrf())
                                .param("adminNote", "Lý do từ chối: Không đủ điều kiện"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/unlock-requests"))
                .andExpect(flash().attributeExists("successMessage"));

        User refreshedUser = users.findById(lockedUser.getId()).orElseThrow();
        assertThat(refreshedUser.getStatus()).isEqualTo(UserStatus.LOCKED);

        AccountUnlockRequest refreshedReq = unlockRequests.findById(req.getId()).orElseThrow();
        assertThat(refreshedReq.getStatus()).isEqualTo(UnlockRequestStatus.REJECTED);
        assertThat(refreshedReq.getAdminNote()).isEqualTo("Lý do từ chối: Không đủ điều kiện");
    }

    @Test
    void adminCanRestoreInactiveStudent() throws Exception {
        mockMvc.perform(
                        post("/admin/students/" + inactiveUser.getId() + "/restore")
                                .with(user("admin-unlock@example.com").roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/students/" + inactiveUser.getId()))
                .andExpect(flash().attributeExists("successMessage"));

        User refreshedUser = users.findById(inactiveUser.getId()).orElseThrow();
        assertThat(refreshedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(refreshedUser.getRemovalReason()).isNull();
    }
}
