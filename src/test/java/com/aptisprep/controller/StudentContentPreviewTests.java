package com.aptisprep.controller;

import static com.aptisprep.security.SessionMvcRequests.csrf;
import static com.aptisprep.security.SessionMvcRequests.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aptisprep.dto.view.StudentCatalogView.Entry;
import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.entity.MockExamQuestionEntity;
import com.aptisprep.entity.QuestionEntity;
import com.aptisprep.entity.QuestionMediaEntity;
import com.aptisprep.entity.ScoringScheme;
import com.aptisprep.entity.User;
import com.aptisprep.enums.RecordStatus;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.repository.ExamAttemptRepository;
import com.aptisprep.repository.UserRepository;
import com.aptisprep.service.storage.StorageService;
import com.aptisprep.service.student.StudentExamCatalogService.Category;
import com.aptisprep.support.TestAudio;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class StudentContentPreviewTests {
    @Autowired MockMvc mvc;
    @Autowired com.aptisprep.repository.MockExamRepository exams;
    @Autowired com.aptisprep.repository.MockExamQuestionRepository examQuestions;
    @Autowired com.aptisprep.repository.MockExamSectionRepository sections;
    @Autowired com.aptisprep.repository.QuestionRepository questions;
    @Autowired com.aptisprep.repository.QuestionMediaRepository media;
    @Autowired com.aptisprep.repository.CertificateRepository certificates;
    @Autowired com.aptisprep.repository.ScoringSchemeRepository schemes;
    @Autowired com.aptisprep.repository.ScoringScaleRuleRepository rules;
    @Autowired StorageService storage;
    @Autowired com.aptisprep.repository.ScoreBandRepository bands;
    @Autowired ExamAttemptRepository attempts;
    @Autowired UserRepository users;
    private final java.util.List<Integer> examIds = new java.util.ArrayList<>();

    @org.junit.jupiter.api.BeforeEach
    void databaseCatalog() {
        var certificate = new com.aptisprep.entity.Certificate();
        certificate.setCode("STUDENT-UI-" + System.nanoTime());
        certificate.setName("Test certificate");
        certificate.setStatus(com.aptisprep.enums.RecordStatus.ACTIVE);
        certificate = certificates.saveAndFlush(certificate);
        String[] skills = {"FULL_MOCK", "READING", "WRITING"};
        String[] titles = {"Thi thử tổng hợp số 01", "Luyện tập Reading", "Luyện tập Writing"};
        for (int i = 0; i < skills.length; i++) {
            var exam = new com.aptisprep.entity.MockExamEntity();
            exam.setTitle(titles[i]);
            exam.setCertificateId(certificate.getId());
            exam.setExamType(i == 0 ? "FULL_MOCK" : "PRACTICE");
            exam.setSkill(skills[i]);
            exam.setTargetLevel(i == 1 ? "B1" : "B2");
            exam.setDurationMinutes(35);
            exam.setStatus("PUBLISHED");
            if (i == 0) attachScoring(exam, com.aptisprep.enums.Skill.codes());
            exam = exams.saveAndFlush(exam);
            examIds.add(exam.getId());
            addExamQuestions(exam);
        }
    }

    @Test
    void temporaryExamCatalogUsesCompactSharedSkillTheme() throws Exception {
        var learner = user(createLearner().getEmail()).roles("STUDENT");
        for (String path : new String[] {"/student/exams", "/student/exams/" + examIds.get(1)}) {
            mvc.perform(get(path).with(learner))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("compact-catalog")))
                    .andExpect(content().string(containsString("data-skill=\"Reading\"")));
        }
        mvc.perform(get("/student/exams/" + examIds.getFirst()).with(learner))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-skill=\"Tổng hợp\"")))
                .andExpect(content().string(containsString("data-skill=\"Listening\"")));
    }

    @Test
    void legacyOrientationUsesCanonicalLearningPath() throws Exception {
        mvc.perform(get("/student/orientation").with(user("student").roles("STUDENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/learning-path"));
    }

    @Test
    void examPagesRenderWithoutGoalOrPlacement() throws Exception {
        var learner = user(createLearner().getEmail()).roles("STUDENT");
        mvc.perform(get("/student/exams").with(learner))
                .andExpect(status().isOk())
                .andExpect(view().name("student/content/list"))
                .andExpect(content().string(not(containsString("minhanh@example.com"))));
        for (int id : examIds) {
            mvc.perform(get("/student/exams/" + id).with(learner))
                    .andExpect(status().isOk())
                    .andExpect(view().name("student/content/detail"));
        }
    }

    @Test
    void filtersAndEmptyStatesWork() throws Exception {
        mvc.perform(
                        get("/student/exams")
                                .param("skill", "Writing")
                                .param("level", "B2")
                                .with(user("student").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Luyện tập Writing")))
                .andExpect(content().string(not(containsString("Thi thử tổng hợp số 01"))));
    }

    @Test
    void examPagesKeepEncodedFiltersAndDoNotRepeatEntries() throws Exception {
        for (int i = 0; i < 3; i++)
            catalogExam("Phân trang & đề " + i, "PRACTICE", "READING", "B1", 20);
        var seen = new HashSet<Integer>();
        for (int page = 0; page < 3; page++) {
            var response =
                    mvc.perform(
                                    get("/student/exams")
                                            .param("q", "Phân trang & đề")
                                            .param("skill", "Reading")
                                            .param("level", "B1")
                                            .param("page", String.valueOf(page))
                                            .param("size", "1")
                                            .with(user("student").roles("STUDENT")))
                            .andExpect(status().isOk())
                            .andExpect(
                                    content()
                                            .string(
                                                    containsString(
                                                            "Trang " + (page + 1) + " / 3")));
            if (page == 0)
                response.andExpect(
                        content()
                                .string(
                                        containsString(
                                                "/student/exams?size=1&amp;q=Ph%C3%A2n%20trang%20%26%20%C4%91%E1%BB%81&amp;skill=Reading&amp;level=B1&amp;state=&amp;examType=PRACTICE&amp;sort=level&amp;page=1")));
            var model = response.andReturn().getModelAndView().getModel();
            var entries = (Page<?>) model.get("catalogPage");
            assertThat(entries.getTotalElements()).isEqualTo(3);
            assertThat(entries.getContent()).hasSize(1);
            var entry = (Entry) entries.getContent().getFirst();
            assertThat(seen.add(entry.id())).isTrue();
        }
        mvc.perform(
                        get("/student/exams")
                                .param("q", "Phân trang & đề")
                                .param("page", "2147483647")
                                .param("size", "1")
                                .with(user("student").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Trang 3 / 3")));
    }

    @Test
    void examPaginationRejectsInvalidBoundsAndEmptySearchHasNoPageLinks() throws Exception {
        for (var params :
                new String[][] {
                    {"page", "-1"},
                    {"size", "0"},
                    {"size", "51"},
                    {"examType", "UNKNOWN"},
                    {"sort", "UNKNOWN"}
                }) {
            mvc.perform(
                            get("/student/exams")
                                    .param(params[0], params[1])
                                    .with(user("student").roles("STUDENT")))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(
                        get("/student/exams")
                                .param("q", "no-exam-matches-this-query")
                                .with(user("student").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("aria-label=\"Trang sau\""))))
                .andExpect(content().string(containsString("0 kết quả")));
    }

    @Test
    void categoriesSeparateExamTypesAndOrderRealPracticeBySkillAndLevel() throws Exception {
        String prefix = "Luyện thi";
        var c1 = catalogExam(prefix + " Reading C1", "PRACTICE", "READING", "C1", 10);
        var a1 = catalogExam(prefix + " Reading A1", "PRACTICE", "READING", "A1", 30);
        var b1 = catalogExam(prefix + " Reading B1", "PRACTICE", "READING", "B1", 20);
        var writing = catalogExam(prefix + " Writing", "PRACTICE", "WRITING", "B1", 25);
        catalogExam(prefix + " Full mock", "FULL_MOCK", "FULL_MOCK", "B2", 90);
        catalogExam(prefix + " Placement", "PLACEMENT", "READING", "A1", 20);
        catalogExam(prefix + " Checkpoint", "CHECKPOINT", "READING", "A1", 20);
        var demo = catalogExam(prefix + " Demo", "PRACTICE", "READING", "A1", 15);
        demo.setDemoOnly(true);
        exams.saveAndFlush(demo);
        var draft = catalogExam(prefix + " Draft", "PRACTICE", "READING", "A1", 15);
        draft.setStatus("DRAFT");
        exams.saveAndFlush(draft);
        var response =
                mvc.perform(
                                get("/student/exams")
                                        .param("q", prefix)
                                        .with(user("student").roles("STUDENT")))
                        .andExpect(status().isOk())
                        .andExpect(content().string(containsString("exam-skill-group")))
                        .andExpect(content().string(not(containsString(prefix + " Full mock"))))
                        .andExpect(content().string(not(containsString(prefix + " Placement"))))
                        .andExpect(content().string(not(containsString(prefix + " Checkpoint"))))
                        .andExpect(content().string(not(containsString(prefix + " Demo"))))
                        .andExpect(content().string(not(containsString(prefix + " Draft"))));
        var model = response.andReturn().getModelAndView().getModel();
        var page = (Page<?>) model.get("catalogPage");
        assertThat(page.getContent())
                .extracting(value -> ((Entry) value).id())
                .containsExactly(a1.getId(), b1.getId(), c1.getId(), writing.getId());
        assertThat((List<?>) model.get("examCategories"))
                .extracting(value -> ((Category) value).count())
                .containsExactly(4L, 1L, 1L, 1L);
        for (var order :
                new String[][] {{"duration", "C1", "B1", "A1"}, {"newest", "B1", "A1", "C1"}}) {
            var sorted =
                    mvc.perform(
                                    get("/student/exams")
                                            .param("q", prefix)
                                            .param("skill", "Reading")
                                            .param("sort", order[0])
                                            .with(user("student").roles("STUDENT")))
                            .andExpect(status().isOk())
                            .andReturn()
                            .getModelAndView()
                            .getModel();
            assertThat(((Page<?>) sorted.get("catalogPage")).getContent())
                    .extracting(value -> ((Entry) value).level())
                    .containsExactly(order[1], order[2], order[3]);
        }
    }

    @Test
    void fullMockUsesActualSectionsAndCheckpointAndPlacementKeepTheirBusinessEntryPoints()
            throws Exception {
        mvc.perform(
                        get("/student/exams")
                                .param("examType", "FULL_MOCK")
                                .param("skill", "Listening")
                                .with(user("student").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Thi thử tổng hợp số 01")))
                .andExpect(content().string(containsString("exam-section-skills")));
        for (String type : List.of("CHECKPOINT", "PLACEMENT")) {
            var exam = catalogExam("Categorized " + type, type, "READING", "A1", 20);
            var detail =
                    mvc.perform(
                                    get("/student/exams/" + exam.getId())
                                            .with(user("student").roles("STUDENT")))
                            .andExpect(status().isOk())
                            .andExpect(
                                    content()
                                            .string(
                                                    not(
                                                            containsString(
                                                                    "/student/exams/"
                                                                            + exam.getId()
                                                                            + "/start"))));
            detail.andExpect(
                    content()
                            .string(
                                    containsString(
                                            type.equals("CHECKPOINT")
                                                    ? "/student/learning-path/Reading"
                                                    : "/student/placement?skill=READING")));
            if ("PLACEMENT".equals(type)) {
                detail.andExpect(
                        content()
                                .string(
                                        containsString(
                                                "class=\"back-link\" href=\"/student/exams?examType=PLACEMENT\"")));
                mvc.perform(
                                get("/student/exams/" + exam.getId())
                                        .param("fromPlacement", "true")
                                        .with(user("student").roles("STUDENT")))
                        .andExpect(status().isOk())
                        .andExpect(
                                content()
                                        .string(
                                                containsString(
                                                        "class=\"back-link\" href=\"/student/placement?skill=READING\"")));
            }
            mvc.perform(
                            get("/student/exams")
                                    .param("examType", type)
                                    .param("q", "Categorized")
                                    .with(user("student").roles("STUDENT")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Categorized " + type)));
        }
    }

    @Test
    void placementOpensTheCombinedExamAndPreventsOverlappingAttempts() throws Exception {
        var learner = user(createLearner().getEmail()).roles("STUDENT");
        var combined = catalogExam("Urban Life placement", "PLACEMENT", "FULL_MOCK", null, 25);
        var reading = catalogExam("Reading placement", "PLACEMENT", "READING", null, 10);
        mvc.perform(get("/student/placement").with(learner))
                .andExpect(status().isOk())
                .andExpect(model().attribute("skill", "FULL_MOCK"))
                .andExpect(content().string(containsString("Urban Life placement")))
                .andExpect(content().string(containsString("5. Speaking")))
                .andExpect(content().string(not(containsString("Reading placement"))));

        var combinedStart =
                post("/student/placement/start")
                        .param("skill", "FULL_MOCK")
                        .param("examId", combined.getId().toString())
                        .with(learner)
                        .with(csrf());
        var readingStart =
                post("/student/placement/start")
                        .param("skill", "READING")
                        .param("examId", reading.getId().toString())
                        .with(learner)
                        .with(csrf());
        String attemptUrl =
                mvc.perform(combinedStart)
                        .andExpect(redirectedUrlPattern("/student/attempts/*"))
                        .andReturn()
                        .getResponse()
                        .getRedirectedUrl();
        mvc.perform(combinedStart).andExpect(redirectedUrl(attemptUrl));
        mvc.perform(readingStart)
                .andExpect(redirectedUrl("/student/placement?skill=READING"))
                .andExpect(flash().attributeExists("error"));
        mvc.perform(
                        post(attemptUrl + "/next-section")
                                .param("sectionEndMinute", "5")
                                .with(learner)
                                .with(csrf()))
                .andExpect(redirectedUrl(attemptUrl))
                .andExpect(flash().attributeCount(0));
        mvc.perform(post(attemptUrl + "/discard").with(learner).with(csrf()))
                .andExpect(redirectedUrl("/student/results"));
        mvc.perform(readingStart).andExpect(redirectedUrlPattern("/student/attempts/*"));
        mvc.perform(combinedStart)
                .andExpect(redirectedUrl("/student/placement?skill=FULL_MOCK"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void absentLevelAndDurationRemainUnknownInsteadOfBreakingTheCatalog() throws Exception {
        var exam = catalogExam("Optional exam metadata", "PRACTICE", "READING", null, 20);
        exam.setDurationMinutes(null);
        exams.saveAndFlush(exam);
        for (String sort : List.of("level", "duration", "newest")) {
            mvc.perform(
                            get("/student/exams")
                                    .param("q", "Optional exam metadata")
                                    .param("sort", sort)
                                    .with(user("student").roles("STUDENT")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Chưa có")));
        }
    }

    @Test
    void incompleteScoringPolicyBlocksAnExamWithoutBreakingTheStudentCatalog() throws Exception {
        var certificate =
                certificates
                        .findById(
                                exams.findById(examIds.getFirst()).orElseThrow().getCertificateId())
                        .orElseThrow();
        var scheme = new ScoringScheme();
        scheme.setCertificate(certificate);
        scheme.setName("Incomplete student catalog policy");
        scheme.setVersion("TEST-" + System.nanoTime());
        scheme.setStatus(RecordStatus.ACTIVE);
        scheme = schemes.saveAndFlush(scheme);

        var exam = catalogExam("Incomplete policy exam", "PRACTICE", "READING", "B1", 35);
        exam.setScoringSchemeId(scheme.getId());
        exams.saveAndFlush(exam);

        mvc.perform(
                        get("/student/exams")
                                .param("q", "Incomplete policy exam")
                                .with(user("student").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("0 kết quả")));
        mvc.perform(get("/student/exams/" + exam.getId()).with(user("student").roles("STUDENT")))
                .andExpect(status().isNotFound());
    }

    @Test
    void startablePracticeExamExposesStartActionInCatalog() throws Exception {
        var exam = catalogExam("Startable practice exam", "PRACTICE", "READING", "B1", 35);
        mvc.perform(
                        get("/student/exams")
                                .param("q", "Startable practice exam")
                                .with(user("student").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bắt đầu làm bài")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "/student/exams/" + exam.getId() + "/start")));
    }

    @Test
    void examDetailOffersLatestResumableAttemptAndCanStillStartANewOne() throws Exception {
        var learner = user(createLearner().getEmail()).roles("STUDENT");
        String detail = "/student/exams/" + examIds.get(1);
        String start = detail + "/start";
        String first =
                mvc.perform(post(start).with(learner).with(csrf()))
                        .andExpect(status().is3xxRedirection())
                        .andReturn()
                        .getResponse()
                        .getRedirectedUrl();
        mvc.perform(get(first).with(learner))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"" + first + "/exit\"")));
        mvc.perform(post(first + "/exit").with(learner)).andExpect(status().isForbidden());
        mvc.perform(post(first + "/exit").with(learner).with(csrf()))
                .andExpect(redirectedUrl("/student/exams"));
        mvc.perform(get(first).with(learner)).andExpect(status().isOk());
        mvc.perform(get(detail).with(learner))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"" + first + "\"")))
                .andExpect(content().string(containsString("Tiếp tục làm bài")))
                .andExpect(content().string(containsString("Bắt đầu lượt mới")));
        mvc.perform(get(detail).with(user(createLearner().getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Tiếp tục làm bài"))))
                .andExpect(content().string(containsString("Bắt đầu làm bài")));

        String second =
                mvc.perform(post(start).with(learner).with(csrf()))
                        .andExpect(status().is3xxRedirection())
                        .andReturn()
                        .getResponse()
                        .getRedirectedUrl();
        assertThat(second).startsWith("/student/attempts/").isNotEqualTo(first);
        mvc.perform(get(detail).with(learner))
                .andExpect(content().string(containsString("href=\"" + second + "\"")));

        int secondId = Integer.parseInt(second.substring(second.lastIndexOf('/') + 1));
        var expired = attempts.findById(secondId).orElseThrow();
        expired.setStartedAt(LocalDateTime.now().minusDays(1));
        attempts.saveAndFlush(expired);
        mvc.perform(get(detail).with(learner))
                .andExpect(content().string(containsString("href=\"" + first + "\"")))
                .andExpect(content().string(not(containsString("href=\"" + second + "\""))));

        mvc.perform(post(first + "/discard").with(learner).with(csrf()))
                .andExpect(flash().attributeExists("successMessage"));
        mvc.perform(get(detail).with(learner))
                .andExpect(content().string(not(containsString("Tiếp tục làm bài"))))
                .andExpect(content().string(containsString("Bắt đầu làm bài")));
    }

    private User createLearner() {
        var user = new User();
        user.setEmail("exam-preview-" + System.nanoTime() + "@test.local");
        user.setFullName("Học viên luyện đề");
        user.setPasswordHash("unused");
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        return users.saveAndFlush(user);
    }

    private MockExamEntity catalogExam(
            String title, String type, String skill, String level, int minutes) {
        var exam = new MockExamEntity();
        exam.setCertificateId(exams.findById(examIds.getFirst()).orElseThrow().getCertificateId());
        exam.setTitle(title);
        exam.setExamType(type);
        exam.setSkill(skill);
        exam.setTargetLevel(level);
        exam.setDurationMinutes(minutes);
        exam.setStatus("PUBLISHED");
        if ("FULL_MOCK".equals(skill)) attachScoring(exam, com.aptisprep.enums.Skill.codes());
        else if (List.of("PLACEMENT", "CHECKPOINT").contains(type))
            attachScoring(exam, List.of(skill));
        var saved = exams.saveAndFlush(exam);
        addExamQuestions(saved);
        return saved;
    }

    private void attachScoring(MockExamEntity exam, List<String> skillCodes) {
        var scheme = new ScoringScheme();
        scheme.setCertificate(certificates.findById(exam.getCertificateId()).orElseThrow());
        scheme.setName("Test policy");
        scheme.setVersion("TEST-" + System.nanoTime());
        scheme.setStatus(RecordStatus.ACTIVE);
        scheme = schemes.saveAndFlush(scheme);
        for (String skill : skillCodes) {
            var rule = new com.aptisprep.entity.ScoringScaleRule();
            rule.setScheme(scheme);
            rule.setSkill(skill);
            rule.setRawMin(java.math.BigDecimal.ZERO);
            rule.setRawMax(java.math.BigDecimal.TEN);
            rule.setScaleMin(java.math.BigDecimal.ZERO);
            rule.setScaleMax(java.math.BigDecimal.TEN);
            rules.saveAndFlush(rule);
            var band = new com.aptisprep.entity.ScoreBand();
            band.setScheme(scheme);
            band.setSkill(skill);
            band.setCefrLevel("A1");
            band.setMinScore(java.math.BigDecimal.ZERO);
            band.setMaxScore(java.math.BigDecimal.TEN);
            bands.saveAndFlush(band);
        }
        exam.setScoringSchemeId(scheme.getId());
    }

    private void addExamQuestions(MockExamEntity exam) {
        boolean fullMock = "FULL_MOCK".equals(exam.getSkill());
        var skills = fullMock ? com.aptisprep.enums.Skill.codes() : List.of(exam.getSkill());
        int position = 0;
        for (String skill : skills) {
            var question = new QuestionEntity();
            question.setContent("Test question");
            question.setQuestionType("SINGLE_CHOICE");
            question.setSkill(skill);
            question.setCefrLevel("A1");
            question.setStatus("PUBLISHED");
            question = questions.saveAndFlush(question);
            if ("LISTENING".equals(skill)) {
                var audio = new QuestionMediaEntity();
                audio.setQuestionId(question.getId());
                audio.setMediaType("AUDIO");
                audio.setMediaUrl(TestAudio.store(storage));
                audio.setOriginalFilename("test.wav");
                audio.setContentType("audio/wav");
                audio.setPosition(1);
                media.saveAndFlush(audio);
            }
            var link = new MockExamQuestionEntity();
            link.setMockExamId(exam.getId());
            link.setQuestionId(question.getId());
            link.setPosition(++position);
            if (fullMock) {
                var section = new com.aptisprep.entity.MockExamSectionEntity();
                section.setMockExamId(exam.getId());
                section.setSkill(skill);
                section.setDisplayOrder(position);
                section.setDurationMinutes(5);
                section.setStatus("ACTIVE");
                link.setMockExamSectionId(sections.saveAndFlush(section).getId());
            }
            examQuestions.saveAndFlush(link);
        }
    }

    @Test
    void boundariesAndMissingEntriesAreHandled() throws Exception {
        for (String path :
                new String[] {
                    "/student/lessons", "/student/exams/7001", "/student/attempts/8001/result"
                }) {
            mvc.perform(get(path)).andExpect(status().is3xxRedirection());
            mvc.perform(get(path).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/student/lessons/7001").with(user("student").roles("STUDENT")))
                .andExpect(status().isNotFound());
        mvc.perform(
                        get("/student/lessons")
                                .param("q", "x".repeat(256))
                                .with(user("student").roles("STUDENT")))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/student/exams/7001/start")
                                .with(csrf())
                                .with(user("student").roles("STUDENT")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void examRoutesStayWithinTheirModuleAndRejectNonNumericIds() throws Exception {
        for (String path : new String[] {"/student/not-a-module", "/student/not-a-module/1"}) {
            mvc.perform(get(path).with(user("student").roles("STUDENT")))
                    .andExpect(status().isNotFound());
        }
        for (String path :
                new String[] {
                    "/student/exams/not-an-id",
                    "/student/attempts/not-an-id",
                    "/student/attempts/not-an-id/result",
                    "/student/attempts/1/media/not-an-id/content",
                    "/student/attempts/1/answers/not-an-id/audio"
                }) {
            mvc.perform(get(path).with(user("student").roles("STUDENT")))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(
                        post("/student/exams/not-an-id/start")
                                .with(user("student").roles("STUDENT"))
                                .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}
