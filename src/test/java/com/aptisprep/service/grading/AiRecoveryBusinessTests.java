package com.aptisprep.service.grading;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.aptisprep.dto.GradingResultDTO;
import com.aptisprep.entity.*;
import com.aptisprep.entity.User;
import com.aptisprep.enums.*;
import com.aptisprep.event.AiAssessmentCompletedEvent;
import com.aptisprep.exception.AiGradingException;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.repository.*;
import com.aptisprep.repository.ProfileApplicationRepository;
import com.aptisprep.repository.UserRepository;
import com.aptisprep.repository.UserSkillProfileRepository;
import com.aptisprep.service.account.UserService;
import com.aptisprep.service.exam.AttemptOutboxService;
import com.aptisprep.service.grading.impl.AiAssessmentServiceImpl;
import com.aptisprep.service.grading.impl.GeminiGradingService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:ai_recovery;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
            "app.ai.enabled=true",
            "app.ai.recovery-ms=86400000",
            "app.attempt-outbox.recovery-ms=86400000"
        })
class AiRecoveryBusinessTests {
    @MockitoBean GeminiGradingService provider;
    @Autowired AiAssessmentServiceImpl ai;
    @Autowired AttemptFinalizationService finalization;
    @Autowired AttemptOutboxService delivery;
    @Autowired AiAssessmentJobRepository jobs;
    @Autowired AiAssessmentRepository assessments;
    @Autowired UserRepository users;
    @Autowired UserService userService;
    @Autowired ExamAttemptRepository attempts;
    @Autowired UserAnswerRepository answers;
    @Autowired QuestionRepository questions;
    @Autowired MockExamRepository exams;
    @Autowired CertificateRepository certificates;
    @Autowired ScoringSchemeRepository schemes;
    @Autowired ScoringScaleRuleRepository rules;
    @Autowired ScoreBandRepository bands;
    @Autowired ExamSkillResultRepository results;
    @Autowired AttemptOutboxRepository outbox;
    @Autowired UserSkillProfileRepository profiles;
    @Autowired ProfileApplicationRepository applications;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired ApplicationEventPublisher events;
    @Autowired JsonMapper json;
    @Autowired com.aptisprep.config.AiProperties properties;

    @BeforeEach
    void isolateQueuesInTheDedicatedInMemoryDatabase() throws Exception {
        properties.setEnabled(true);
        // Never clean a configured SQL Server or a shared application database.
        try (var connection = Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:ai_recovery"));
        }
        jdbc.update("delete from ai_assessments");
        jdbc.update("delete from ai_assessment_jobs");
        jdbc.update("delete from attempt_outbox");
        when(provider.grade(anyString(), anyString())).thenReturn(grade(20));
    }

    @Test
    void twoExamScoresRemainFifteenDespiteCompletedLessonFifty() {
        var f = exam(2);
        var first = examJob(f, 0, AiJobState.COMPLETED, 10);
        examJob(f, 1, AiJobState.COMPLETED, 20);
        var lesson = collidingLesson(f, AiJobState.COMPLETED, 50);
        finalization.applyAiAssessment(event(first));
        assertEquals(new BigDecimal("15.00"), result(f).getSubjectiveRawScore());
        assertEquals(
                new BigDecimal("15.00"),
                attempts.findById(f.attempt().getId()).orElseThrow().getFinalScaleScore());
        assertEquals(
                new BigDecimal("10.00"),
                answers.findById(f.answers().get(0).getId()).orElseThrow().getAiScore());
        assertEquals(
                new BigDecimal("20.00"),
                answers.findById(f.answers().get(1).getId()).orElseThrow().getAiScore());
        assertEquals(
                f.attempt().getId(), jobs.findById(lesson.getId()).orElseThrow().getAttemptId());
        assertEquals(
                "LESSON_SUBMISSION", jobs.findById(lesson.getId()).orElseThrow().getSourceType());
        verifyNoInteractions(provider);
    }

    @Test
    void lessonErrorDoesNotMakeAPendingExamAnError() {
        var f = exam(2);
        var first = examJob(f, 0, AiJobState.COMPLETED, 10);
        examJob(f, 1, AiJobState.PENDING, null);
        collidingLesson(f, AiJobState.ERROR, null);
        finalization.applyAiAssessment(event(first));
        assertEquals("WAITING", result(f).getGradingStatus());
        assertEquals(
                "WAITING_GRADING",
                attempts.findById(f.attempt().getId()).orElseThrow().getStatus());
        assertNull(result(f).getRawScore());
    }

    @Test
    void retryRunsOnlyTheExamJobAndNeverTheCollidingLesson() {
        var f = exam(2);
        var failed = examJob(f, 0, AiJobState.ERROR, null);
        examJob(f, 1, AiJobState.COMPLETED, 20);
        var lesson = collidingLesson(f, AiJobState.ERROR, null);
        f.attempt().setStatus("ERROR");
        attempts.saveAndFlush(f.attempt());
        when(provider.grade(failed.getTaskContent(), failed.getLockedContent()))
                .thenReturn(grade(10));
        finalization.retryFailedGrading(f.user().getId(), f.attempt().getId());
        verify(provider).grade(failed.getTaskContent(), failed.getLockedContent());
        verify(provider, never()).grade(lesson.getTaskContent(), lesson.getLockedContent());
        assertEquals(AiJobState.ERROR, jobs.findById(lesson.getId()).orElseThrow().getState());
        assertEquals(0, jobs.findById(lesson.getId()).orElseThrow().getRetryCount());
        assertEquals(new BigDecimal("15.00"), result(f).getSubjectiveRawScore());
    }

    @Test
    void realQueryExcludesWrongOwnerVersionPolicyRubricAndWrongAnswerIsNotAggregated() {
        var f = exam(1);
        var accepted = examJob(f, 0, AiJobState.COMPLETED, 10);
        var wrongOwner = examJob(f, 0, AiJobState.COMPLETED, 50);
        wrongOwner.setUserId(user().getId());
        jobs.saveAndFlush(wrongOwner);
        var wrongVersion = examJob(f, 0, AiJobState.COMPLETED, 50);
        wrongVersion.setAnswerVersion(2);
        jobs.saveAndFlush(wrongVersion);
        var wrongPolicy = examJob(f, 0, AiJobState.COMPLETED, 50);
        wrongPolicy.setPolicyVersion("wrong");
        jobs.saveAndFlush(wrongPolicy);
        var wrongRubric = examJob(f, 0, AiJobState.COMPLETED, 50);
        wrongRubric.setRubricVersion("wrong");
        jobs.saveAndFlush(wrongRubric);
        var wrongSource = examJob(f, 0, AiJobState.COMPLETED, 50);
        wrongSource.setSourceId(999999);
        jobs.saveAndFlush(wrongSource);
        var other = exam(1);
        var wrongAnswer = examJob(f, 0, AiJobState.COMPLETED, 50);
        wrongAnswer.setAnswerId(other.answers().getFirst().getId());
        wrongAnswer.setSourceId(wrongAnswer.getAnswerId());
        jobs.saveAndFlush(wrongAnswer);
        var selected = jobs.findExamJobs(f.user().getId(), f.attempt().getId(), 1, "p1", "r1");
        assertEquals(
                Set.of(accepted.getId(), wrongSource.getId(), wrongAnswer.getId()),
                new HashSet<>(selected.stream().map(AiAssessmentJob::getId).toList()));
        finalization.applyAiAssessment(event(accepted));
        assertEquals(new BigDecimal("10.00"), result(f).getSubjectiveRawScore());
    }

    @Test
    void mismatchedAssessmentPolicyIsNotCountedEvenWhenJobMetadataMatches() {
        var f = exam(2);
        var first = examJob(f, 0, AiJobState.COMPLETED, 10);
        var second = examJob(f, 1, AiJobState.COMPLETED, 50);
        var assessment = assessment(second);
        assessment.setPolicyVersion("different-policy");
        assessments.saveAndFlush(assessment);
        finalization.applyAiAssessment(event(first));
        assertEquals("WAITING", result(f).getGradingStatus());
        assertNull(result(f).getRawScore());
        assertNull(answers.findById(second.getAnswerId()).orElseThrow().getAiScore());
    }

    @Test
    void pendingJobSurvivesRestartAndRecoveryDoesNotCallProviderTwice() {
        var job = lesson(AiJobState.PENDING, 0);
        restarted().recoverAiJobs();
        restarted().recoverAiJobs();
        assertEquals(AiJobState.COMPLETED, jobs.findById(job.getId()).orElseThrow().getState());
        assertEquals(1, jobs.findById(job.getId()).orElseThrow().getRetryCount());
        verify(provider).grade(job.getTaskContent(), job.getLockedContent());
    }

    @Test
    void disablingAiDoesNotConsumePendingRetries() {
        var job = lesson(AiJobState.PENDING, 0);
        properties.setEnabled(false);
        restarted().recoverAiJobs();
        assertEquals(AiJobState.PENDING, jobs.findById(job.getId()).orElseThrow().getState());
        assertEquals(0, jobs.findById(job.getId()).orElseThrow().getRetryCount());
        verifyNoInteractions(provider);
        properties.setEnabled(true);
        restarted().recoverAiJobs();
        assertEquals(AiJobState.COMPLETED, jobs.findById(job.getId()).orElseThrow().getState());
    }

    @Test
    void aFailedPendingClaimIsDeferredThenPickedUpOnTheNextEligiblePass() {
        var job = lesson(AiJobState.PENDING, 0);
        var repository =
                mock(
                        AiAssessmentJobRepository.class,
                        org.mockito.AdditionalAnswers.delegatesTo(jobs));
        var failOnce = new java.util.concurrent.atomic.AtomicBoolean(true);
        doAnswer(
                        call -> {
                            if (failOnce.getAndSet(false))
                                throw new org.springframework.dao
                                        .TransientDataAccessResourceException(
                                        "Simulated restart gap");
                            return jobs.findByAssessmentKeyForUpdate(job.getAssessmentKey());
                        })
                .when(repository)
                .findByAssessmentKeyForUpdate(job.getAssessmentKey());
        var interrupted =
                new AiAssessmentServiceImpl(
                        repository,
                        assessments,
                        userService,
                        provider,
                        provider,
                        mock(com.aptisprep.service.storage.StorageService.class),
                        transactionManager,
                        events,
                        delivery,
                        null);
        interrupted.recoverAiJobs();
        var pending = jobs.findById(job.getId()).orElseThrow();
        assertEquals(AiJobState.PENDING, pending.getState());
        assertTrue(pending.getNextRetryAt().isAfter(LocalDateTime.now()));
        interrupted.recoverAiJobs();
        verifyNoInteractions(provider);
        pending.setNextRetryAt(LocalDateTime.now().minusSeconds(1));
        jobs.saveAndFlush(pending);
        restarted().recoverAiJobs();
        assertEquals(AiJobState.COMPLETED, jobs.findById(job.getId()).orElseThrow().getState());
        verify(provider).grade(job.getTaskContent(), job.getLockedContent());
    }

    @Test
    void staleProcessingLeaseIsReclaimedWithoutACommittedPendingGap() {
        var job = lesson(AiJobState.PROCESSING, 1);
        age(job);
        when(provider.grade(job.getTaskContent(), job.getLockedContent()))
                .thenAnswer(
                        call -> {
                            var claimed = jobs.findById(job.getId()).orElseThrow();
                            assertEquals(AiJobState.PROCESSING, claimed.getState());
                            assertNotEquals("old-token", claimed.getProcessingToken());
                            assertEquals(2, claimed.getRetryCount());
                            return grade(20);
                        });
        restarted().recoverAiJobs();
        assertEquals(AiJobState.COMPLETED, jobs.findById(job.getId()).orElseThrow().getState());
    }

    @Test
    void exhaustedInterruptedExamBecomesErrorWithDurableCallbackAndNoProviderCall() {
        var f = exam(1);
        var job = examJob(f, 0, AiJobState.PROCESSING, null);
        job.setRetryCount(3);
        job.setProcessingToken("old-token");
        jobs.saveAndFlush(job);
        age(job);
        restarted().recoverAiJobs();
        assertEquals(AiJobState.ERROR, jobs.findById(job.getId()).orElseThrow().getState());
        assertNull(jobs.findById(job.getId()).orElseThrow().getProcessingToken());
        assertNull(assessment(job).getScore());
        assertTrue(outbox.findByEventKey(callbackKey(job, "ERROR")).isPresent());
        assertEquals("ERROR", attempts.findById(f.attempt().getId()).orElseThrow().getStatus());
        assertThrows(
                BusinessRuleException.class,
                () -> ai.retry(job.getAssessmentKey(), f.user().getId(), false));
        verifyNoInteractions(provider);
    }

    @Test
    void exhaustedPendingIsNotLeftPermanentlyPending() {
        var job = lesson(AiJobState.PENDING, 3);
        ai.recoverAiJobs();
        ai.recoverAiJobs();
        assertEquals(AiJobState.ERROR, jobs.findById(job.getId()).orElseThrow().getState());
        verifyNoInteractions(provider);
    }

    @Test
    void freshProcessingAndDeferredPendingAreNotClaimedEarly() {
        var active = lesson(AiJobState.PROCESSING, 1);
        var delayed = lesson(AiJobState.PENDING, 0);
        delayed.setNextRetryAt(LocalDateTime.now().plusHours(1));
        jobs.saveAndFlush(delayed);
        ai.recoverAiJobs();
        assertEquals("old-token", jobs.findById(active.getId()).orElseThrow().getProcessingToken());
        assertEquals(AiJobState.PENDING, jobs.findById(delayed.getId()).orElseThrow().getState());
        verifyNoInteractions(provider);
    }

    @Test
    void lateProviderSuccessCannotOverwriteTheNewClaim() throws Exception {
        lateProvider(false);
    }

    @Test
    void lateProviderErrorCannotEraseTheNewSuccess() throws Exception {
        lateProvider(true);
    }

    private void lateProvider(boolean error) throws Exception {
        var job = lesson(AiJobState.PENDING, 0);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var calls = new AtomicInteger();
        when(provider.grade(job.getTaskContent(), job.getLockedContent()))
                .thenAnswer(
                        call -> {
                            if (calls.incrementAndGet() == 1) {
                                entered.countDown();
                                assertTrue(release.await(15, TimeUnit.SECONDS));
                                if (error) throw new AiGradingException("Old provider failed late");
                                return grade(10);
                            }
                            return grade(20);
                        });
        var pool = Executors.newSingleThreadExecutor();
        try {
            var old = pool.submit(() -> ai.process(job.getAssessmentKey()));
            assertTrue(entered.await(10, TimeUnit.SECONDS));
            age(job);
            restarted().recoverAiJobs();
            release.countDown();
            assertEquals(new BigDecimal("20.00"), old.get(10, TimeUnit.SECONDS).score());
            assertEquals(new BigDecimal("20.00"), assessment(job).getScore());
            assertEquals(2, calls.get());
            assertEquals(2, jobs.findById(job.getId()).orElseThrow().getRetryCount());
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentRetryHasOneValidClaimAndOneProviderInvocation() throws Exception {
        var job = lesson(AiJobState.ERROR, 1);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        when(provider.grade(job.getTaskContent(), job.getLockedContent()))
                .thenAnswer(
                        call -> {
                            entered.countDown();
                            assertTrue(release.await(15, TimeUnit.SECONDS));
                            return grade(20);
                        });
        var pool = Executors.newSingleThreadExecutor();
        try {
            var first = pool.submit(() -> ai.retry(job.getAssessmentKey(), job.getUserId(), false));
            assertTrue(entered.await(10, TimeUnit.SECONDS));
            assertThrows(
                    BusinessRuleException.class,
                    () -> restarted().retry(job.getAssessmentKey(), job.getUserId(), false));
            assertEquals("PENDING", restarted().process(job.getAssessmentKey()).state());
            assertEquals(
                    AiJobState.PROCESSING, jobs.findById(job.getId()).orElseThrow().getState());
            release.countDown();
            first.get(10, TimeUnit.SECONDS);
            verify(provider).grade(job.getTaskContent(), job.getLockedContent());
            assertEquals(2, jobs.findById(job.getId()).orElseThrow().getRetryCount());
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void completedJobAndLostCallbackReplayOnceIntoResultAndProfile() {
        var f = exam(1);
        var job = examJob(f, 0, AiJobState.COMPLETED, 30);
        properties.setEnabled(false);
        restarted().recoverAiJobs();
        assertEquals(
                "WAITING_GRADING",
                attempts.findById(f.attempt().getId()).orElseThrow().getStatus());
        delivery.recoverPending();
        delivery.recoverPending();
        ai.process(job.getAssessmentKey());
        restarted().recoverAiJobs();
        delivery.recoverPending();
        assertCompletedOnce(f, 30);
        assertEquals(
                "DISPATCHED",
                outbox.findByEventKey(callbackKey(job, "COMPLETED")).orElseThrow().getStatus());
        verifyNoInteractions(provider);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void processingOutboxAfterRestartIsReplayedWithoutDuplicateProfile(boolean legacyJson) {
        var f = exam(1);
        var job = examJob(f, 0, AiJobState.COMPLETED, 30);
        delivery.ensureAiAssessmentCompleted(event(job));
        var row = outbox.findByEventKey(callbackKey(job, "COMPLETED")).orElseThrow();
        if (legacyJson) row.setPayload(json.writeValueAsString(event(job)));
        row.setStatus("PROCESSING");
        row.setNextAttemptAt(LocalDateTime.now().minusSeconds(1));
        outbox.saveAndFlush(row);
        restarted().recoverAiJobs();
        delivery.recoverPending();
        delivery.recoverPending();
        assertCompletedOnce(f, 30);
        verifyNoInteractions(provider);
    }

    @Test
    void callbackBackfillDoesNotKeepScanningTheSameFiftyDeliveredJobs() {
        var f = exam(1);
        AiAssessmentJob last = null;
        for (int n = 0; n < 51; n++) {
            last = examJob(f, 0, AiJobState.COMPLETED, 20);
            if (n < 50) {
                delivery.ensureAiAssessmentCompleted(event(last));
                var row = outbox.findByEventKey(callbackKey(last, "COMPLETED")).orElseThrow();
                row.setStatus("DISPATCHED");
                outbox.saveAndFlush(row);
            }
        }
        assertEquals(
                List.of(last.getAssessmentKey()),
                jobs.findCompletedWithoutCallback(PageRequest.of(0, 50)));
        restarted().recoverAiJobs();
        assertTrue(outbox.findByEventKey(callbackKey(last, "COMPLETED")).isPresent());
        assertTrue(jobs.findCompletedWithoutCallback(PageRequest.of(0, 50)).isEmpty());
        verifyNoInteractions(provider);
    }

    @Test
    void concurrentCallbackBackfillCreatesOneOutboxRecord() throws Exception {
        var f = exam(1);
        var job = examJob(f, 0, AiJobState.COMPLETED, 30);
        var pool = Executors.newFixedThreadPool(2);
        try {
            for (var task :
                    pool.invokeAll(
                            List.<Callable<Void>>of(
                                    () -> {
                                        restarted().recoverAiJobs();
                                        return null;
                                    },
                                    () -> {
                                        restarted().recoverAiJobs();
                                        return null;
                                    }))) task.get(10, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        assertEquals(
                1,
                outbox.findAll().stream()
                        .filter(x -> callbackKey(job, "COMPLETED").equals(x.getEventKey()))
                        .count());
        var queued = outbox.findByEventKey(callbackKey(job, "COMPLETED")).orElseThrow();
        assertEquals("PENDING", queued.getStatus());
        // Simulate the next scheduler tick explicitly. Database timestamp rounding and
        // the Windows clock must not decide whether an immediately created row is due.
        queued.setNextAttemptAt(LocalDateTime.now().minusSeconds(1));
        outbox.saveAndFlush(queued);
        assertTrue(
                outbox
                        .findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(
                                List.of("PENDING", "PROCESSING"), LocalDateTime.now())
                        .stream()
                        .anyMatch(row -> row.getId().equals(queued.getId())),
                () -> "Callback not due: " + queued.getNextAttemptAt());
        delivery.recoverPending();
        assertCompletedOnce(f, 30);
        verifyNoInteractions(provider);
    }

    private void assertCompletedOnce(Exam f, int score) {
        assertEquals("COMPLETED", attempts.findById(f.attempt().getId()).orElseThrow().getStatus());
        assertEquals(BigDecimal.valueOf(score).setScale(2), result(f).getRawScore());
        assertEquals(1, results.findByAttemptIdOrderBySkillAsc(f.attempt().getId()).size());
        assertEquals(
                "A1",
                profiles.findByUserIdAndSkill(f.user().getId(), "WRITING")
                        .orElseThrow()
                        .getCurrentLevel());
        assertEquals(
                1,
                applications.findAll().stream()
                        .filter(x -> x.getAttemptId().equals(f.attempt().getId()))
                        .count());
        assertEquals(
                1,
                outbox.findAll().stream()
                        .filter(
                                x ->
                                        x.getEventKey()
                                                .equals(
                                                        "attempt:"
                                                                + f.attempt().getId()
                                                                + ":completed"))
                        .count());
    }

    private AiAssessmentServiceImpl restarted() {
        return new AiAssessmentServiceImpl(
                jobs,
                assessments,
                userService,
                provider,
                provider,
                mock(com.aptisprep.service.storage.StorageService.class),
                transactionManager,
                events,
                delivery,
                properties);
    }

    private ExamSkillResultEntity result(Exam f) {
        return results.findByAttemptIdAndSkill(f.attempt().getId(), "WRITING").orElseThrow();
    }

    private GradingResultDTO grade(int score) {
        return new GradingResultDTO(
                BigDecimal.valueOf(score), "A1", "Feedback", "mock", "test-model");
    }

    private AiAssessment assessment(AiAssessmentJob job) {
        return assessments
                .findFirstByJobIdAndAnswerVersionOrderByIdDesc(job.getId(), job.getAnswerVersion())
                .orElseThrow();
    }

    private String callbackKey(AiAssessmentJob job, String state) {
        return "ai:" + job.getAssessmentKey() + ":v" + job.getAnswerVersion() + ":" + state;
    }

    private AiAssessmentCompletedEvent event(AiAssessmentJob job) {
        return new AiAssessmentCompletedEvent(
                job.getAssessmentKey(),
                job.getUserId(),
                job.getAttemptId(),
                job.getAnswerId(),
                job.getState().name(),
                job.getLastError(),
                job.getAnswerVersion());
    }

    private void age(AiAssessmentJob job) {
        jdbc.update(
                "update ai_assessment_jobs set updated_at = ? where id = ?",
                LocalDateTime.now().minusMinutes(30),
                job.getId());
    }

    private User user() {
        var value = new User();
        value.setFullName("AI regression learner");
        value.setEmail(UUID.randomUUID() + "@test.local");
        value.setPasswordHash("unused");
        value.setRole(Role.STUDENT);
        value.setStatus(UserStatus.ACTIVE);
        return users.saveAndFlush(value);
    }

    private AiAssessmentJob lesson(AiJobState state, int retry) {
        var job = base(user().getId(), state);
        job.setSourceType("LESSON_SUBMISSION");
        job.setSourceId(42);
        job.setRetryCount(retry);
        if (state == AiJobState.PROCESSING) job.setProcessingToken("old-token");
        return persist(job, null);
    }

    private AiAssessmentJob collidingLesson(Exam f, AiJobState state, Integer score) {
        var job = base(f.user().getId(), state);
        job.setSourceType("LESSON_SUBMISSION");
        job.setSourceId(42);
        job.setAttemptId(f.attempt().getId());
        job.setAnswerId(f.answers().getFirst().getId());
        job.setTaskContent("Lesson only");
        job.setLockedContent("Lesson answer");
        return persist(job, score);
    }

    private AiAssessmentJob examJob(Exam f, int answer, AiJobState state, Integer score) {
        var job = base(f.user().getId(), state);
        job.setSourceType("EXAM_ANSWER");
        job.setAttemptId(f.attempt().getId());
        job.setAnswerId(f.answers().get(answer).getId());
        job.setSourceId(job.getAnswerId());
        return persist(job, score);
    }

    private AiAssessmentJob base(int user, AiJobState state) {
        var job = new AiAssessmentJob();
        job.setAssessmentKey("recovery-" + UUID.randomUUID());
        job.setUserId(user);
        job.setAnswerVersion(1);
        job.setPolicyVersion("p1");
        job.setRubricVersion("r1");
        job.setSkill("WRITING");
        job.setTaskType("WRITING");
        job.setTaskContent("Task " + UUID.randomUUID());
        job.setLockedContent("Saved answer " + UUID.randomUUID());
        job.setMaxScore(BigDecimal.valueOf(50));
        job.setRetryCount(0);
        job.setState(state);
        return job;
    }

    private AiAssessmentJob persist(AiAssessmentJob job, Integer score) {
        job = jobs.saveAndFlush(job);
        var value = new AiAssessment();
        value.setJob(job);
        value.setAssessmentKey(job.getAssessmentKey());
        value.setAnswerId(job.getAnswerId());
        value.setAnswerVersion(job.getAnswerVersion());
        value.setPolicyVersion("p1");
        value.setRubricVersion("r1");
        value.setState(
                job.getState() == AiJobState.COMPLETED
                        ? AiAssessmentState.COMPLETED
                        : job.getState() == AiJobState.ERROR
                                ? AiAssessmentState.ERROR
                                : AiAssessmentState.PENDING);
        if (score != null) {
            value.setScore(BigDecimal.valueOf(score));
            value.setEstimatedCefr("A1");
            value.setFeedback("Feedback");
            value.setProvider("mock");
            value.setModel("test-model");
        }
        assessments.saveAndFlush(value);
        return job;
    }

    private Exam exam(int count) {
        var user = user();
        var certificate = new Certificate();
        certificate.setCode("AI-" + UUID.randomUUID());
        certificate.setName("AI test certificate");
        certificate.setStatus(RecordStatus.ACTIVE);
        certificate = certificates.saveAndFlush(certificate);
        var scheme = new ScoringScheme();
        scheme.setCertificate(certificate);
        scheme.setName("AI test policy");
        scheme.setVersion("p1");
        scheme.setStatus(RecordStatus.ACTIVE);
        scheme = schemes.saveAndFlush(scheme);
        var rule = new ScoringScaleRule();
        rule.setScheme(scheme);
        rule.setSkill("WRITING");
        rule.setRawMin(BigDecimal.ZERO);
        rule.setRawMax(BigDecimal.valueOf(50));
        rule.setScaleMin(BigDecimal.ZERO);
        rule.setScaleMax(BigDecimal.valueOf(50));
        rules.saveAndFlush(rule);
        var band = new ScoreBand();
        band.setScheme(scheme);
        band.setSkill("WRITING");
        band.setCefrLevel("A1");
        band.setMinScore(BigDecimal.ZERO);
        band.setMaxScore(BigDecimal.valueOf(50));
        bands.saveAndFlush(band);
        var exam = new MockExamEntity();
        exam.setCertificateId(certificate.getId());
        exam.setScoringSchemeId(scheme.getId());
        exam.setTitle("AI recovery exam");
        exam.setExamType("PLACEMENT");
        exam.setSkill("WRITING");
        exam.setStatus("PUBLISHED");
        exam = exams.saveAndFlush(exam);
        var attempt = new ExamAttemptEntity();
        attempt.setUserId(user.getId());
        attempt.setMockExamId(exam.getId());
        attempt.setScoringSchemeId(scheme.getId());
        attempt.setPolicyVersion("p1");
        attempt.setRubricVersion("r1");
        attempt.setAnswerVersion(1);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setStatus("WAITING_GRADING");
        attempt.setGradingStatus("WAITING");
        attempt = attempts.saveAndFlush(attempt);
        var answerList = new ArrayList<UserAnswerEntity>();
        for (int n = 0; n < count; n++) {
            var question = new QuestionEntity();
            question.setContent("Task " + n);
            question.setSkill("WRITING");
            question.setQuestionType("WRITING_TEXT");
            question.setStatus("PUBLISHED");
            question = questions.saveAndFlush(question);
            var answer = new UserAnswerEntity();
            answer.setAttemptId(attempt.getId());
            answer.setQuestionId(question.getId());
            answer.setStatus("ANSWERED");
            answer.setTextAnswer("Saved writing");
            answerList.add(answers.saveAndFlush(answer));
        }
        var result = new ExamSkillResultEntity();
        result.setAttemptId(attempt.getId());
        result.setSkill("WRITING");
        result.setGradingStatus("WAITING");
        results.saveAndFlush(result);
        return new Exam(user, attempt, answerList);
    }

    private record Exam(User user, ExamAttemptEntity attempt, List<UserAnswerEntity> answers) {}
}
