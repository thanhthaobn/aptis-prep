package com.aptisprep.controller;

import com.aptisprep.entity.User;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.repository.UserRepository;
import com.aptisprep.repository.ExamAttemptRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.entity.ExamAttemptEntity;
import com.aptisprep.entity.MockExamEntity;
import java.math.BigDecimal;
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
    @Autowired ExamAttemptRepository attempts;
    @Autowired MockExamRepository exams;

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

    @Test
    void realHistoryShowsExamTitleAndAppliesDatabaseFilters() throws Exception {
        User learner = learner("filtered-results@test.local");
        MockExamEntity exam = new MockExamEntity();
        exam.setCertificateId(1); exam.setTitle("Reading Part 1 — Email ngắn");
        exam.setExamType("PRACTICE"); exam.setSkill("READING"); exam.setTargetLevel("A1");
        exam.setDurationMinutes(20); exam.setStatus("PUBLISHED");
        exams.saveAndFlush(exam);
        ExamAttemptEntity attempt = new ExamAttemptEntity();
        attempt.setUserId(learner.getId()); attempt.setMockExamId(exam.getId());
        attempt.setStatus("COMPLETED"); attempt.setGradingStatus("COMPLETED");
        attempt.setFinalScaleScore(new BigDecimal("42.00")); attempt.setEstimatedOverallCefr("B1");
        attempts.saveAndFlush(attempt);

        mvc.perform(get("/student/results").param("q", "Email ngắn").param("skill", "READING")
                        .param("examType", "PRACTICE").param("state", "COMPLETED")
                        .with(user(learner.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reading Part 1 — Email ngắn")))
                .andExpect(content().string(containsString("42.00 điểm")));

        mvc.perform(get("/student/results").param("examType", "PLACEMENT")
                        .with(user(learner.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Reading Part 1 — Email ngắn"))));
    }

    @Test
    void learnerCannotOpenAnotherLearnersResult() throws Exception {
        User owner = learner("result-owner@test.local");
        User intruder = learner("result-intruder@test.local");
        MockExamEntity exam = new MockExamEntity();
        exam.setCertificateId(1); exam.setTitle("Checkpoint Reading A1"); exam.setExamType("CHECKPOINT");
        exam.setSkill("READING"); exam.setTargetLevel("A1"); exam.setDurationMinutes(15); exam.setStatus("PUBLISHED");
        exams.saveAndFlush(exam);
        ExamAttemptEntity attempt = new ExamAttemptEntity(); attempt.setUserId(owner.getId());
        attempt.setMockExamId(exam.getId()); attempt.setStatus("COMPLETED"); attempt.setGradingStatus("COMPLETED");
        attempts.saveAndFlush(attempt);

        mvc.perform(get("/student/attempts/{id}/result", attempt.getId())
                        .with(user(intruder.getEmail()).roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    private User learner(String email) {
        User learner = new User(); learner.setFullName("Học viên kết quả"); learner.setEmail(email);
        learner.setPasswordHash("unused"); learner.setRole(Role.STUDENT); learner.setStatus(UserStatus.ACTIVE);
        return users.saveAndFlush(learner);
    }
}
