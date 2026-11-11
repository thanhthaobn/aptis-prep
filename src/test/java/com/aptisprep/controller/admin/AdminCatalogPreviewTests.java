package com.aptisprep.controller.admin;

import static com.aptisprep.security.SessionMvcRequests.csrf;
import static com.aptisprep.security.SessionMvcRequests.user;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aptisprep.entity.*;
import com.aptisprep.entity.User;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.repository.*;
import com.aptisprep.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AdminCatalogPreviewTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired com.aptisprep.repository.QuestionRepository questions;
    @Autowired com.aptisprep.repository.MockExamRepository exams;
    @Autowired com.aptisprep.repository.CertificateRepository certificates;
    private final java.util.List<Integer> questionIds = new java.util.ArrayList<>();
    private final java.util.List<Integer> examIds = new java.util.ArrayList<>();

    @BeforeEach
    void seedDatabaseContent() {
        var certificate = new com.aptisprep.entity.Certificate();
        certificate.setCode("UI-" + System.nanoTime());
        certificate.setName("Test certificate");
        certificate.setStatus(com.aptisprep.enums.RecordStatus.ACTIVE);
        certificate = certificates.saveAndFlush(certificate);
        for (int i = 0; i < 3; i++) {
            var question = new com.aptisprep.entity.QuestionEntity();
            question.setContent(i == 1 ? "Viết thư cho câu lạc bộ" : "Chọn từ phù hợp trong câu");
            question.setSkill(i == 1 ? "WRITING" : "READING");
            question.setCefrLevel("B1");
            question.setQuestionType(i == 1 ? "WRITING_TEXT" : "SINGLE_CHOICE");
            question.setStatus("DRAFT");
            questionIds.add(questions.saveAndFlush(question).getId());
            var exam = new com.aptisprep.entity.MockExamEntity();
            exam.setCertificateId(certificate.getId());
            exam.setTitle("Database exam " + System.nanoTime());
            exam.setExamType("PRACTICE");
            exam.setSkill("READING");
            exam.setTargetLevel("B1");
            exam.setDurationMinutes(35);
            exam.setStatus("DRAFT");
            examIds.add(exams.saveAndFlush(exam).getId());
        }
    }

    @BeforeEach
    void seedAdmin() {
        if (users.findByEmailIgnoreCase("admin@aptis.local").isEmpty()) {
            User value = new User();
            value.setFullName("Test Admin");
            value.setEmail("admin@aptis.local");
            value.setPasswordHash("test");
            value.setRole(Role.ADMIN);
            value.setStatus(UserStatus.ACTIVE);
            users.save(value);
        }
    }

    @Test
    void allCatalogPagesAndFormsRenderForAdmin() throws Exception {
        String[] modules = {"questions", "exams"};
        int[] ids = {questionIds.getFirst(), examIds.getFirst()};
        for (int i = 0; i < modules.length; i++) {
            String base = "/admin/" + modules[i];
            mvc.perform(get(base).with(user("admin@aptis.local").roles("ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/catalog/list"));
            for (int offset = 0; offset < 3; offset++) {
                int id = i == 0 ? questionIds.get(offset) : examIds.get(offset);
                mvc.perform(get(base + "/" + id).with(user("admin@aptis.local").roles("ADMIN")))
                        .andExpect(status().isOk())
                        .andExpect(view().name("admin/catalog/detail"));
            }
            if (!modules[i].equals("results")) {
                for (String path : new String[] {base + "/create", base + "/" + ids[i] + "/edit"}) {
                    mvc.perform(get(path).with(user("admin@aptis.local").roles("ADMIN")))
                            .andExpect(status().isOk())
                            .andExpect(view().name("admin/catalog/form"));
                }
            }
        }
        mvc.perform(get("/admin/results").with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/result/list"));
    }

    @Test
    void searchFiltersAndEmptyStateRender() throws Exception {
        mvc.perform(
                        get("/admin/questions")
                                .param("skill", "WRITING")
                                .param("status", "DRAFT")
                                .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Viết thư cho câu lạc bộ")))
                .andExpect(content().string(not(containsString("Chọn từ phù hợp trong câu"))));
        mvc.perform(
                        get("/admin/results")
                                .param("q", "minh anh")
                                .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Không có kết quả phù hợp")));
    }

    @Test
    void catalogPagesRejectStudentRole() throws Exception {
        for (String path :
                new String[] {
                    "/admin/lessons",
                    "/admin/questions/3001",
                    "/admin/exams/create",
                    "/admin/results/5001"
                }) {
            mvc.perform(get(path).with(user("student@aptis.local").roles("STUDENT")))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void catalogRoutesDoNotCaptureOtherAdminModules() throws Exception {
        for (String path :
                new String[] {
                    "/admin/not-a-module", "/admin/not-a-module/1",
                    "/admin/not-a-module/create", "/admin/not-a-module/1/edit"
                }) {
            mvc.perform(get(path).with(user("admin@aptis.local").roles("ADMIN")))
                    .andExpect(status().isNotFound());
        }
        mvc.perform(
                        get("/admin/certificates/create")
                                .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/settings/certificate"));
        mvc.perform(
                        get("/admin/scoring-schemes/create")
                                .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/settings/scheme"));
    }

    @Test
    void publishedExamCanBeStoppedFromAdminDetail() throws Exception {
        var exam = exams.findById(examIds.getFirst()).orElseThrow();
        exam.setStatus("PUBLISHED");
        exams.saveAndFlush(exam);

        mvc.perform(
                        post("/admin/exams/{id}/unpublish", exam.getId())
                                .with(user("admin@aptis.local").roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/exams/" + exam.getId()));

        assertEquals("ARCHIVED", exams.findById(exam.getId()).orElseThrow().getStatus());
    }

    @Test
    void catalogAndMediaRoutesRejectIdsThatCannotBeConvertedToIntegers() throws Exception {
        for (String path :
                new String[] {
                    "/admin/questions/not-an-id",
                    "/admin/exams/not-an-id/edit",
                    "/admin/questions/1/media/not-an-id/content",
                    "/admin/certificates/not-an-id",
                    "/admin/scoring-schemes/not-an-id/edit",
                    "/admin/results/not-an-id"
                }) {
            mvc.perform(get(path).with(user("admin@aptis.local").roles("ADMIN")))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(
                        post("/admin/questions/1/media/not-an-id/delete")
                                .with(user("admin@aptis.local").roles("ADMIN"))
                                .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}
