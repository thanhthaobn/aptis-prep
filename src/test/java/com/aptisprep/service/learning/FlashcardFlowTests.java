package com.aptisprep.service.learning;
import com.aptisprep.dto.form.FlashcardSetForm;
import com.aptisprep.entity.User;
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
 @Autowired FlashcardSetService service; @Autowired UserRepository users;
 @Autowired UserFlashcardProgressRepository progress; @Autowired MockMvc mvc;
 private User learner(String email){var u=new User();u.setFullName("Learner");u.setEmail(email);u.setPasswordHash("unused");u.setRole(Role.STUDENT);u.setStatus(UserStatus.ACTIVE);return users.saveAndFlush(u);}
 private Integer create(){var f=new FlashcardSetForm();f.setTitle("Daily routines");f.setCefrLevel("A1");f.setStatus("PUBLISHED");var row=new FlashcardSetForm.Row();row.setTerm("get up");row.setDefinition("thức dậy");f.getRows().add(row);return service.save(null,f);}
 @Test void remembersPerLearnerAndPreservesRemovedCardHistory(){var a=learner("cards-a@test.local");var b=learner("cards-b@test.local");var id=create();var card=service.study(id,a.getEmail()).getFirst();service.mark(id,card.id(),"MASTERED",a.getEmail());service.mark(id,card.id(),"MASTERED",a.getEmail());assertThat(service.study(id,a.getEmail()).getFirst().state()).isEqualTo("MASTERED");assertThat(service.study(id,b.getEmail()).getFirst().state()).isEqualTo("LEARNING");assertThat(progress.findByUserIdAndFlashcardId(a.getId(),card.id()).orElseThrow().getReviewCount()).isEqualTo(1);var f=service.form(id);f.getRows().clear();var row=new FlashcardSetForm.Row();row.setTerm("breakfast");row.setDefinition("bữa sáng");f.getRows().add(row);service.save(id,f);assertThat(service.study(id,a.getEmail())).hasSize(1);assertThat(progress.findByUserIdAndFlashcardId(a.getId(),card.id())).isPresent();}
 @Test void pagesRenderAndWritesRequireCsrfAndRole() throws Exception {var a=learner("cards-web@test.local");var id=create();var card=service.study(id,a.getEmail()).getFirst();mvc.perform(get("/admin/flashcards").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());mvc.perform(get("/admin/flashcards/"+id+"/edit").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());mvc.perform(get("/student/flashcards").with(user(a.getEmail()).roles("STUDENT"))).andExpect(status().isOk());mvc.perform(get("/student/flashcards/"+id).with(user(a.getEmail()).roles("STUDENT"))).andExpect(status().isOk());String url="/student/flashcards/"+id+"/cards/"+card.id()+"/state";mvc.perform(post(url).param("state","MASTERED").with(user(a.getEmail()).roles("STUDENT"))).andExpect(status().isForbidden());mvc.perform(post(url).param("state","MASTERED").with(csrf()).with(user(a.getEmail()).roles("STUDENT"))).andExpect(status().isNoContent());mvc.perform(get("/admin/flashcards/create").with(user(a.getEmail()).roles("STUDENT"))).andExpect(status().isForbidden());}
 @Test void learnerCanArchiveOnlyOwnSet(){var a=learner("cards-owner@test.local");var b=learner("cards-other@test.local");var f=new FlashcardSetForm();f.setTitle("My words");f.setCefrLevel("A1");var row=new FlashcardSetForm.Row();row.setTerm("focus");row.setDefinition("tập trung");f.getRows().add(row);var id=service.savePersonal(null,f,a.getEmail());assertThat(service.listForStudent(a.getEmail())).anyMatch(s->s.id().equals(id));assertThatThrownBy(()->service.archivePersonal(id,b.getEmail())).isInstanceOf(com.aptisprep.exception.ResourceNotFoundException.class);service.archivePersonal(id,a.getEmail());assertThat(service.listForStudent(a.getEmail())).noneMatch(s->s.id().equals(id));}
}
