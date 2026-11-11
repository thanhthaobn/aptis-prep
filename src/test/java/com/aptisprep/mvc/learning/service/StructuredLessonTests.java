package com.aptisprep.mvc.learning.service;

import static com.aptisprep.mvc.common.security.SessionMvcRequests.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aptisprep.mvc.auth.entity.User;
import com.aptisprep.mvc.auth.repository.UserRepository;
import com.aptisprep.mvc.common.config.StorageProperties;
import com.aptisprep.mvc.common.enums.*;
import com.aptisprep.mvc.common.exception.BusinessRuleException;
import com.aptisprep.mvc.learning.dto.*;
import com.aptisprep.mvc.learning.dto.LearningPathViewsDTO.StageLessonDTO;
import com.aptisprep.mvc.learning.dto.LessonFormDTO;
import com.aptisprep.mvc.learning.entity.*;
import com.aptisprep.mvc.learning.entity.Lesson;
import com.aptisprep.mvc.learning.entity.LessonWorkSubmission;
import com.aptisprep.mvc.learning.repository.*;
import com.aptisprep.mvc.learning.service.impl.*;
import com.aptisprep.mvc.storage.service.StorageService;
import com.aptisprep.mvc.storage.support.TestAudio;
import java.nio.file.Files;
import java.util.Map;
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
class StructuredLessonTests {
    @Autowired LessonService lessons;
    @Autowired LessonTeachingService teaching;
    @Autowired UserRepository users;
    @Autowired MockMvc mvc;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired LessonResponseRepository responseRepository;
    @Autowired LearningPathService learningPaths;
    @Autowired StorageService storage;
    @Autowired StorageProperties storageProperties;

    @Test
    void libraryShowsCompletedLessonsFromPathAndKeepsProgressPrivate() throws Exception {
        var student = learner("library-progress@test.local");
        var other = learner("library-other@test.local");
        var lesson = create("Reading");
        learningPaths.ensurePaths(student.getId());
        learningPaths.completeLesson(student.getId(), lesson.getId());
        assertThat(
                        learningPaths.stages(student.getEmail(), "Reading").stream()
                                .flatMap(stage -> stage.lessons().stream())
                                .filter(item -> item.id().equals(lesson.getId())))
                .allMatch(StageLessonDTO::completed)
                .hasSize(1);
        mvc.perform(
                        get("/student/lessons")
                                .param("skill", "Reading")
                                .param("level", "A1")
                                .with(user(student.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "data-lesson-id=\""
                                                        + lesson.getId()
                                                        + "\" data-learning-status=\"COMPLETED\"")))
                .andExpect(content().string(containsString("Đã hoàn thành")))
                .andExpect(content().string(containsString("Ôn lại →")));
        mvc.perform(
                        get("/student/lessons")
                                .param("skill", "Reading")
                                .param("level", "A1")
                                .with(user(other.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "data-lesson-id=\""
                                                        + lesson.getId()
                                                        + "\" data-learning-status=\"NOT_STARTED\"")));
        lessons.recordOpen(other.getEmail(), lesson.getId());
        assertThat(lessons.lessonStatuses(other.getEmail(), java.util.List.of(lesson.getId())))
                .containsEntry(lesson.getId(), "IN_PROGRESS");
    }

    @Test
    void writingSubmissionsKeepExactTextAndDraftsDoNotCreateHistory() throws Exception {
        var a = learner("writing-history@test.local");
        var l = create("Writing");
        teaching.saveResponse(l.getId(), "Draft only", a.getEmail());
        assertThat(teaching.submissionHistory(l.getId(), a.getEmail())).isEmpty();
        teaching.submitResponse(l.getId(), "First saved essay", a.getEmail());
        teaching.submitResponse(l.getId(), "Second saved essay", a.getEmail());
        entityManager.flush();
        entityManager.clear();
        assertThat(teaching.submissionHistory(l.getId(), a.getEmail()))
                .extracting(LessonWorkSubmission::getText)
                .containsExactly("Second saved essay", "First saved essay");
        mvc.perform(get("/student/lessons/" + l.getId()).with(user(a.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("First saved essay")));
    }

    @Test
    void speakingRequiresRecordingAndOldRecordingsRemainPrivateAndReplayable() throws Exception {
        var a = learner("speaking-history@test.local");
        var b = learner("speaking-other@test.local");
        var l = create("Speaking");
        var q = teaching.questions(l.getId()).getFirst();
        teaching.grade(l.getId(), Map.of("q" + q.id(), "1"), a.getEmail());
        teaching.saveResponse(l.getId(), "Transcript alone", a.getEmail());
        assertThat(teaching.completion(l.getId(), a.getEmail()).eligible()).isFalse();
        assertThatThrownBy(
                        () -> teaching.submitResponse(l.getId(), "Transcript alone", a.getEmail()))
                .isInstanceOf(BusinessRuleException.class);
        byte[] bytes = {0x1a, 0x45, (byte) 0xdf, (byte) 0xa3, 0, 0, 0, 0};
        teaching.saveRecording(
                l.getId(),
                new org.springframework.mock.web.MockMultipartFile(
                        "recording", "answer.webm", "audio/webm", bytes),
                a.getEmail());
        var first = teaching.submissionHistory(l.getId(), a.getEmail()).getFirst();
        teaching.saveRecording(
                l.getId(),
                new org.springframework.mock.web.MockMultipartFile(
                        "recording", "answer.webm", "audio/webm", bytes),
                a.getEmail());
        entityManager.flush();
        entityManager.clear();
        assertThat(teaching.completion(l.getId(), a.getEmail()).eligible()).isTrue();
        assertThat(teaching.submissionHistory(l.getId(), a.getEmail())).hasSize(2);
        String url = "/student/lessons/" + l.getId() + "/submissions/" + first.getId() + "/audio";
        mvc.perform(get(url).with(user(a.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(bytes));
        mvc.perform(get(url).with(user(b.getEmail()).roles("STUDENT")))
                .andExpect(status().isNotFound());
    }

    @Test
    void knowledgeHistoryStoresQuestionAndSelectedAnswerEvenWhenLessonChanges() {
        var a = learner("check-history@test.local");
        var l = create("Reading");
        var q = teaching.questions(l.getId()).getFirst();
        teaching.grade(l.getId(), Map.of("q" + q.id(), "0"), a.getEmail());
        assertThat(teaching.completion(l.getId(), a.getEmail()).eligible()).isFalse();
        var f = lessons.form(l.getId());
        f.getChecks().getFirst().setPrompt("New question after editing");
        lessons.save(l.getId(), f);
        entityManager.flush();
        entityManager.clear();
        var answer = teaching.history(l.getId(), a.getEmail()).getFirst().answers().getFirst();
        assertThat(answer.prompt()).isEqualTo("Choose the correct strategy");
        assertThat(answer.selected()).isEqualTo("First");
        assertThat(answer.correct()).isEqualTo("Second");
    }

    @Test
    void directVideoUploadPersistsAndChecksAccessAndFormat() throws Exception {
        var a = learner("video-student@test.local");
        var f = lessons.newForm("Reading", "A1");
        f.setTitle("Uploaded video lesson");
        f.setStatus("PUBLISHED");
        byte[] bytes = {0, 0, 0, 12, 'f', 't', 'y', 'p', 'm', 'p', '4', '2'};
        var video =
                new org.springframework.mock.web.MockMultipartFile(
                        "video", "lesson.mp4", "video/mp4", bytes);
        var l = lessons.save(null, f, null, false, null, false, video, false);
        Integer id = l.getId();
        entityManager.flush();
        entityManager.clear();
        assertThat(lessons.require(id).getVideoPath()).isNotBlank();
        String url = "/lesson-media/" + id + "/video";
        mvc.perform(get(url)).andExpect(status().is3xxRedirection());
        mvc.perform(get(url).with(user(a.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("video/mp4"))
                .andExpect(content().bytes(bytes));
        mvc.perform(get(url).header("Range", "bytes=0-3").with(user(a.getEmail()).roles("STUDENT")))
                .andExpect(status().isPartialContent())
                .andExpect(content().bytes(new byte[] {0, 0, 0, 12}));
        lessons.archive(id);
        mvc.perform(get(url).with(user(a.getEmail()).roles("STUDENT")))
                .andExpect(status().isNotFound());
        var bad =
                new org.springframework.mock.web.MockMultipartFile(
                        "video", "fake.mp4", "video/mp4", new byte[] {1, 2, 3});
        assertThatThrownBy(() -> lessons.save(id, f, null, false, null, false, bad, false))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void listeningRequiresEnoughPlaybackAndReceptiveSkillsHaveNoNotesForm() throws Exception {
        var a = learner("listen-progress@test.local");
        var l = create("Listening");
        l.setAudioPath(TestAudio.store(storage));
        entityManager.flush();
        String url = "/student/lessons/" + l.getId() + "/audio-played";
        mvc.perform(
                        post(url)
                                .with(csrf())
                                .with(user(a.getEmail()).roles("STUDENT"))
                                .param("duration", "100")
                                .param("listened", "1"))
                .andExpect(status().isBadRequest());
        assertThat(teaching.completion(l.getId(), a.getEmail()).audioPlayed()).isFalse();
        mvc.perform(
                        post(url)
                                .with(csrf())
                                .with(user(a.getEmail()).roles("STUDENT"))
                                .param("duration", "100")
                                .param("listened", "90"))
                .andExpect(status().isBadRequest());
        assertThat(teaching.completion(l.getId(), a.getEmail()).audioPlayed()).isFalse();
        for (String skill : java.util.List.of("Reading", "Listening", "Grammar & Vocabulary")) {
            var lesson = create(skill);
            mvc.perform(
                            get("/student/lessons/" + lesson.getId())
                                    .with(user(a.getEmail()).roles("STUDENT")))
                    .andExpect(status().isOk())
                    .andExpect(
                            content().string(not(containsString("id=\"lesson-response-form\""))));
        }
    }

    @Test
    void pathOverviewShowsCompactFactsAndSkillPageKeepsTheStageScale() throws Exception {
        var u = learner("stage-scale@test.local");
        create("Reading");
        String overview =
                mvc.perform(get("/student/learning-path").with(user(u.getEmail()).roles("STUDENT")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(overview)
                .contains("Hiện tại", "Đích chặng", "Mục tiêu")
                .contains("class=\"stage-scale\"", "Thang chặng CEFR")
                .doesNotContain("Xem chặng học");
        String detail =
                mvc.perform(
                                get("/student/learning-path/Reading")
                                        .with(user(u.getEmail()).roles("STUDENT")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(detail)
                .contains("Bài học được giao trong chặng", "Reading lesson", "Thang chặng CEFR")
                .doesNotContain(
                        "Các bài A1 giúp bạn làm quen", "Thang cấp độ không phải điểm đã đạt")
                .doesNotContain(
                        "Trình độ khi bắt đầu",
                        "Cấp độ cần xác nhận",
                        "Mục tiêu cuối",
                        "Thư viện toàn bộ cấp độ để tự ôn");
    }

    @Test
    void libraryGroupsByTopicAndStartsCollapsedEvenWithFilters() throws Exception {
        var u = learner("tree@test.local");
        create("Reading");
        String html =
                mvc.perform(
                                get("/student/lessons")
                                        .param("skill", "Reading")
                                        .param("q", "Everyday life")
                                        .with(user(u.getEmail()).roles("STUDENT")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(html)
                .contains("library-skill", "library-level", "Everyday life", "Reading lesson")
                .doesNotContain(" open", "Cập nhật lộ trình");
    }

    @Test
    void practiceWorkspacesMatchTheSkillAndListeningKeepsAudioWithQuestions() throws Exception {
        var u = learner("practice-layout@test.local");
        for (String skill : LessonService.SKILLS) {
            var lesson = create(skill);
            if (skill.equals("Listening")) {
                lesson.setAudioPath(TestAudio.store(storage));
                lesson.setAudioName("Bài nghe.wav");
            }
            String html =
                    mvc.perform(
                                    get("/student/lessons/" + lesson.getId())
                                            .with(user(u.getEmail()).roles("STUDENT")))
                            .andExpect(status().isOk())
                            .andReturn()
                            .getResponse()
                            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            assertThat(html).doesNotContain("Kết quả không quy đổi CEFR.");
            if (skill.equals("Writing") || skill.equals("Speaking"))
                assertThat(html.substring(html.indexOf("id=\"practice\"")))
                        .doesNotContain("class=\"lesson-question\"");
            if (!skill.equals("Reading")) assertThat(html).doesNotContain("Câu hỏi đọc hiểu");
            switch (skill) {
                case "Writing" ->
                        assertThat(html)
                                .contains(
                                        "Thực hành viết", "Bài viết của bạn", "Ôn phương pháp viết")
                                .doesNotContain("id=\"speaking-recorder\"");
                case "Speaking" -> {
                    assertThat(html).contains("Thực hành nói", "Ôn phương pháp nói");
                    assertThat(html.indexOf("id=\"speaking-recorder\""))
                            .isLessThan(html.indexOf("id=\"lesson-response-form\""));
                }
                case "Listening" -> {
                    assertThat(html).contains("Câu hỏi nghe hiểu");
                    assertThat(html.indexOf("id=\"practice\""))
                            .isLessThan(html.indexOf("id=\"lesson-audio\""));
                    assertThat(html.indexOf("id=\"lesson-audio\""))
                            .isLessThan(html.indexOf("Câu hỏi nghe hiểu"));
                }
                default -> {}
            }
        }
    }

    @Test
    void legacyImagePathRemainsReadableInHistoryAfterLessonChanges() throws Exception {
        var owner = learner("media-history-owner@test.local");
        var other = learner("media-history-other@test.local");
        var lesson = create("Reading");
        String relativePath = "curriculum/images/history-" + lesson.getId() + ".svg";
        byte[] bytes =
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1 1\"></svg>"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var file = storageProperties.getUploadDir().resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, bytes);
        lesson.setImagePath(relativePath);
        lesson.setImageName("history.svg");
        entityManager.flush();

        mvc.perform(
                        get("/lesson-media/" + lesson.getId() + "/image")
                                .with(user(owner.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/svg+xml"))
                .andExpect(content().bytes(bytes));
        var question = teaching.questions(lesson.getId()).getFirst();
        teaching.grade(lesson.getId(), Map.of("q" + question.id(), "1"), owner.getEmail());
        var history = teaching.history(lesson.getId(), owner.getEmail()).getFirst();
        String url =
                "/student/lessons/"
                        + lesson.getId()
                        + "/history/attempts/"
                        + history.id()
                        + "/media/image";
        lesson.setImagePath(null);
        entityManager.flush();
        entityManager.clear();

        assertThat(
                        teaching.historicalMedia(
                                        lesson.getId(),
                                        history.id(),
                                        "image",
                                        owner.getEmail(),
                                        true)
                                .path())
                .isEqualTo(relativePath);
        mvc.perform(get(url).with(user(owner.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/svg+xml"))
                .andExpect(content().bytes(bytes));
        mvc.perform(get(url).with(user(other.getEmail()).roles("STUDENT")))
                .andExpect(status().isNotFound());
        mvc.perform(get(url)).andExpect(status().is3xxRedirection());
    }

    private User learner(String email) {
        var u = new User();
        u.setEmail(email);
        u.setFullName("Learner");
        u.setRole(Role.STUDENT);
        u.setStatus(UserStatus.ACTIVE);
        u.setPasswordHash("unused");
        return users.saveAndFlush(u);
    }

    private Lesson create(String skill) {
        var f = lessons.newForm(skill, "A1");
        f.setTitle(skill + " lesson");
        f.setTopic("Everyday life");
        f.setStatus("PUBLISHED");
        f.setObjectives("Learn strategies");
        f.setStrategy("Step one");
        f.setExampleText("Example");
        f.setAnalysis("Analysis");
        f.setPracticePrompt("Try writing");
        f.setSampleResponse("Model");
        f.setVideoUrl("https://youtu.be/yiwhUC4S5_o");
        f.setSourceUrl("https://www.britishcouncil.org/");
        var q = new LessonFormDTO.CheckForm();
        q.setPrompt("Choose the correct strategy");
        q.setOptionA("First");
        q.setOptionB("Second");
        q.setOptionC("Third");
        q.setCorrectOption(1);
        q.setExplanation("PRIVATE_EXPLANATION");
        f.getChecks().add(q);
        return lessons.save(null, f);
    }

    @Test
    void allSkillViewsAndEditorRenderWithoutDisclosingKeys() throws Exception {
        var u = learner("structured@test.local");
        for (String skill : LessonService.SKILLS) {
            var l = create(skill);
            mvc.perform(
                            get("/student/lessons/" + l.getId())
                                    .with(user(u.getEmail()).roles("STUDENT")))
                    .andExpect(status().isOk())
                    .andExpect(
                            content()
                                    .string(
                                            containsString(
                                                    "youtube-nocookie.com/embed/yiwhUC4S5_o")))
                    .andExpect(content().string(not(containsString("PRIVATE_EXPLANATION"))));
            mvc.perform(
                            get("/admin/lessons/" + l.getId() + "/edit")
                                    .with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk());
            mvc.perform(
                            get("/student/learning-path/" + skill)
                                    .with(user(u.getEmail()).roles("STUDENT")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Everyday life")));
        }
        mvc.perform(get("/admin/lessons").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void answersAreGradedOnServerAndDraftsBelongToTheirLearner() {
        var a = learner("author-a@test.local");
        var b = learner("author-b@test.local");
        var l = create("Reading");
        var q = teaching.questions(l.getId()).getFirst();
        assertThat(teaching.completion(l.getId(), a.getEmail()).eligible()).isFalse();
        assertThat(teaching.grade(l.getId(), Map.of("q" + q.id(), "1"), a.getEmail()).score())
                .isEqualTo(1);
        assertThat(teaching.grade(l.getId(), Map.of("q" + q.id(), "0"), a.getEmail()).score())
                .isZero();
        assertThat(teaching.history(l.getId(), a.getEmail())).hasSize(2);
        assertThat(teaching.completion(l.getId(), a.getEmail()).eligible()).isTrue();
        assertThatThrownBy(() -> teaching.grade(l.getId(), Map.of(), a.getEmail()))
                .isInstanceOf(BusinessRuleException.class);
        var writing = create("Writing");
        teaching.saveResponse(writing.getId(), "My work", a.getEmail());
        assertThat(teaching.response(writing.getId(), a.getEmail())).isEqualTo("My work");
        assertThat(teaching.response(writing.getId(), b.getEmail())).isEmpty();
    }

    @Test
    void rejectsUnsafeVideoAndSources() {
        assertThatThrownBy(() -> teaching.videoId("javascript:alert(1)"))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(
                        () -> teaching.videoId("https://youtube.com.evil.test/watch?v=yiwhUC4S5_o"))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(teaching.videoId("https://www.youtube.com/watch?v=yiwhUC4S5_o&feature=share"))
                .isEqualTo("yiwhUC4S5_o");
    }

    @Test
    void knowledgeChecksMayBeAbsentAndMayHaveTwoOrSixOptions() {
        var empty = lessons.newForm("Reading", "A2");
        empty.setTitle("No check");
        empty.setStatus("DRAFT");
        var noCheck = lessons.save(null, empty);
        assertThat(teaching.questions(noCheck.getId())).isEmpty();
        var f = lessons.newForm("Listening", "A2");
        f.setTitle("Flexible checks");
        f.setStatus("DRAFT");
        var two = new LessonFormDTO.CheckForm();
        two.setPrompt("Two choices");
        two.setOptionA("Yes");
        two.setOptionB("No");
        two.setCorrectOption(1);
        two.setExplanation("No is correct");
        var six = new LessonFormDTO.CheckForm();
        six.setPrompt("Six choices");
        six.setOptionA("A");
        six.setOptionB("B");
        six.setOptionC("C");
        six.setOptionD("D");
        six.setOptionE("E");
        six.setOptionF("F");
        six.setCorrectOption(5);
        six.setExplanation("F is correct");
        f.getChecks().add(two);
        f.getChecks().add(six);
        var lesson = lessons.save(null, f);
        assertThat(teaching.questions(lesson.getId()))
                .extracting(q -> q.options().size())
                .containsExactly(2, 6);
    }
}
