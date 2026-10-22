package com.aptisprep.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class StudentContentPreviewTests {
    @Autowired MockMvc mvc;
    @Autowired com.aptisprep.repository.MockExamRepository exams;
    @Autowired com.aptisprep.repository.MockExamSectionRepository sections;
    @Autowired com.aptisprep.repository.CertificateRepository certificates;
    private final java.util.List<Integer> examIds = new java.util.ArrayList<>();

    @org.junit.jupiter.api.BeforeEach
    void databaseCatalog() {
        var certificate = new com.aptisprep.entity.Certificate(); certificate.setCode("STUDENT-UI-" + System.nanoTime());
        certificate.setName("Test certificate"); certificate.setStatus(com.aptisprep.enums.RecordStatus.ACTIVE); certificate = certificates.saveAndFlush(certificate);
        String[] skills = {"FULL_MOCK", "READING", "WRITING"};
        String[] titles = {"Thi thử tổng hợp số 01", "Luyện tập Reading", "Luyện tập Writing"};
        for (int i=0;i<skills.length;i++) {
            var exam = new com.aptisprep.entity.MockExamEntity(); exam.setTitle(titles[i]); exam.setCertificateId(certificate.getId());
            exam.setExamType(i==0?"FULL_MOCK":"PRACTICE"); exam.setSkill(skills[i]); exam.setTargetLevel(i==1?"B1":"B2");
            exam.setDurationMinutes(35); exam.setStatus("PUBLISHED"); exam = exams.saveAndFlush(exam); examIds.add(exam.getId());
            if(i==0){var section=new com.aptisprep.entity.MockExamSectionEntity();section.setMockExamId(exam.getId());section.setSkill("LISTENING");section.setDisplayOrder(1);section.setDurationMinutes(20);section.setStatus("ACTIVE");sections.saveAndFlush(section);}
        }
    }
    @Test void temporaryExamCatalogUsesCompactSharedSkillTheme() throws Exception {
        for(String path : new String[]{"/student/exams", "/student/exams/"+examIds.get(1)}) {
            mvc.perform(get(path).with(user("student").roles("STUDENT")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("compact-catalog")))
                .andExpect(content().string(containsString("data-skill=\"Reading\"")));
        }
        mvc.perform(get("/student/exams/"+examIds.getFirst()).with(user("student").roles("STUDENT")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("data-skill=\"Tổng hợp\"")))
            .andExpect(content().string(containsString("data-skill=\"Listening\"")));
    }
    @Test void legacyOrientationUsesCanonicalLearningPath() throws Exception {
        mvc.perform(get("/student/orientation").with(user("student").roles("STUDENT")))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/student/learning-path"));
    }
    @Test void examPagesRenderWithoutGoalOrPlacement() throws Exception {
        mvc.perform(get("/student/exams").with(user("new@student.local").roles("STUDENT")))
            .andExpect(status().isOk()).andExpect(view().name("student/content/list"))
            .andExpect(content().string(not(containsString("minhanh@example.com"))));
        for(int id:examIds) {
            mvc.perform(get("/student/exams/"+id).with(user("new@student.local").roles("STUDENT")))
                .andExpect(status().isOk()).andExpect(view().name("student/content/detail"));
        }
    }
    @Test void filtersAndEmptyStatesWork() throws Exception {
        mvc.perform(get("/student/exams").param("skill","Writing").param("level","B2").with(user("student").roles("STUDENT")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Luyện tập Writing")))
            .andExpect(content().string(not(containsString("Thi thử tổng hợp số 01"))));
    }
    @Test void boundariesAndMissingEntriesAreHandled() throws Exception {
        for(String path : new String[]{"/student/lessons","/student/exams/7001","/student/attempts/8001/result"}) {
            mvc.perform(get(path)).andExpect(status().is3xxRedirection());
            mvc.perform(get(path).with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/student/lessons/7001").with(user("student").roles("STUDENT"))).andExpect(status().isNotFound());
        mvc.perform(get("/student/lessons").param("q","x".repeat(256)).with(user("student").roles("STUDENT"))).andExpect(status().isBadRequest());
        mvc.perform(post("/student/exams/7001/start").with(csrf()).with(user("student").roles("STUDENT"))).andExpect(status().is4xxClientError());
    }
}
