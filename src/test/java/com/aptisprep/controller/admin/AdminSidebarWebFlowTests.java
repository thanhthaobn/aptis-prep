package com.aptisprep.controller.admin;

import static com.aptisprep.security.SessionMvcRequests.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aptisprep.entity.AccountUnlockRequest;
import com.aptisprep.entity.User;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UnlockRequestStatus;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.repository.AccountUnlockRequestRepository;
import com.aptisprep.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AdminSidebarWebFlowTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountUnlockRequestRepository unlockRequestRepository;

    private static final String ADMIN_EMAIL = "admin-sidebar-test@example.com";

    @BeforeEach
    void setUp() {
        unlockRequestRepository.deleteAll();

        userRepository
                .findByEmailIgnoreCase(ADMIN_EMAIL)
                .orElseGet(
                        () -> {
                            User admin = new User();
                            admin.setEmail(ADMIN_EMAIL);
                            admin.setFullName("Admin Sidebar Test");
                            admin.setRole(Role.ADMIN);
                            admin.setStatus(UserStatus.ACTIVE);
                            admin.setPasswordHash("test-hash");
                            return userRepository.saveAndFlush(admin);
                        });
    }

    @ParameterizedTest(
            name =
                    "Admin GET URL [{0}] must have shared sidebar with unlock-requests and exactly one active link")
    @ValueSource(
            strings = {
                "/admin/dashboard",
                "/admin/students",
                "/admin/unlock-requests",
                "/admin/students/unlock-requests",
                "/admin/lessons",
                "/admin/lessons/create",
                "/admin/questions",
                "/admin/questions/create",
                "/admin/exams",
                "/admin/exams/create",
                "/admin/results",
                "/admin/flashcards",
                "/admin/flashcards/create",
                "/admin/certificates",
                "/admin/certificates/create",
                "/admin/scoring-schemes",
                "/admin/scoring-schemes/create"
            })
    void adminPages_mustHaveUnlockRequestsInSidebarAndExactlyOneActiveMenu(String url)
            throws Exception {
        MvcResult result =
                mockMvc.perform(get(url).with(user(ADMIN_EMAIL).roles("ADMIN")))
                        .andExpect(status().isOk())
                        .andReturn();

        String html = result.getResponse().getContentAsString();

        // Must have the shared sidebar
        assertThat(html).contains("class=\"app-sidebar\"");

        // Must have unlock-requests link in sidebar
        assertThat(html).contains("href=\"/admin/unlock-requests\"");
        assertThat(html).contains("Yêu cầu mở khóa");

        // Must have Flashcard and Scoring in sidebar
        assertThat(html).contains("href=\"/admin/flashcards\"");
        assertThat(html).contains("href=\"/admin/scoring-schemes\"");
        assertThat(html).contains("href=\"/admin/certificates\"");

        // Count how many sidebar-links are active within the sidebar
        Pattern sidebarPattern =
                Pattern.compile(
                        "<aside[^>]*class=\"[^\"]*app-sidebar[^\"]*\"[\\s\\S]*?</aside>",
                        Pattern.CASE_INSENSITIVE);
        Matcher sidebarMatcher = sidebarPattern.matcher(html);
        assertThat(sidebarMatcher.find())
                .as("Sidebar <aside class='app-sidebar'> must be present on " + url)
                .isTrue();

        String sidebarHtml = sidebarMatcher.group();

        // Find all active sidebar-links in the sidebar
        Pattern activeLinkPattern =
                Pattern.compile(
                        "<a[^>]*class=\"[^\"]*sidebar-link[^\"]*active[^\"]*\"[^>]*>",
                        Pattern.CASE_INSENSITIVE);
        Matcher activeMatcher = activeLinkPattern.matcher(sidebarHtml);

        int activeCount = 0;
        while (activeMatcher.find()) {
            activeCount++;
        }

        assertThat(activeCount)
                .as("Page " + url + " must have exactly ONE active sidebar menu item")
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Trang /admin/unlock-requests must have full sidebar including Flashcard and Scoring, and unlock-requests is active")
    void unlockRequestsPage_mustHaveCompleteSidebarAndCorrectActiveItem() throws Exception {
        MvcResult result =
                mockMvc.perform(
                                get("/admin/unlock-requests")
                                        .with(user(ADMIN_EMAIL).roles("ADMIN")))
                        .andExpect(status().isOk())
                        .andReturn();

        String html = result.getResponse().getContentAsString();

        // Contains all 10 sidebar navigation items
        assertThat(html).contains("/admin/dashboard");
        assertThat(html).contains("/admin/students");
        assertThat(html).contains("/admin/unlock-requests");
        assertThat(html).contains("/admin/lessons");
        assertThat(html).contains("/admin/questions");
        assertThat(html).contains("/admin/exams");
        assertThat(html).contains("/admin/results");
        assertThat(html).contains("/admin/flashcards");
        assertThat(html).contains("/admin/certificates");
        assertThat(html).contains("/admin/scoring-schemes");

        // In sidebar, only unlock-requests link has active class
        Pattern sidebarPattern =
                Pattern.compile(
                        "<aside[^>]*class=\"[^\"]*app-sidebar[^\"]*\"[\\s\\S]*?</aside>",
                        Pattern.CASE_INSENSITIVE);
        Matcher sidebarMatcher = sidebarPattern.matcher(html);
        assertThat(sidebarMatcher.find()).isTrue();
        String sidebarHtml = sidebarMatcher.group();

        // Check unlock-requests link has class 'active'
        Pattern unlockActivePattern =
                Pattern.compile(
                        "<a[^>]*href=\"/admin/unlock-requests\"[^>]*class=\"[^\"]*active[^\"]*\"|<a[^>]*class=\"[^\"]*active[^\"]*\"[^>]*href=\"/admin/unlock-requests\"",
                        Pattern.CASE_INSENSITIVE);
        assertThat(unlockActivePattern.matcher(sidebarHtml).find())
                .as("unlock-requests link must be marked active on /admin/unlock-requests")
                .isTrue();

        // Check lessons link does NOT have class 'active'
        Pattern lessonsActivePattern =
                Pattern.compile(
                        "<a[^>]*href=\"/admin/lessons\"[^>]*class=\"[^\"]*active[^\"]*\"|<a[^>]*class=\"[^\"]*active[^\"]*\"[^>]*href=\"/admin/lessons\"",
                        Pattern.CASE_INSENSITIVE);
        assertThat(lessonsActivePattern.matcher(sidebarHtml).find())
                .as("lessons link must NOT be marked active on /admin/unlock-requests")
                .isFalse();
    }

    @Test
    @DisplayName("Sidebar badge displays pending unlock requests count when > 0, and hides when 0")
    void sidebarBadge_displaysPendingCountWhenGreaterThanZero() throws Exception {
        // Initially 0 pending requests
        MvcResult result0 =
                mockMvc.perform(get("/admin/dashboard").with(user(ADMIN_EMAIL).roles("ADMIN")))
                        .andExpect(status().isOk())
                        .andReturn();

        String html0 = result0.getResponse().getContentAsString();
        assertThat(html0).doesNotContain("class=\"sidebar-badge\"");

        // Create 2 pending requests
        User student = new User();
        student.setEmail("student-pending@example.com");
        student.setFullName("Student Pending");
        student.setRole(Role.STUDENT);
        student.setStatus(UserStatus.LOCKED);
        student.setPasswordHash("test-hash");
        student = userRepository.saveAndFlush(student);

        AccountUnlockRequest req1 = new AccountUnlockRequest();
        req1.setUser(student);
        req1.setMessage("Unlock please 1");
        req1.setStatus(UnlockRequestStatus.PENDING);
        req1.setCreatedAt(LocalDateTime.now());
        unlockRequestRepository.saveAndFlush(req1);

        // When 1 pending request: badge should show "1"
        MvcResult result1 =
                mockMvc.perform(get("/admin/lessons").with(user(ADMIN_EMAIL).roles("ADMIN")))
                        .andExpect(status().isOk())
                        .andReturn();

        String html1 = result1.getResponse().getContentAsString();
        assertThat(html1).contains("class=\"sidebar-badge\"");
        assertThat(html1).contains(">1</span>");
    }
}
