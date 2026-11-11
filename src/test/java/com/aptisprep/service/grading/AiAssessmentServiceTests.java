package com.aptisprep.service.grading;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.aptisprep.dto.AiAssessmentDTO.*;
import com.aptisprep.dto.GradingResultDTO;
import com.aptisprep.entity.*;
import com.aptisprep.entity.User;
import com.aptisprep.enums.*;
import com.aptisprep.event.AiAssessmentCompletedEvent;
import com.aptisprep.exception.AiGradingException;
import com.aptisprep.repository.*;
import com.aptisprep.service.account.UserService;
import com.aptisprep.service.exam.AttemptOutboxService;
import com.aptisprep.service.grading.impl.AiAssessmentServiceImpl;
import com.aptisprep.service.storage.StorageService;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AiAssessmentServiceTests {
    @Mock AiAssessmentJobRepository jobs;
    @Mock AiAssessmentRepository assessments;
    @Mock UserService users;
    @Mock WritingGradingService writing;
    @Mock SpeakingGradingService speaking;
    @Mock StorageService storage;
    @Mock ApplicationEventPublisher events;
    @Mock AttemptOutboxService outbox;

    private AiAssessmentService service;
    private AiAssessmentJob job;
    private AiAssessment assessment;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new JdbcDataSource();
        ((JdbcDataSource) dataSource).setURL("jdbc:h2:mem:ai_tx;DB_CLOSE_DELAY=-1");
        service =
                new AiAssessmentServiceImpl(
                        jobs,
                        assessments,
                        users,
                        writing,
                        speaking,
                        storage,
                        new DataSourceTransactionManager(dataSource),
                        events,
                        outbox,
                        null);
        job = new AiAssessmentJob();
        job.setAssessmentKey("assessment-1");
        job.setUserId(1);
        job.setAttemptId(2);
        job.setAnswerId(3);
        job.setSourceId(3);
        job.setAnswerVersion(1);
        job.setSkill("WRITING");
        job.setTaskType("WRITING");
        job.setTaskContent("Write an email");
        job.setLockedContent("Dear team");
        job.setRubricVersion("r1");
        job.setPolicyVersion("p1");
        job.setMaxScore(new BigDecimal("50"));
        job.setState(AiJobState.PENDING);
        assessment = new AiAssessment();
        assessment.setJob(job);
        assessment.setAssessmentKey("assessment-1");
        assessment.setAnswerId(3);
        assessment.setAnswerVersion(1);
        assessment.setState(AiAssessmentState.PENDING);
        assessment.setRubricVersion("r1");
        assessment.setPolicyVersion("p1");
        lenient().when(jobs.findByAssessmentKey("assessment-1")).thenReturn(Optional.of(job));
        lenient()
                .when(jobs.findByAssessmentKeyForUpdate("assessment-1"))
                .thenReturn(Optional.of(job));
        lenient()
                .when(jobs.save(any(AiAssessmentJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient()
                .when(assessments.findFirstByJobIdAndAnswerVersionOrderByIdDesc(any(), eq(1)))
                .thenReturn(Optional.of(assessment));
        lenient()
                .when(assessments.save(any(AiAssessment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        User owner = new User();
        owner.setId(1);
        owner.setRole(Role.STUDENT);
        owner.setStatus(UserStatus.ACTIVE);
        lenient().when(users.findById(1)).thenReturn(Optional.of(owner));
    }

    @Test
    void successfulJobIsPersistedAndSecondProcessDoesNotCallProvider() {
        when(writing.grade("Write an email", "Dear team"))
                .thenReturn(
                        new GradingResultDTO(
                                new BigDecimal("32.5"),
                                "B1",
                                "Gợi ý rõ ràng",
                                "mock",
                                "test-model"));

        AssessmentView first = service.process("assessment-1");
        AssessmentView second = service.process("assessment-1");

        assertEquals("COMPLETED", first.state());
        assertEquals(new BigDecimal("32.5"), first.score());
        assertEquals("B1", first.estimatedCefr());
        assertEquals(first.state(), second.state());
        verify(writing, times(1)).grade("Write an email", "Dear team");
        verify(events, times(2)).publishEvent(any(AiAssessmentCompletedEvent.class));
    }

    @Test
    void providerErrorKeepsScoreNullAndRetryCanRunWithLimit() {
        when(writing.grade("Write an email", "Dear team"))
                .thenThrow(new AiGradingException("Dịch vụ chấm tạm thời không khả dụng."))
                .thenReturn(
                        new GradingResultDTO(
                                new BigDecimal("30"), "B1", "Feedback", "mock", "test-model"));

        AssessmentView failed = service.process("assessment-1");
        assertEquals("ERROR", failed.state());
        assertNull(failed.score());
        assertNotNull(failed.errorMessage());
        var eventCaptor = org.mockito.ArgumentCaptor.forClass(AiAssessmentCompletedEvent.class);
        verify(events).publishEvent(eventCaptor.capture());
        assertEquals("ERROR", eventCaptor.getAllValues().getFirst().state());
        verify(outbox).enqueueAiAssessmentCompleted(eventCaptor.getValue());

        AssessmentView retried = service.retry("assessment-1", 1, false);
        assertEquals("COMPLETED", retried.state());
        assertEquals(new BigDecimal("30"), retried.score());
        verify(writing, times(2)).grade("Write an email", "Dear team");
        var retriedEvents = org.mockito.ArgumentCaptor.forClass(AiAssessmentCompletedEvent.class);
        verify(events, times(2)).publishEvent(retriedEvents.capture());
        assertEquals(
                java.util.List.of("ERROR", "COMPLETED"),
                retriedEvents.getAllValues().stream()
                        .map(AiAssessmentCompletedEvent::state)
                        .toList());
        verify(outbox).enqueueAiAssessmentCompleted(retriedEvents.getAllValues().getLast());
        assertEquals(2, job.getRetryCount());
        assertNull(job.getProcessingToken());
        verifyNoMoreInteractions(events, outbox);
    }

    @Test
    void speakingUsesTheLockedTranscriptPipeline() {
        job.setSkill("SPEAKING");
        job.setTaskType("SPEAKING");
        job.setTaskContent("Talk about hobbies");
        job.setTranscript("I enjoy reading books.");
        when(speaking.gradeTranscript("Talk about hobbies", "I enjoy reading books."))
                .thenReturn(
                        new GradingResultDTO(
                                new BigDecimal("30"),
                                "B1",
                                "Chưa đánh giá phát âm.",
                                "mock",
                                "test-model",
                                "I enjoy reading books."));

        AssessmentView result = service.process("assessment-1");

        assertEquals("COMPLETED", result.state());
        assertEquals("I enjoy reading books.", result.transcript());
        verify(speaking).gradeTranscript("Talk about hobbies", "I enjoy reading books.");
    }

    @Test
    void retryTranscribesSavedExamAudioAndKeepsTheItemLabel() {
        job.setSkill("SPEAKING");
        job.setTaskType("SPEAKING");
        job.setTaskContent("Talk about hobbies");
        job.setLockedContent("Mục 1:\n[Chưa trả lời]\n\nMục 2:\nattempts/2/audio/test.webm");
        Path audio = Path.of("attempts/2/audio/test.webm");
        when(storage.localPath("attempts/2/audio/test.webm")).thenReturn(audio);
        when(speaking.transcribe(audio))
                .thenThrow(new AiGradingException("Transcription failed"))
                .thenReturn("I enjoy reading.");
        String transcript = "Mục 2:\nI enjoy reading.";
        when(speaking.gradeTranscript("Talk about hobbies", transcript))
                .thenReturn(
                        new GradingResultDTO(
                                BigDecimal.TEN,
                                "A1",
                                "Feedback",
                                "mock",
                                "test-model",
                                transcript));

        assertEquals("ERROR", service.process("assessment-1").state());
        verify(speaking, never()).gradeTranscript(anyString(), anyString());
        assertEquals(transcript, service.retry("assessment-1", 1, false).transcript());
        assertEquals("COMPLETED", service.process("assessment-1").state());
        verify(speaking, times(2)).transcribe(audio);
        verify(speaking).gradeTranscript("Talk about hobbies", transcript);
    }

    @Test
    void staleProcessingJobIsReclaimedAfterRestart() {
        job.setState(AiJobState.PROCESSING);
        job.setProcessingToken("old-token");
        job.setRetryCount(1);
        ReflectionTestUtils.setField(job, "updatedAt", LocalDateTime.now().minusMinutes(30));
        when(writing.grade("Write an email", "Dear team"))
                .thenReturn(
                        new GradingResultDTO(
                                new BigDecimal("30"), "B1", "Feedback", "mock", "test-model"));

        AssessmentView result = service.process("assessment-1");

        assertEquals("COMPLETED", result.state());
        assertNull(job.getProcessingToken());
        verify(writing).grade("Write an email", "Dear team");
    }

    @Test
    void requestWithSameKeyReturnsExistingAssessmentWithoutCreatingAnotherJob() {
        User user = new User();
        user.setId(1);
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        when(users.findById(1)).thenReturn(Optional.of(user));
        job.setAssessmentKey("new-key");
        job.setTaskContent("Write");
        job.setLockedContent("Answer");
        when(jobs.findByAssessmentKeyForUpdate("new-key"))
                .thenReturn(Optional.empty(), Optional.of(job));
        when(assessments.findFirstByJobIdAndAnswerVersionOrderByIdDesc(any(), eq(1)))
                .thenReturn(Optional.of(assessment));

        AssessmentRequest request =
                new AssessmentRequest(
                        "new-key",
                        1,
                        2,
                        3,
                        1,
                        "WRITING",
                        "WRITING",
                        "Write",
                        "Answer",
                        "r1",
                        "p1",
                        new BigDecimal("50"),
                        null);
        service.request(request);
        service.request(request);

        verify(jobs, times(1)).save(any(AiAssessmentJob.class));
        verify(assessments, times(1)).save(any(AiAssessment.class));
    }

    @Test
    void acceptsTwoWritingItemsWhenEachItemIsWithinLimit() {
        User user = new User();
        user.setId(1);
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        when(users.findById(1)).thenReturn(Optional.of(user));
        when(jobs.findByAssessmentKeyForUpdate("two-items")).thenReturn(Optional.empty());

        String first = "a".repeat(11_000);
        String second = "b".repeat(11_000);
        AssessmentRequest request =
                new AssessmentRequest(
                        "two-items",
                        1,
                        2,
                        3,
                        1,
                        "WRITING",
                        "WRITING",
                        "Write message one\nWrite message two",
                        first + "\n\n" + second,
                        "r1",
                        "p1",
                        new BigDecimal("50"),
                        null);

        assertDoesNotThrow(() -> service.request(request));
        verify(jobs).save(any(AiAssessmentJob.class));
    }

    @Test
    void rejectsPayloadAfterTaskAndSubmissionAreCombined() {
        AssessmentRequest request =
                new AssessmentRequest(
                        "too-large",
                        1,
                        2,
                        3,
                        1,
                        "WRITING",
                        "WRITING",
                        "t".repeat(50_000),
                        "a".repeat(50_001),
                        "r1",
                        "p1",
                        new BigDecimal("50"),
                        null);

        assertThrows(
                com.aptisprep.exception.BusinessRuleException.class,
                () -> service.request(request));
        verifyNoInteractions(jobs, assessments);

        AssessmentRequest combined =
                new AssessmentRequest(
                        "combined-too-large",
                        1,
                        2,
                        3,
                        1,
                        "WRITING",
                        "WRITING",
                        "t".repeat(25_000),
                        "a".repeat(25_001),
                        "r1",
                        "p1",
                        new BigDecimal("50"),
                        null);
        assertThrows(
                com.aptisprep.exception.BusinessRuleException.class,
                () -> service.request(combined));
        verifyNoInteractions(jobs, assessments);
    }

    @Test
    void lessonFeedbackNeverPublishesAnExamEventEvenWithCollidingIds() {
        job.setSourceType("LESSON_SUBMISSION");
        job.setSourceId(3);
        when(writing.grade("Write an email", "Dear team"))
                .thenReturn(
                        new GradingResultDTO(
                                new BigDecimal("32.5"), "B1", "Feedback", "mock", "test-model"));
        assertEquals("COMPLETED", service.process("assessment-1").state());
        verifyNoInteractions(events);
    }

    @Test
    void lessonFailureDoesNotPublishAnExamFailure() {
        job.setSourceType("LESSON_SUBMISSION");
        job.setSourceId(3);
        when(writing.grade("Write an email", "Dear team"))
                .thenThrow(new AiGradingException("Unavailable"));
        assertEquals("ERROR", service.process("assessment-1").state());
        verifyNoInteractions(events);
    }

    @Test
    void nonOwnerCannotReadAndStudentCannotUseAdminFlag() {
        User other = new User();
        other.setId(9);
        other.setRole(Role.STUDENT);
        other.setStatus(UserStatus.ACTIVE);
        when(users.findById(9)).thenReturn(Optional.of(other));
        assertThrows(
                com.aptisprep.exception.AccessDeniedException.class,
                () -> service.get("assessment-1", 9, false));
        assertThrows(
                com.aptisprep.exception.AccessDeniedException.class,
                () -> service.get("assessment-1", 1, true));
    }

    @Test
    void legacyLessonRequestCanBeReusedWithoutInventingExamIds() {
        job.setSourceType("LESSON_SUBMISSION");
        job.setSourceId(42);
        job.setMaxScore(new BigDecimal("50.00"));
        service.request(
                new AssessmentRequest(
                        "assessment-1",
                        1,
                        null,
                        null,
                        1,
                        "WRITING",
                        "WRITING",
                        "Write an email",
                        "Dear team",
                        "r1",
                        "p1",
                        new BigDecimal("50"),
                        null,
                        "LESSON_SUBMISSION",
                        42));
        verify(jobs, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void newLessonJobPersistsItsSourceAndKeepsExamIdsNull() {
        when(jobs.findByAssessmentKeyForUpdate("lesson-new")).thenReturn(Optional.empty());
        service.request(
                new AssessmentRequest(
                        "lesson-new",
                        1,
                        null,
                        null,
                        1,
                        "WRITING",
                        "WRITING",
                        "Write an email",
                        "Dear team",
                        "r1",
                        "p1",
                        new BigDecimal("50"),
                        null,
                        "LESSON_SUBMISSION",
                        42));
        var captured = org.mockito.ArgumentCaptor.forClass(AiAssessmentJob.class);
        verify(jobs).save(captured.capture());
        assertEquals("LESSON_SUBMISSION", captured.getValue().getSourceType());
        assertEquals(42, captured.getValue().getSourceId());
        assertNull(captured.getValue().getAttemptId());
        assertNull(captured.getValue().getAnswerId());
        verifyNoInteractions(events);
    }
}
