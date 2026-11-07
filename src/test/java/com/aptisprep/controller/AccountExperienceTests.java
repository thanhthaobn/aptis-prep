package com.aptisprep.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aptisprep.entity.User;
import com.aptisprep.enums.*;
import com.aptisprep.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountExperienceTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired com.aptisprep.repository.LessonRepository lessons;
    @Autowired com.aptisprep.repository.UserSkillProfileRepository profiles;
    @Autowired com.aptisprep.service.learning.LearningPathService learning;

    @Test
    void placementWithoutGoalShowsAssessedLevelAndNextStageSeparately() throws Exception {
        User user = student("placement-path-experience@example.com");
        var profile = new com.aptisprep.entity.UserSkillProfile();
        profile.setUser(user);
        profile.setSkill("READING");
        profile.setCurrentLevel("A2");
        profiles.saveAndFlush(profile);
        var lesson = new com.aptisprep.entity.Lesson();
        lesson.setTitle("Reading B1 experience");
        lesson.setSkill("Reading");
        lesson.setCefrLevel("B1");
        lesson.setStatus("PUBLISHED");
        lesson.setDisplayOrder(9901);
        lessons.saveAndFlush(lesson);
        learning.ensurePaths(user.getId());
        var stage = learning.stages(user.getEmail(), "Reading").getFirst();
        assertThat(stage.currentLevel()).isEqualTo("A2");
        assertThat(stage.targetLevel()).isEqualTo("B1");
        assertThat(stage.goalLevel()).isNull();
        mvc.perform(get("/student/learning-path").with(user(user.getEmail()).roles("STUDENT")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đích chặng")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mục tiêu")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "Đặt mục tiêu cá nhân")));
    }

    @Test
    void passwordChangeInvalidatesAnotherPreviouslyAuthenticatedSession() throws Exception {
        User user = student("session-experience@example.com");
        var result =
                mvc.perform(
                                post("/login")
                                        .with(csrf())
                                        .param("email", user.getEmail())
                                        .param("password", "Hoc-tieng-Anh-2026"))
                        .andExpect(status().is3xxRedirection())
                        .andReturn();
        var session =
                (org.springframework.mock.web.MockHttpSession)
                        result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        user.setPasswordHash(encoder.encode("New-session-password-2026"));
        users.saveAndFlush(user);
        mvc.perform(get("/student/profile").session(session))
                .andExpect(redirectedUrl("/login?passwordChanged"));
    }

    @Test
    void registrationNormalizesBeforeValidationAndSupportsSingleUnicodeName() throws Exception {
        mvc.perform(
                        post("/register")
                                .with(csrf())
                                .param("fullName", "  Thảo  ")
                                .param("email", "  NEW-EXPERIENCE@EXAMPLE.COM  ")
                                .param("password", "Hoc-tieng-Anh-2026")
                                .param("confirmPassword", "Hoc-tieng-Anh-2026"))
                .andExpect(redirectedUrl("/login"));
        User saved = users.findByEmailIgnoreCase("new-experience@example.com").orElseThrow();
        assertThat(saved.getFullName()).isEqualTo("Thảo");
        assertThat(saved.getEmail()).isEqualTo("new-experience@example.com");
    }

    @Test
    void rejectsInvalidNameAndCommonPasswordWithAccessibleFieldErrors() throws Exception {
        mvc.perform(
                        post("/register")
                                .with(csrf())
                                .param("fullName", "12345!!!")
                                .param("email", "invalid-experience@example.com")
                                .param("password", "password123")
                                .param("confirmPassword", "password123"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "fullName", "password"))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "aria-invalid=\"true\"")));
        assertThat(users.existsByEmailIgnoreCase("invalid-experience@example.com")).isFalse();
    }

    @Test
    void duplicateEmailAppearsAtTheEmailField() throws Exception {
        student("duplicate-experience@example.com");
        mvc.perform(
                        post("/register")
                                .with(csrf())
                                .param("fullName", "Thảo")
                                .param("email", " DUPLICATE-EXPERIENCE@example.com ")
                                .param("password", "Hoc-tieng-Anh-2026")
                                .param("confirmPassword", "Hoc-tieng-Anh-2026"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "email"))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "duplicate-experience@example.com")));
    }

    @Test
    void changesPasswordOnlyAfterCheckingCurrentPasswordAndCsrf() throws Exception {
        User student = student("password-experience@example.com");
        mvc.perform(post("/student/password").with(user(student.getEmail()).roles("STUDENT")))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/student/password")
                                .with(user(student.getEmail()).roles("STUDENT"))
                                .with(csrf())
                                .param("currentPassword", "wrong")
                                .param("password", "Mat-khau-moi-2026")
                                .param("confirmPassword", "Mat-khau-moi-2026"))
                .andExpect(view().name("auth/password"));
        assertThat(
                        encoder.matches(
                                "Hoc-tieng-Anh-2026",
                                users.findById(student.getId()).orElseThrow().getPasswordHash()))
                .isTrue();
        mvc.perform(
                        post("/student/password")
                                .with(user(student.getEmail()).roles("STUDENT"))
                                .with(csrf())
                                .param("currentPassword", "Hoc-tieng-Anh-2026")
                                .param("password", "Mat-khau-moi-2026")
                                .param("confirmPassword", "Mat-khau-moi-2026"))
                .andExpect(redirectedUrl("/login"));
        assertThat(
                        encoder.matches(
                                "Mat-khau-moi-2026",
                                users.findById(student.getId()).orElseThrow().getPasswordHash()))
                .isTrue();
    }

    @Test
    void accountAndRecoveryPagesRenderAndFilteredHistoryExplainsEmptyResults() throws Exception {
        student("pages-experience@example.com");
        mvc.perform(get("/forgot-password")).andExpect(status().isOk());
        mvc.perform(get("/reset-password").param("token", "invalid")).andExpect(status().isOk());
        for (String route :
                new String[] {
                    "/student/profile",
                    "/student/password",
                    "/student/dashboard",
                    "/student/learning-path"
                })
            mvc.perform(get(route).with(user("pages-experience@example.com").roles("STUDENT")))
                    .andExpect(status().isOk());
        mvc.perform(
                        get("/student/results")
                                .param("q", "no-match")
                                .with(user("pages-experience@example.com").roles("STUDENT")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "Không có kết quả phù hợp")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "id=\"main-content\"")));
        mvc.perform(
                        get("/student/results")
                                .with(user("pages-experience@example.com").roles("STUDENT")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "Bạn chưa làm bài thi nào")));
    }

    private User student(String email) {
        User user = new User();
        user.setFullName("Thảo");
        user.setEmail(email);
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash(encoder.encode("Hoc-tieng-Anh-2026"));
        return users.saveAndFlush(user);
    }
}
