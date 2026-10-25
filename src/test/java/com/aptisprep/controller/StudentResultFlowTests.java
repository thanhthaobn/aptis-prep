package com.aptisprep.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aptisprep.entity.ExamAttemptEntity;
import com.aptisprep.entity.ExamSkillResultEntity;
import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.entity.User;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.repository.ExamAttemptRepository;
import com.aptisprep.repository.ExamSkillResultRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.repository.UserRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class StudentResultFlowTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ExamAttemptRepository attempts;
    @Autowired MockExamRepository exams;
    @Autowired ExamSkillResultRepository skills;

    @ParameterizedTest
    @ValueSource(strings = {"COMPLETED", "WAITING_GRADING", "ERROR", "EXPIRED", "IN_PROGRESS"})
    void ownersResultAndPlacementResultRenderRealStatesWithoutInventingScores(String state)
            throws Exception {
        User learner = learner("render-" + state + "@test.local");
        MockExamEntity exam = new MockExamEntity();
        exam.setCertificateId(1);
        exam.setTitle("Kết quả thật " + state);
        exam.setExamType("PLACEMENT");
        exam.setSkill("WRITING");
        exam.setTargetLevel("B1");
        exam.setDurationMinutes(20);
        exam.setStatus("PUBLISHED");
        exams.saveAndFlush(exam);
        ExamAttemptEntity attempt = new ExamAttemptEntity();
        attempt.setUserId(learner.getId());
        attempt.setMockExamId(exam.getId());
        attempt.setStatus(state);
        attempt.setGradingStatus(state);
        if (!state.equals("IN_PROGRESS")) attempt.setSubmittedAt(java.time.LocalDateTime.now());
        attempts.saveAndFlush(attempt);
        ExamSkillResultEntity skill = new ExamSkillResultEntity();
        skill.setAttemptId(attempt.getId());
        skill.setSkill("WRITING");
        boolean complete = state.equals("COMPLETED") || state.equals("EXPIRED");
        skill.setGradingStatus(
                complete ? "COMPLETED" : state.equals("ERROR") ? "ERROR" : "WAITING");
        skill.setGradingMethod(state.equals("ERROR") ? "AI_ERROR" : "AI");
        if (complete) {
            skill.setRawScore(new BigDecimal("35"));
            skill.setScaleScore(new BigDecimal("35"));
            skill.setCefrLevel("B1");
        }
        skills.saveAndFlush(skill);
        for (String path :
                new String[] {
                    "/student/attempts/" + attempt.getId() + "/result",
                    "/student/placement/result?attemptId=" + attempt.getId()
                }) {
            var result =
                    mvc.perform(get(path).with(user(learner.getEmail()).roles("STUDENT")))
                            .andExpect(status().isOk())
                            .andExpect(content().string(containsString("Mã lượt thi")));
            if (state.equals("ERROR"))
                result.andExpect(content().string(containsString("Chấm lại phần lỗi")));
            if (state.equals("IN_PROGRESS"))
                result.andExpect(content().string(containsString("Tiếp tục làm bài")));
            if (!complete)
                result.andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                containsString("class=\"score-val\""))));
        }
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                        "/student/attempts/" + attempt.getId() + "/retry-grading")
                                .with(user(learner.getEmail()).roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

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
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                containsString("Luyện tập Reading"))));
    }

    @Test
    void invalidPaginationIsRejected() throws Exception {
        mvc.perform(
                        get("/student/results")
                                .param("size", "51")
                                .with(user("student").roles("STUDENT")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void realHistoryShowsExamTitleAndAppliesDatabaseFilters() throws Exception {
        User learner = learner("filtered-results@test.local");
        MockExamEntity exam = new MockExamEntity();
        exam.setCertificateId(1);
        exam.setTitle("Reading Part 1 — Email ngắn");
        exam.setExamType("PRACTICE");
        exam.setSkill("READING");
        exam.setTargetLevel("A1");
        exam.setDurationMinutes(20);
        exam.setStatus("PUBLISHED");
        exams.saveAndFlush(exam);
        ExamAttemptEntity attempt = new ExamAttemptEntity();
        attempt.setUserId(learner.getId());
        attempt.setMockExamId(exam.getId());
        attempt.setStatus("COMPLETED");
        attempt.setGradingStatus("COMPLETED");
        attempt.setFinalScaleScore(new BigDecimal("42.00"));
        attempt.setEstimatedOverallCefr("B1");
        attempts.saveAndFlush(attempt);

        mvc.perform(
                        get("/student/results")
                                .param("q", "Email ngắn")
                                .param("skill", "READING")
                                .param("examType", "PRACTICE")
                                .param("state", "COMPLETED")
                                .with(user(learner.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reading Part 1 — Email ngắn")))
                .andExpect(content().string(containsString("42.00 điểm")));

        mvc.perform(
                        get("/student/results")
                                .param("examType", "PLACEMENT")
                                .with(user(learner.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                containsString("Reading Part 1 — Email ngắn"))));
    }

    @Test
    void learnerCannotOpenAnotherLearnersResult() throws Exception {
        User owner = learner("result-owner@test.local");
        User intruder = learner("result-intruder@test.local");
        MockExamEntity exam = new MockExamEntity();
        exam.setCertificateId(1);
        exam.setTitle("Checkpoint Reading A1");
        exam.setExamType("CHECKPOINT");
        exam.setSkill("READING");
        exam.setTargetLevel("A1");
        exam.setDurationMinutes(15);
        exam.setStatus("PUBLISHED");
        exams.saveAndFlush(exam);
        ExamAttemptEntity attempt = new ExamAttemptEntity();
        attempt.setUserId(owner.getId());
        attempt.setMockExamId(exam.getId());
        attempt.setStatus("COMPLETED");
        attempt.setGradingStatus("COMPLETED");
        attempts.saveAndFlush(attempt);

        mvc.perform(
                        get("/student/attempts/{id}/result", attempt.getId())
                                .with(user(intruder.getEmail()).roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadRealResultAndRejectsAnInvertedDateRange() throws Exception {
        User admin = learner("result-admin@test.local");
        admin.setRole(Role.ADMIN);
        users.saveAndFlush(admin);
        mvc.perform(
                        get("/admin/results")
                                .param("from", "2026-10-22")
                                .param("to", "2026-10-21")
                                .with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/admin/results").with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/result/list"));
        User owner = learner("admin-result-owner@test.local");
        var exam = new MockExamEntity();
        exam.setCertificateId(1);
        exam.setTitle("Kết quả để quản trị xem");
        exam.setExamType("PRACTICE");
        exam.setSkill("READING");
        exam.setStatus("PUBLISHED");
        exams.saveAndFlush(exam);
        var attempt = new ExamAttemptEntity();
        attempt.setUserId(owner.getId());
        attempt.setMockExamId(exam.getId());
        attempt.setStatus("ERROR");
        attempt.setGradingStatus("ERROR");
        attempts.saveAndFlush(attempt);
        mvc.perform(
                        get("/admin/results/" + attempt.getId())
                                .with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Chấm chưa thành công")));
        mvc.perform(
                        get("/admin/results/" + attempt.getId())
                                .with(user(owner.getEmail()).roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    private User learner(String email) {
        User learner = new User();
        learner.setFullName("Học viên kết quả");
        learner.setEmail(email);
        learner.setPasswordHash("unused");
        learner.setRole(Role.STUDENT);
        learner.setStatus(UserStatus.ACTIVE);
        return users.saveAndFlush(learner);
    }
}
