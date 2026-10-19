package com.aptisprep.service.learning;
import com.aptisprep.dto.form.LessonForm;
import com.aptisprep.entity.*;
import com.aptisprep.enums.*;
import com.aptisprep.repository.*;
import com.aptisprep.exception.BusinessRuleException;
import java.util.Map;
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
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,TransactionalTestExecutionListener.class})
class StructuredLessonTests {
 @Autowired LessonService lessons; @Autowired LessonTeachingService teaching; @Autowired UserRepository users; @Autowired MockMvc mvc;
 private User learner(String email){var u=new User();u.setEmail(email);u.setFullName("Learner");u.setRole(Role.STUDENT);u.setStatus(UserStatus.ACTIVE);u.setPasswordHash("unused");return users.saveAndFlush(u);}
 private Lesson create(String skill){var f=lessons.newForm(skill,"A1");f.setTitle(skill+" lesson");f.setTopic("Everyday life");f.setStatus("PUBLISHED");f.setObjectives("Learn strategies");f.setStrategy("Step one");f.setExampleText("Example");f.setAnalysis("Analysis");f.setPracticePrompt("Try writing");f.setSampleResponse("Model");f.setVideoUrl("https://youtu.be/yiwhUC4S5_o");f.setSourceUrl("https://www.britishcouncil.org/");var q=new LessonForm.CheckForm();q.setPrompt("Choose the correct strategy");q.setOptionA("First");q.setOptionB("Second");q.setOptionC("Third");q.setCorrectOption(1);q.setExplanation("PRIVATE_EXPLANATION");f.getChecks().add(q);return lessons.save(null,f);}
 @Test void allSkillViewsAndEditorRenderWithoutDisclosingKeys() throws Exception {var u=learner("structured@test.local");for(String skill:LessonService.SKILLS){var l=create(skill);mvc.perform(get("/student/lessons/"+l.getId()).with(user(u.getEmail()).roles("STUDENT"))).andExpect(status().isOk()).andExpect(content().string(containsString("youtube-nocookie.com/embed/yiwhUC4S5_o"))).andExpect(content().string(not(containsString("PRIVATE_EXPLANATION"))));mvc.perform(get("/admin/lessons/"+l.getId()+"/edit").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());mvc.perform(get("/student/learning-path/"+skill).with(user(u.getEmail()).roles("STUDENT"))).andExpect(status().isOk()).andExpect(content().string(containsString("Everyday life")));}mvc.perform(get("/admin/lessons").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());}
 @Test void answersAreGradedOnServerAndDraftsBelongToTheirLearner(){var a=learner("author-a@test.local");var b=learner("author-b@test.local");var l=create("Reading");var q=teaching.questions(l.getId()).getFirst();assertThat(teaching.completion(l.getId(),a.getEmail()).eligible()).isFalse();assertThat(teaching.grade(l.getId(),Map.of("q"+q.id(),"1"),a.getEmail()).score()).isEqualTo(1);assertThat(teaching.grade(l.getId(),Map.of("q"+q.id(),"0"),a.getEmail()).score()).isZero();assertThat(teaching.history(l.getId(),a.getEmail())).hasSize(2);assertThat(teaching.completion(l.getId(),a.getEmail()).eligible()).isTrue();assertThatThrownBy(()->teaching.grade(l.getId(),Map.of(),a.getEmail())).isInstanceOf(BusinessRuleException.class);teaching.saveResponse(l.getId(),"My work",a.getEmail());assertThat(teaching.response(l.getId(),a.getEmail())).isEqualTo("My work");assertThat(teaching.response(l.getId(),b.getEmail())).isEmpty();}
 @Test void rejectsUnsafeVideoAndSources(){assertThatThrownBy(()->LessonTeachingService.videoId("javascript:alert(1)")).isInstanceOf(BusinessRuleException.class);assertThatThrownBy(()->LessonTeachingService.videoId("https://youtube.com.evil.test/watch?v=yiwhUC4S5_o")).isInstanceOf(BusinessRuleException.class);assertThat(LessonTeachingService.videoId("https://www.youtube.com/watch?v=yiwhUC4S5_o&feature=share")).isEqualTo("yiwhUC4S5_o");}
 @Test void knowledgeChecksMayBeAbsentAndMayHaveTwoOrSixOptions(){var empty=lessons.newForm("Reading","A2");empty.setTitle("No check");empty.setStatus("DRAFT");var noCheck=lessons.save(null,empty);assertThat(teaching.questions(noCheck.getId())).isEmpty();var f=lessons.newForm("Listening","A2");f.setTitle("Flexible checks");f.setStatus("DRAFT");var two=new LessonForm.CheckForm();two.setPrompt("Two choices");two.setOptionA("Yes");two.setOptionB("No");two.setCorrectOption(1);two.setExplanation("No is correct");var six=new LessonForm.CheckForm();six.setPrompt("Six choices");six.setOptionA("A");six.setOptionB("B");six.setOptionC("C");six.setOptionD("D");six.setOptionE("E");six.setOptionF("F");six.setCorrectOption(5);six.setExplanation("F is correct");f.getChecks().add(two);f.getChecks().add(six);var lesson=lessons.save(null,f);assertThat(teaching.questions(lesson.getId())).extracting(q->q.options().size()).containsExactly(2,6);}
}
