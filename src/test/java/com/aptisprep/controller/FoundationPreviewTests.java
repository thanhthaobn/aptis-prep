package com.aptisprep.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aptisprep.entity.User;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FoundationPreviewTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    @BeforeEach
    void seedRealStudent() {
        if (users.findByEmailIgnoreCase("new@example.com").isEmpty()) {
            User value = new User();
            value.setFullName("New Student");
            value.setEmail("new@example.com");
            value.setPasswordHash("test-hash");
            value.setRole(Role.STUDENT);
            value.setStatus(UserStatus.ACTIVE);
            users.save(value);
        }
    }

    @Test
    void profileAndPlacementRenderWithoutWrites() throws Exception {
        mvc.perform(get("/student/profile").with(user("new@example.com").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("new@example.com")))
                .andExpect(content().string(containsString("Chuỗi ngày học của bạn")))
                .andExpect(content().string(containsString("activity-weeks")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString("Giao diện minh họa"))));
        mvc.perform(get("/student/placement").with(user("new@example.com").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Học trước, kiểm tra sau")));
        mvc.perform(get("/admin/certificates").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("api-key"))))
                .andExpect(content().string(not(containsString("spring.datasource"))));
    }

    @Test
    void accountScreensEnforceRoles() throws Exception {
        for (String path : new String[] {"/student/profile", "/student/placement"}) {
            mvc.perform(get(path)).andExpect(status().is3xxRedirection());
            mvc.perform(get(path).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/admin/settings").with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void privateUploadsAreNotServedDirectly() throws Exception {
        for (String role : new String[] {"STUDENT", "ADMIN"}) {
            mvc.perform(get("/uploads/recording.wav").with(user("demo").roles(role)))
                    .andExpect(status().isForbidden());
        }
    }
}
