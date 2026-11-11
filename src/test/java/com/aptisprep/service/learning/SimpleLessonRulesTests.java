package com.aptisprep.service.learning;

import static com.aptisprep.security.SessionMvcRequests.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aptisprep.dto.form.*;
import com.aptisprep.dto.form.LessonFormDTO;
import com.aptisprep.dto.view.*;
import com.aptisprep.dto.view.LessonAuthorProfileDTO;
import com.aptisprep.entity.*;
import com.aptisprep.entity.FlashcardSet;
import com.aptisprep.entity.LessonEngagement;
import com.aptisprep.entity.User;
import com.aptisprep.enums.*;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.repository.*;
import com.aptisprep.repository.FlashcardSetRepository;
import com.aptisprep.repository.LearningPathItemRepository;
import com.aptisprep.repository.UserRepository;
import com.aptisprep.service.learning.impl.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
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
class SimpleLessonRulesTests {
    @Autowired LessonService lessons;
    @Autowired LessonTeachingService teaching;
    @Autowired UserRepository users;
    @Autowired MockMvc mvc;
    @Autowired jakarta.persistence.EntityManager em;
    @Autowired LearningPathService learningPaths;
    @Autowired LearningPathItemRepository pathItems;
    @Autowired FlashcardSetRepository flashcardSets;

    private User learner(String email) {
        var u = new User();
        u.setEmail(email);
        u.setFullName("Learner");
        u.setPasswordHash("unused");
        u.setRole(Role.STUDENT);
        u.setStatus(UserStatus.ACTIVE);
        return users.saveAndFlush(u);
    }

    private LessonFormDTO form(String skill) {
        var f = lessons.newForm(skill, "A1");
        f.setTitle("Simple " + skill);
        f.setStatus("PUBLISHED");
        f.setPracticePrompt("Original task");
        f.setExampleText("ORIGINAL_PASSAGE");
        return f;
    }

    private void quiz(LessonFormDTO f) {
        var q = new LessonFormDTO.CheckForm();
        q.setPrompt("Original question");
        q.setOptionA("One");
        q.setOptionB("Two");
        q.setCorrectOption(1);
        q.setExplanation("Two is correct");
        f.getChecks().add(q);
    }

    private MockMultipartFile audio(byte[] bytes) {
        return new MockMultipartFile("audio", "clip.wav", "audio/wav", bytes);
    }

    private MockMultipartFile image(byte[] bytes) {
        return new MockMultipartFile("image", "picture.png", "image/png", bytes);
    }

    private MockMultipartFile video() {
        return new MockMultipartFile(
                "video",
                "clip.mp4",
                "video/mp4",
                new byte[] {0, 0, 0, 12, 'f', 't', 'y', 'p', 'm', 'p', '4', '2'});
    }

    private String page(Integer id, String email) throws Exception {
        return mvc.perform(get("/student/lessons/" + id).with(user(email).roles("STUDENT")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    void simpleEditorHasSkillLabelsUploadInputsAndReleaseSettings() throws Exception {
        for (var skill : LessonService.SKILLS) {
            var result =
                    mvc.perform(
                                    get("/admin/lessons/create")
                                            .param("skill", skill)
                                            .with(user("admin").roles("ADMIN")))
                            .andExpect(status().isOk())
                            .andReturn();
            String html =
                    result.getResponse()
                            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            assertThat(html)
                    .contains(
                            LessonAuthorProfileDTO.forSkill(skill).promptLabel(),
                            LessonAuthorProfileDTO.forSkill(skill).exampleLabel(),
                            "name=\"image\"",
                            "name=\"video\"",
                            "name=\"audio\"",
                            "sampleVisibility",
                            "transcriptVisibility")
                    .doesNotContain("block-list", "Thêm khối");
        }
    }

    @Test
    void minimalProductiveLessonDoesNotRequireAllOptionalTeachingFields() {
        for (var skill : java.util.List.of("Writing", "Speaking")) {
            var f = form(skill);
            f.setExampleText(null);
            assertThat(lessons.save(null, f).getMaterial().getPracticePrompt())
                    .isEqualTo("Original task");
            var bad = form(skill);
            bad.setPracticePrompt(null);
            assertThatThrownBy(() -> lessons.save(null, bad))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("đề thực hành");
        }
    }

    @Test
    void checksRequireAnswerExplanationAndConsecutiveOptions() {
        var f = form("Reading");
        quiz(f);
        f.getChecks().getFirst().setExplanation(null);
        assertThatThrownBy(() -> lessons.save(null, f)).isInstanceOf(BusinessRuleException.class);
        f.getChecks().getFirst().setExplanation("Reason");
        f.getChecks().getFirst().setOptionB(null);
        f.getChecks().getFirst().setOptionC("Third");
        assertThatThrownBy(() -> lessons.save(null, f)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void listeningReleaseIsServerGatedAndNewAudioResetsListening() throws Exception {
        var u = learner("release-listening@test.local");
        var f = form("Listening");
        quiz(f);
        f.setTranscript("HIDDEN_TRANSCRIPT");
        f.setSampleResponse("HIDDEN_MODEL");
        var l = lessons.save(null, f, audio(new byte[] {1, 2, 3}), false);
        String first = page(l.getId(), u.getEmail());
        assertThat(first).doesNotContain("HIDDEN_TRANSCRIPT", "HIDDEN_MODEL");
        var q = teaching.questions(l.getId()).getFirst();
        teaching.grade(l.getId(), java.util.Map.of("q" + q.id(), "0"), u.getEmail());
        assertThat(page(l.getId(), u.getEmail()))
                .contains("HIDDEN_MODEL")
                .doesNotContain("HIDDEN_TRANSCRIPT");
        // Fixture for this release-policy test; playback verification has its own service tests.
        var verified = new LessonEngagement();
        verified.setUserId(u.getId());
        verified.setLessonId(l.getId());
        verified.setAudioPlayedAt(java.time.LocalDateTime.now());
        verified.setVerifiedAudioPath(l.getAudioPath());
        em.persist(verified);
        assertThat(page(l.getId(), u.getEmail())).contains("HIDDEN_TRANSCRIPT");
        var edit = lessons.form(l.getId());
        lessons.save(l.getId(), edit, audio(new byte[] {4, 5, 6}), false);
        em.flush();
        em.clear();
        assertThat(page(l.getId(), u.getEmail())).doesNotContain("HIDDEN_TRANSCRIPT");
    }

    @Test
    void writingModelWaitsForSubmissionOrCompletionAndAutosaveDoesNotUnlock() throws Exception {
        var u = learner("release-writing@test.local");
        var f = form("Writing");
        f.setSampleResponse("PRIVATE_WRITING_MODEL");
        f.setSampleVisibility("AFTER_COMPLETE");
        var l = lessons.save(null, f);
        teaching.saveResponse(l.getId(), "Draft answer", u.getEmail());
        assertThat(page(l.getId(), u.getEmail()))
                .doesNotContain("PRIVATE_WRITING_MODEL", "Yêu cầu AI nhận xét");
        teaching.submitResponse(l.getId(), "Submitted answer", u.getEmail());
        assertThat(page(l.getId(), u.getEmail())).doesNotContain("PRIVATE_WRITING_MODEL");
        lessons.complete(u.getEmail(), l.getId());
        assertThat(page(l.getId(), u.getEmail())).contains("PRIVATE_WRITING_MODEL");
    }

    @Test
    void speakingOnlyRecordingCountsAsSubmissionAndIsSavedInDb() throws Exception {
        var u = learner("release-speaking@test.local");
        var f = form("Speaking");
        f.setSampleResponse("SPEAKING_MODEL");
        var l = lessons.save(null, f);
        teaching.saveResponse(l.getId(), "Transcript without recording", u.getEmail());
        assertThat(page(l.getId(), u.getEmail())).doesNotContain("SPEAKING_MODEL");
        teaching.saveRecording(
                l.getId(),
                new MockMultipartFile(
                        "recording",
                        "answer.webm",
                        "audio/webm",
                        new byte[] {0x1a, 0x45, (byte) 0xdf, (byte) 0xa3}),
                u.getEmail());
        em.flush();
        em.clear();
        assertThat(teaching.submissionHistory(l.getId(), u.getEmail()).getFirst().getAudioPath())
                .isNotBlank();
        assertThat(page(l.getId(), u.getEmail())).contains("SPEAKING_MODEL");
    }

    @Test
    void quizHistoryKeepsPassageOptionsAndMediaAfterEditingAndRemovingAllQuestions()
            throws Exception {
        var u = learner("simple-history@test.local");
        var other = learner("simple-history-other@test.local");
        var f = form("Reading");
        quiz(f);
        var l =
                lessons.save(
                        null, f, null, false, image(new byte[] {1, 2, 3}), false, video(), false);
        var q = teaching.questions(l.getId()).getFirst();
        teaching.grade(l.getId(), java.util.Map.of("q" + q.id(), "1"), u.getEmail());
        var original = teaching.history(l.getId(), u.getEmail()).getFirst();
        String prefix =
                "/student/lessons/" + l.getId() + "/history/attempts/" + original.id() + "/media/";
        var edit = lessons.form(l.getId());
        edit.setExampleText("NEW_PASSAGE");
        edit.getChecks().clear();
        lessons.save(l.getId(), edit, null, false, image(new byte[] {4, 5, 6}), false, null, true);
        em.flush();
        em.clear();
        var history = teaching.history(l.getId(), u.getEmail()).getFirst();
        assertThat(history.context().getExampleText()).isEqualTo("ORIGINAL_PASSAGE");
        assertThat(history.answers().getFirst().options()).containsExactly("One", "Two");
        assertThat(page(l.getId(), u.getEmail()))
                .contains("ORIGINAL_PASSAGE", "Đề bài tại thời điểm nộp", "Lịch sử làm bài");
        mvc.perform(get(prefix + "image").with(user(u.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
        mvc.perform(
                        get(prefix + "video")
                                .header("Range", "bytes=0-3")
                                .with(user(u.getEmail()).roles("STUDENT")))
                .andExpect(status().isPartialContent());
        mvc.perform(get(prefix + "image").with(user(other.getEmail()).roles("STUDENT")))
                .andExpect(status().isNotFound());
    }

    @Test
    void writingHistoryKeepsOriginalTaskTextWordLimitsAndImage() throws Exception {
        var u = learner("simple-writing-history@test.local");
        var f = form("Writing");
        f.setMinWordCount(2);
        f.setMaxWordCount(30);
        var l = lessons.save(null, f, null, false, image(new byte[] {8, 7, 6}), false);
        teaching.submitResponse(l.getId(), "My original answer", u.getEmail());
        var edit = lessons.form(l.getId());
        edit.setPracticePrompt("Changed writing task");
        edit.setExampleText("Changed example");
        edit.setMinWordCount(10);
        lessons.save(l.getId(), edit, null, false, null, true);
        em.flush();
        em.clear();
        var s = teaching.submissionHistory(l.getId(), u.getEmail()).getFirst();
        assertThat(s.getPrompt()).isEqualTo("Original task");
        assertThat(s.getText()).isEqualTo("My original answer");
        assertThat(s.getContext().getMinWords()).isEqualTo(2);
        mvc.perform(
                        get("/student/lessons/"
                                        + l.getId()
                                        + "/history/submissions/"
                                        + s.getId()
                                        + "/media/image")
                                .with(user(u.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[] {8, 7, 6}));
    }

    @Test
    void mediaAppearsExactlyOnceAtTheSkillPracticePositionAndIsProtected() throws Exception {
        var u = learner("simple-media-position@test.local");
        for (var skill : LessonService.SKILLS) {
            var f = form(skill);
            var l =
                    lessons.save(
                            null,
                            f,
                            skill.equals("Listening") ? audio(new byte[] {1, 2, 3}) : null,
                            false,
                            image(new byte[] {1, 2, 3}),
                            false,
                            video(),
                            false);
            String html = page(l.getId(), u.getEmail()),
                    src = "src=\"/lesson-media/" + l.getId() + "/image\"";
            assertThat(html.split(java.util.regex.Pattern.quote(src), -1)).hasSize(2);
            int position = html.indexOf(src);
            if (skill.equals("Reading") || skill.equals("Grammar & Vocabulary"))
                assertThat(position)
                        .isBetween(html.indexOf("id=\"example\""), html.indexOf("id=\"practice\""));
            else assertThat(position).isGreaterThan(html.indexOf("id=\"practice\""));
            if (skill.equals("Writing"))
                assertThat(position).isLessThan(html.indexOf("id=\"lesson-response-form\""));
            if (skill.equals("Speaking"))
                assertThat(position).isLessThan(html.indexOf("id=\"speaking-recorder\""));
            mvc.perform(
                            get("/lesson-media/" + l.getId() + "/video")
                                    .with(user(u.getEmail()).roles("STUDENT")))
                    .andExpect(status().isOk());
            lessons.archive(l.getId());
            mvc.perform(
                            get("/lesson-media/" + l.getId() + "/image")
                                    .with(user(u.getEmail()).roles("STUDENT")))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void listeningShowsPreparationStepsAndAudioWithoutMislabelledExample() throws Exception {
        var u = learner("listening-preparation@test.local");
        var f = form("Listening");
        f.setExampleText("A library announcement.");
        var l = lessons.save(null, f, audio(new byte[] {1, 2, 3}), false);
        String html = page(l.getId(), u.getEmail());
        assertThat(html)
                .contains(
                        "Trước khi nghe",
                        "Đọc câu hỏi, xác định thông tin cần nghe",
                        "Nghe lượt đầu để nắm ý chính",
                        "Nghe lại để kiểm tra chi tiết",
                        "id=\"lesson-audio\"")
                .doesNotContain("Ví dụ có hướng dẫn", "Ảnh đề bài thực hành");
        assertThat(html.indexOf("<h2>Trước khi nghe</h2>"))
                .isLessThan(html.indexOf("id=\"lesson-audio\""));
    }

    @Test
    void attachedFlashcardsNeverBlockLessonCompletion() throws Exception {
        var u = learner("optional-flashcards@test.local");
        var f = form("Reading");
        quiz(f);
        var l = lessons.save(null, f);
        var set = new FlashcardSet();
        set.setTitle("Reading vocabulary");
        set.setCefrLevel("A1");
        set.setStatus("PUBLISHED");
        set.setVisibility("PUBLIC");
        set.setLesson(l);
        flashcardSets.saveAndFlush(set);
        var q = teaching.questions(l.getId()).getFirst();
        teaching.grade(l.getId(), java.util.Map.of("q" + q.id(), "1"), u.getEmail());
        assertThat(page(l.getId(), u.getEmail()))
                .contains("Ôn từ vựng riêng, không bắt buộc để hoàn thành bài học.");
        assertThat(teaching.completion(l.getId(), u.getEmail()).eligible()).isTrue();
        assertThat(lessons.complete(u.getEmail(), l.getId()).getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void checkpointListsOnlyIncompleteRequiredLessonsWithLinks() throws Exception {
        var u = learner("checkpoint-missing-ui@test.local");
        var firstForm = form("Reading");
        firstForm.setTitle("Completed required reading");
        var first = lessons.save(null, firstForm);
        var secondForm = form("Reading");
        secondForm.setTitle("Remaining required reading");
        var second = lessons.save(null, secondForm);
        var extraForm = form("Reading");
        extraForm.setTitle("Optional extra reading");
        var extra = lessons.save(null, extraForm);
        var summary =
                learningPaths.ensureAndSummaries(u.getEmail()).stream()
                        .filter(p -> p.skill().equals("Reading"))
                        .findFirst()
                        .orElseThrow();
        for (var item : pathItems.findByLearningPathIdOrderByPositionAsc(summary.pathId()))
            if (item.getLesson().getId().equals(extra.getId())) {
                item.setRequired(false);
                pathItems.saveAndFlush(item);
            }
        lessons.complete(u.getEmail(), first.getId());
        // A status without a completion timestamp is not sufficient for the checkpoint.
        for (var item : pathItems.findByLearningPathIdOrderByPositionAsc(summary.pathId()))
            if (item.getLesson().getId().equals(second.getId())) {
                item.setStatus("COMPLETED");
                pathItems.saveAndFlush(item);
            }
        String html =
                mvc.perform(
                                get("/student/learning-path/Reading")
                                        .with(user(u.getEmail()).roles("STUDENT")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(html).contains("Đã hoàn thành 1/2 bài bắt buộc.", "Còn 1 bài cần hoàn thành");
        var missing =
                java.util.regex.Pattern.compile(
                                "<ul\\s+class=\"checkpoint-missing-list\"\\s*>(.*?)</ul\\s*>",
                                java.util.regex.Pattern.DOTALL)
                        .matcher(html);
        assertThat(missing.find()).isTrue();
        assertThat(missing.group(1))
                .contains("Remaining required reading", "/student/lessons/" + second.getId())
                .doesNotContain("Completed required reading", "Optional extra reading");
    }

    @Test
    void releaseSettingsRejectUnknownAndInapplicableRules() {
        var f = form("Reading");
        f.setSampleVisibility("AFTER_LISTEN");
        assertThatThrownBy(() -> lessons.save(null, f)).isInstanceOf(BusinessRuleException.class);
        f.setSampleVisibility("unknown");
        assertThatThrownBy(() -> lessons.save(null, f)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void afterListeningCannotBePublishedWithoutAudio() {
        var f = form("Listening");
        f.setTranscript("Transcript");
        assertThatThrownBy(() -> lessons.save(null, f))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("tải audio");
    }
}
