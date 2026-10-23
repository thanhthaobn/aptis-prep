package com.aptisprep;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.aptisprep.dto.form.*;
import com.aptisprep.entity.*;
import com.aptisprep.enums.*;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.repository.*;
import com.aptisprep.service.account.AccountProfileService;
import com.aptisprep.service.admin.CatalogService;
import com.aptisprep.service.contract.ScoringSchemeUsageContract;
import com.aptisprep.service.exam.ExamAttemptService;
import com.aptisprep.service.learning.FlashcardSetService;
import com.aptisprep.service.scoring.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class V2BusinessRegressionTests {
    @Autowired CatalogService catalog;
    @Autowired ScoringAdminService scoring;
    @Autowired ScoringSchemeUsageContract usage;
    @Autowired CertificateSchemeService available;
    @Autowired AccountProfileService accounts;
    @Autowired ExamAttemptService engine;
    @Autowired FlashcardSetService flashcards;
    @Autowired UserRepository users;
    @Autowired CertificateRepository certificates;
    @Autowired ScoringSchemeRepository schemes;
    @Autowired ScoringScaleRuleRepository rules;
    @Autowired ScoreBandRepository bands;
    @Autowired MockExamRepository exams;
    @Autowired MockExamQuestionRepository links;
    @Autowired MockExamSectionRepository sections;
    @Autowired UserAnswerRepository answers;
    @Autowired QuestionRepository questions;
    @Autowired QuestionItemRepository questionItems;
    @Autowired QuestionOptionRepository questionOptions;
    @Autowired UserFlashcardProgressRepository progress;
    @Autowired MockMvc mvc;
    User admin, learner;
    Certificate certificate;
    ScoringScheme scheme;

    @BeforeEach void setup() {
        admin = createUser(Role.ADMIN); learner = createUser(Role.STUDENT);
        certificate = new Certificate(); certificate.setCode("V2-"+System.nanoTime()); certificate.setName("V2 certificate"); certificate.setStatus(RecordStatus.ACTIVE); certificate=certificates.saveAndFlush(certificate);
        scheme = new ScoringScheme(); scheme.setCertificate(certificate); scheme.setName("V2 policy"); scheme.setVersion("v2-test"); scheme.setStatus(RecordStatus.ACTIVE); scheme=schemes.saveAndFlush(scheme);
    }
    private User createUser(Role role) {
        var value = new User(); value.setFullName("V2 "+role); value.setEmail("v2-"+UUID.randomUUID()+"@test.local"); value.setPasswordHash("unused"); value.setRole(role); value.setStatus(UserStatus.ACTIVE); return users.saveAndFlush(value);
    }
    private QuestionForm question(String type,String skill) {
        var form=new QuestionForm();form.setContent("V2 question");form.setSkill(skill);form.setCefrLevel("A1");form.setQuestionType(type);form.setStatus("DRAFT");
        var item=new QuestionForm.ItemForm();item.setContent("First item");item.setCorrectPosition(1);
        var a=new QuestionForm.OptionForm("A");a.setContent("A");var b=new QuestionForm.OptionForm("B");b.setContent("B");item.setOptions(new ArrayList<>(List.of(a,b)));form.setItems(new ArrayList<>(List.of(item)));return form;
    }
    private MockExamEntity exam(Integer schemeId,String skill,String type) {
        var exam=new MockExamEntity();exam.setCertificateId(certificate.getId());exam.setScoringSchemeId(schemeId);exam.setTitle("V2 exam");exam.setExamType(type);exam.setSkill(skill);exam.setTargetLevel("A1");exam.setDurationMinutes(30);exam.setStatus("PUBLISHED");return exams.saveAndFlush(exam);
    }
    private void link(MockExamEntity exam,Integer questionId) {
        var link=new MockExamQuestionEntity();link.setMockExamId(exam.getId());link.setQuestionId(questionId);link.setPosition(1);links.saveAndFlush(link);
    }
    @Test void realUsageBeanProtectsExistingPolicyAndCopiesConfigurationToANewVersion() {
        var rule=rule(); scoring.saveScaleRule(admin.getEmail(),scheme.getId(),"READING",rule);
        var band=new ScoreBandForm();band.setSkill("READING");band.setCefrLevel("A1");band.setMinScore(BigDecimal.ZERO);band.setMaxScore(BigDecimal.valueOf(50));scoring.saveBand(admin.getEmail(),scheme.getId(),null,band);
        exam(scheme.getId(),"READING","PRACTICE"); assertTrue(usage.isUsed(scheme.getId()));
        assertThrows(BusinessRuleException.class,()->scoring.saveScaleRule(admin.getEmail(),scheme.getId(),"READING",rule));
        assertThrows(BusinessRuleException.class,()->scoring.deleteBand(admin.getEmail(),scheme.getId(),bands.findBySchemeIdOrderBySkillAscMinScoreAsc(scheme.getId()).getFirst().getId()));
        var form=new ScoringSchemeForm();form.setCertificateId(certificate.getId());form.setName("V2 new policy");form.setStatus("ACTIVE");form.setVersion("v3-test");
        int replacement=scoring.saveScheme(admin.getEmail(),scheme.getId(),form);
        assertNotEquals(scheme.getId(),replacement); assertTrue(rules.findBySchemeIdAndSkillIgnoreCase(replacement,"READING").isPresent());
        assertEquals(1,bands.findBySchemeIdOrderBySkillAscMinScoreAsc(replacement).size());
    }
    private ScoringScaleRuleForm rule() {var f=new ScoringScaleRuleForm();f.setRawMin(BigDecimal.ZERO);f.setRawMax(BigDecimal.valueOf(40));f.setScaleMin(BigDecimal.ZERO);f.setScaleMax(BigDecimal.valueOf(50));return f;}
    @Test void bandsWithoutRawPolicyCannotBeUsedThenConfiguredPolicyBecomesAvailable() {
        var band=new ScoreBand();band.setScheme(scheme);band.setSkill("READING");band.setCefrLevel("A1");band.setMinScore(BigDecimal.ZERO);band.setMaxScore(BigDecimal.valueOf(50));bands.saveAndFlush(band);
        assertThrows(BusinessRuleException.class,()->available.requireAvailableScheme(certificate.getId(),scheme.getId(),"READING"));
        scoring.saveScaleRule(admin.getEmail(),scheme.getId(),"READING",rule());
        assertEquals(scheme.getId(),available.requireAvailableScheme(certificate.getId(),scheme.getId(),"READING").id());
    }
    @Test void orderingUsesItemsWithoutOptionsAndRejectsDuplicateOrMissingPositions() {
        var form=question("ORDERING","READING");form.getItems().getFirst().setOptions(new ArrayList<>());
        var second=new QuestionForm.ItemForm();second.setContent("Second item");second.setCorrectPosition(1);second.setOptions(new ArrayList<>());form.getItems().add(second);
        assertThrows(BusinessRuleException.class,()->catalog.saveQuestion(null,form,null));
        second.setCorrectPosition(2);int id=catalog.saveQuestion(null,form,null);catalog.publishQuestion(id);assertEquals("PUBLISHED",questions.findById(id).orElseThrow().getStatus());
    }
    @Test void skillAndQuestionTypeMustMatchAtTheBackend() {
        assertThrows(BusinessRuleException.class,()->catalog.saveQuestion(null,question("SINGLE_CHOICE","WRITING"),null));
        assertThrows(BusinessRuleException.class,()->catalog.saveQuestion(null,question("SPEAKING_AUDIO","READING"),null));
    }
    @Test void questionDifficultyAndPublishedStatusArePersistedFromTheAdminForm() {
        var form = question("SINGLE_CHOICE", "READING");
        form.setDifficulty("HARD");
        form.setStatus("PUBLISHED");
        int id = catalog.saveQuestion(null, form, null);
        var saved = questions.findById(id).orElseThrow();
        assertEquals("HARD", saved.getDifficulty());
        assertEquals("PUBLISHED", saved.getStatus());
        form.setDifficulty("IMPOSSIBLE");
        assertThrows(BusinessRuleException.class, () -> catalog.saveQuestion(null, form, null));
    }
    @Test void questionEditorClearsFieldsThatDoNotBelongToTheSelectedType() {
        var form = question("WRITING_TEXT", "WRITING");
        form.setReadingPassage("This passage does not belong to Writing");
        form.setPreparationSeconds(30);
        form.getItems().getFirst().setMinWordCount(20);
        form.getItems().getFirst().setMaxWordCount(80);
        int id = catalog.saveQuestion(null, form, null);
        var item = questionItems.findByQuestionIdOrderByPosition(id).getFirst();
        assertNull(item.getCorrectPosition());
        assertEquals(20, item.getMinWordCount());
        assertEquals(80, item.getMaxWordCount());
        assertTrue(questionOptions.findByQuestionItemIdOrderByPosition(item.getId()).isEmpty());
        assertNull(questions.findById(id).orElseThrow().getReadingPassage());
        assertNull(questions.findById(id).orElseThrow().getPreparationSeconds());
    }
    @Test void writingKeepsAllItemsInCanonicalAnswerAndDoesNotEraseEarlierItems() {
        var form=question("WRITING_TEXT","WRITING");var second=new QuestionForm.ItemForm();second.setContent("Second");form.getItems().add(second);
        int questionId=catalog.saveQuestion(null,form,null);catalog.publishQuestion(questionId);var exam=exam(null,"WRITING","PRACTICE");link(exam,questionId);
        int attempt=engine.start(learner.getId(),exam.getId(),"v2-writing").attemptId();var page=engine.page(learner.getId(),attempt);
        var items=page.questions().getFirst().items();engine.saveAnswer(learner.getId(),attempt,questionId,items.get(0).itemId(),null,null,"First response");
        engine.saveAnswer(learner.getId(),attempt,questionId,items.get(1).itemId(),null,null,"Second response");
        assertEquals("First response\n--- ITEM ---\nSecond response",answers.findByAttemptIdAndQuestionId(attempt,questionId).orElseThrow().getTextAnswer());
    }
    @Test void unschemedPracticeCompletesWithRawScoreWithoutInventingCefr() {
        int id=catalog.saveQuestion(null,question("SINGLE_CHOICE","READING"),null);catalog.publishQuestion(id);var exam=exam(null,"READING","PRACTICE");link(exam,id);
        int attempt=engine.start(learner.getId(),exam.getId(),"v2-raw").attemptId();engine.submit(learner.getId(),attempt,"submit");
        var result=engine.result(learner.getId(),attempt);assertEquals("COMPLETED",result.state());assertNull(result.skills().getFirst().estimatedLevel());assertEquals(0,result.skills().getFirst().rawScore().intValue());
    }
    @Test void goalsRoundTripTargetScoresAndRejectScoresWithoutATargetLevel() {
        var form=new GoalForm();form.setReading("B1");form.getTargetScores().put("READING",BigDecimal.valueOf(35));accounts.saveGoal(learner.getEmail(),form);
        assertEquals(0,BigDecimal.valueOf(35).compareTo(accounts.formFrom(accounts.snapshot(learner.getEmail())).getTargetScores().get("READING")));
        form.getTargetScores().put("WRITING",BigDecimal.valueOf(40));assertThrows(BusinessRuleException.class,()->accounts.saveGoal(learner.getEmail(),form));
    }
    @Test void repeatedStatusIsANewReviewButReplayedKeyIsNot() {
        var form=new FlashcardSetForm();form.setTitle("V2 private cards");form.setCefrLevel("A1");var row=new FlashcardSetForm.Row();row.setTerm("test");row.setDefinition("kiểm tra");form.getRows().add(row);
        int set=flashcards.savePersonal(null,form,learner.getEmail()),card=flashcards.study(set,learner.getEmail()).getFirst().id();
        flashcards.mark(set,card,"MASTERED",learner.getEmail(),"review-action-0001");flashcards.mark(set,card,"MASTERED",learner.getEmail(),"review-action-0002");flashcards.mark(set,card,"MASTERED",learner.getEmail(),"review-action-0001");
        var state=progress.findByUserIdAndFlashcardId(learner.getId(),card).orElseThrow();assertEquals(2,state.getReviewCount());assertTrue(state.getIntervalDays()>0);assertNotNull(state.getNextReviewAt());
    }
    @Test void dashboardAndGoalsRenderWithoutAFabricatedTarget() throws Exception {
        mvc.perform(get("/student/dashboard").with(user(learner.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa đặt mục tiêu")));
        mvc.perform(get("/student/goals").with(user(learner.getEmail()).roles("STUDENT")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("targetScores[READING]")));
    }

    @Test void fullMockRejectsMissingSkillsAndAcceptsAllFiveSkills() {
        var form=new ExamForm();form.setTitle("Complete full mock");form.setExamType("FULL_MOCK");form.setCertificateId(certificate.getId());
        for (String skill:List.of("READING","LISTENING")) {
            var section=new ExamForm.SectionForm();section.setSkill(skill);section.setDurationMinutes(20);form.getSections().add(section);
        }
        assertThrows(BusinessRuleException.class,()->catalog.saveExam(null,form));
        for (String skill:List.of("GRAMMAR_VOCABULARY","WRITING","SPEAKING")) {
            var section=new ExamForm.SectionForm();section.setSkill(skill);section.setDurationMinutes(20);form.getSections().add(section);
        }
        int id=catalog.saveExam(null,form);assertEquals(5,sections.findByMockExamIdOrderById(id).size());
        assertThrows(BusinessRuleException.class,()->catalog.publishExam(id)); // Empty drafts are not publishable.
    }

    @Test void singleSkillExamCanBePublishedWithoutASchemeTargetOrSection() {
        int questionId=catalog.saveQuestion(null,question("SINGLE_CHOICE","READING"),null);catalog.publishQuestion(questionId);
        var form=new ExamForm();form.setTitle("Sectionless practice");form.setCertificateId(certificate.getId());form.setTargetLevel(null);form.setQuestionIds(String.valueOf(questionId));
        int id=catalog.saveExam(null,form);catalog.publishExam(id);
        assertTrue(sections.findByMockExamIdOrderById(id).isEmpty());
        assertNull(links.findByMockExamIdOrderByPosition(id).getFirst().getMockExamSectionId());
        assertNull(exams.findById(id).orElseThrow().getTargetLevel());
    }

    @Test void rawScaleConfigurationRendersSavesAndRejectsInvalidRanges() throws Exception {
        mvc.perform(get("/admin/scoring-schemes/"+scheme.getId()+"/edit").param("skill","READING").with(user(admin.getEmail()).roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("scale-rule")));
        mvc.perform(post("/admin/scoring-schemes/"+scheme.getId()+"/scale-rule").with(csrf()).with(user(admin.getEmail()).roles("ADMIN"))
                .param("skill","READING").param("rawMin","0").param("rawMax","40").param("scaleMin","0").param("scaleMax","50"))
                .andExpect(status().is3xxRedirection());
        assertTrue(rules.findBySchemeIdAndSkillIgnoreCase(scheme.getId(),"READING").isPresent());
        var invalid=rule();invalid.setRawMax(BigDecimal.ZERO);assertThrows(BusinessRuleException.class,()->scoring.saveScaleRule(admin.getEmail(),scheme.getId(),"READING",invalid));
        var outOfRange=rule();outOfRange.setScaleMax(BigDecimal.valueOf(51));
        assertThrows(BusinessRuleException.class,()->scoring.saveScaleRule(admin.getEmail(),scheme.getId(),"READING",outOfRange));
    }

    @Test void goalFormBindsAnOptionalTargetScore() throws Exception {
        mvc.perform(post("/student/goals").with(csrf()).with(user(learner.getEmail()).roles("STUDENT"))
                .param("reading","B1").param("targetScores[READING]","35.5"))
                .andExpect(status().is3xxRedirection());
        assertEquals(0,new BigDecimal("35.5").compareTo(accounts.snapshot(learner.getEmail()).goal().targetScores().get("READING")));
    }

    @Test void bandsCannotExceedTheConfiguredScale() {
        var narrow=rule();narrow.setScaleMax(BigDecimal.valueOf(40));scoring.saveScaleRule(admin.getEmail(),scheme.getId(),"READING",narrow);
        var band=new ScoreBandForm();band.setSkill("READING");band.setCefrLevel("A1");band.setMinScore(BigDecimal.ZERO);band.setMaxScore(BigDecimal.valueOf(50));
        assertThrows(BusinessRuleException.class,()->scoring.saveBand(admin.getEmail(),scheme.getId(),null,band));
    }
}
