package com.aptisprep.service.exam;

import com.aptisprep.config.AiProperties;
import com.aptisprep.entity.QuestionEntity;
import com.aptisprep.entity.UserAnswerEntity;
import com.aptisprep.event.AiAssessmentCompletedEvent;
import com.aptisprep.event.AttemptSubmittedEvent;
import com.aptisprep.repository.QuestionRepository;
import com.aptisprep.repository.UserAnswerRepository;
import com.aptisprep.service.contract.AiAssessmentContract;
import java.math.BigDecimal;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AttemptGradingOrchestrationService {
    private final AiProperties properties;
    private final AiAssessmentContract ai;
    private final QuestionRepository questions;
    private final UserAnswerRepository answers;
    private final ExamAttemptService attempts;

    public AttemptGradingOrchestrationService(AiProperties properties, AiAssessmentContract ai,
            QuestionRepository questions, UserAnswerRepository answers, ExamAttemptService attempts) {
        this.properties = properties; this.ai = ai; this.questions = questions; this.answers = answers; this.attempts = attempts;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmitted(AttemptSubmittedEvent event) {
        if (!properties.isEnabled()) return;
        for (UserAnswerEntity answer : answers.findByAttemptIdOrderByQuestionIdAsc(event.attemptId())) {
            QuestionEntity question = questions.findById(answer.getQuestionId()).orElse(null);
            if (question == null || (!"WRITING_TEXT".equals(question.getQuestionType()) && !"SPEAKING_AUDIO".equals(question.getQuestionType()))) continue;
            String submission = "WRITING_TEXT".equals(question.getQuestionType()) ? answer.getTextAnswer() : answer.getTranscript();
            if (submission == null || submission.isBlank()) continue;
            String taskType = "WRITING_TEXT".equals(question.getQuestionType()) ? "WRITING" : "SPEAKING";
            String key = "attempt:" + event.attemptId() + ":answer:" + answer.getId() + ":v1";
            AiAssessmentContract.AssessmentRequest request = new AiAssessmentContract.AssessmentRequest(key, event.userId(), event.attemptId(), answer.getId(), 1,
                    question.getSkill(), taskType, question.getContent(), question.getInstruction() == null ? question.getContent() : question.getInstruction() + "\n" + question.getContent(),
                    "aptis-rubric-v1", "scale-v1", BigDecimal.valueOf(50), answer.getTranscript());
            ai.request(request);
            ai.process(key);
        }
    }

    @EventListener
    public void onAssessmentCompleted(AiAssessmentCompletedEvent event) { attempts.applyAiAssessment(event); }
}
