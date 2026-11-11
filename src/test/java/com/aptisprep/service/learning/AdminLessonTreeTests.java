package com.aptisprep.service.learning;

import static com.aptisprep.security.SessionMvcRequests.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aptisprep.dto.form.*;
import com.aptisprep.dto.form.LessonFormDTO;
import com.aptisprep.dto.view.*;
import com.aptisprep.entity.*;
import com.aptisprep.entity.Lesson;
import com.aptisprep.repository.*;
import com.aptisprep.repository.LessonRepository;
import com.aptisprep.service.learning.impl.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    TransactionalTestExecutionListener.class
})
class AdminLessonTreeTests {
    @Autowired MockMvc mvc;
    @Autowired LessonRepository lessons;

    @Test
    void treeRendersAndCreationPrefillsBranchIncludingArchivedOrder() throws Exception {
        var lesson = new Lesson();
        lesson.setTitle("Reading branch lesson");
        lesson.setDescription("Content");
        lesson.setSkill("Reading");
        lesson.setCefrLevel("A2");
        lesson.setDisplayOrder(71);
        lesson.setStatus("ARCHIVED");
        lessons.saveAndFlush(lesson);
        mvc.perform(get("/admin/lessons").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Đã lưu trữ")))
                .andExpect(content().string(containsString("Thêm bài vào Reading A2")));
        var result =
                mvc.perform(
                                get("/admin/lessons/create")
                                        .param("skill", "Reading")
                                        .param("level", "A2")
                                        .with(user("admin").roles("ADMIN")))
                        .andExpect(status().isOk())
                        .andExpect(
                                content().string(containsString("id=\"lesson-author-profiles\"")))
                        .andExpect(content().string(containsString("data-skill=\"Speaking\"")))
                        .andExpect(content().string(containsString("Đề viết học viên cần nộp")))
                        .andReturn();
        var form = (LessonFormDTO) result.getModelAndView().getModel().get("form");
        assertThat(form.getSkill()).isEqualTo("Reading");
        assertThat(form.getCefrLevel()).isEqualTo("A2");
        assertThat(form.getDisplayOrder()).isEqualTo(72);
        mvc.perform(
                        get("/admin/lessons/create")
                                .param("skill", "unknown")
                                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/lessons").with(user("student").roles("STUDENT")))
                .andExpect(status().isForbidden());
    }
}
