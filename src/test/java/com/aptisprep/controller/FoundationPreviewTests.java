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
class FoundationPreviewTests {
    @Autowired MockMvc mvc;
    @Test void profileAndPlacementRenderWithoutWrites() throws Exception {
        mvc.perform(get("/student/profile").with(user("new@example.com").roles("STUDENT")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("new@example.com")))
            .andExpect(content().string(not(containsString("Giao diện minh họa"))));
        mvc.perform(get("/student/placement").with(user("new@example.com").roles("STUDENT")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Bỏ qua và khám phá")));
        mvc.perform(get("/admin/certificates").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(content().string(not(containsString("api-key"))))
            .andExpect(content().string(not(containsString("spring.datasource"))));
    }
    @Test void accountScreensEnforceRoles() throws Exception {
        for(String path : new String[]{"/student/profile","/student/placement"}) {
            mvc.perform(get(path)).andExpect(status().is3xxRedirection());
            mvc.perform(get(path).with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/admin/settings").with(user("student").roles("STUDENT"))).andExpect(status().isForbidden());
    }
    @Test void privateUploadsAreNotServedDirectly() throws Exception {
        for(String role : new String[]{"STUDENT","ADMIN"}) {
            mvc.perform(get("/uploads/recording.wav").with(user("demo").roles(role))).andExpect(status().isForbidden());
        }
    }
}
