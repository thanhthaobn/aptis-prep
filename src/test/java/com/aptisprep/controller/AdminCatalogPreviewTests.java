package com.aptisprep.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import com.aptisprep.entity.User;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AdminCatalogPreviewTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    @BeforeEach void seedAdmin(){if(users.findByEmailIgnoreCase("admin@aptis.local").isEmpty()){User value=new User();value.setFullName("Test Admin");value.setEmail("admin@aptis.local");value.setPasswordHash("test");value.setRole(Role.ADMIN);value.setStatus(UserStatus.ACTIVE);users.save(value);}}

    @Test
    void allCatalogPagesAndFormsRenderForAdmin() throws Exception {
        String[] modules = {"questions", "exams"};
        int[] ids = {3001, 4001};
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
        mvc.perform(get("/admin/results").with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(view().name("admin/result/list"));
    }

    @Test
    void searchFiltersAndEmptyStateRender() throws Exception {
        mvc.perform(get("/admin/questions").param("skill", "Writing").param("status", "DRAFT")
                        .with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Viết thư cho câu lạc bộ")))
                .andExpect(content().string(not(containsString("Chọn từ phù hợp trong câu"))));
        mvc.perform(get("/admin/results").param("q", "minh anh").with(user("admin@aptis.local").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Không có kết quả phù hợp")));
    }

    @Test
    void catalogPagesRejectStudentRole() throws Exception {
        for (String path : new String[]{"/admin/lessons", "/admin/questions/3001", "/admin/exams/create", "/admin/results/5001"}) {
            mvc.perform(get(path).with(user("student@aptis.local").roles("STUDENT")))
                    .andExpect(status().isForbidden());
        }
    }
}
