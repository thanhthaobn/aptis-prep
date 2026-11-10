package com.aptisprep.service.exam;

import static com.aptisprep.security.SessionMvcRequests.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aptisprep.entity.Certificate;
import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.entity.MockExamQuestionEntity;
import com.aptisprep.entity.MockExamSectionEntity;
import com.aptisprep.entity.QuestionEntity;
import com.aptisprep.entity.QuestionItemEntity;
import com.aptisprep.entity.QuestionMediaEntity;
import com.aptisprep.entity.QuestionOptionEntity;
import com.aptisprep.entity.ScoreBand;
import com.aptisprep.entity.ScoringScaleRule;
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
import com.aptisprep.repository.MockExamSectionRepository;
import com.aptisprep.repository.QuestionItemRepository;
import com.aptisprep.repository.QuestionMediaRepository;
import com.aptisprep.repository.QuestionOptionRepository;
import com.aptisprep.repository.QuestionRepository;
import com.aptisprep.repository.ScoreBandRepository;
import com.aptisprep.repository.ScoringScaleRuleRepository;
import com.aptisprep.repository.ScoringSchemeRepository;
import com.aptisprep.repository.UserRepository;
import com.aptisprep.service.storage.StorageService;
import com.aptisprep.support.TestAudio;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class ExamAttemptConcurrencyTests {
    @Autowired ExamAttemptService attempts;
    @Autowired UserRepository users;
    @Autowired CertificateRepository certificates;
    @Autowired ScoringSchemeRepository schemes;
    @Autowired MockExamRepository exams;
    @Autowired MockExamQuestionRepository examQuestions;
    @Autowired MockExamSectionRepository examSections;
    @Autowired com.aptisprep.service.admin.AdminExamService adminExams;
    @Autowired QuestionRepository questions;
    @Autowired QuestionItemRepository items;
    @Autowired QuestionOptionRepository options;
    @Autowired QuestionMediaRepository media;
    @Autowired ScoreBandRepository bands;
    @Autowired ScoringScaleRuleRepository scaleRules;
    @Autowired ExamAttemptRepository attemptRepository;
    @Autowired ExamAttemptSnapshotRepository snapshots;
    @Autowired AttemptOutboxRepository outbox;
    @Autowired com.aptisprep.repository.LearningPathItemRepository pathItems;
    @Autowired AttemptOutboxService outboxService;
    @Autowired StorageService storage;
    @Autowired com.aptisprep.service.learning.LessonService lessonService;
    @Autowired com.aptisprep.service.learning.LearningPathService learningPaths;
    @Autowired com.aptisprep.repository.LearningPathRepository learningPathRepository;
    @Autowired com.aptisprep.repository.UserSkillProfileRepository skillProfiles;
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired com.aptisprep.service.student.StudentExamCatalogService studentCatalog;

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
        int attemptId =
                attempts.start(fixture.user().getId(), fixture.exam().getId(), "media-content-key")
                        .attemptId();
        String token =
                attempts.playMedia(fixture.user().getId(), attemptId, fixture.media().getId())
                        .contentToken();

        var metadata =
                attempts.mediaContent(
                        fixture.user().getId(), attemptId, fixture.media().getId(), token);
        var range =
                attempts.mediaContent(
                        fixture.user().getId(), attemptId, fixture.media().getId(), token);

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
    void attemptKeepsQuestionAndItemSnapshotAfterCatalogContentChanges() {
        Fixture fixture = fixture("snapshot-content");
        int attemptId =
                attempts.start(
                                fixture.user().getId(),
                                fixture.exam().getId(),
                                "snapshot-content-key")
                        .attemptId();

        fixture.question().setContent("Edited question after the attempt started");
        questions.saveAndFlush(fixture.question());
        fixture.item().setContent("Edited item after the attempt started");
        items.saveAndFlush(fixture.item());

        var page = attempts.page(fixture.user().getId(), attemptId);
        assertEquals("Audio question", page.questions().getFirst().content());
        assertEquals("Choose the answer", page.questions().getFirst().items().getFirst().content());
    }

    @Test
    void editingAnAttemptedExamPreservesExistingAndReconstructedSnapshots() throws Exception {
        Fixture original = fixture("editable-with-history");
        Fixture replacement = fixture("editable-replacement");
        int examId = original.exam().getId();
        int userId = original.user().getId();
        int completed = attempts.start(userId, examId, "legacy-completed").attemptId();
        attempts.saveAnswer(
                userId,
                completed,
                original.question().getId(),
                original.item().getId(),
                original.option().getId(),
                null,
                null);
        attempts.submit(userId, completed, "submit-legacy-completed");
        snapshots.delete(snapshots.findByAttemptId(completed).orElseThrow());
        snapshots.flush();
        int ongoing = attempts.start(userId, examId, "existing-snapshot").attemptId();

        adminExams.unpublishExam(examId);
        var edit = adminExams.examForm(examId);
        edit.setTitle("Revised exam");
        edit.setQuestionIds(replacement.question().getId().toString());
        adminExams.saveExam(examId, edit);
        adminExams.publishExam(examId);

        assertEquals(original.exam().getTitle(), attempts.result(userId, completed).examTitle());
        assertEquals(
                original.question().getId(),
                attempts.review(userId, completed).questions().getFirst().questionId());
        var student = user(original.user().getEmail()).roles("STUDENT");
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                        "/student/attempts/{id}/review", completed)
                                .with(student))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                                .isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                                .string(org.hamcrest.Matchers.containsString("Đã chọn")));
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                        "/student/attempts/{id}/result", completed)
                                .with(student))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                                .isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                                .string(
                                        org.hamcrest.Matchers.allOf(
                                                org.hamcrest.Matchers.containsString(
                                                        "Điểm theo kỹ năng"),
                                                org.hamcrest.Matchers.containsString(
                                                        "Xem lại bài làm"),
                                                org.hamcrest.Matchers.not(
                                                        org.hamcrest.Matchers.containsString(
                                                                "result-navi")))));
        assertEquals(
                original.question().getId(),
                attempts.page(userId, ongoing).questions().getFirst().questionId());
        int next = attempts.start(userId, examId, "new-revision").attemptId();
        assertEquals(
                replacement.question().getId(),
                attempts.page(userId, next).questions().getFirst().questionId());
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
                attempts.startCheckpoint(
                                fixture.user().getId(),
                                fixture.exam().getId(),
                                readyPath(fixture),
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
                attempts.startCheckpoint(
                                fixture.user().getId(),
                                fixture.exam().getId(),
                                readyPath(fixture),
                                "checkpoint:" + fixture.user().getId() + ":1:after:0")
                        .attemptId();
        attempts.submit(fixture.user().getId(), id, "checkpoint-empty-submit");
        var result = attempts.result(fixture.user().getId(), id);
        assertEquals("COMPLETED", result.state());
        assertEquals(0, result.skills().getFirst().scaleScore().signum());
        org.junit.jupiter.api.Assertions.assertFalse(result.skills().getFirst().passed());
    }

    @Test
    void passingStartedA1CheckpointOpensA2AfterNewLessonRefresh() {
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
        var added = lessonService.newForm("Listening", "A1");
        added.setTitle("A1 lesson published during the checkpoint");
        added.setStatus("PUBLISHED");
        lessonService.save(null, added);
        learningPaths.refreshPaths(fixture.user().getEmail());
        assertEquals(
                "ARCHIVED", learningPathRepository.findById(old.getId()).orElseThrow().getStatus());
        var refreshed =
                learningPathRepository
                        .findFirstByUserIdAndSkillAndStatusOrderByCreatedAtDesc(
                                fixture.user().getId(), "Listening", "ACTIVE")
                        .orElseThrow();
        assertEquals(
                "locked",
                learningPaths.summary(fixture.user().getId(), refreshed.getId()).checkpointState());
        attempts.submit(fixture.user().getId(), id, "checkpoint-path-submit");
        assertTrue(attempts.result(fixture.user().getId(), id).skills().getFirst().passed());
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
                "ARCHIVED",
                learningPathRepository.findById(refreshed.getId()).orElseThrow().getStatus());
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

        assertThrows(
                BusinessRuleException.class,
                () ->
                        attempts.start(
                                fixture.user().getId(),
                                fixture.exam().getId(),
                                "checkpoint:forged"));
        assertTrue(
                attempts.startCheckpoint(
                                        fixture.user().getId(),
                                        fixture.exam().getId(),
                                        readyPath(fixture),
                                        "verified-path")
                                .attemptId()
                        > 0);
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void listeningTranscriptStaysInTheSnapshotAndNeverReachesStudentHtmlOrDto() throws Exception {
        var f = fixture("private-transcript");
        String secret = "PRIVATE LISTENING TRANSCRIPT 8271";
        f.media().setTranscript(secret);
        media.saveAndFlush(f.media());
        int id = attempts.start(f.user().getId(), f.exam().getId(), "private-audio").attemptId();
        f.media().setTranscript("CHANGED LIVE TRANSCRIPT");
        media.saveAndFlush(f.media());
        String dto =
                tools.jackson.databind.json.JsonMapper.builder()
                        .build()
                        .writeValueAsString(attempts.page(f.user().getId(), id));
        org.junit.jupiter.api.Assertions.assertFalse(dto.contains(secret));
        org.junit.jupiter.api.Assertions.assertFalse(dto.contains("transcript"));
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                        "/student/attempts/" + id)
                                .with(user(f.user().getEmail()).roles("STUDENT")))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                                .isOk())
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(secret))))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "data-speech"))));
        var lockedMedia =
                snapshots.findByAttemptId(id).orElseThrow().getQuestions().stream()
                        .flatMap(question -> question.getMedia().stream())
                        .filter(entry -> entry.getSourceMediaId().equals(f.media().getId()))
                        .findFirst()
                        .orElseThrow();
        assertEquals(secret, lockedMedia.getTranscript());
        assertEquals(1, attempts.playMedia(f.user().getId(), id, f.media().getId()).playCount());
        assertEquals(2, attempts.playMedia(f.user().getId(), id, f.media().getId()).playCount());
        assertThrows(
                BusinessRuleException.class,
                () -> attempts.playMedia(f.user().getId(), id, f.media().getId()));
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void speechOnlyOrMissingAudioUsesBrowserSpeechAndKeepsPlayLimit() {
        var f = fixture("speech-not-audio");
        f.media().setMediaUrl("speech://legacy");
        f.media().setTranscript("Secret transcript");
        media.saveAndFlush(f.media());
        int speechAttemptId =
                attempts.start(f.user().getId(), f.exam().getId(), "speech-only").attemptId();
        var speechPlayback =
                attempts.playMedia(f.user().getId(), speechAttemptId, f.media().getId());
        assertTrue(speechPlayback.browserSpeech());
        assertNull(speechPlayback.contentToken());
        assertEquals("Secret transcript", speechPlayback.speechText());

        f.media().setMediaUrl("questions/missing-audio.wav");
        media.saveAndFlush(f.media());
        int missingAttemptId =
                attempts.start(f.user().getId(), f.exam().getId(), "missing-file").attemptId();
        var missingPlayback =
                attempts.playMedia(f.user().getId(), missingAttemptId, f.media().getId());
        assertTrue(missingPlayback.browserSpeech());
        assertEquals("Secret transcript", missingPlayback.speechText());
        assertEquals(2, attemptRepository.countByUserId(f.user().getId()));
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void checkpointRechecksOwnerStatusAndCompletionAtEngineBoundary() {
        var f = fixture("checkpoint-verified-context");
        f.exam().setExamType("CHECKPOINT");
        f.exam().setTargetLevel("A1");
        exams.saveAndFlush(f.exam());
        int pathId = readyPath(f);
        var other = fixture("checkpoint-foreign-owner");
        assertThrows(
                BusinessRuleException.class,
                () ->
                        attempts.startCheckpoint(
                                other.user().getId(), f.exam().getId(), pathId, "foreign-owner"));
        var path = learningPathRepository.findById(pathId).orElseThrow();
        path.setStatus("ARCHIVED");
        learningPathRepository.saveAndFlush(path);
        assertThrows(
                BusinessRuleException.class,
                () ->
                        attempts.startCheckpoint(
                                f.user().getId(), f.exam().getId(), pathId, "archived-path"));
        path.setStatus("ACTIVE");
        learningPathRepository.saveAndFlush(path);
        var required = pathItems.findByLearningPathIdOrderByPositionAsc(pathId).getFirst();
        required.setCompletedAt(null);
        pathItems.saveAndFlush(required);
        assertThrows(
                BusinessRuleException.class,
                () ->
                        attempts.startCheckpoint(
                                f.user().getId(), f.exam().getId(), pathId, "incomplete-required"));
        assertEquals(0, attemptRepository.countByUserId(f.user().getId()));
        required.setCompletedAt(LocalDateTime.now());
        pathItems.saveAndFlush(required);
        int first =
                attempts.startCheckpoint(
                                f.user().getId(), f.exam().getId(), pathId, "verified-idempotency")
                        .attemptId();
        assertEquals(
                first,
                attempts.startCheckpoint(
                                f.user().getId(), f.exam().getId(), pathId, "verified-idempotency")
                        .attemptId());
    }

    @Test
    void sectionWindowsRejectEarlyAndLateAnswersAndUnpublishedAttemptCanStillSubmit()
            throws Exception {
        Fixture fixture = fixture("section-windows");
        int examId = fixture.exam().getId();
        int userId = fixture.user().getId();
        var second = new QuestionEntity();
        second.setContent("Second section question");
        second.setQuestionType("SINGLE_CHOICE");
        second.setSkill("LISTENING");
        second.setStatus("PUBLISHED");
        int secondId = questions.saveAndFlush(second).getId();
        var extraItem = new QuestionItemEntity();
        extraItem.setQuestionId(fixture.question().getId());
        extraItem.setContent("Second item in the same audio");
        extraItem.setPosition(2);
        items.saveAndFlush(extraItem);
        var secondSectionItem = new QuestionItemEntity();
        secondSectionItem.setQuestionId(secondId);
        secondSectionItem.setContent("First item of the next section");
        secondSectionItem.setPosition(1);
        items.saveAndFlush(secondSectionItem);
        QuestionMediaEntity secondAudio = new QuestionMediaEntity();
        secondAudio.setQuestionId(secondId);
        secondAudio.setMediaType("AUDIO");
        secondAudio.setMediaUrl(TestAudio.store(storage));
        secondAudio.setOriginalFilename("second.wav");
        secondAudio.setContentType("audio/wav");
        secondAudio.setPosition(1);
        media.saveAndFlush(secondAudio);
        var firstLink = examQuestions.findByMockExamIdOrderByPosition(examId).getFirst();
        var secondLink = new MockExamQuestionEntity();
        secondLink.setMockExamId(examId);
        secondLink.setQuestionId(secondId);
        secondLink.setPosition(2);
        for (var link : List.of(firstLink, secondLink)) {
            var section = new MockExamSectionEntity();
            section.setMockExamId(examId);
            section.setSkill("LISTENING");
            section.setDisplayOrder(link.getPosition());
            section.setDurationMinutes(2);
            section.setStatus("ACTIVE");
            link.setMockExamSectionId(examSections.saveAndFlush(section).getId());
            examQuestions.saveAndFlush(link);
        }
        int attemptId = attempts.start(userId, examId, "timed-section").attemptId();
        var firstPage = attempts.page(userId, attemptId);
        assertEquals(
                List.of(1, 2),
                firstPage.questions().getFirst().items().stream()
                        .map(ExamAttemptService.ItemPage::position)
                        .toList());
        assertEquals(2, firstPage.totalItems());
        assertEquals(2, firstPage.sections().size());
        assertEquals("OPEN", firstPage.sections().getFirst().state());
        assertEquals("UPCOMING", firstPage.sections().getLast().state());
        attempts.saveAnswer(
                userId,
                attemptId,
                fixture.question().getId(),
                fixture.item().getId(),
                fixture.option().getId(),
                null,
                null);
        assertTrue(attempts.page(userId, attemptId).nextSection());
        assertEquals(1, attempts.page(userId, attemptId).answeredItems());
        assertEquals(
                fixture.question().getId(),
                attempts.page(userId, attemptId).questions().getFirst().questionId());
        assertThrows(
                BusinessRuleException.class,
                () -> attempts.saveAnswer(userId, attemptId, secondId, null, null, null, null));
        attempts.advanceSection(userId, attemptId, 4);
        assertEquals(
                fixture.question().getId(),
                attempts.page(userId, attemptId).questions().getFirst().questionId());
        attempts.advanceSection(userId, attemptId, 2);
        var next = attempts.page(userId, attemptId);
        assertEquals("COMPLETED", next.sections().getFirst().state());
        assertEquals("OPEN", next.sections().getLast().state());
        assertEquals(1, next.totalItems());
        assertEquals(0, next.answeredItems());
        assertEquals(
                secondSectionItem.getId(), next.questions().getFirst().items().getFirst().itemId());
        assertEquals(1, next.questions().getFirst().items().getFirst().position());
        assertFalse(next.nextSection());
        assertTrue(next.remainingSeconds() > 115 && next.remainingSeconds() <= 120);
        attempts.advanceSection(userId, attemptId, 2);
        assertEquals(next.deadline(), attempts.page(userId, attemptId).deadline());
        assertEquals(
                secondId, attempts.page(userId, attemptId).questions().getFirst().questionId());
        assertThrows(
                BusinessRuleException.class,
                () ->
                        attempts.saveAnswer(
                                userId,
                                attemptId,
                                fixture.question().getId(),
                                fixture.item().getId(),
                                fixture.option().getId(),
                                null,
                                null));
        adminExams.unpublishExam(examId);
        assertEquals("ARCHIVED", exams.findById(examId).orElseThrow().getStatus());
        assertThrows(
                com.aptisprep.exception.ResourceNotFoundException.class,
                () -> attempts.start(userId, examId, "new-after-unpublish"));
        attempts.submit(userId, attemptId, "submit-after-unpublish");
        assertEquals("COMPLETED", attemptRepository.findById(attemptId).orElseThrow().getStatus());
    }

    private Integer readyPath(Fixture fixture) {
        var form = lessonService.newForm("Listening", "A1");
        form.setTitle("Required checkpoint lesson");
        form.setStatus("PUBLISHED");
        var lesson = lessonService.save(null, form);
        var path = new com.aptisprep.entity.LearningPath();
        path.setUser(fixture.user());
        path.setSkill("Listening");
        path.setTargetLevel("A1");
        path.setPathSource("FOUNDATION");
        path.setStatus("ACTIVE");
        path.setCheckpointExamId(fixture.exam().getId());
        path = learningPathRepository.saveAndFlush(path);
        var item = new com.aptisprep.entity.LearningPathItem();
        item.setLearningPath(path);
        item.setLesson(lesson);
        item.setPosition(1);
        item.setRequired(true);
        item.setStatus("COMPLETED");
        item.setCompletedAt(LocalDateTime.now());
        pathItems.saveAndFlush(item);
        return path.getId();
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
        ScoringScaleRule scaleRule = new ScoringScaleRule();
        scaleRule.setScheme(scheme);
        scaleRule.setSkill("LISTENING");
        scaleRule.setRawMin(java.math.BigDecimal.ZERO);
        scaleRule.setRawMax(java.math.BigDecimal.valueOf(40));
        scaleRule.setScaleMin(java.math.BigDecimal.ZERO);
        scaleRule.setScaleMax(java.math.BigDecimal.valueOf(50));
        scaleRules.saveAndFlush(scaleRule);
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
        audio.setMediaUrl(TestAudio.store(storage));
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
