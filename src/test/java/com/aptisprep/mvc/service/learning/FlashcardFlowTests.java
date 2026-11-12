package com.aptisprep.mvc.service.learning;

import static com.aptisprep.mvc.security.SessionMvcRequests.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aptisprep.mvc.dto.FlashcardDTO;
import com.aptisprep.mvc.dto.FlashcardSetDTO;
import com.aptisprep.mvc.entity.Lesson;
import com.aptisprep.mvc.entity.User;
import com.aptisprep.mvc.enums.*;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.service.storage.StorageService;
import com.aptisprep.mvc.support.TestAudio;
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
class FlashcardFlowTests {
    @Test
    void sharedTreeOrdersSkillsAndLevelsAndExcludesPersonalSets() {
        var a2 = setView(1, "Reading A2", "A2", false, "Reading");
        var a1 = setView(2, "Reading A1", "A1", false, "Reading");
        var grammar = setView(3, "Grammar", "B1", false, "Grammar & Vocabulary");
        var mine = setView(4, "Private", "A1", true, null);
        var tree = service.groupBySkillAndLevel(java.util.List.of(a2, mine, a1, grammar));
        assertThat(tree.keySet()).containsExactly("Grammar & Vocabulary", "Reading");
        assertThat(tree.get("Reading"))
                .extracting(streamValue -> streamValue.getLevel())
                .containsExactly("A1", "A2");
        assertThat(tree.get("Reading")).containsExactly(a1, a2);
    }

    @Test
    void adminTreeKeepsDraftsUnassignedSetsAndEditLinks() throws Exception {
        var id = create();
        var set = sets.findById(id).orElseThrow();
        set.setStatus("DRAFT");
        sets.saveAndFlush(set);
        var orphan = new com.aptisprep.mvc.entity.FlashcardSet();
        orphan.setTitle("Unassigned legacy draft");
        orphan.setCefrLevel("A1");
        orphan.setStatus("DRAFT");
        sets.saveAndFlush(orphan);
        var result =
                mvc.perform(get("/admin/flashcards").with(user("admin").roles("ADMIN")))
                        .andExpect(status().isOk())
                        .andExpect(
                                content()
                                        .string(
                                                org.hamcrest.Matchers.containsString(
                                                        "flashcard-skill-branch")))
                        .andExpect(
                                content()
                                        .string(
                                                org.hamcrest.Matchers.containsString(
                                                        "flashcard-level-branch")))
                        .andExpect(
                                content()
                                        .string(
                                                org.hamcrest.Matchers.containsString(
                                                        "Chưa phân loại")))
                        .andExpect(
                                content()
                                        .string(
                                                org.hamcrest.Matchers.containsString(
                                                        "Unassigned legacy draft")))
                        .andExpect(
                                content().string(org.hamcrest.Matchers.containsString("Bản nháp")))
                        .andExpect(
                                content()
                                        .string(
                                                org.hamcrest.Matchers.containsString(
                                                        "/admin/flashcards/" + id + "/edit")))
                        .andReturn();
        @SuppressWarnings("unchecked")
        var tree =
                (java.util.Map<String, java.util.List<FlashcardSetDTO>>)
                        result.getModelAndView().getModel().get("lessonBranches");
        assertThat(tree.values().stream().flatMap(streamValue -> streamValue.stream()).toList())
                .extracting(streamValue -> streamValue.getId())
                .contains(id)
                .doesNotContain(orphan.getId());
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("<details class=\"flashcard-skill-branch\" open");
    }

    @Test
    void personalSectionStaysVisibleWhenEmptyOrFilteredOut() throws Exception {
        var a = learner("cards-empty-mine@test.local");
        create();
        mvc.perform(get("/student/flashcards").with(user(a.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bộ thẻ của tôi")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "Bạn chưa có bộ thẻ cá nhân đang hoạt"
                                                                + " động"))));
        var f = new FlashcardSetDTO();
        f.setTitle("Private practice");
        f.setCefrLevel("A1");
        var card = new FlashcardDTO();
        card.setFrontContent("learn");
        card.setBackContent("học");
        f.getFlashcards().add(card);
        service.savePersonal(null, f, a.getEmail());
        mvc.perform(
                        get("/student/flashcards")
                                .param("source", "lesson")
                                .with(user(a.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "Bộ lọc hiện tại đang ẩn các bộ thẻ của bạn")));
    }

    @Test
    void searchFiltersAndOverviewUseOnlyVisibleSets() throws Exception {
        var a = learner("cards-search@test.local");
        var id = create();
        var result =
                mvc.perform(
                                get("/student/flashcards")
                                        .param("q", "DAILY")
                                        .param("skill", "Grammar & Vocabulary")
                                        .param("level", "A1")
                                        .with(user(a.getEmail()).roles("STUDENT")))
                        .andExpect(status().isOk())
                        .andReturn();
        @SuppressWarnings("unchecked")
        var found =
                (java.util.List<FlashcardSetDTO>) result.getModelAndView().getModel().get("sets");
        assertThat(found).extracting(streamValue -> streamValue.getId()).containsExactly(id);
        assertThat(result.getModelAndView().getModel())
                .containsEntry("totalSets", 1)
                .containsEntry("totalCards", 1);
        mvc.perform(
                        get("/student/flashcards")
                                .param("q", "no-such-flashcard")
                                .with(user(a.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "Không có bộ thẻ phù hợp")));
    }

    @Autowired FlashcardSetService service;
    @Autowired UserRepository users;
    @Autowired LessonRepository lessons;
    @Autowired UserFlashcardProgressRepository progress;
    @Autowired MockMvc mvc;
    @Autowired FlashcardSetRepository sets;
    @Autowired StorageService storage;

    @Test
    void sourceFilterSeparatesMySetsFromLessonSetsAndHidesOtherOwners() throws Exception {
        var owner = learner("filter-owner@test.local");
        var other = learner("filter-other@test.local");
        create();
        var f = new FlashcardSetDTO();
        f.setTitle("My private vocabulary");
        var card = new FlashcardDTO();
        card.setFrontContent("study");
        card.setBackContent("học");
        f.getFlashcards().add(card);
        service.savePersonal(null, f, owner.getEmail());
        f.setTitle("Other owner's private vocabulary");
        service.savePersonal(null, f, other.getEmail());
        var auth = user(owner.getEmail()).roles("STUDENT");
        mvc.perform(get("/student/flashcards").param("source", "mine").with(auth))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "My private vocabulary")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "Daily routines"))))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "Other owner"))));
        mvc.perform(get("/student/flashcards").param("source", "lesson").with(auth))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Daily routines")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "My private vocabulary"))));
        mvc.perform(get("/student/flashcards").param("source", "invalid").with(auth))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lessonAndFlashcardCatalogsLinkInBothDirections() throws Exception {
        var student = learner("linked-catalog@test.local");
        var id = create();
        var set = sets.findById(id).orElseThrow();
        var lessonId = set.getLesson().getId();
        var auth = user(student.getEmail()).roles("STUDENT");
        mvc.perform(get("/student/lessons").with(auth))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "/student/flashcards/" + id)));
        mvc.perform(get("/student/flashcards").with(auth))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bài học nguồn")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "/student/lessons/" + lessonId)))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "data-skill=\"Grammar &amp; Vocabulary\"")));
        mvc.perform(get("/student/learning-path/Grammar & Vocabulary").with(auth))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "/student/flashcards/" + id)));
        set.getLesson().setStatus("ARCHIVED");
        lessons.saveAndFlush(set.getLesson());
        assertThat(service.listForStudent(student.getEmail())).noneMatch(s -> s.getId().equals(id));
        mvc.perform(get("/student/flashcards/" + id).with(auth)).andExpect(status().isNotFound());
    }

    @Test
    void uploadedAudioSupportsPlaybackButRemainsProtected() throws Exception {
        var student = learner("bundled-audio@test.local");
        var lesson = new Lesson();
        lesson.setTitle("Travel announcement");
        lesson.setSkill("Listening");
        lesson.setCefrLevel("A2");
        lesson.setDisplayOrder(4001);
        lesson.setStatus("PUBLISHED");
        lesson.setAudioPath(TestAudio.store(storage));
        lesson.setAudioName("Travel announcement.wav");
        lessons.saveAndFlush(lesson);
        String url = "/lesson-media/" + lesson.getId() + "/audio";
        mvc.perform(get(url)).andExpect(status().is3xxRedirection());
        mvc.perform(
                        get(url).header("Range", "bytes=0-43")
                                .with(user(student.getEmail()).roles("STUDENT")))
                .andExpect(status().isPartialContent())
                .andExpect(content().contentType("audio/wav"))
                .andExpect(
                        content()
                                .bytes(
                                        storage.load(lesson.getAudioPath())
                                                .getInputStream()
                                                .readNBytes(44)));
        lesson.setStatus("ARCHIVED");
        lessons.saveAndFlush(lesson);
        mvc.perform(get(url).with(user(student.getEmail()).roles("STUDENT")))
                .andExpect(status().isNotFound());
    }

    private User learner(String email) {
        var u = new User();
        u.setFullName("Learner");
        u.setEmail(email);
        u.setPasswordHash("unused");
        u.setRole(Role.STUDENT);
        u.setStatus(UserStatus.ACTIVE);
        return users.saveAndFlush(u);
    }

    private Integer create() {
        var lesson = new Lesson();
        lesson.setTitle("Daily routines lesson " + System.nanoTime());
        lesson.setSkill("Grammar & Vocabulary");
        lesson.setCefrLevel("A1");
        lesson.setDisplayOrder((int) (System.nanoTime() % 1000000000));
        lesson.setStatus("PUBLISHED");
        lesson = lessons.saveAndFlush(lesson);
        var f = new FlashcardSetDTO();
        f.setTitle("Daily routines");
        f.setCefrLevel("A1");
        f.setStatus("PUBLISHED");
        f.setLessonId(lesson.getId());
        var card = new FlashcardDTO();
        card.setFrontContent("get up");
        card.setBackContent("thức dậy");
        f.getFlashcards().add(card);
        return service.saveAdmin(null, f);
    }

    @Test
    void remembersPerLearnerAndPreservesRemovedCardHistory() {
        var a = learner("cards-a@test.local");
        var b = learner("cards-b@test.local");
        var id = create();
        var card = service.study(id, a.getEmail()).getFirst();
        service.mark(id, card.getId(), "MASTERED", a.getEmail());
        service.mark(id, card.getId(), "MASTERED", a.getEmail());
        assertThat(service.study(id, a.getEmail()).getFirst().getState()).isEqualTo("MASTERED");
        assertThat(service.study(id, b.getEmail()).getFirst().getState()).isEqualTo("LEARNING");
        assertThat(
                        progress.findByUserIdAndFlashcardId(a.getId(), card.getId())
                                .orElseThrow()
                                .getReviewCount())
                .isEqualTo(2);
        var f = service.form(id);
        f.getFlashcards().clear();
        var replacementCard = new FlashcardDTO();
        replacementCard.setFrontContent("breakfast");
        replacementCard.setBackContent("bữa sáng");
        f.getFlashcards().add(replacementCard);
        service.saveAdmin(id, f);
        assertThat(service.study(id, a.getEmail())).hasSize(1);
        assertThat(progress.findByUserIdAndFlashcardId(a.getId(), card.getId())).isPresent();
    }

    @Test
    void pagesRenderAndWritesRequireCsrfAndRole() throws Exception {
        var a = learner("cards-web@test.local");
        var id = create();
        var card = service.study(id, a.getEmail()).getFirst();
        mvc.perform(get("/admin/flashcards").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/flashcards/list"));
        mvc.perform(get("/admin/flashcards/" + id + "/edit").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/flashcards/form"))
                .andExpect(
                        content()
                                .string(org.hamcrest.Matchers.containsString("name=\"lessonId\"")));
        mvc.perform(get("/student/flashcards").with(user(a.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("student/flashcards/list"));
        mvc.perform(get("/student/flashcards/create").with(user(a.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("student/flashcards/form"))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "name=\"lessonId\""))));
        mvc.perform(get("/student/flashcards/" + id).with(user(a.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "Ôn lại từ chưa thuộc")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Học lại tất cả")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "Những từ cần học lại")));
        String url = "/student/flashcards/" + id + "/cards/" + card.getId() + "/state";
        mvc.perform(
                        post(url)
                                .param("state", "MASTERED")
                                .param("reviewKey", "test-review-00000001")
                                .with(user(a.getEmail()).roles("STUDENT")))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post(url)
                                .param("state", "MASTERED")
                                .param("reviewKey", "test-review-00000001")
                                .with(csrf())
                                .with(user(a.getEmail()).roles("STUDENT")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/admin/flashcards/create").with(user(a.getEmail()).roles("STUDENT")))
                .andExpect(status().isForbidden());
    }

    @Test
    void learnerCanArchiveOnlyOwnSet() {
        var a = learner("cards-owner@test.local");
        var b = learner("cards-other@test.local");
        var f = new FlashcardSetDTO();
        f.setTitle("My words");
        f.setCefrLevel("A1");
        var card = new FlashcardDTO();
        card.setFrontContent("focus");
        card.setBackContent("tập trung");
        f.getFlashcards().add(card);
        var id = service.savePersonal(null, f, a.getEmail());
        assertThat(service.listForStudent(a.getEmail())).anyMatch(s -> s.getId().equals(id));
        assertThatThrownBy(() -> service.archivePersonal(id, b.getEmail()))
                .isInstanceOf(com.aptisprep.mvc.exception.ResourceNotFoundException.class);
        service.archivePersonal(id, a.getEmail());
        assertThat(service.listForStudent(a.getEmail())).noneMatch(s -> s.getId().equals(id));
    }

    private static FlashcardSetDTO setView(
            int id, String title, String level, boolean personal, String skill) {
        var value = new FlashcardSetDTO();
        value.setId(id);
        value.setTitle(title);
        value.setLevel(level);
        value.setPersonal(personal);
        value.setLessonSkill(skill);
        return value;
    }
}
