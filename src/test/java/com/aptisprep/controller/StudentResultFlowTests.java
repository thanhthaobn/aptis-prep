package com.aptisprep.controller;

import static com.aptisprep.security.SessionMvcRequests.csrf;
import static com.aptisprep.security.SessionMvcRequests.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.aptisprep.service.contract.IntegrationModels.HistoryPage;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
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
            if (state.equals("IN_PROGRESS")) {
                mvc.perform(get(path).with(user(learner.getEmail()).roles("STUDENT")))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(redirectedUrl("/student/attempts/" + attempt.getId()));
                continue;
            }
            var result =
                    mvc.perform(get(path).with(user(learner.getEmail()).roles("STUDENT")))
                            .andExpect(status().isOk())
                            .andExpect(content().string(containsString(exam.getTitle())))
                            .andExpect(
                                    content()
                                            .string(
                                                    org.hamcrest.Matchers.not(
                                                            containsString("Mã lượt thi"))));
            if (state.equals("ERROR"))
                result.andExpect(content().string(containsString("Chấm lại phần lỗi")));
            result.andExpect(
                    content()
                            .string(
                                    complete
                                            ? containsString(">35</td>")
                                            : org.hamcrest.Matchers.not(
                                                    containsString(">35</td>"))));
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
                .andExpect(content().string(containsString("Bạn chưa làm bài thi nào")))
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
    void historyPaginationKeepsFiltersAndOwnershipAndClampsPastLastPage() throws Exception {
        var owner = learner("pagination-owner@test.local");
        var other = learner("pagination-other@test.local");
        var exam = new MockExamEntity();
        exam.setCertificateId(1);
        exam.setTitle("Reading & Email");
        exam.setExamType("PRACTICE");
        exam.setSkill("READING");
        exam.setStatus("PUBLISHED");
        exams.saveAndFlush(exam);
        var expected = new ArrayList<Integer>();
        for (int i = 0; i < 4; i++) {
            var attempt = new ExamAttemptEntity();
            attempt.setUserId(i == 3 ? other.getId() : owner.getId());
            attempt.setRequestKey("pagination-" + i);
            attempt.setMockExamId(exam.getId());
            attempt.setStatus("COMPLETED");
            attempt.setGradingStatus("COMPLETED");
            attempts.saveAndFlush(attempt);
            if (i < 3) expected.add(attempt.getId());
        }
        var seen = new HashSet<Integer>();
        for (int page : new int[] {0, 1}) {
            var response =
                    mvc.perform(
                                    get("/student/results")
                                            .param("q", "Reading & Email")
                                            .param("skill", "READING")
                                            .param("examType", "PRACTICE")
                                            .param("state", "COMPLETED")
                                            .param("page", String.valueOf(page))
                                            .param("size", "2")
                                            .with(user(owner.getEmail()).roles("STUDENT")))
                            .andExpect(status().isOk())
                            .andExpect(
                                    content()
                                            .string(
                                                    containsString(
                                                            "Trang " + (page + 1) + " / 2")));
            if (page == 0)
                response.andExpect(
                        content()
                                .string(
                                        containsString(
                                                "/student/results?size=2&amp;q=Reading%20%26%20Email&amp;skill=READING&amp;examType=PRACTICE&amp;state=COMPLETED&amp;page=1")));
            var history =
                    (HistoryPage) response.andReturn().getModelAndView().getModel().get("history");
            assertThat(history.totalItems()).isEqualTo(3);
            assertThat(history.items()).hasSize(page == 0 ? 2 : 1);
            for (var item : history.items()) {
                assertThat(seen.add(item.attemptId())).isTrue();
            }
        }
        assertThat(seen).containsExactlyInAnyOrderElementsOf(expected);
        mvc.perform(
                        get("/student/results")
                                .param("page", "2147483647")
                                .param("size", "2")
                                .with(user(owner.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Trang 2 / 2")));
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

    @Test
    void onlyTheOwnerCanDiscardAnUnsubmittedAttemptFromHistory() throws Exception {
        var learner = learner("discard-owner@test.local");
        var other = learner("discard-other@test.local");
        var exam = new MockExamEntity();
        exam.setCertificateId(1);
        exam.setTitle("Đề có lượt làm dở");
        exam.setExamType("PRACTICE");
        exam.setSkill("READING");
        exam.setStatus("PUBLISHED");
        exams.saveAndFlush(exam);
        var draft = new ExamAttemptEntity();
        draft.setUserId(learner.getId());
        draft.setMockExamId(exam.getId());
        draft.setStatus("IN_PROGRESS");
        draft.setRequestKey("discard-draft");
        attempts.saveAndFlush(draft);
        var completed = new ExamAttemptEntity();
        completed.setUserId(learner.getId());
        completed.setMockExamId(exam.getId());
        completed.setStatus("COMPLETED");
        attempts.saveAndFlush(completed);
        var scoredDraft = new ExamAttemptEntity();
        scoredDraft.setUserId(learner.getId());
        scoredDraft.setMockExamId(exam.getId());
        scoredDraft.setStatus("IN_PROGRESS");
        scoredDraft.setRequestKey("discard-scored-draft");
        scoredDraft.setFinalScaleScore(java.math.BigDecimal.TEN);
        attempts.saveAndFlush(scoredDraft);
        var expired = new ExamAttemptEntity();
        expired.setUserId(learner.getId());
        expired.setMockExamId(exam.getId());
        expired.setStatus("EXPIRED");
        expired.setRequestKey("discard-expired-draft");
        attempts.saveAndFlush(expired);

        var owner = user(learner.getEmail()).roles("STUDENT");
        String draftUrl = "/student/attempts/" + draft.getId();
        String discardUrl = draftUrl + "/discard";
        String completedUrl = "/student/attempts/" + completed.getId();
        String expiredDiscardUrl = "/student/attempts/" + expired.getId() + "/discard";
        mvc.perform(get("/student/results").with(owner))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(discardUrl)))
                .andExpect(content().string(containsString(expiredDiscardUrl)))
                .andExpect(
                        content()
                                .string(
                                        not(
                                                containsString(
                                                        "/student/attempts/"
                                                                + scoredDraft.getId()
                                                                + "/discard"))))
                .andExpect(content().string(containsString(completedUrl + "/discard")));
        mvc.perform(post(discardUrl).with(owner)).andExpect(status().isForbidden());
        mvc.perform(post(discardUrl).with(user(other.getEmail()).roles("STUDENT")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post(completedUrl + "/discard").with(owner).with(csrf()))
                .andExpect(redirectedUrl("/student/results"))
                .andExpect(flash().attributeExists("successMessage"));
        mvc.perform(post(expiredDiscardUrl).with(owner).with(csrf()))
                .andExpect(redirectedUrl("/student/results"))
                .andExpect(flash().attributeExists("successMessage"));
        mvc.perform(
                        post("/student/attempts/{id}/discard", scoredDraft.getId())
                                .with(owner)
                                .with(csrf()))
                .andExpect(redirectedUrl("/student/results"))
                .andExpect(flash().attributeExists("error"));

        for (int i = 0; i < 2; i++) {
            mvc.perform(post(discardUrl).with(owner).with(csrf()))
                    .andExpect(redirectedUrl("/student/results"))
                    .andExpect(flash().attributeExists("successMessage"));
        }
        mvc.perform(get("/student/results").with(owner))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(discardUrl))))
                .andExpect(content().string(not(containsString(draftUrl + "/result"))))
                .andExpect(content().string(not(containsString(completedUrl + "/result"))));
        mvc.perform(get("/student/attempts/{id}", draft.getId()).with(owner))
                .andExpect(status().isNotFound());
        assertThat(attempts.findByUserIdAndRequestKey(learner.getId(), "discard-draft")).isEmpty();
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
