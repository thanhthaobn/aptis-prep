package com.aptisprep.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aptisprep.entity.*;
import com.aptisprep.enums.*;
import com.aptisprep.repository.*;
import java.util.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminNestedFormBindingTests {
    private static final String ADMIN = "nested-form-admin@test.invalid";
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired QuestionRepository questions;
    @Autowired QuestionItemRepository questionItems;
    @Autowired QuestionOptionRepository questionOptions;
    @Autowired MockExamRepository exams;
    @Autowired CertificateRepository certificates;

    @BeforeEach
    void admin() {
        var admin = new User();
        admin.setFullName("Form regression admin");
        admin.setEmail(ADMIN);
        admin.setPasswordHash("unused");
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        users.saveAndFlush(admin);
    }

    @Test
    void questionItemsAndOptionsSaveAndEditWithoutMixingValuesOrCorrectAnswers() throws Exception {
        var request =
                questionRequest()
                        .param("items[0].content", "First task")
                        .param("items[0].options[0].content", "First A")
                        .param("items[0].options[1].content", "First B")
                        .param("items[0].correctPosition", "2")
                        .param("items[1].content", "Second task <keep & escape>")
                        .param("items[1].options[0].content", "Second A <&>")
                        .param("items[1].options[1].content", "Second B")
                        .param("items[1].correctPosition", "1");
        var saved = mvc.perform(request).andExpect(status().is3xxRedirection()).andReturn();
        String location = saved.getResponse().getRedirectedUrl();
        String html =
                mvc.perform(get(location + "/edit").with(user(ADMIN).roles("ADMIN")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertTextarea(html, "items[0].content", "First task");
        assertTextarea(html, "items[1].content", "Second task <keep & escape>");
        assertInput(html, "items[0].options[0].content", "First A", false);
        assertInput(html, "items[1].options[0].content", "Second A <&>", false);
        assertInput(html, "items[0].correctPosition", "2", true);
        assertInput(html, "items[1].correctPosition", "1", true);
        assertTextarea(html, "content", "Nested form question");
        assertTextarea(html, "instruction", "Saved instruction <&>");
        assertSelected(html, "skill", "READING");
        assertSelected(html, "questionType", "MATCHING");
        assertSelected(html, "cefrLevel", "B1");
        assertSelected(html, "status", "DRAFT");
        assertInput(html, "partNo", "2", false);
        assertUniqueIds(html);
        assertEquals(
                1,
                Pattern.compile("<form\\b[^>]*class=\"[^\"]*catalog-preview-form", Pattern.DOTALL)
                        .matcher(html)
                        .results()
                        .count());
    }

    @Test
    void invalidQuestionKeepsEveryEnteredItemAndSelectedRadioOnTheForm() throws Exception {
        long before = questions.count();
        String html =
                mvc.perform(
                                questionRequest()
                                        .param("items[0].content", "Valid first task")
                                        .param("items[0].options[0].content", "A")
                                        .param("items[0].options[1].content", "B")
                                        .param("items[0].correctPosition", "2")
                                        .param("items[1].content", "Keep this <second task>")
                                        .param("items[1].options[0].content", "Only one option")
                                        .param("items[1].correctPosition", "1"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("admin/catalog/form"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertTrue(html.contains("Mỗi ý hỏi phải có ít nhất 2 phương án"));
        assertTextarea(html, "items[1].content", "Keep this <second task>");
        assertInput(html, "items[0].options[1].content", "B", false);
        assertInput(html, "items[0].correctPosition", "2", true);
        assertInput(html, "items[1].correctPosition", "1", true);
        assertEquals(before, questions.count());
    }

    @Test
    void fullMockSectionsKeepTheirOwnSkillDurationAndQuestionSelection() throws Exception {
        var certificate = new Certificate();
        certificate.setCode("NESTED-FORM");
        certificate.setName("Form test certificate");
        certificate.setStatus(RecordStatus.ACTIVE);
        certificate = certificates.saveAndFlush(certificate);
        var request =
                post("/admin/exams")
                        .with(user(ADMIN).roles("ADMIN"))
                        .with(csrf())
                        .param("title", "Five sections")
                        .param("examType", "FULL_MOCK")
                        .param("durationMinutes", "150")
                        .param("certificateId", certificate.getId().toString())
                        .param("description", "Preserved exam description <&>")
                        .param("demoOnly", "true")
                        .param("status", "DRAFT");
        var skillCodes = Skill.codes();
        var questionIds = new ArrayList<Integer>();
        for (int index = 0; index < skillCodes.size(); index++) {
            String skill = skillCodes.get(index);
            var question = new QuestionEntity();
            question.setContent("Question for " + skill);
            question.setSkill(skill);
            question.setQuestionType(
                    skill.equals("WRITING")
                            ? "WRITING_TEXT"
                            : skill.equals("SPEAKING") ? "SPEAKING_AUDIO" : "SINGLE_CHOICE");
            question.setStatus("PUBLISHED");
            int questionId = questions.saveAndFlush(question).getId();
            var item = new QuestionItemEntity();
            item.setQuestionId(questionId);
            item.setContent("Preview item for " + skill);
            item.setPosition(1);
            item.setResponseSeconds(skill.equals("SPEAKING") ? 30 : null);
            item = questionItems.saveAndFlush(item);
            if (!Set.of("WRITING", "SPEAKING").contains(skill)) {
                var option = new QuestionOptionEntity();
                option.setQuestionItemId(item.getId());
                option.setContent("Visible option for " + skill);
                option.setOptionCode("A");
                option.setCorrect(true);
                option.setPosition(1);
                questionOptions.saveAndFlush(option);
            }
            questionIds.add(questionId);
            request.param("sections[" + index + "].skill", skill)
                    .param("sections[" + index + "].durationMinutes", String.valueOf(10 + index))
                    .param("sections[" + index + "].questionIds", String.valueOf(questionId));
        }
        var saved = mvc.perform(request).andExpect(status().is3xxRedirection()).andReturn();
        String html =
                mvc.perform(
                                get(saved.getResponse().getRedirectedUrl() + "/edit")
                                        .with(user(ADMIN).roles("ADMIN")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        for (int index = 0; index < skillCodes.size(); index++) {
            assertSelected(html, "sections[" + index + "].skill", skillCodes.get(index));
            assertInput(
                    html,
                    "sections[" + index + "].durationMinutes",
                    String.valueOf(10 + index),
                    false);
            assertInput(
                    html,
                    "sections[" + index + "].questionIds",
                    questionIds.get(index).toString(),
                    false);
        }
        assertUniqueIds(html);
        assertInput(html, "title", "Five sections", false);
        assertFalse(html.contains("name=\"durationMinutes\""));
        assertTextarea(html, "description", "Preserved exam description <&>");
        assertSelected(html, "examType", "FULL_MOCK");
        assertSelected(html, "certificateId", certificate.getId().toString());
        assertInput(html, "demoOnly", "true", true);
        String location = saved.getResponse().getRedirectedUrl();
        int examId = Integer.parseInt(location.substring(location.lastIndexOf('/') + 1));
        assertEquals(60, exams.findById(examId).orElseThrow().getDurationMinutes());
        var detail = mvc.perform(get(location).with(user(ADMIN).roles("ADMIN")));
        for (String skill : skillCodes) {
            detail.andExpect(
                    content()
                            .string(org.hamcrest.Matchers.containsString("Question for " + skill)));
            detail.andExpect(
                    content()
                            .string(
                                    org.hamcrest.Matchers.containsString(
                                            "Preview item for " + skill)));
        }
        detail.andExpect(status().isOk());
        detail.andExpect(
                content()
                        .string(
                                org.hamcrest.Matchers.containsString(
                                        "Visible option for READING")));
        assertTrue(exams.findById(examId).orElseThrow().isDemoOnly());
        var update =
                post(location + "/edit")
                        .with(user(ADMIN).roles("ADMIN"))
                        .with(csrf())
                        .param("title", "Five sections")
                        .param("examType", "FULL_MOCK")
                        .param("durationMinutes", "150")
                        .param("certificateId", certificate.getId().toString())
                        .param("status", "DRAFT")
                        .param("_demoOnly", "on");
        for (int index = 0; index < skillCodes.size(); index++) {
            update.param("sections[" + index + "].skill", skillCodes.get(index))
                    .param("sections[" + index + "].durationMinutes", String.valueOf(10 + index))
                    .param(
                            "sections[" + index + "].questionIds",
                            questionIds.get(index).toString());
        }
        mvc.perform(update).andExpect(status().is3xxRedirection());
        assertFalse(
                exams.findById(examId).orElseThrow().isDemoOnly(),
                "Unchecking the checkbox must clear a previously enabled flag");
        assertEquals(
                1,
                Pattern.compile("<form\\b[^>]*class=\"[^\"]*catalog-preview-form", Pattern.DOTALL)
                        .matcher(html)
                        .results()
                        .count());
    }

    @Test
    void lessonValidationKeepsSixOptionsAndSelectedFWithoutWritingALesson() throws Exception {
        String html =
                mvc.perform(
                                post("/admin/lessons")
                                        .with(user(ADMIN).roles("ADMIN"))
                                        .with(csrf())
                                        .param("title", "")
                                        .param("skill", "Reading")
                                        .param("cefrLevel", "A1")
                                        .param("status", "DRAFT")
                                        .param("displayOrder", "1")
                                        .param("checks[0].prompt", "Preserve all six <options>")
                                        .param("checks[0].optionA", "Answer A")
                                        .param("checks[0].optionB", "Answer B")
                                        .param("checks[0].optionC", "Answer C")
                                        .param("checks[0].optionD", "Answer D")
                                        .param("checks[0].optionE", "Answer E")
                                        .param("checks[0].optionF", "Answer F <&>")
                                        .param("checks[0].correctOption", "5"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("admin/lesson/form"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertTextarea(html, "checks[0].prompt", "Preserve all six <options>");
        assertTextarea(html, "checks[0].optionF", "Answer F <&>");
        assertSelected(html, "checks[0].correctOption", "5");
        assertTrue(html.contains("role=\"alert\""));
        assertFalse(html.contains("stat.index"));
        assertUniqueIds(html);
    }

    private MockHttpServletRequestBuilder questionRequest() {
        return post("/admin/questions")
                .with(user(ADMIN).roles("ADMIN"))
                .with(csrf())
                .param("content", "Nested form question")
                .param("instruction", "Saved instruction <&>")
                .param("partNo", "2")
                .param("skill", "READING")
                .param("questionType", "MATCHING")
                .param("cefrLevel", "B1")
                .param("difficulty", "MEDIUM")
                .param("status", "DRAFT");
    }

    private void assertInput(String html, String name, String value, boolean checked) {
        assertTrue(
                Pattern.compile("<input\\b[^>]*>", Pattern.DOTALL)
                        .matcher(html)
                        .results()
                        .map(match -> match.group())
                        .anyMatch(
                                tag ->
                                        tag.contains("name=\"" + name + "\"")
                                                && tag.contains(
                                                        "value=\""
                                                                + HtmlUtils.htmlEscape(value)
                                                                + "\"")
                                                && (!checked || tag.contains("checked"))),
                name + "=" + value);
    }

    private void assertTextarea(String html, String name, String value) {
        assertTrue(
                Pattern.compile(
                                "<textarea\\b[^>]*name=\""
                                        + Pattern.quote(name)
                                        + "\"[^>]*>(.*?)</textarea>",
                                Pattern.DOTALL)
                        .matcher(html)
                        .results()
                        .anyMatch(match -> match.group(1).equals(HtmlUtils.htmlEscape(value))),
                name);
    }

    private void assertSelected(String html, String name, String value) {
        String select =
                Pattern.compile(
                                "<select\\b[^>]*name=\""
                                        + Pattern.quote(name)
                                        + "\"[^>]*>(.*?)</select>",
                                Pattern.DOTALL)
                        .matcher(html)
                        .results()
                        .map(match -> match.group(1))
                        .findFirst()
                        .orElseThrow();
        assertTrue(
                Pattern.compile("<option\\b[^>]*>", Pattern.DOTALL)
                        .matcher(select)
                        .results()
                        .map(match -> match.group())
                        .anyMatch(
                                tag ->
                                        tag.contains("value=\"" + value + "\"")
                                                && tag.contains("selected")),
                name);
    }

    private void assertUniqueIds(String html) {
        var ids = new HashSet<String>();
        Pattern.compile("\\sid=\"([^\"]+)\"")
                .matcher(html)
                .results()
                .forEach(
                        match ->
                                assertTrue(
                                        ids.add(match.group(1)),
                                        "Duplicate id: " + match.group(1)));
    }
}
