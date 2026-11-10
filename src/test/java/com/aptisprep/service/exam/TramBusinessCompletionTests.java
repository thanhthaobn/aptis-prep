package com.aptisprep.service.exam;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aptisprep.entity.*;
import com.aptisprep.mvc.auth.entity.User;
import com.aptisprep.mvc.auth.repository.UserRepository;
import com.aptisprep.mvc.catalog.entity.*;
import com.aptisprep.mvc.catalog.repository.*;
import com.aptisprep.mvc.catalog.service.*;
import com.aptisprep.mvc.catalog.service.CatalogService;
import com.aptisprep.mvc.catalog.service.impl.ExamCatalogServiceImpl;
import com.aptisprep.mvc.common.config.AiProperties;
import com.aptisprep.mvc.common.enums.*;
import com.aptisprep.mvc.common.event.AttemptSubmittedEvent;
import com.aptisprep.mvc.common.exception.BusinessRuleException;
import com.aptisprep.mvc.common.exception.ResourceNotFoundException;
import com.aptisprep.mvc.scoring.entity.*;
import com.aptisprep.mvc.scoring.repository.*;
import com.aptisprep.repository.*;
import com.aptisprep.service.contract.*;
import com.aptisprep.service.contract.IntegrationModels.LockedAttempt;
import com.aptisprep.service.grading.*;
import com.aptisprep.service.placement.PlacementExamCatalogService;
import com.aptisprep.service.student.StudentExamCatalogService;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@RecordApplicationEvents
class TramBusinessCompletionTests {
    @Autowired ExamAttemptService engine;
    @Autowired ExamGradingPayloadFactory gradingPayloads;
    @Autowired UserRepository users;
    @Autowired CertificateRepository certificates;
    @Autowired MockExamRepository exams;
    @Autowired MockExamQuestionRepository links;
    @Autowired QuestionRepository questions;
    @Autowired QuestionItemRepository items;
    @Autowired QuestionOptionRepository options;
    @Autowired ExamAttemptRepository attempts;
    @Autowired AttemptOutboxRepository outbox;
    @Autowired AttemptOutboxService delivery;
    @Autowired ApplicationEvents events;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired UserAnswerRepository answers;
    @Autowired StudentExamCatalogService library;
    @Autowired PlacementExamCatalogService placement;
    @Autowired ExamCatalogServiceImpl published;
    @Autowired CatalogService admin;
    @Autowired ScoringSchemeRepository schemes;
    @Autowired ScoringScaleRuleRepository scaleRules;
    @Autowired ScoreBandRepository bands;
    private final JsonMapper json = JsonMapper.builder().build();

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void snapshotThroughOutboxToAiRetainsOldPromptsAndMatchingAnswers(boolean legacyJson) {
        var f = fixture(2);
        int id = start(f);
        save(f, id, 0, "Answer to first prompt");
        save(f, id, 1, "Answer to second prompt");
        f.question().setInstruction("ADMIN CHANGED INSTRUCTION");
        questions.saveAndFlush(f.question());
        for (var item : f.items()) {
            item.setContent("ADMIN CHANGED ITEM");
            item.setMinWordCount(999);
            items.saveAndFlush(item);
        }
        engine.submit(f.user().getId(), id, "submit");
        var row = outbox.findByEventKey("attempt:" + id + ":submitted").orElseThrow();
        assertEquals("DISPATCHED", row.getStatus());
        if (legacyJson) {
            var original = events.stream(AttemptSubmittedEvent.class).findFirst().orElseThrow();
            row.setPayload(json.writeValueAsString(original));
        }
        events.clear();
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        status -> {
                            row.setStatus("PROCESSING");
                            row.setNextAttemptAt(LocalDateTime.now().minusSeconds(1));
                            outbox.saveAndFlush(row);
                            delivery.recoverPending();
                        });
        var replayed =
                events.stream(AttemptSubmittedEvent.class)
                        .filter(event -> event.attemptId().equals(id))
                        .findFirst()
                        .orElseThrow();
        assertEquals("DISPATCHED", outbox.findById(row.getId()).orElseThrow().getStatus());
        var ai = mock(AiAssessmentContract.class);
        var finalization = mock(AttemptFinalizationService.class);
        when(finalization.accepts(any())).thenReturn(true);
        var config = new AiProperties();
        config.setEnabled(true);
        var orchestration =
                new AttemptGradingOrchestrationService(
                        config, ai, finalization, mock(AttemptOutboxService.class));
        orchestration.onSubmitted(replayed);
        var request = ArgumentCaptor.forClass(AiAssessmentContract.AssessmentRequest.class);
        verify(ai).request(request.capture());
        assertEquals(
                "Original instructions\nOriginal question\n\nYÊU CẦU THEO TỪNG MỤC:"
                        + "\n\nMục 1:\nOriginal prompt 1\nGiới hạn từ: tối thiểu 5 tối đa 5000 từ."
                        + "\n\nMục 2:\nOriginal prompt 2\nGiới hạn từ: tối thiểu 5 tối đa 5000 từ.",
                request.getValue().taskContent());
        assertEquals(
                "Mục 1:\nAnswer to first prompt\n\nMục 2:\nAnswer to second prompt",
                request.getValue().lockedContent());
        assertFalse(request.getValue().taskContent().contains("ADMIN CHANGED"));
    }

    @Test
    void actualLearnerPageDoesNotSerializeInternalAnswerKeys() {
        var f = fixture(2);
        var option = new QuestionOptionEntity();
        option.setQuestionItemId(f.items().getFirst().getId());
        option.setContent("Visible option text");
        option.setOptionCode("A");
        option.setPosition(1);
        option.setCorrect(true);
        options.saveAndFlush(option);
        String page = json.writeValueAsString(engine.page(f.user().getId(), start(f)));
        for (String secret :
                List.of(
                        "\"correct\"",
                        "\"correctPosition\"",
                        "\"correctOptionId\"",
                        "\"gradingContext\"",
                        "\"lockedQuestionSnapshot\"")) {
            assertFalse(page.contains(secret), secret);
        }
    }

    @Test
    void twoElevenThousandCharacterItemsSubmitAndUpdatingSecondKeepsFirst() {
        var f = fixture(2);
        int id = start(f);
        save(f, id, 0, "a".repeat(11_000));
        save(f, id, 1, "b".repeat(11_000));
        save(f, id, 1, "c".repeat(11_000));
        assertEquals(
                "a".repeat(11_000) + "\n--- ITEM ---\n" + "c".repeat(11_000),
                answers.findByAttemptIdAndQuestionId(id, f.question().getId())
                        .orElseThrow()
                        .getTextAnswer());
        assertDoesNotThrow(() -> engine.submit(f.user().getId(), id, "submit"));
        assertNotNull(attempts.findById(id).orElseThrow().getSubmittedAt());
    }

    @Test
    void combinedLimitIncludesLabelsAndRejectionLeavesAttemptEditable() {
        var f = fixture(3);
        int id = start(f);
        save(f, id, 0, "a".repeat(20_000));
        save(f, id, 1, "b".repeat(20_000));
        save(f, id, 2, "c");
        LockedAttempt draft =
                new TransactionTemplate(transactionManager)
                        .execute(
                                status ->
                                        gradingPayloads.build(attempts.findById(id).orElseThrow()));
        var answer = draft.answers().getFirst();
        int overhead =
                GradingContextFormatter.task(answer).length()
                        + GradingContextFormatter.submission(answer).length()
                        - 40_001;
        int available = GradingTextLimits.MAX_QUESTION_CHARS - 40_000 - overhead;
        save(f, id, 2, "c".repeat(available + 1));
        assertThrows(
                BusinessRuleException.class, () -> engine.submit(f.user().getId(), id, "too-long"));
        var stillEditable = attempts.findById(id).orElseThrow();
        assertEquals("IN_PROGRESS", stillEditable.getStatus());
        assertNull(stillEditable.getSubmittedAt());
        assertTrue(outbox.findByEventKey("attempt:" + id + ":submitted").isEmpty());
        save(f, id, 2, "c".repeat(available));
        assertDoesNotThrow(() -> engine.submit(f.user().getId(), id, "exact-limit"));
    }

    @Test
    void itemLimitRejectsWithoutErasingSavedText() {
        var f = fixture(2);
        int id = start(f);
        save(f, id, 0, "a".repeat(20_000));
        assertThrows(BusinessRuleException.class, () -> save(f, id, 0, "b".repeat(20_001)));
        assertEquals(
                "a".repeat(20_000),
                engine.page(f.user().getId(), id)
                        .questions()
                        .getFirst()
                        .items()
                        .getFirst()
                        .textAnswer());
        save(f, id, 1, "Second response");
    }

    @Test
    void demoIsHiddenAndCannotStartButExistingAttemptAndAdminHistoryRemain() {
        var f = fixture(2);
        int id = start(f);
        f.exam().setDemoOnly(true);
        exams.saveAndFlush(f.exam());
        assertTrue(
                library.find("exams", "", "", "", "").stream()
                        .noneMatch(x -> Objects.equals(x.id(), f.exam().getId())));
        assertThrows(
                ResourceNotFoundException.class, () -> library.entry("exams", f.exam().getId()));
        assertTrue(
                placement.findPublished("WRITING").stream()
                        .noneMatch(x -> x.id().equals(f.exam().getId())));
        assertThrows(
                ResourceNotFoundException.class,
                () -> published.publishedExam(f.user().getId(), f.exam().getId()));
        assertThrows(
                BusinessRuleException.class,
                () -> engine.start(f.user().getId(), f.exam().getId(), "new-demo-run"));
        assertNotNull(admin.entry("exams", f.exam().getId()));
        assertTrue(admin.examForm(f.exam().getId()).isDemoOnly());
        assertNotNull(engine.page(f.user().getId(), id));
        save(f, id, 0, "Historical saved response");
        assertDoesNotThrow(() -> engine.submit(f.user().getId(), id, "finish-legacy"));
        assertNotNull(engine.result(f.user().getId(), id));
        assertTrue(exams.findById(f.exam().getId()).orElseThrow().isDemoOnly());
    }

    @Test
    void ordinaryPlacementRemainsAvailableAndDemoCheckpointIsNotSelected() {
        var f = fixture(2);
        assertTrue(
                placement.findPublished("WRITING").stream()
                        .anyMatch(x -> x.id().equals(f.exam().getId())));
        assertNotNull(published.publishedExam(f.user().getId(), f.exam().getId()));
        f.exam().setExamType("CHECKPOINT");
        f.exam().setTargetLevel("C2");
        f.exam().setDemoOnly(true);
        exams.saveAndFlush(f.exam());
        assertThrows(ResourceNotFoundException.class, () -> published.checkpoint("WRITING", "C2"));
    }

    @Test
    void realPlacementDoesNotAutomaticallyResumeAnOldDemoAttempt() {
        var f = fixture(2);
        int demoAttempt = start(f);
        f.exam().setDemoOnly(true);
        exams.saveAndFlush(f.exam());
        var real = new MockExamEntity();
        real.setTitle("Real Placement after demo");
        real.setCertificateId(f.exam().getCertificateId());
        real.setScoringSchemeId(f.exam().getScoringSchemeId());
        real.setExamType("PLACEMENT");
        real.setSkill("WRITING");
        real.setStatus("PUBLISHED");
        real.setDurationMinutes(30);
        real = exams.saveAndFlush(real);
        var link = new MockExamQuestionEntity();
        link.setMockExamId(real.getId());
        link.setQuestionId(f.question().getId());
        link.setPosition(1);
        links.saveAndFlush(link);
        int actual =
                engine.startPlacement(f.user().getId(), real.getId(), "real-placement").attemptId();
        assertNotEquals(demoAttempt, actual);
        assertEquals(real.getId(), attempts.findById(actual).orElseThrow().getMockExamId());
        assertEquals(demoAttempt, attempts.findById(demoAttempt).orElseThrow().getId());
    }

    @Test
    void adminDemoFlagRoundTripsEvenAfterRenamingTheExam() {
        var f = fixture(2);
        admin.unpublishExam(f.exam().getId());
        var form = admin.examForm(f.exam().getId());
        form.setDemoOnly(true);
        form.setTitle("Ordinary-looking renamed exam");
        admin.saveExam(f.exam().getId(), form);
        assertTrue(admin.examForm(f.exam().getId()).isDemoOnly());
        assertTrue(exams.findById(f.exam().getId()).orElseThrow().isDemoOnly());
        assertTrue(library.find("exams", "Ordinary-looking renamed", "", "", "").isEmpty());
    }

    private int start(Fixture f) {
        return engine.startPlacement(
                        f.user().getId(), f.exam().getId(), "test:" + UUID.randomUUID())
                .attemptId();
    }

    private void save(Fixture f, int attempt, int item, String text) {
        engine.saveAnswer(
                f.user().getId(),
                attempt,
                f.question().getId(),
                f.items().get(item).getId(),
                null,
                null,
                text);
    }

    private Fixture fixture(int count) {
        var user = new User();
        user.setEmail("tram-completion-" + UUID.randomUUID() + "@test.local");
        user.setFullName("Regression learner");
        user.setPasswordHash("unused");
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        user = users.saveAndFlush(user);
        var certificate = new Certificate();
        certificate.setCode("TRAM-" + UUID.randomUUID());
        certificate.setName("Regression certificate");
        certificate.setStatus(RecordStatus.ACTIVE);
        certificate = certificates.saveAndFlush(certificate);
        var scheme = new ScoringScheme();
        scheme.setCertificate(certificate);
        scheme.setName("Placement regression policy");
        scheme.setVersion("test-v1");
        scheme.setStatus(RecordStatus.ACTIVE);
        scheme = schemes.saveAndFlush(scheme);
        var rule = new ScoringScaleRule();
        rule.setScheme(scheme);
        rule.setSkill("WRITING");
        rule.setRawMin(java.math.BigDecimal.ZERO);
        rule.setRawMax(java.math.BigDecimal.valueOf(50));
        rule.setScaleMin(java.math.BigDecimal.ZERO);
        rule.setScaleMax(java.math.BigDecimal.valueOf(50));
        scaleRules.saveAndFlush(rule);
        var band = new ScoreBand();
        band.setScheme(scheme);
        band.setSkill("WRITING");
        band.setCefrLevel("A1");
        band.setMinScore(java.math.BigDecimal.ZERO);
        band.setMaxScore(java.math.BigDecimal.valueOf(50));
        bands.saveAndFlush(band);
        var question = new QuestionEntity();
        question.setContent("Original question");
        question.setInstruction("Original instructions");
        question.setQuestionType("WRITING_TEXT");
        question.setSkill("WRITING");
        question.setStatus("PUBLISHED");
        question = questions.saveAndFlush(question);
        var itemList = new ArrayList<QuestionItemEntity>();
        for (int n = 1; n <= count; n++) {
            var item = new QuestionItemEntity();
            item.setQuestionId(question.getId());
            item.setContent("Original prompt " + n);
            item.setPosition(n);
            item.setCorrectPosition(2);
            item.setMinWordCount(5);
            item.setMaxWordCount(5000);
            itemList.add(items.saveAndFlush(item));
        }
        var exam = new MockExamEntity();
        exam.setCertificateId(certificate.getId());
        exam.setScoringSchemeId(scheme.getId());
        exam.setTitle("Regression exam");
        exam.setExamType("PLACEMENT");
        exam.setSkill("WRITING");
        exam.setTargetLevel("B1");
        exam.setStatus("PUBLISHED");
        exam.setDurationMinutes(30);
        exam = exams.saveAndFlush(exam);
        var link = new MockExamQuestionEntity();
        link.setMockExamId(exam.getId());
        link.setQuestionId(question.getId());
        link.setPosition(1);
        links.saveAndFlush(link);
        return new Fixture(user, exam, question, itemList);
    }

    private record Fixture(
            User user,
            MockExamEntity exam,
            QuestionEntity question,
            List<QuestionItemEntity> items) {}
}
