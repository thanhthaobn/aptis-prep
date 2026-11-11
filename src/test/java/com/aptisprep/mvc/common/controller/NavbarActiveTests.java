package com.aptisprep.mvc.common.controller;

import static com.aptisprep.mvc.common.security.SessionMvcRequests.user;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aptisprep.mvc.auth.entity.User;
import com.aptisprep.mvc.auth.repository.UserRepository;
import com.aptisprep.mvc.common.enums.Role;
import com.aptisprep.mvc.common.enums.UserStatus;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

@SpringBootTest
@AutoConfigureMockMvc
class NavbarActiveTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    @BeforeEach
    void seedRealAccounts() {
        saveIfMissing("Test Student", "student@aptis.local", Role.STUDENT);
        saveIfMissing("Test Admin", "admin@aptis.local", Role.ADMIN);
    }

    @Test
    void homeRemainsActiveEvenAfterRoleLogin() throws Exception {
        for (String role : new String[] {"STUDENT", "ADMIN"}) {
            mvc.perform(get("/").with(user("demo-" + role).roles(role)))
                    .andExpect(status().isOk())
                    .andExpect(activeHref("/"));
        }
    }

    @Test
    void roleAreasRemainActiveOnDetailPages() throws Exception {
        for (String path :
                new String[] {
                    "/student/dashboard", "/student/lessons", "/student/exams", "/student/profile"
                }) {
            mvc.perform(get(path).with(user("student@aptis.local").roles("STUDENT")))
                    .andExpect(status().isOk())
                    .andExpect(activeHref("/student/dashboard"));
        }
        for (String path :
                new String[] {"/admin/dashboard", "/admin/students", "/admin/certificates"}) {
            mvc.perform(get(path).with(user("admin@aptis.local").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(activeHref("/admin/dashboard"));
        }
    }

    @Test
    void anonymousPagesMarkTheirOwnLink() throws Exception {
        for (String path : new String[] {"/", "/login", "/register"}) {
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(activeHref(path));
        }
    }

    private static ResultMatcher activeHref(String expected) {
        return result -> {
            String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
            var nav =
                    Pattern.compile(
                                    "<nav[^>]*aria-label=\"Điều hướng chính\"[^>]*>(.*?)</nav>",
                                    Pattern.DOTALL)
                            .matcher(html);
            assertTrue(nav.find(), "Navbar must render");
            var links =
                    Pattern.compile("<a\\b[^>]*aria-current=\"page\"[^>]*>").matcher(nav.group(1));
            assertTrue(links.find(), "One active navbar link is required");
            String link = links.group();
            assertTrue(link.contains("href=\"" + expected + "\""), link);
            assertTrue(
                    Pattern.compile("class=\"[^\"]*\\bactive\\b[^\"]*\"").matcher(link).find(),
                    link);
            assertFalse(links.find(), "Only one navbar link may be active");
        };
    }

    private void saveIfMissing(String name, String email, Role role) {
        if (users.findByEmailIgnoreCase(email).isPresent()) return;
        User value = new User();
        value.setFullName(name);
        value.setEmail(email);
        value.setPasswordHash("test-hash");
        value.setRole(role);
        value.setStatus(UserStatus.ACTIVE);
        users.save(value);
    }
}
