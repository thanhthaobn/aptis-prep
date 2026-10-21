package com.aptisprep.service.exam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aptisprep.entity.Certificate;
import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.entity.MockExamQuestionEntity;
import com.aptisprep.entity.QuestionEntity;
import com.aptisprep.entity.QuestionMediaEntity;
import com.aptisprep.entity.ScoringScheme;
import com.aptisprep.entity.User;
import com.aptisprep.enums.RecordStatus;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.repository.AttemptOutboxRepository;
import com.aptisprep.repository.CertificateRepository;
import com.aptisprep.repository.ExamAttemptRepository;
import com.aptisprep.repository.ExamAttemptSnapshotRepository;
import com.aptisprep.repository.MockExamQuestionRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.repository.QuestionMediaRepository;
import com.aptisprep.repository.QuestionRepository;
import com.aptisprep.repository.ScoringSchemeRepository;
import com.aptisprep.repository.UserRepository;
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
class ExamAttemptConcurrencyTests {
    @Autowired ExamAttemptService attempts;
    @Autowired UserRepository users;
    @Autowired CertificateRepository certificates;
    @Autowired ScoringSchemeRepository schemes;
    @Autowired MockExamRepository exams;
    @Autowired MockExamQuestionRepository examQuestions;
    @Autowired QuestionRepository questions;
    @Autowired QuestionMediaRepository media;
    @Autowired ExamAttemptRepository attemptRepository;
    @Autowired ExamAttemptSnapshotRepository snapshots;
    @Autowired AttemptOutboxRepository outbox;

    @Test
    void concurrentStartWithSameRequestKeyCreatesOneAttemptAndSnapshot() throws Exception {
        Fixture fixture = fixture("start");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> results = pool.invokeAll(List.of(
                    task(() -> attempts.start(fixture.user().getId(), fixture.exam().getId(), "same-key").attemptId()),
                    task(() -> attempts.start(fixture.user().getId(), fixture.exam().getId(), "same-key").attemptId())));
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
        int attemptId = attempts.start(fixture.user().getId(), fixture.exam().getId(), "submit-key").attemptId();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Void>> results = pool.invokeAll(List.of(
                    task(() -> { attempts.submit(fixture.user().getId(), attemptId, "submit-1"); return null; }),
                    task(() -> { attempts.submit(fixture.user().getId(), attemptId, "submit-2"); return null; })));
            results.forEach(this::getUnchecked);
            assertEquals(1, outbox.findByEventKey("attempt:" + attemptId + ":completed").stream().count());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void listeningCannotBePlayedMoreThanTwiceAndExpiredResultRemainsReadable() {
        Fixture fixture = fixture("media");
        int attemptId = attempts.start(fixture.user().getId(), fixture.exam().getId(), "media-key").attemptId();
        assertEquals(1, attempts.playMedia(fixture.user().getId(), attemptId, fixture.media().getId()).playCount());
        assertEquals(2, attempts.playMedia(fixture.user().getId(), attemptId, fixture.media().getId()).playCount());
        assertThrows(BusinessRuleException.class, () -> attempts.playMedia(fixture.user().getId(), attemptId, fixture.media().getId()));

        var attempt = attemptRepository.findById(attemptId).orElseThrow();
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(2));
        attemptRepository.saveAndFlush(attempt);
        assertTrue(attempts.page(fixture.user().getId(), attemptId).deadline().isBefore(java.time.Instant.now()));
        attempts.submit(fixture.user().getId(), attemptId, "expired-submit");
        assertEquals("EXPIRED", attempts.result(fixture.user().getId(), attemptId).state());
    }

    private Fixture fixture(String suffix) {
        User user = new User(); user.setFullName("Concurrent learner"); user.setEmail("attempt-" + suffix + "-" + System.nanoTime() + "@test.local");
        user.setPasswordHash("unused"); user.setRole(Role.STUDENT); user.setStatus(UserStatus.ACTIVE); user = users.saveAndFlush(user);
        Certificate certificate = new Certificate(); certificate.setName("Test certificate"); certificate.setCode("TEST-" + suffix + "-" + System.nanoTime()); certificate.setStatus(RecordStatus.ACTIVE); certificate = certificates.saveAndFlush(certificate);
        ScoringScheme scheme = new ScoringScheme(); scheme.setCertificate(certificate); scheme.setName("Test scheme"); scheme.setVersion("v1"); scheme.setStatus(RecordStatus.ACTIVE); scheme = schemes.saveAndFlush(scheme);
        MockExamEntity exam = new MockExamEntity(); exam.setCertificateId(certificate.getId()); exam.setScoringSchemeId(scheme.getId()); exam.setTitle("Test exam"); exam.setExamType("PRACTICE"); exam.setSkill("LISTENING"); exam.setDurationMinutes(1); exam.setStatus("PUBLISHED"); exam = exams.saveAndFlush(exam);
        QuestionEntity question = new QuestionEntity(); question.setContent("Audio question"); question.setQuestionType("SINGLE_CHOICE"); question.setSkill("LISTENING"); question.setStatus("PUBLISHED"); question = questions.saveAndFlush(question);
        MockExamQuestionEntity link = new MockExamQuestionEntity(); link.setMockExamId(exam.getId()); link.setQuestionId(question.getId()); link.setPosition(1); examQuestions.saveAndFlush(link);
        QuestionMediaEntity audio = new QuestionMediaEntity(); audio.setQuestionId(question.getId()); audio.setMediaType("AUDIO"); audio.setMediaUrl("tests/audio.wav"); audio.setOriginalFilename("audio.wav"); audio.setContentType("audio/wav"); audio.setPosition(1); audio = media.saveAndFlush(audio);
        return new Fixture(user, exam, audio);
    }

    private <T> Callable<T> task(Callable<T> task) { return task; }
    private void getUnchecked(Future<Void> future) { try { future.get(); } catch (Exception exception) { throw new AssertionError(exception); } }
    private record Fixture(User user, MockExamEntity exam, QuestionMediaEntity media) {}
}
