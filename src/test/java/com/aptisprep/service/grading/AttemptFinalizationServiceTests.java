package com.aptisprep.service.grading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.aptisprep.entity.AiAssessment;
import com.aptisprep.entity.AiAssessmentJob;
import com.aptisprep.entity.ExamAttemptEntity;
import com.aptisprep.entity.ExamSkillResultEntity;
import com.aptisprep.entity.QuestionEntity;
import com.aptisprep.entity.UserAnswerEntity;
import com.aptisprep.enums.AiAssessmentState;
import com.aptisprep.enums.AiJobState;
import com.aptisprep.event.AiAssessmentCompletedEvent;
import com.aptisprep.repository.AiAssessmentJobRepository;
import com.aptisprep.repository.AiAssessmentRepository;
import com.aptisprep.repository.ExamAttemptRepository;
import com.aptisprep.repository.ExamAttemptSnapshotRepository;
import com.aptisprep.repository.ExamSkillResultRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.repository.QuestionRepository;
import com.aptisprep.repository.UserAnswerRepository;
import com.aptisprep.service.contract.AiAssessmentContract;
import com.aptisprep.service.contract.ScoringPolicyContract;
import com.aptisprep.service.exam.AttemptOutboxService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.h2.jdbcx.JdbcDataSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AttemptFinalizationServiceTests {
    @Mock ExamAttemptRepository attempts;
    @Mock ExamSkillResultRepository skillResults;
    @Mock MockExamRepository exams;
    @Mock UserAnswerRepository answers;
    @Mock QuestionRepository questions;
    @Mock ExamAttemptSnapshotRepository snapshots;
    @Mock AiAssessmentContract ai;
    @Mock AiAssessmentJobRepository jobs;
    @Mock AiAssessmentRepository assessments;
    @Mock ScoringPolicyContract scoring;
    @Mock AttemptOutboxService outbox;
    @Mock ApplicationEventPublisher events;

    private AttemptFinalizationService service;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:finalization_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        service = new AttemptFinalizationService(attempts, skillResults, exams, answers, questions, snapshots,
                ai, jobs, assessments, scoring, outbox, events, new DataSourceTransactionManager(dataSource));
    }

    @Test
    void failedAnswerOfSameSkillStaysRetryableAfterAnotherAnswerSucceeds() {
        ExamAttemptEntity attempt = new ExamAttemptEntity();
        attempt.setUserId(2); attempt.setMockExamId(99); attempt.setStatus("WAITING_GRADING");
        attempt.setAnswerVersion(2); attempt.setScoringSchemeId(1); attempt.setPolicyVersion("p1"); attempt.setRubricVersion("r1");
        ReflectionTestUtils.setField(attempt, "id", 1);

        UserAnswerEntity failedAnswer = answer(10, 100);
        UserAnswerEntity successfulAnswer = answer(11, 101);
        QuestionEntity failedQuestion = question(100);
        QuestionEntity successfulQuestion = question(101);
        ExamSkillResultEntity result = new ExamSkillResultEntity();
        result.setAttemptId(1); result.setSkill("WRITING");

        AiAssessmentJob failedJob = job(20, "failed", AiJobState.ERROR);
        AiAssessmentJob successfulJob = job(21, "successful", AiJobState.COMPLETED);
        AiAssessment failedAssessment = assessment(failedJob, AiAssessmentState.ERROR, null);
        AiAssessment successfulAssessment = assessment(successfulJob, AiAssessmentState.COMPLETED, new BigDecimal("35"));

        when(attempts.findByIdAndUserIdForUpdate(1, 2)).thenReturn(Optional.of(attempt));
        when(answers.findByIdAndAttemptId(11, 1)).thenReturn(Optional.of(successfulAnswer));
        when(answers.findByAttemptIdOrderByQuestionIdAsc(1)).thenReturn(List.of(failedAnswer, successfulAnswer));
        when(questions.findById(100)).thenReturn(Optional.of(failedQuestion));
        when(questions.findById(101)).thenReturn(Optional.of(successfulQuestion));
        when(skillResults.findByAttemptIdAndSkill(1, "WRITING")).thenReturn(Optional.of(result));
        when(jobs.findByAttemptIdAndAnswerVersion(1, 2)).thenReturn(List.of(failedJob, successfulJob));
        when(assessments.findFirstByJobIdAndAnswerVersionOrderByIdDesc(20, 2)).thenReturn(Optional.of(failedAssessment));
        when(assessments.findFirstByJobIdAndAnswerVersionOrderByIdDesc(21, 2)).thenReturn(Optional.of(successfulAssessment));
        when(ai.getForGrading("successful")).thenReturn(new AiAssessmentContract.AssessmentView(
                31, "successful", 11, 2, "COMPLETED", new BigDecimal("35"), "B1", "ok", null,
                "r1", "p1", "mock", "test", null, null, null));

        service.applyAiAssessment(new AiAssessmentCompletedEvent("successful", 2, 1, 11, "COMPLETED", null, 2));

        assertEquals("ERROR", result.getGradingStatus());
        assertEquals("AI_ERROR", result.getGradingMethod());
        assertEquals("ERROR", attempt.getStatus());
        verify(scoring, never()).scale(any(), any(), any());
    }

    private UserAnswerEntity answer(int id, int questionId) {
        UserAnswerEntity answer = new UserAnswerEntity();
        ReflectionTestUtils.setField(answer, "id", id);
        answer.setAttemptId(1); answer.setQuestionId(questionId); answer.setStatus("ANSWERED");
        return answer;
    }

    private QuestionEntity question(int id) {
        QuestionEntity question = new QuestionEntity();
        ReflectionTestUtils.setField(question, "id", id);
        question.setSkill("WRITING"); question.setQuestionType("WRITING_TEXT"); question.setContent("Task");
        return question;
    }

    private AiAssessmentJob job(int id, String key, AiJobState state) {
        AiAssessmentJob job = new AiAssessmentJob();
        ReflectionTestUtils.setField(job, "id", id);
        job.setAssessmentKey(key); job.setAttemptId(1); job.setAnswerVersion(2); job.setAnswerId(id - 10);
        job.setSkill("WRITING"); job.setState(state);
        return job;
    }

    private AiAssessment assessment(AiAssessmentJob job, AiAssessmentState state, BigDecimal score) {
        AiAssessment assessment = new AiAssessment();
        assessment.setJob(job); assessment.setAssessmentKey(job.getAssessmentKey()); assessment.setAnswerId(job.getAnswerId());
        assessment.setAnswerVersion(2); assessment.setState(state); assessment.setScore(score);
        return assessment;
    }
}
