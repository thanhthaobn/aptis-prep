package com.aptisprep.service.exam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aptisprep.entity.Certificate;
import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.entity.MockExamQuestionEntity;
import com.aptisprep.entity.QuestionEntity;
import com.aptisprep.entity.QuestionItemEntity;
import com.aptisprep.entity.QuestionMediaEntity;
import com.aptisprep.entity.QuestionOptionEntity;
import com.aptisprep.entity.ScoreBand;
import com.aptisprep.entity.ScoringScheme;
import com.aptisprep.entity.User;
import com.aptisprep.enums.RecordStatus;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.exception.AccessDeniedException;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.repository.AttemptOutboxRepository;
import com.aptisprep.repository.CertificateRepository;
import com.aptisprep.repository.ExamAttemptRepository;
import com.aptisprep.repository.ExamAttemptSnapshotRepository;
import com.aptisprep.repository.MockExamQuestionRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.repository.QuestionItemRepository;
import com.aptisprep.repository.QuestionMediaRepository;
import com.aptisprep.repository.QuestionOptionRepository;
import com.aptisprep.repository.QuestionRepository;
import com.aptisprep.repository.ScoreBandRepository;
import com.aptisprep.repository.ScoringSchemeRepository;
import com.aptisprep.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@SpringBootTest
class ExamAttemptConcurrencyTests {
    @Autowired ExamAttemptService attempts;
    @Autowired UserRepository users;
    @Autowired CertificateRepository certificates;
    @Autowired ScoringSchemeRepository schemes;
    @Autowired MockExamRepository exams;
    @Autowired MockExamQuestionRepository examQuestions;
    @Autowired QuestionRepository questions;
    @Autowired QuestionItemRepository items;
    @Autowired QuestionOptionRepository options;
    @Autowired QuestionMediaRepository media;
    @Autowired ScoreBandRepository bands;
    @Autowired ExamAttemptRepository attemptRepository;
    @Autowired ExamAttemptSnapshotRepository snapshots;
    @Autowired AttemptOutboxRepository outbox;
    @Autowired AttemptOutboxService outboxService;
    @Autowired com.aptisprep.service.learning.LessonService lessonService;
    @Autowired com.aptisprep.service.learning.LearningPathService learningPaths;
    @Autowired com.aptisprep.repository.LearningPathRepository learningPathRepository;
    @Autowired com.aptisprep.repository.UserSkillProfileRepository skillProfiles;

    @Test
    void concurrentStartWithSameRequestKeyCreatesOneAttemptAndSnapshot() throws Exception {
        Fixture fixture = fixture("start");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> results =
                    pool.invokeAll(
                            List.of(
                                    task(
                                            () ->
                                                    attempts.start(
                                                                    fixture.user().getId(),
                                                                    fixture.exam().getId(),
                                                                    "same-key")
                                                            .attemptId()),
                                    task(
                                            () ->
                                                    attempts.start(
                                                                    fixture.user().getId(),
                                                                    fixture.exam().getId(),
                                                                    "same-key")
                                                            .attemptId())));
            assertEquals(results.get(0).get(), results.get(1).get());
            assertEquals(1, attemptRepository.countByUserId(fixture.user().getId()));
            assertEquals(1, snapshots.findByAttemptId(results.get(0).get()).stream().count());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentSubmitPublishesOneDurableCompletedEvent() throws Exception {
        Fixture fixture = fixture("submit");
        int attemptId =
                attempts.start(fixture.user().getId(), fixture.exam().getId(), "submit-key")
                        .attemptId();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Void>> results =
                    pool.invokeAll(
                            List.of(
                                    task(
                                            () -> {
                                                attempts.submit(
                                                        fixture.user().getId(),
                                                        attemptId,
                                                        "submit-1");
                                                return null;
                                            }),
                                    task(
                                            () -> {
                                                attempts.submit(
                                                        fixture.user().getId(),
                                                        attemptId,
                                                        "submit-2");
                                                return null;
                                            })));
            results.forEach(this::getUnchecked);
            assertEquals(
                    1,
                    outbox.findByEventKey("attempt:" + attemptId + ":completed").stream().count());
            var submitted = outbox.findByEventKey("attempt:" + attemptId + ":submitted");
            assertTrue(
                    submitted.isEmpty(),
                    "Completed objective attempt must not enqueue subjective grading");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void listeningCannotBePlayedMoreThanTwiceAndExpiredResultRemainsReadable() {
        Fixture fixture = fixture("media");
        int attemptId =
                attempts.start(fixture.user().getId(), fixture.exam().getId(), "media-key")
                        .attemptId();
        assertEquals(
                1,
                attempts.playMedia(fixture.user().getId(), attemptId, fixture.media().getId())
                        .playCount());
        assertEquals(
                2,
                attempts.playMedia(fixture.user().getId(), attemptId, fixture.media().getId())
                        .playCount());
        assertThrows(
                BusinessRuleException.class,
                () ->
                        attempts.playMedia(
                                fixture.user().getId(), attemptId, fixture.media().getId()));

        var attempt = attemptRepository.findById(attemptId).orElseThrow();
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(2));
        attemptRepository.saveAndFlush(attempt);
        assertTrue(
                attempts.page(fixture.user().getId(), attemptId)
                        .deadline()
                        .isBefore(java.time.Instant.now()));
        assertThrows(
                BusinessRuleException.class,
                () ->
                        attempts.playMedia(
                                fixture.user().getId(), attemptId, fixture.media().getId()));
        attempts.submit(fixture.user().getId(), attemptId, "expired-submit");
        assertEquals("EXPIRED", attempts.result(fixture.user().getId(), attemptId).state());
    }

    @Test
    void listeningContentTokenSupportsBrowserMetadataAndRangeRequests() {
        Fixture fixture = fixture("media-content");
        int attemptId = attempts.start(fixture.user().getId(), fixture.exam().getId(), "media-content-key").attemptId();
        String token = attempts.playMedia(fixture.user().getId(), attemptId, fixture.media().getId()).contentToken();

        var metadata = attempts.mediaContent(fixture.user().getId(), attemptId, fixture.media().getId(), token);
        var range = attempts.mediaContent(fixture.user().getId(), attemptId, fixture.media().getId(), token);

        assertTrue(metadata.resource().exists());
        assertTrue(range.resource().exists());
        assertEquals("audio/wav", metadata.contentType());
    }

    @Test
    void ownerAndQuestionBoundariesAreEnforcedAndReloadKeepsAnswer() {
        Fixture fixture = fixture("owner");
        User otherDraft = new User();
        otherDraft.setFullName("Other learner");
        otherDraft.setEmail("other-" + System.nanoTime() + "@test.local");
        otherDraft.setPasswordHash("unused");
        otherDraft.setRole(Role.STUDENT);
        otherDraft.setStatus(UserStatus.ACTIVE);
        final User other = users.saveAndFlush(otherDraft);
        int attemptId =
                attempts.start(fixture.user().getId(), fixture.exam().getId(), "owner-key")
                        .attemptId();

        attempts.saveAnswer(
                fixture.user().getId(),
                attemptId,
                fixture.question().getId(),
                fixture.item().getId(),
                fixture.option().getId(),
                null,
                null);
        assertEquals(
                fixture.option().getId(),
                attempts.page(fixture.user().getId(), attemptId)
                        .questions()
                        .getFirst()
                        .items()
                        .getFirst()
                        .selectedOptionId());
        assertThrows(AccessDeniedException.class, () -> attempts.page(other.getId(), attemptId));
        assertThrows(
                AccessDeniedException.class,
                () ->
                        attempts.saveAnswer(
                                other.getId(),
                                attemptId,
                                fixture.question().getId(),
                                fixture.item().getId(),
                                fixture.option().getId(),
                                null,
                                null));
        assertThrows(
                AccessDeniedException.class,
                () -> attempts.submit(other.getId(), attemptId, "other-submit"));
        assertThrows(
                AccessDeniedException.class,
                () -> attempts.playMedia(other.getId(), attemptId, fixture.media().getId()));

        QuestionEntity foreignQuestionDraft = new QuestionEntity();
        foreignQuestionDraft.setContent("Foreign question");
        foreignQuestionDraft.setQuestionType("SINGLE_CHOICE");
        foreignQuestionDraft.setSkill("LISTENING");
        foreignQuestionDraft.setStatus("PUBLISHED");
        final QuestionEntity foreignQuestion = questions.saveAndFlush(foreignQuestionDraft);
        assertThrows(
                BusinessRuleException.class,
                () ->
                        attempts.saveAnswer(
                                fixture.user().getId(),
                                attemptId,
                                foreignQuestion.getId(),
                                fixture.item().getId(),
                                fixture.option().getId(),
                                null,
                                null));
    }

    @Test
    void terminalRequestKeyIsIdempotentInsteadOfCreatingRetake() {
        Fixture fixture = fixture("retake");
        int first =
                attempts.start(fixture.user().getId(), fixture.exam().getId(), "same-terminal-key")
                        .attemptId();
        attempts.submit(fixture.user().getId(), first, "submit-terminal");
        int second =
                attempts.start(fixture.user().getId(), fixture.exam().getId(), "same-terminal-key")
                        .attemptId();
        assertEquals(first, second);
        assertEquals(1, attemptRepository.countByUserId(fixture.user().getId()));
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void a1CheckpointWithFullMarksPersistsA1AndPassesInsteadOfJumpingToC2() {
        Fixture fixture = fixture("checkpoint-cap");
        fixture.exam().setExamType("CHECKPOINT");
        fixture.exam().setTargetLevel("A1");
        exams.saveAndFlush(fixture.exam());
        var band =
                bands.findBySchemeIdAndSkillOrderByMinScoreAsc(
                                fixture.exam().getScoringSchemeId(), "LISTENING")
                        .getFirst();
        band.setCefrLevel("C2");
        bands.saveAndFlush(band);
        int id =
                attempts.start(
                                fixture.user().getId(),
                                fixture.exam().getId(),
                                "checkpoint:" + fixture.user().getId() + ":1:after:0")
                        .attemptId();
        attempts.saveAnswer(
                fixture.user().getId(),
                id,
                fixture.question().getId(),
                fixture.item().getId(),
                fixture.option().getId(),
                null,
                null);
        attempts.submit(fixture.user().getId(), id, "checkpoint-full-submit");
        var result = attempts.result(fixture.user().getId(), id);
        assertEquals("COMPLETED", result.state());
        assertEquals("A1", result.skills().getFirst().estimatedLevel());
        assertTrue(result.skills().getFirst().passed());
        assertEquals("A1", attemptRepository.findById(id).orElseThrow().getEstimatedOverallCefr());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void a1CheckpointWithNoCorrectAnswersDoesNotPassDespiteLowestA1Band() {
        Fixture fixture = fixture("checkpoint-zero");
        fixture.exam().setExamType("CHECKPOINT");
        fixture.exam().setTargetLevel("A1");
        exams.saveAndFlush(fixture.exam());
        int id =
                attempts.start(
                                fixture.user().getId(),
                                fixture.exam().getId(),
                                "checkpoint:" + fixture.user().getId() + ":1:after:0")
                        .attemptId();
        attempts.submit(fixture.user().getId(), id, "checkpoint-empty-submit");
        var result = attempts.result(fixture.user().getId(), id);
        assertEquals("COMPLETED", result.state());
        assertEquals(0, result.skills().getFirst().scaleScore().signum());
        org.junit.jupiter.api.Assertions.assertFalse(result.skills().getFirst().passed());
    }

    @Test
    void passingRealA1CheckpointCreatesOnlyTheA2Stage() {
        Fixture fixture = fixture("checkpoint-path");
        fixture.exam().setExamType("CHECKPOINT");
        fixture.exam().setTargetLevel("A1");
        exams.saveAndFlush(fixture.exam());
        var band =
                bands.findBySchemeIdAndSkillOrderByMinScoreAsc(
                                fixture.exam().getScoringSchemeId(), "LISTENING")
                        .getFirst();
        band.setCefrLevel("C2");
        bands.saveAndFlush(band);
        var a1 = lessonService.newForm("Listening", "A1");
        a1.setTitle("A1 listening foundation");
        a1.setStatus("PUBLISHED");
        var first = lessonService.save(null, a1);
        var a2 = lessonService.newForm("Listening", "A2");
        a2.setTitle("A2 listening continuation");
        a2.setStatus("PUBLISHED");
        lessonService.save(null, a2);
        learningPaths.ensurePaths(fixture.user().getId());
        lessonService.complete(fixture.user().getEmail(), first.getId());
        var old =
                learningPathRepository
                        .findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(
                                fixture.user().getId(), "Listening", "ACTIVE")
                        .orElseThrow();
        int id = learningPaths.startCheckpoint(fixture.user().getId(), old.getId(), null);
        attempts.saveAnswer(
                fixture.user().getId(),
                id,
                fixture.question().getId(),
                fixture.item().getId(),
                fixture.option().getId(),
                null,
                null);
        attempts.submit(fixture.user().getId(), id, "checkpoint-path-submit");
        assertEquals(
                "A1",
                skillProfiles
                        .findByUserIdAndSkill(fixture.user().getId(), "LISTENING")
                        .orElseThrow()
                        .getCurrentLevel());
        assertEquals(
                "COMPLETED",
                learningPathRepository.findById(old.getId()).orElseThrow().getStatus());
        assertEquals(
                "A2",
                learningPathRepository
                        .findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(
                                fixture.user().getId(), "Listening", "ACTIVE")
                        .orElseThrow()
                        .getTargetLevel());
        learningPaths.applyCheckpoint(fixture.user().getId(), id);
        assertEquals(
                1,
                learningPathRepository
                        .findByUserIdAndStatusOrderBySkillAscCreatedAtDesc(
                                fixture.user().getId(), "ACTIVE")
                        .size());
    }

    @Test
    void checkpointCannotBypassLearningPathStartBoundary() {
        Fixture fixture = fixture("checkpoint-boundary");
        fixture.exam().setExamType("CHECKPOINT");
        fixture.exam().setTargetLevel("A1");
        exams.saveAndFlush(fixture.exam());

        assertThrows(
                BusinessRuleException.class,
                () ->
                        attempts.start(
                                fixture.user().getId(), fixture.exam().getId(), "exam:generic"));
        assertEquals(0, attemptRepository.countByUserId(fixture.user().getId()));

        assertTrue(
                attempts.start(
                                        fixture.user().getId(),
                                        fixture.exam().getId(),
                                        "checkpoint:" + fixture.user().getId() + ":1:after:0")
                                .attemptId()
                        > 0);
    }

    private Fixture fixture(String suffix) {
        User user = new User();
        user.setFullName("Concurrent learner");
        user.setEmail("attempt-" + suffix + "-" + System.nanoTime() + "@test.local");
        user.setPasswordHash("unused");
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        user = users.saveAndFlush(user);
        Certificate certificate = new Certificate();
        certificate.setName("Test certificate");
        certificate.setCode("TEST-" + suffix + "-" + System.nanoTime());
        certificate.setStatus(RecordStatus.ACTIVE);
        certificate = certificates.saveAndFlush(certificate);
        ScoringScheme scheme = new ScoringScheme();
        scheme.setCertificate(certificate);
        scheme.setName("Test scheme");
        scheme.setVersion("v1");
        scheme.setStatus(RecordStatus.ACTIVE);
        scheme = schemes.saveAndFlush(scheme);
        ScoreBand band = new ScoreBand();
        band.setScheme(scheme);
        band.setSkill("LISTENING");
        band.setCefrLevel("A1");
        band.setMinScore(java.math.BigDecimal.ZERO);
        band.setMaxScore(java.math.BigDecimal.valueOf(50));
        bands.saveAndFlush(band);
        MockExamEntity exam = new MockExamEntity();
        exam.setCertificateId(certificate.getId());
        exam.setScoringSchemeId(scheme.getId());
        exam.setTitle("Test exam");
        exam.setExamType("PRACTICE");
        exam.setSkill("LISTENING");
        exam.setDurationMinutes(1);
        exam.setStatus("PUBLISHED");
        exam = exams.saveAndFlush(exam);
        QuestionEntity question = new QuestionEntity();
        question.setContent("Audio question");
        question.setQuestionType("SINGLE_CHOICE");
        question.setSkill("LISTENING");
        question.setStatus("PUBLISHED");
        question = questions.saveAndFlush(question);
        QuestionItemEntity item = new QuestionItemEntity();
        item.setQuestionId(question.getId());
        item.setContent("Choose the answer");
        item.setPosition(1);
        item = items.saveAndFlush(item);
        QuestionOptionEntity option = new QuestionOptionEntity();
        option.setQuestionItemId(item.getId());
        option.setContent("Correct");
        option.setOptionCode("A");
        option.setCorrect(true);
        option.setPosition(1);
        options.saveAndFlush(option);
        MockExamQuestionEntity link = new MockExamQuestionEntity();
        link.setMockExamId(exam.getId());
        link.setQuestionId(question.getId());
        link.setPosition(1);
        examQuestions.saveAndFlush(link);
        QuestionMediaEntity audio = new QuestionMediaEntity();
        audio.setQuestionId(question.getId());
        audio.setMediaType("AUDIO");
        audio.setMediaUrl("curriculum/audio/listening-a2.wav");
        audio.setOriginalFilename("audio.wav");
        audio.setContentType("audio/wav");
        audio.setPosition(1);
        audio = media.saveAndFlush(audio);
        return new Fixture(user, exam, question, item, option, audio);
    }

    private <T> Callable<T> task(Callable<T> task) {
        return task;
    }

    private void getUnchecked(Future<Void> future) {
        try {
            future.get();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private record Fixture(
            User user,
            MockExamEntity exam,
            QuestionEntity question,
            QuestionItemEntity item,
            QuestionOptionEntity option,
            QuestionMediaEntity media) {}
}
