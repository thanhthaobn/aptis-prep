package com.aptisprep.service.learning;

import static com.aptisprep.security.SessionMvcRequests.user;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aptisprep.dto.form.*;
import com.aptisprep.dto.form.FlashcardSetFormDTO;
import com.aptisprep.dto.view.*;
import com.aptisprep.entity.*;
import com.aptisprep.entity.Lesson;
import com.aptisprep.entity.User;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.repository.*;
import com.aptisprep.repository.LessonRepository;
import com.aptisprep.repository.UserRepository;
import com.aptisprep.service.learning.impl.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
class FlashcardDetachedViewTests {
    @Autowired MockMvc mvc;
    @Autowired FlashcardSetService service;
    @Autowired UserRepository users;
    @Autowired LessonRepository lessons;
    @Autowired FlashcardRepository cards;
    @Autowired FlashcardSetRepository sets;

    // Intentionally no test transaction: the view renders after the service's
    // persistence context has closed, exactly as with open-in-view=false.
    @Test
    void libraryAndPersonalStudyPagesRenderWithoutAnOpenPersistenceContext() throws Exception {
        var learner = new User();
        learner.setFullName("Detached learner");
        learner.setEmail("detached-view@test.local");
        learner.setPasswordHash("unused");
        learner.setRole(Role.STUDENT);
        learner.setStatus(UserStatus.ACTIVE);
        learner = users.saveAndFlush(learner);
        var lesson = new Lesson();
        lesson.setTitle("A lesson loaded in another transaction");
        lesson.setSkill("Writing");
        lesson.setCefrLevel("B1");
        lesson.setDisplayOrder(987654);
        lesson.setStatus("PUBLISHED");
        lesson = lessons.saveAndFlush(lesson);
        Integer libraryId = null, personalId = null;
        try {
            var form = new FlashcardSetFormDTO();
            form.setTitle("Email phrases");
            form.setCefrLevel("B1");
            form.setStatus("PUBLISHED");
            form.setLessonId(lesson.getId());
            var row = new FlashcardSetFormDTO.Row();
            row.setTerm("Kind regards");
            row.setDefinition("Trân trọng");
            form.getRows().add(row);
            libraryId = service.saveAdmin(null, form);
            personalId = service.savePersonal(null, form, learner.getEmail());
            mvc.perform(
                            get("/student/flashcards/" + libraryId)
                                    .with(user(learner.getEmail()).roles("STUDENT")))
                    .andExpect(status().isOk())
                    .andExpect(view().name("student/flashcards/study"))
                    .andExpect(content().string(containsString("data-skill=\"Writing\"")))
                    .andExpect(content().string(containsString(lesson.getTitle())))
                    .andExpect(
                            content().string(containsString("/student/lessons/" + lesson.getId())));
            mvc.perform(
                            get("/student/flashcards/" + personalId)
                                    .with(user(learner.getEmail()).roles("STUDENT")))
                    .andExpect(status().isOk())
                    .andExpect(view().name("student/flashcards/study"))
                    .andExpect(content().string(containsString("class=\"back-link\"")));
        } finally {
            for (Integer setId : new Integer[] {libraryId, personalId}) {
                if (setId != null) {
                    cards.deleteAll(
                            cards.findBySetIdAndStatusOrderByDisplayOrderAsc(setId, "ACTIVE"));
                    sets.deleteById(setId);
                }
            }
            lessons.deleteById(lesson.getId());
            users.deleteById(learner.getId());
        }
    }
}
