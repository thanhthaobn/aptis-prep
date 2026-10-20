package com.aptisprep.controller;

import com.aptisprep.entity.User;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
class StudentResultFlowTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    @Test
    void databaseLearnerUsesRealEmptyHistoryInsteadOfFixtureResults() throws Exception {
        User learner = new User();
        learner.setFullName("Nguyễn An");
        learner.setEmail("real-results@test.local");
        learner.setPasswordHash("unused");
        learner.setRole(Role.STUDENT);
        learner.setStatus(UserStatus.ACTIVE);
        users.saveAndFlush(learner);

        mvc.perform(get("/student/results").with(user(learner.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("student/results/list"))
                .andExpect(content().string(containsString("Bạn chưa có kết quả nào")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Luyện tập Reading"))));
    }

    @Test
    void invalidPaginationIsRejected() throws Exception {
        mvc.perform(get("/student/results").param("size", "51")
                        .with(user("student").roles("STUDENT")))
                .andExpect(status().isBadRequest());
    }
}
