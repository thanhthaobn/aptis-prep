package com.aptisprep.mvc.service.exam.impl;

import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.service.exam.*;
import com.aptisprep.mvc.service.grading.AttemptFinalizationService;
import com.aptisprep.mvc.service.grading.ObjectiveGradingService;
import com.aptisprep.mvc.service.scoring.ScoringPolicyService;
import com.aptisprep.mvc.service.storage.StorageService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/** Public attempt façade; workflow details live in the command workflow implementation. */
@Service
public class ExamAttemptServiceImpl extends ExamAttemptWorkflowServiceImpl {
    public ExamAttemptServiceImpl(
            UserRepository userRepository,
            MockExamRepository mockExamRepository,
            MockExamQuestionRepository mockExamQuestionRepository,
            QuestionMediaRepository questionMediaRepository,
            UserMediaActivityRepository userMediaActivityRepository,
            ExamSnapshotService examSnapshots,
            ExamGradingPayloadService gradingPayloads,
            ExamAttemptRepository examAttemptRepository,
            UserAnswerRepository userAnswerRepository,
            UserAnswerDetailRepository userAnswerDetailRepository,
            ExamSkillResultRepository examSkillResultRepository,
            ScoringPolicyService scoring,
            StorageService storage,
            AttemptOutboxService outbox,
            AttemptFinalizationService finalization,
            ObjectiveGradingService objectiveGrading,
            ApplicationEventPublisher events,
            ExamAnswerService answerWriter,
            ExamReadinessService startValidator,
            CheckpointStartService checkpointValidator,
            ExamAttemptQueryService attemptQueries) {
        super(
                userRepository,
                mockExamRepository,
                mockExamQuestionRepository,
                questionMediaRepository,
                userMediaActivityRepository,
                examSnapshots,
                gradingPayloads,
                examAttemptRepository,
                userAnswerRepository,
                userAnswerDetailRepository,
                examSkillResultRepository,
                scoring,
                storage,
                outbox,
                finalization,
                objectiveGrading,
                events,
                answerWriter,
                startValidator,
                checkpointValidator,
                attemptQueries);
    }
}
