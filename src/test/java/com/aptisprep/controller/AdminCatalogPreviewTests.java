package com.aptisprep.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@SpringBootTest
@AutoConfigureMockMvc
class AdminCatalogPreviewTests {
    @Autowired MockMvc mvc;

    @Test
    void allCatalogPagesAndFormsRenderForAdmin() throws Exception {
        String[] modules = {"lessons", "questions", "exams", "results"};
        int[] ids = {2001, 3001, 4001, 5001};
        for (int i = 0; i < modules.length; i++) {
            String base = "/admin/" + modules[i];
            mvc.perform(get(base).with(user("admin@aptis.local").roles("ADMIN")))
                    .andExpect(status().isOk()).andExpect(view().name("admin/catalog/list"));
            for (int offset = 0; offset < 3; offset++) {
                mvc.perform(get(base + "/" + (ids[i] + offset)).with(user("admin@aptis.local").roles("ADMIN")))
                        .andExpect(status().isOk()).andExpect(view().name("admin/catalog/detail"));
            }
            if (!modules[i].equals("results")) {
                for (String path : new String[]{base + "/create", base + "/" + ids[i] + "/edit"}) {
                    mvc.perform(get(path).with(user("admin@aptis.local").roles("ADMIN")))
                            .andExpect(status().isOk()).andExpect(view().name("admin/catalog/form"));
                }
            }
        }
    }

    @Test
    void searchFiltersAndEmptyStateRender() throws Exception {
        mvc.perform(get("/admin/questions").param("skill", "Writing").param("status", "DRAFT")
                        .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Viết thư cho câu lạc bộ")))
                .andExpect(content().string(not(containsString("Chọn từ phù hợp trong câu"))));
        mvc.perform(get("/admin/results").param("q", "minh anh").with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Nguyễn Minh Anh")));
        mvc.perform(get("/admin/lessons").param("q", "no-such-content").with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Không tìm thấy nội dung phù hợp")));
    }

    @Test
    void catalogPagesRejectStudentRole() throws Exception {
        for (String path : new String[]{"/admin/lessons", "/admin/questions/3001", "/admin/exams/create", "/admin/results/5001"}) {
            mvc.perform(get(path).with(user("student@aptis.local").roles("STUDENT")))
                    .andExpect(status().isForbidden());
        }
    }
}
