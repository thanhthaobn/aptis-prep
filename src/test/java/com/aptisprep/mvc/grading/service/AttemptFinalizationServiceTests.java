package com.aptisprep.mvc.grading.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aptisprep.mvc.catalog.entity.QuestionEntity;
import com.aptisprep.mvc.catalog.service.ExamCatalogService;
import com.aptisprep.mvc.common.enums.AiAssessmentState;
import com.aptisprep.mvc.common.enums.AiJobState;
import com.aptisprep.mvc.common.event.AiAssessmentCompletedEvent;
import com.aptisprep.mvc.exam.entity.ExamAttemptEntity;
import com.aptisprep.mvc.exam.entity.UserAnswerEntity;
import com.aptisprep.mvc.exam.service.AttemptOutboxService;
import com.aptisprep.mvc.exam.service.ExamGradingPersistenceService;
import com.aptisprep.mvc.grading.dto.AiAssessmentDTO;
import com.aptisprep.mvc.grading.entity.AiAssessment;
import com.aptisprep.mvc.grading.entity.AiAssessmentJob;
import com.aptisprep.mvc.grading.entity.ExamSkillResultEntity;
import com.aptisprep.mvc.grading.repository.AiAssessmentJobRepository;
import com.aptisprep.mvc.grading.repository.AiAssessmentRepository;
import com.aptisprep.mvc.grading.repository.ExamSkillResultRepository;
import com.aptisprep.mvc.grading.service.impl.AttemptFinalizationServiceImpl;
import com.aptisprep.mvc.scoring.service.ScoringPolicyService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
class AttemptFinalizationServiceTests {
    @Mock ExamGradingPersistenceService examGrading;
    @Mock ExamSkillResultRepository skillResults;
    @Mock ExamCatalogService examCatalogService;
    @Mock AiAssessmentService ai;
    @Mock AiAssessmentJobRepository jobs;
    @Mock AiAssessmentRepository assessments;
    @Mock ScoringPolicyService scoring;
    @Mock AttemptOutboxService outbox;
    @Mock ApplicationEventPublisher events;

    private AttemptFinalizationService service;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:finalization_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        service =
                new AttemptFinalizationServiceImpl(
                        examGrading,
                        skillResults,
                        examCatalogService,
                        ai,
                        jobs,
                        assessments,
                        scoring,
                        outbox,
                        events,
                        new DataSourceTransactionManager(dataSource));
    }

    @Test
    void failedAnswerOfSameSkillStaysRetryableAfterAnotherAnswerSucceeds() {
        ExamAttemptEntity attempt = new ExamAttemptEntity();
        attempt.setUserId(2);
        attempt.setMockExamId(99);
        attempt.setStatus("WAITING_GRADING");
        attempt.setAnswerVersion(2);
        attempt.setScoringSchemeId(1);
        attempt.setPolicyVersion("p1");
        attempt.setRubricVersion("r1");
        ReflectionTestUtils.setField(attempt, "id", 1);

        UserAnswerEntity failedAnswer = answer(10, 100);
        UserAnswerEntity successfulAnswer = answer(11, 101);
        QuestionEntity failedQuestion = question(100);
        QuestionEntity successfulQuestion = question(101);
        ExamSkillResultEntity result = new ExamSkillResultEntity();
        result.setAttemptId(1);
        result.setSkill("WRITING");

        AiAssessmentJob failedJob = job(20, "failed", AiJobState.ERROR);
        AiAssessmentJob successfulJob = job(21, "successful", AiJobState.COMPLETED);
        AiAssessment failedAssessment = assessment(failedJob, AiAssessmentState.ERROR, null);
        AiAssessment successfulAssessment =
                assessment(successfulJob, AiAssessmentState.COMPLETED, new BigDecimal("35"));

        when(examGrading.findAttemptByIdAndUserIdForUpdate(1, 2)).thenReturn(Optional.of(attempt));
        when(jobs.findByAssessmentKey("successful")).thenReturn(Optional.of(successfulJob));
        when(examGrading.findAnswerByIdAndAttemptId(10, 1)).thenReturn(Optional.of(failedAnswer));
        when(examGrading.findAnswerByIdAndAttemptId(11, 1))
                .thenReturn(Optional.of(successfulAnswer));
        when(examGrading.findAnswersByAttemptId(1))
                .thenReturn(List.of(failedAnswer, successfulAnswer));
        when(examCatalogService.findQuestionById(100)).thenReturn(Optional.of(failedQuestion));
        when(examCatalogService.findQuestionById(101)).thenReturn(Optional.of(successfulQuestion));
        when(skillResults.findByAttemptIdAndSkill(1, "WRITING")).thenReturn(Optional.of(result));
        when(jobs.findExamJobs(2, 1, 2, "p1", "r1")).thenReturn(List.of(failedJob, successfulJob));
        when(assessments.findFirstByJobIdAndAnswerVersionOrderByIdDesc(20, 2))
                .thenReturn(Optional.of(failedAssessment));
        when(assessments.findFirstByJobIdAndAnswerVersionOrderByIdDesc(21, 2))
                .thenReturn(Optional.of(successfulAssessment));
        when(ai.getForGrading("successful"))
                .thenReturn(
                        new AiAssessmentDTO.AssessmentView(
                                31,
                                "successful",
                                11,
                                2,
                                "COMPLETED",
                                new BigDecimal("35"),
                                "B1",
                                "ok",
                                null,
                                "r1",
                                "p1",
                                "mock",
                                "test",
                                null,
                                null,
                                null));

        service.applyAiAssessment(
                new AiAssessmentCompletedEvent("successful", 2, 1, 11, "COMPLETED", null, 2));

        assertEquals("ERROR", result.getGradingStatus());
        assertEquals("AI_ERROR", result.getGradingMethod());
        assertEquals("ERROR", attempt.getStatus());
        verify(scoring, never()).scale(any(), any(), any());
    }

    @Test
    void lessonCallbackWithCollidingExamIdsCannotChangeExam() {
        AiAssessmentJob lessonJob = job(20, "lesson", AiJobState.ERROR);
        lessonJob.setSourceType("LESSON_SUBMISSION");
        when(jobs.findByAssessmentKey("lesson")).thenReturn(Optional.of(lessonJob));

        service.applyAiAssessment(
                new AiAssessmentCompletedEvent("lesson", 2, 1, 10, "ERROR", "failed", 2));

        verifyNoInteractions(examGrading, skillResults, ai);
    }

    @Test
    void retryDoesNotSelectLessonFailureWithMatchingLegacyMetadata() {
        ExamAttemptEntity attempt = new ExamAttemptEntity();
        attempt.setUserId(2);
        attempt.setStatus("ERROR");
        attempt.setAnswerVersion(2);
        attempt.setPolicyVersion("p1");
        attempt.setRubricVersion("r1");
        ReflectionTestUtils.setField(attempt, "id", 1);
        AiAssessmentJob lessonJob = job(20, "lesson", AiJobState.ERROR);
        lessonJob.setSourceType("LESSON_SUBMISSION");
        when(examGrading.findAttemptByIdAndUserIdForUpdate(1, 2)).thenReturn(Optional.of(attempt));
        when(jobs.findExamJobsByState(2, 1, 2, "p1", "r1", AiJobState.ERROR))
                .thenReturn(List.of(lessonJob));

        assertThrows(
                com.aptisprep.mvc.common.exception.BusinessRuleException.class,
                () -> service.retryFailedGrading(2, 1));
        verify(ai, never()).retry(anyString(), anyInt(), anyBoolean());
    }

    private UserAnswerEntity answer(int id, int questionId) {
        UserAnswerEntity answer = new UserAnswerEntity();
        ReflectionTestUtils.setField(answer, "id", id);
        answer.setAttemptId(1);
        answer.setQuestionId(questionId);
        answer.setStatus("ANSWERED");
        return answer;
    }

    private QuestionEntity question(int id) {
        QuestionEntity question = new QuestionEntity();
        ReflectionTestUtils.setField(question, "id", id);
        question.setSkill("WRITING");
        question.setQuestionType("WRITING_TEXT");
        return question;
    }

    private AiAssessmentJob job(int id, String key, AiJobState state) {
        AiAssessmentJob job = new AiAssessmentJob();
        ReflectionTestUtils.setField(job, "id", id);
        job.setAssessmentKey(key);
        job.setUserId(2);
        job.setAttemptId(1);
        job.setAnswerId(id == 20 ? 10 : 11);
        job.setAnswerVersion(2);
        job.setSkill("WRITING");
        job.setSourceType("EXAM_ANSWER");
        job.setSourceId(id == 20 ? 10 : 11);
        job.setPolicyVersion("p1");
        job.setRubricVersion("r1");
        job.setState(state);
        return job;
    }

    private AiAssessment assessment(
            AiAssessmentJob job, AiAssessmentState state, BigDecimal score) {
        AiAssessment assessment = new AiAssessment();
        assessment.setJob(job);
        assessment.setAssessmentKey(job.getAssessmentKey());
        assessment.setAnswerId(job.getAnswerId());
        assessment.setAnswerVersion(job.getAnswerVersion());
        assessment.setPolicyVersion("p1");
        assessment.setRubricVersion("r1");
        assessment.setState(state);
        assessment.setScore(score);
        return assessment;
    }
}
