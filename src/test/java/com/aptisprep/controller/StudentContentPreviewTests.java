package com.aptisprep.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StudentContentPreviewTests {
    @Autowired MockMvc mvc;
    @Test void examsAndResultsUseCompactSharedSkillTheme() throws Exception {
        for(String path : new String[]{"/student/exams", "/student/results", "/student/exams/7002", "/student/attempts/8001/result"}) {
            mvc.perform(get(path).with(user("student").roles("STUDENT")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("compact-catalog")))
                .andExpect(content().string(containsString("data-skill=\"Reading\"")));
        }
        mvc.perform(get("/student/exams/7001").with(user("student").roles("STUDENT")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("data-skill=\"Tổng hợp\"")))
            .andExpect(content().string(containsString("data-skill=\"Listening\"")));
    }
    @Test void lessonsExposeSkillAndKindForSharedPalette() throws Exception {
        mvc.perform(get("/student/lessons/6002").with(user("student").roles("STUDENT")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("data-kind=\"Flashcard\"")));
        mvc.perform(get("/student/lessons/6005").with(user("student").roles("STUDENT")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("data-skill=\"Speaking\"")));
    }
    @Test void writingAndSpeakingHaveLessonsAndPracticeLayouts() throws Exception {
        for(String skill : new String[]{"Writing","Speaking"}) {
            mvc.perform(get("/student/lessons").param("skill",skill).with(user("student").roles("STUDENT")))
                .andExpect(status().isOk()).andExpect(content().string(not(containsString("Không tìm thấy nội dung phù hợp"))));
        }
        mvc.perform(get("/student/lessons/6004").with(user("student").roles("STUDENT")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Đoạn văn của bạn")))
            .andExpect(content().string(containsString("Nội dung nhập chưa được lưu")));
        mvc.perform(get("/student/lessons/6005").with(user("student").roles("STUDENT")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Chưa bật ghi âm")))
            .andExpect(content().string(not(containsString("writingPractice"))));
    }
    @Test void orientationLinksToEachLessonSkill() throws Exception {
        var page = mvc.perform(get("/student/orientation").with(user("student").roles("STUDENT")))
            .andExpect(status().isOk());
        for(String skill : new String[]{"Reading","Listening","Writing","Speaking"}) {
            page.andExpect(content().string(containsString("/student/lessons?skill=" + skill)));
        }
        page.andExpect(content().string(containsString("/student/lessons?skill=Grammar")));
        for(String skill : new String[]{"Reading","Listening","Writing","Speaking","Grammar & Vocabulary"}) {
            mvc.perform(get("/student/lessons").param("skill",skill).with(user("student").roles("STUDENT")))
                .andExpect(status().isOk()).andExpect(model().attribute("selectedSkill",skill));
        }
    }
    @Test void allPagesRenderWithoutGoalOrPlacement() throws Exception {
        for (String module : new String[]{"lessons","exams","results"}) {
            mvc.perform(get("/student/" + module).with(user("new@student.local").roles("STUDENT")))
                .andExpect(status().isOk()).andExpect(view().name("student/content/list"))
                .andExpect(content().string(not(containsString("Dữ liệu minh họa"))))
                .andExpect(content().string(not(containsString("minhanh@example.com"))));
        }
        for(int i=1;i<=3;i++) {
            for(String path : new String[]{"/student/lessons/"+(6000+i),"/student/exams/"+(7000+i),"/student/attempts/"+(8000+i)+"/result"}) {
                mvc.perform(get(path).with(user("new@student.local").roles("STUDENT")))
                    .andExpect(status().isOk()).andExpect(view().name("student/content/detail"));
            }
        }
    }
    @Test void filtersAndEmptyStatesWork() throws Exception {
        mvc.perform(get("/student/lessons").param("q","lien ket").with(user("student").roles("STUDENT")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Liên kết ý trong bài đọc")))
            .andExpect(content().string(not(containsString("Từ vựng về học tập"))));
        mvc.perform(get("/student/exams").param("skill","Writing").param("level","B2").with(user("student").roles("STUDENT")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Luyện tập Writing")))
            .andExpect(content().string(not(containsString("Thi thử tổng hợp số 01"))));
        mvc.perform(get("/student/results").param("state","GRADING").with(user("student").roles("STUDENT")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Luyện tập Writing")))
            .andExpect(content().string(not(containsString("Luyện tập Reading"))));
        mvc.perform(get("/student/lessons").param("q","not-found").with(user("student").roles("STUDENT")))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Không tìm thấy nội dung phù hợp")));
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
