package com.aptisprep.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityFlowTests {

    @Autowired
    private MockMvc mockMvc;

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
    void studentCanExploreOrientationWithoutGoalOrPlacement() throws Exception {
        mockMvc.perform(get("/student/orientation").with(user("new-student@aptis.local").roles("STUDENT")))
                .andExpect(status().isOk()).andExpect(view().name("student/learning/orientation"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Không cần đặt mục tiêu hoặc làm bài đầu vào")));
    }

    @Test
    void orientationRequiresStudentLogin() throws Exception {
        mockMvc.perform(get("/student/orientation")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/student/orientation").with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanSearchStudentPreviewWithoutAccents() throws Exception {
        mockMvc.perform(get("/admin/students").param("q", "minh anh")
                        .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(view().name("admin/student/list"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Nguyễn Minh Anh")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Trần Hoàng Nam"))));
    }

    @Test
    void studentPreviewFilterAndEmptyStateRender() throws Exception {
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
    void studentDetailRendersProgressAndBothAccountActions() throws Exception {
        for (int id : new int[]{1001, 1002, 1003, 1004}) {
            mockMvc.perform(get("/admin/students/" + id).with(user("admin@aptis.local").roles("ADMIN")))
                    .andExpect(status().isOk()).andExpect(view().name("admin/student/detail"));
        }
    }

    @Test
    void studentCannotAccessAdminStudentPages() throws Exception {
        for (String url : new String[]{"/admin/students", "/admin/students/1001"}) {
            mockMvc.perform(get(url).with(user("student@aptis.local").roles("STUDENT")))
                    .andExpect(status().isForbidden());
        }
    }

}
