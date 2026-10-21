package com.aptisprep.service.learning;
import com.aptisprep.dto.form.FlashcardSetForm;
import com.aptisprep.entity.User;
import com.aptisprep.entity.Lesson;
import com.aptisprep.enums.*;
import com.aptisprep.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,TransactionalTestExecutionListener.class})
class FlashcardFlowTests {
 @Autowired FlashcardSetService service; @Autowired UserRepository users; @Autowired LessonRepository lessons;
 @Autowired UserFlashcardProgressRepository progress; @Autowired MockMvc mvc;
 @Autowired FlashcardSetRepository sets;
 @Test void sourceFilterSeparatesMySetsFromLessonSetsAndHidesOtherOwners() throws Exception {
  var owner=learner("filter-owner@test.local");var other=learner("filter-other@test.local");create();
  var f=new FlashcardSetForm();f.setTitle("My private vocabulary");var row=new FlashcardSetForm.Row();row.setTerm("study");row.setDefinition("học");f.getRows().add(row);
  service.savePersonal(null,f,owner.getEmail());f.setTitle("Other owner's private vocabulary");service.savePersonal(null,f,other.getEmail());
  var auth=user(owner.getEmail()).roles("STUDENT");
  mvc.perform(get("/student/flashcards").param("source","mine").with(auth)).andExpect(status().isOk())
    .andExpect(content().string(org.hamcrest.Matchers.containsString("My private vocabulary")))
    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Daily routines"))))
    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Other owner"))));
  mvc.perform(get("/student/flashcards").param("source","lesson").with(auth)).andExpect(status().isOk())
    .andExpect(content().string(org.hamcrest.Matchers.containsString("Daily routines")))
    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("My private vocabulary"))));
  mvc.perform(get("/student/flashcards").param("source","invalid").with(auth)).andExpect(status().isBadRequest());
 }
 @Test void lessonAndFlashcardCatalogsLinkInBothDirections() throws Exception {
  var student=learner("linked-catalog@test.local");var id=create();var set=sets.findById(id).orElseThrow();var lessonId=set.getLesson().getId();
  var auth=user(student.getEmail()).roles("STUDENT");
  mvc.perform(get("/student/lessons").with(auth)).andExpect(status().isOk())
    .andExpect(content().string(org.hamcrest.Matchers.containsString("/student/flashcards/"+id)));
  mvc.perform(get("/student/flashcards").with(auth)).andExpect(status().isOk())
    .andExpect(content().string(org.hamcrest.Matchers.containsString("Bài học nguồn")))
    .andExpect(content().string(org.hamcrest.Matchers.containsString("/student/lessons/"+lessonId)))
    .andExpect(content().string(org.hamcrest.Matchers.containsString("data-skill=\"Grammar &amp; Vocabulary\"")));
  mvc.perform(get("/student/learning-path/Grammar & Vocabulary").with(auth)).andExpect(status().isOk())
    .andExpect(content().string(org.hamcrest.Matchers.containsString("/student/flashcards/"+id)));
  set.getLesson().setStatus("ARCHIVED");lessons.saveAndFlush(set.getLesson());
  assertThat(service.listForStudent(student.getEmail())).noneMatch(s->s.id().equals(id));
  mvc.perform(get("/student/flashcards/"+id).with(auth)).andExpect(status().isNotFound());
 }
 @Test void bundledAudioSupportsPlaybackButRemainsProtected() throws Exception {
  var student=learner("bundled-audio@test.local");
  var lesson=new Lesson();lesson.setTitle("Travel announcement");lesson.setSkill("Listening");lesson.setCefrLevel("A2");lesson.setDisplayOrder(4001);lesson.setStatus("PUBLISHED");
  lesson.setAudioPath("curriculum/audio/listening-a2.wav");lesson.setAudioName("Travel announcement.wav");lessons.saveAndFlush(lesson);
  String url="/lesson-media/"+lesson.getId()+"/audio";
  mvc.perform(get(url)).andExpect(status().is3xxRedirection());
  mvc.perform(get(url).header("Range","bytes=0-43").with(user(student.getEmail()).roles("STUDENT")))
    .andExpect(status().isPartialContent()).andExpect(content().contentType("audio/wav"))
    .andExpect(content().bytes(new org.springframework.core.io.ClassPathResource(lesson.getAudioPath()).getInputStream().readNBytes(44)));
  lesson.setStatus("ARCHIVED");lessons.saveAndFlush(lesson);
  mvc.perform(get(url).with(user(student.getEmail()).roles("STUDENT"))).andExpect(status().isNotFound());
 }
 private User learner(String email){var u=new User();u.setFullName("Learner");u.setEmail(email);u.setPasswordHash("unused");u.setRole(Role.STUDENT);u.setStatus(UserStatus.ACTIVE);return users.saveAndFlush(u);}
 private Integer create(){var lesson=new Lesson();lesson.setTitle("Daily routines lesson "+System.nanoTime());lesson.setSkill("Grammar & Vocabulary");lesson.setCefrLevel("A1");lesson.setDisplayOrder((int)(System.nanoTime()%1000000000));lesson.setStatus("PUBLISHED");lesson=lessons.saveAndFlush(lesson);var f=new FlashcardSetForm();f.setTitle("Daily routines");f.setCefrLevel("A1");f.setStatus("PUBLISHED");f.setLessonId(lesson.getId());var row=new FlashcardSetForm.Row();row.setTerm("get up");row.setDefinition("thức dậy");f.getRows().add(row);return service.saveAdmin(null,f);}
 @Test void remembersPerLearnerAndPreservesRemovedCardHistory(){var a=learner("cards-a@test.local");var b=learner("cards-b@test.local");var id=create();var card=service.study(id,a.getEmail()).getFirst();service.mark(id,card.id(),"MASTERED",a.getEmail());service.mark(id,card.id(),"MASTERED",a.getEmail());assertThat(service.study(id,a.getEmail()).getFirst().state()).isEqualTo("MASTERED");assertThat(service.study(id,b.getEmail()).getFirst().state()).isEqualTo("LEARNING");assertThat(progress.findByUserIdAndFlashcardId(a.getId(),card.id()).orElseThrow().getReviewCount()).isEqualTo(1);var f=service.form(id);f.getRows().clear();var row=new FlashcardSetForm.Row();row.setTerm("breakfast");row.setDefinition("bữa sáng");f.getRows().add(row);service.saveAdmin(id,f);assertThat(service.study(id,a.getEmail())).hasSize(1);assertThat(progress.findByUserIdAndFlashcardId(a.getId(),card.id())).isPresent();}
 @Test void pagesRenderAndWritesRequireCsrfAndRole() throws Exception {var a=learner("cards-web@test.local");var id=create();var card=service.study(id,a.getEmail()).getFirst();mvc.perform(get("/admin/flashcards").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());mvc.perform(get("/admin/flashcards/"+id+"/edit").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());mvc.perform(get("/student/flashcards").with(user(a.getEmail()).roles("STUDENT"))).andExpect(status().isOk());mvc.perform(get("/student/flashcards/"+id).with(user(a.getEmail()).roles("STUDENT"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Ôn lại từ chưa thuộc"))).andExpect(content().string(org.hamcrest.Matchers.containsString("Học lại tất cả"))).andExpect(content().string(org.hamcrest.Matchers.containsString("Những từ cần học lại")));String url="/student/flashcards/"+id+"/cards/"+card.id()+"/state";mvc.perform(post(url).param("state","MASTERED").with(user(a.getEmail()).roles("STUDENT"))).andExpect(status().isForbidden());mvc.perform(post(url).param("state","MASTERED").with(csrf()).with(user(a.getEmail()).roles("STUDENT"))).andExpect(status().isNoContent());mvc.perform(get("/admin/flashcards/create").with(user(a.getEmail()).roles("STUDENT"))).andExpect(status().isForbidden());}
 @Test void learnerCanArchiveOnlyOwnSet(){var a=learner("cards-owner@test.local");var b=learner("cards-other@test.local");var f=new FlashcardSetForm();f.setTitle("My words");f.setCefrLevel("A1");var row=new FlashcardSetForm.Row();row.setTerm("focus");row.setDefinition("tập trung");f.getRows().add(row);var id=service.savePersonal(null,f,a.getEmail());assertThat(service.listForStudent(a.getEmail())).anyMatch(s->s.id().equals(id));assertThatThrownBy(()->service.archivePersonal(id,b.getEmail())).isInstanceOf(com.aptisprep.exception.ResourceNotFoundException.class);service.archivePersonal(id,a.getEmail());assertThat(service.listForStudent(a.getEmail())).noneMatch(s->s.id().equals(id));}
}
