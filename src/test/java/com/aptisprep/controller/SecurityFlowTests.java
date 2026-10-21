package com.aptisprep.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.BeforeEach;
import com.aptisprep.entity.User;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.repository.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @BeforeEach
    void seedRealStudents() {
        saveAdminIfMissing();
        saveIfMissing("Demo Student", "student@aptis.local", UserStatus.ACTIVE);
        saveIfMissing("Nguyễn Minh Anh", "minhanh@example.com", UserStatus.ACTIVE);
        saveIfMissing("Trần Hoàng Nam", "hoangnam@example.com", UserStatus.LOCKED);
        saveIfMissing("Lê Thu Hà", "thuha@example.com", UserStatus.ACTIVE);
        saveIfMissing("Phạm Gia Huy", "giahuy@example.com", UserStatus.INACTIVE);
    }

    @Test
    void adminLockUnlockRequiresCsrfAndInvalidatesNextStudentRequest() throws Exception {
        int id = users.findByEmailIgnoreCase("minhanh@example.com").orElseThrow().getId();
        mockMvc.perform(post("/admin/students/" + id + "/lock")
                        .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/students/" + id + "/lock")
                        .with(user("admin@aptis.local").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/students/" + id));
        org.junit.jupiter.api.Assertions.assertEquals(UserStatus.LOCKED,
                users.findById(id).orElseThrow().getStatus());

        mockMvc.perform(get("/student/dashboard")
                        .with(user("minhanh@example.com").roles("STUDENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?locked"));

        mockMvc.perform(post("/admin/students/" + id + "/unlock")
                        .with(user("admin@aptis.local").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        org.junit.jupiter.api.Assertions.assertEquals(UserStatus.ACTIVE,
                users.findById(id).orElseThrow().getStatus());
    }

    @Test
    void publicHomeIsAccessible() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void studentDashboardRequiresLogin() throws Exception {
        mockMvc.perform(get("/student/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void studentCanOpenStudentDashboard() throws Exception {
        mockMvc.perform(get("/student/dashboard").with(user("student@aptis.local").roles("STUDENT")))
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
        mockMvc.perform(get("/student/orientation").with(user("new-student@aptis.local").roles("STUDENT")))
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
        mockMvc.perform(get("/admin/students").param("q", "minh anh")
                        .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(view().name("admin/student/list"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Nguyễn Minh Anh")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Trần Hoàng Nam"))));
    }

    @Test
    void studentFilterAndEmptyStateRender() throws Exception {
        mockMvc.perform(get("/admin/students").param("status", "LOCKED")
                        .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Trần Hoàng Nam")));
        mockMvc.perform(get("/admin/students").param("q", "no-such-student")
                        .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Không tìm thấy học viên phù hợp")));
    }

    @Test
    void studentDetailRendersRealAccounts() throws Exception {
        for (String email : new String[]{"minhanh@example.com", "hoangnam@example.com", "thuha@example.com", "giahuy@example.com"}) {
            int id = users.findByEmailIgnoreCase(email).orElseThrow().getId();
            mockMvc.perform(get("/admin/students/" + id).with(user("admin@aptis.local").roles("ADMIN")))
                    .andExpect(status().isOk()).andExpect(view().name("admin/student/detail"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                            .string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")));
        }
    }

    @Test
    void studentCannotAccessAdminStudentPages() throws Exception {
        int id = users.findByEmailIgnoreCase("minhanh@example.com").orElseThrow().getId();
        for (String url : new String[]{"/admin/students", "/admin/students/" + id}) {
            mockMvc.perform(get(url).with(user("student@aptis.local").roles("STUDENT")))
                    .andExpect(status().isForbidden());
        }
    }

    private void saveIfMissing(String name, String email, UserStatus status) {
        if (users.findByEmailIgnoreCase(email).isPresent()) return;
        User value = new User(); value.setFullName(name); value.setEmail(email); value.setPasswordHash("test-hash"); value.setRole(Role.STUDENT); value.setStatus(status); users.save(value);
    }

    private void saveAdminIfMissing() {
        if (users.findByEmailIgnoreCase("admin@aptis.local").isPresent()) return;
        User value = new User(); value.setFullName("System Admin"); value.setEmail("admin@aptis.local");
        value.setPasswordHash("test-hash"); value.setRole(Role.ADMIN); value.setStatus(UserStatus.ACTIVE); users.save(value);
    }

}
