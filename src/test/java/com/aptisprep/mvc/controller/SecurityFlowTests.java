package com.aptisprep.mvc.controller;

import static com.aptisprep.mvc.security.SessionMvcRequests.csrf;
import static com.aptisprep.mvc.security.SessionMvcRequests.user;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.aptisprep.mvc.entity.User;
import com.aptisprep.mvc.enums.Role;
import com.aptisprep.mvc.enums.UserStatus;
import com.aptisprep.mvc.repository.UserRepository;
import com.aptisprep.mvc.support.auth.*;
import com.aptisprep.mvc.support.auth.PasswordHasher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityFlowTests {

    @Autowired private MockMvc mockMvc;

    @Autowired private UserRepository users;
    @Autowired private PasswordHasher passwords;

    @Test
    void loginRotatesTheSessionAndLogoutInvalidatesIt() throws Exception {
        User student = new User();
        student.setFullName("Session Student");
        student.setEmail("session-login-student@example.com");
        student.setRole(Role.STUDENT);
        student.setStatus(UserStatus.ACTIVE);
        student.setPasswordHash(passwords.encode("Session-Test-Password-42"));
        users.saveAndFlush(student);

        mockMvc.perform(
                        post("/login")
                                .param("email", student.getEmail())
                                .param("password", "Session-Test-Password-42"))
                .andExpect(status().isForbidden());

        var loginPage = mockMvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        var anonymous = (MockHttpSession) loginPage.getRequest().getSession(false);
        assertNotNull(anonymous);
        String anonymousId = anonymous.getId();
        var csrfField =
                Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"")
                        .matcher(loginPage.getResponse().getContentAsString());
        assertTrue(csrfField.find(), "The rendered login form must contain the CSRF field");
        String csrfToken = csrfField.group(1);
        assertFalse(csrfField.find(), "The login form only needs one CSRF field");
        var login =
                mockMvc.perform(
                                post("/login")
                                        .session(anonymous)
                                        .param("_csrf", csrfToken)
                                        .param("email", student.getEmail())
                                        .param("password", "Session-Test-Password-42"))
                        .andExpect(redirectedUrl("/student/dashboard"))
                        .andReturn();
        var authenticated = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(authenticated);
        assertNotEquals(anonymousId, authenticated.getId());
        assertTrue(anonymous.isInvalid());
        mockMvc.perform(get("/student/dashboard").session(authenticated))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/dashboard").session(authenticated))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/logout").session(authenticated)).andExpect(status().isForbidden());
        mockMvc.perform(post("/logout").session(authenticated).with(csrf()))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("successMessage", "Bạn đã đăng xuất."));
        assertTrue(authenticated.isInvalid());
        mockMvc.perform(get("/student/dashboard").session(authenticated))
                .andExpect(redirectedUrl("/login"));
    }

    @BeforeEach
    void seedRealStudents() {
        saveAdminIfMissing();
        saveIfMissing("Test Student", "student@aptis.local", UserStatus.ACTIVE);
        saveIfMissing("Nguyễn Minh Anh", "minhanh@example.com", UserStatus.ACTIVE);
        saveIfMissing("Trần Hoàng Nam", "hoangnam@example.com", UserStatus.LOCKED);
        saveIfMissing("Lê Thu Hà", "thuha@example.com", UserStatus.ACTIVE);
        saveIfMissing("Phạm Gia Huy", "giahuy@example.com", UserStatus.INACTIVE);
    }

    @Test
    void adminLockUnlockRequiresCsrfAndInvalidatesNextStudentRequest() throws Exception {
        int id = users.findByEmailIgnoreCase("minhanh@example.com").orElseThrow().getId();
        mockMvc.perform(
                        post("/admin/students/" + id + "/lock")
                                .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        post("/admin/students/" + id + "/lock")
                                .with(user("admin@aptis.local").roles("ADMIN"))
                                .param("reason", "Vi phạm quy định sử dụng")
                                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/students/" + id));
        org.junit.jupiter.api.Assertions.assertEquals(
                UserStatus.LOCKED, users.findById(id).orElseThrow().getStatus());

        mockMvc.perform(
                        get("/student/dashboard")
                                .with(user("minhanh@example.com").roles("STUDENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?locked"));

        mockMvc.perform(
                        post("/admin/students/" + id + "/unlock")
                                .with(user("admin@aptis.local").roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals(
                UserStatus.ACTIVE, users.findById(id).orElseThrow().getStatus());
    }

    @Test
    void lockedAccountLoginShowsTheAdministratorReason() throws Exception {
        User student = saveIfMissing("Bị khóa", "locked-login@example.com", UserStatus.LOCKED);
        student.setPasswordHash(passwords.encode("Locked-password-42"));
        student.setLockReason("Tài khoản vi phạm quy định");
        users.saveAndFlush(student);

        mockMvc.perform(
                        post("/login")
                                .with(csrf())
                                .param("email", student.getEmail())
                                .param("password", "Locked-password-42"))
                .andExpect(redirectedUrl("/account-locked"))
                .andExpect(flash().attribute("lockReason", "Tài khoản vi phạm quy định"));

        mockMvc.perform(get("/account-locked")).andExpect(status().isOk());
    }

    @Test
    void publicHomeIsAccessible() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("index"));
    }

    @Test
    void registrationWithoutPasswordsReturnsValidationErrorsWithoutCreatingAnAccount()
            throws Exception {
        long before = users.count();
        mockMvc.perform(
                        post("/register")
                                .with(csrf())
                                .param("fullName", "Form validation")
                                .param("email", "missing-password@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(
                        model().attributeHasFieldErrors(
                                        "registerForm", "password", "confirmPassword"));
        org.junit.jupiter.api.Assertions.assertEquals(before, users.count());
    }

    @Test
    void registrationWithDifferentPasswordsReturnsAFieldErrorWithoutCreatingAnAccount()
            throws Exception {
        long before = users.count();
        mockMvc.perform(
                        post("/register")
                                .with(csrf())
                                .param("fullName", "Form validation")
                                .param("email", "mismatched-password@example.com")
                                .param("password", "Test-password-123")
                                .param("confirmPassword", "Test-password-456"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "confirmPassword"));
        org.junit.jupiter.api.Assertions.assertEquals(before, users.count());
    }

    @Test
    void studentDashboardRequiresLogin() throws Exception {
        mockMvc.perform(get("/student/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void studentCanOpenStudentDashboard() throws Exception {
        mockMvc.perform(
                        get("/student/dashboard")
                                .with(user("student@aptis.local").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("student/dashboard"));
    }

    @Test
    void adminCanOpenAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard").with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }

    @Test
    void legacyOrientationRedirectsToLearningPath() throws Exception {
        mockMvc.perform(
                        get("/student/orientation")
                                .with(user("new-student@aptis.local").roles("STUDENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/learning-path"));
    }

    @Test
    void orientationRequiresStudentLogin() throws Exception {
        mockMvc.perform(get("/student/orientation")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/student/orientation").with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanSearchRealStudentsWithoutAccents() throws Exception {
        mockMvc.perform(
                        get("/admin/students")
                                .param("q", "minh anh")
                                .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/student/list"))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                                .string(org.hamcrest.Matchers.containsString("Nguyễn Minh Anh")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "Trần Hoàng Nam"))));
    }

    @Test
    void studentFilterAndEmptyStateRender() throws Exception {
        mockMvc.perform(
                        get("/admin/students")
                                .param("status", "LOCKED")
                                .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                                .string(org.hamcrest.Matchers.containsString("Trần Hoàng Nam")));
        mockMvc.perform(
                        get("/admin/students")
                                .param("q", "no-such-student")
                                .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "Không tìm thấy học viên phù hợp")));
    }

    @Test
    void studentDetailRendersRealAccounts() throws Exception {
        for (String email :
                new String[] {
                    "minhanh@example.com",
                    "hoangnam@example.com",
                    "thuha@example.com",
                    "giahuy@example.com"
                }) {
            int id = users.findByEmailIgnoreCase(email).orElseThrow().getId();
            mockMvc.perform(
                            get("/admin/students/" + id)
                                    .with(user("admin@aptis.local").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/student/detail"))
                    .andExpect(
                            org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                    .content()
                                    .string(
                                            org.hamcrest.Matchers.containsString(
                                                    "name=\"_csrf\"")));
        }
    }

    @Test
    void studentCannotAccessAdminStudentPages() throws Exception {
        int id = users.findByEmailIgnoreCase("minhanh@example.com").orElseThrow().getId();
        for (String url : new String[] {"/admin/students", "/admin/students/" + id}) {
            mockMvc.perform(get(url).with(user("student@aptis.local").roles("STUDENT")))
                    .andExpect(status().isForbidden());
        }
    }

    private User saveIfMissing(String name, String email, UserStatus status) {
        var existing = users.findByEmailIgnoreCase(email);
        if (existing.isPresent()) return existing.get();
        User value = new User();
        value.setFullName(name);
        value.setEmail(email);
        value.setPasswordHash("test-hash");
        value.setRole(Role.STUDENT);
        value.setStatus(status);
        return users.save(value);
    }

    private void saveAdminIfMissing() {
        if (users.findByEmailIgnoreCase("admin@aptis.local").isPresent()) return;
        User value = new User();
        value.setFullName("System Admin");
        value.setEmail("admin@aptis.local");
        value.setPasswordHash("test-hash");
        value.setRole(Role.ADMIN);
        value.setStatus(UserStatus.ACTIVE);
        users.save(value);
    }
}
