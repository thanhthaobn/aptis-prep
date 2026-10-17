package com.aptisprep.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminScoringPreviewTests {
    @Autowired MockMvc mvc;
    @Test void allScoringPagesRender() throws Exception {
        for(String path : new String[]{"/admin/certificates","/admin/scoring-schemes","/admin/certificates/create","/admin/certificates/9101","/admin/certificates/9101/edit","/admin/scoring-schemes/create","/admin/scoring-schemes/9201","/admin/scoring-schemes/9201/edit"}) {
            mvc.perform(get(path).with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("api-key"))))
                .andExpect(content().string(not(containsString("spring.datasource"))));
        }
        mvc.perform(get("/admin/settings").with(user("admin").roles("ADMIN")))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/certificates"));
    }
    @Test void filtersAndRolesAreValidated() throws Exception {
        mvc.perform(get("/admin/scoring-schemes/9201").param("skill","Speaking").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(model().attribute("selectedSkill","Speaking"));
        mvc.perform(get("/admin/scoring-schemes/9201").param("skill","Grammar & Vocabulary").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Chưa có khoảng điểm")));
        for(String path : new String[]{"/admin/certificates","/admin/certificates/9101/edit","/admin/scoring-schemes/9201"}) {
            mvc.perform(get(path)).andExpect(status().is3xxRedirection());
            mvc.perform(get(path).with(user("student").roles("STUDENT"))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/admin/certificates/9999").with(user("admin").roles("ADMIN"))).andExpect(status().isNotFound());
        mvc.perform(get("/admin/scoring-schemes/9201").param("skill","invalid").with(user("admin").roles("ADMIN"))).andExpect(status().isBadRequest());
    }
}
