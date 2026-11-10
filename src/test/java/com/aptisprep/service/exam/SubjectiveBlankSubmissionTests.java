package com.aptisprep.service.exam;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.aptisprep.entity.*;
import com.aptisprep.mvc.common.config.AiProperties;
import com.aptisprep.mvc.common.enums.*;
import com.aptisprep.mvc.common.exception.AiGradingException;
import com.aptisprep.repository.*;
import com.aptisprep.service.grading.GeminiGradingService;
import com.aptisprep.service.grading.GradingResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:blank_subjective;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
            "app.ai.recovery-ms=86400000",
            "app.attempt-outbox.recovery-ms=86400000"
        })
class SubjectiveBlankSubmissionTests {
    @Autowired ExamAttemptService attempts;
    @Autowired AiProperties properties;
    @Autowired UserRepository users;
    @Autowired CertificateRepository certificates;
    @Autowired ScoringSchemeRepository schemes;
    @Autowired ScoringScaleRuleRepository rules;
    @Autowired ScoreBandRepository bands;
    @Autowired MockExamRepository exams;
    @Autowired QuestionRepository questions;
    @Autowired QuestionItemRepository items;
    @Autowired MockExamQuestionRepository links;
    @Autowired AiAssessmentJobRepository jobs;
    @Autowired ExamAttemptRepository attemptRepository;
    @Autowired ExamSkillResultRepository resultRepository;
    @Autowired UserAnswerRepository answerRepository;
    @MockitoBean GeminiGradingService provider;

    @ParameterizedTest
    @CsvSource({"WRITING,true", "WRITING,false", "SPEAKING,true", "SPEAKING,false"})
    void submittedBlankSkillsReceiveZeroWithoutCallingAi(String skill, boolean enabled) {
        properties.setEnabled(enabled);
        Fixture fixture = fixture(skill, 1, true);
        int attemptId =
                attempts.start(
                                fixture.user().getId(),
                                fixture.exam().getId(),
                                UUID.randomUUID().toString())
                        .attemptId();
        assertEquals("IN_PROGRESS", attempts.result(fixture.user().getId(), attemptId).state());
        attempts.submit(fixture.user().getId(), attemptId, "blank-submit");
        var result = attempts.result(fixture.user().getId(), attemptId);
        assertEquals("COMPLETED", result.state());
        assertEquals(0, result.skills().getFirst().rawScore().signum());
        assertEquals(0, result.skills().getFirst().scaleScore().signum());
        attempts.submit(fixture.user().getId(), attemptId, "blank-submit");
        assertEquals(
                0,
                attempts.result(fixture.user().getId(), attemptId)
                        .skills()
                        .getFirst()
                        .rawScore()
                        .signum());
        verifyNoInteractions(provider);
    }

    @Test
    void legacyEmptyErrorCanBeExplicitlyRegradedWithoutDeletingAnswers() {
        properties.setEnabled(false);
        Fixture fixture = fixture("WRITING", 2, true);
        int attemptId =
                attempts.start(fixture.user().getId(), fixture.exam().getId(), "legacy-empty")
                        .attemptId();
        attempts.submit(fixture.user().getId(), attemptId, "legacy-empty-submit");
        ExamAttemptEntity saved = attemptRepository.findById(attemptId).orElseThrow();
        saved.setStatus("ERROR");
        saved.setGradingStatus("ERROR");
        saved.setFinalScaleScore(null);
        saved.setEstimatedOverallCefr(null);
        attemptRepository.saveAndFlush(saved);
        ExamSkillResultEntity skill =
                resultRepository.findByAttemptIdAndSkill(attemptId, "WRITING").orElseThrow();
        skill.setGradingMethod("AI_EMPTY");
        skill.setGradingStatus("ERROR");
        skill.setRawScore(null);
        skill.setScaleScore(null);
        skill.setCefrLevel(null);
        resultRepository.saveAndFlush(skill);
        for (UserAnswerEntity answer :
                answerRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId)) {
            answer.setAiScore(null);
            answer.setAiFeedback(null);
            answer.setAiProvider(null);
            answer.setAiModel(null);
            answer.setGradedAt(null);
            answer.setStatus("SUBMITTED");
            answerRepository.saveAndFlush(answer);
        }
        List<Integer> answerIds =
                answerRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId).stream()
                        .map(UserAnswerEntity::getId)
                        .toList();
        assertTrue(attempts.canRegradeEmpty(fixture.user().getId(), attemptId));
        attempts.regradeEmpty(fixture.user().getId(), attemptId);
        attempts.regradeEmpty(fixture.user().getId(), attemptId);
        assertEquals("COMPLETED", attempts.result(fixture.user().getId(), attemptId).state());
        assertEquals(
                0,
                attempts.result(fixture.user().getId(), attemptId)
                        .skills()
                        .getFirst()
                        .rawScore()
                        .signum());
        assertEquals(
                answerIds,
                answerRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId).stream()
                        .map(UserAnswerEntity::getId)
                        .toList());
    }

    @Test
    void regradeIsOwnershipChecked() {
        Fixture owner = fixture("WRITING", 1, true);
        Fixture other = fixture("WRITING", 1, true);
        int attemptId =
                attempts.start(owner.user().getId(), owner.exam().getId(), "owner-check")
                        .attemptId();
        assertThrows(
                com.aptisprep.mvc.common.exception.AccessDeniedException.class,
                () -> attempts.canRegradeEmpty(other.user().getId(), attemptId));
        assertThrows(
                com.aptisprep.mvc.common.exception.AccessDeniedException.class,
                () -> attempts.regradeEmpty(other.user().getId(), attemptId));
    }

    @Test
    void disabledAiKeepsSavedTextAsAnErrorAndCannotRegradeItToZero() {
        properties.setEnabled(false);
        Fixture fixture = fixture("WRITING", 1, true);
        int attemptId =
                attempts.start(fixture.user().getId(), fixture.exam().getId(), "disabled-text")
                        .attemptId();
        QuestionItemEntity item = fixture.items().getFirst();
        attempts.saveAnswer(
                fixture.user().getId(),
                attemptId,
                item.getQuestionId(),
                item.getId(),
                null,
                null,
                "This answer is not empty.");
        attempts.submit(fixture.user().getId(), attemptId, "disabled-text-submit");
        assertEquals("ERROR", attempts.result(fixture.user().getId(), attemptId).state());
        assertNull(
                attempts.result(fixture.user().getId(), attemptId)
                        .skills()
                        .getFirst()
                        .scaleScore());
        assertFalse(attempts.canRegradeEmpty(fixture.user().getId(), attemptId));
        assertThrows(
                com.aptisprep.mvc.common.exception.BusinessRuleException.class,
                () -> attempts.regradeEmpty(fixture.user().getId(), attemptId));
        verifyNoInteractions(provider);
    }

    @Test
    void unscaledBlankPracticeStillCompletesWithRawZero() {
        properties.setEnabled(false);
        Fixture fixture = fixture("WRITING", 1, false);
        int attemptId =
                attempts.start(fixture.user().getId(), fixture.exam().getId(), "unscaled-blank")
                        .attemptId();
        attempts.submit(fixture.user().getId(), attemptId, "unscaled-blank-submit");
        var result = attempts.result(fixture.user().getId(), attemptId);
        assertEquals("COMPLETED", result.state());
        assertEquals(0, result.skills().getFirst().rawScore().signum());
        assertNull(result.skills().getFirst().scaleScore());
        assertNull(result.skills().getFirst().estimatedLevel());
    }

    @Test
    void answeredQuestionAndBlankQuestionAreBothIncludedInTheAverage() {
        properties.setEnabled(true);
        when(provider.grade(anyString(), anyString()))
                .thenReturn(
                        new GradingResult(
                                BigDecimal.valueOf(20),
                                "A1",
                                "Test feedback",
                                "Gemini",
                                "test-model"));
        Fixture fixture = fixture("WRITING", 2, true);
        int attemptId =
                attempts.start(fixture.user().getId(), fixture.exam().getId(), "partly-blank")
                        .attemptId();
        QuestionItemEntity item = fixture.items().getFirst();
        attempts.saveAnswer(
                fixture.user().getId(),
                attemptId,
                item.getQuestionId(),
                item.getId(),
                null,
                null,
                "This is my saved answer.");
        attempts.submit(fixture.user().getId(), attemptId, "partly-blank-submit");
        var result = attempts.result(fixture.user().getId(), attemptId);
        assertEquals("COMPLETED", result.state());
        assertEquals(0, new BigDecimal("10.00").compareTo(result.skills().getFirst().rawScore()));
        verify(provider).grade(anyString(), anyString());
    }

    @Test
    void providerFailureForASavedAnswerRemainsAnErrorInsteadOfZero() {
        properties.setEnabled(true);
        when(provider.grade(anyString(), anyString()))
                .thenThrow(new AiGradingException("Provider unavailable"));
        Fixture fixture = fixture("WRITING", 1, true);
        int attemptId =
                attempts.start(fixture.user().getId(), fixture.exam().getId(), "real-error")
                        .attemptId();
        QuestionItemEntity item = fixture.items().getFirst();
        attempts.saveAnswer(
                fixture.user().getId(),
                attemptId,
                item.getQuestionId(),
                item.getId(),
                null,
                null,
                "A saved answer must not become zero.");
        attempts.submit(fixture.user().getId(), attemptId, "real-error-submit");
        var result = attempts.result(fixture.user().getId(), attemptId);
        assertEquals("ERROR", result.state());
        assertNull(result.skills().getFirst().scaleScore());
        assertFalse(attempts.canRegradeEmpty(fixture.user().getId(), attemptId));
        assertThrows(
                com.aptisprep.mvc.common.exception.BusinessRuleException.class,
                () -> attempts.regradeEmpty(fixture.user().getId(), attemptId));
    }

    private Fixture fixture(String skill, int questionCount, boolean scaled) {
        User user = new User();
        user.setFullName("Blank submission test");
        user.setEmail(UUID.randomUUID() + "@test.invalid");
        user.setPasswordHash("unused");
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        user = users.saveAndFlush(user);
        Certificate certificate = new Certificate();
        certificate.setName("Test certificate");
        certificate.setCode(UUID.randomUUID().toString());
        certificate.setStatus(RecordStatus.ACTIVE);
        certificate = certificates.saveAndFlush(certificate);
        ScoringScheme scheme = new ScoringScheme();
        scheme.setCertificate(certificate);
        scheme.setName("Test scoring scheme");
        scheme.setVersion("v1");
        scheme.setStatus(RecordStatus.ACTIVE);
        scheme = schemes.saveAndFlush(scheme);
        ScoringScaleRule rule = new ScoringScaleRule();
        rule.setScheme(scheme);
        rule.setSkill(skill);
        rule.setRawMin(BigDecimal.ZERO);
        rule.setRawMax(BigDecimal.valueOf(50));
        rule.setScaleMin(BigDecimal.ZERO);
        rule.setScaleMax(BigDecimal.valueOf(50));
        rules.saveAndFlush(rule);
        ScoreBand band = new ScoreBand();
        band.setScheme(scheme);
        band.setSkill(skill);
        band.setCefrLevel("A1");
        band.setMinScore(BigDecimal.ZERO);
        band.setMaxScore(BigDecimal.valueOf(50));
        bands.saveAndFlush(band);
        MockExamEntity exam = new MockExamEntity();
        exam.setCertificateId(certificate.getId());
        exam.setScoringSchemeId(scaled ? scheme.getId() : null);
        exam.setTitle("Blank answer practice");
        exam.setExamType("PRACTICE");
        exam.setSkill(skill);
        exam.setStatus("PUBLISHED");
        exam.setDurationMinutes(10);
        exam = exams.saveAndFlush(exam);
        List<QuestionItemEntity> questionItems = new java.util.ArrayList<>();
        for (int position = 1; position <= questionCount; position++) {
            QuestionEntity question = new QuestionEntity();
            question.setContent("Answer task " + position);
            question.setSkill(skill);
            question.setQuestionType("WRITING".equals(skill) ? "WRITING_TEXT" : "SPEAKING_AUDIO");
            question.setStatus("PUBLISHED");
            question = questions.saveAndFlush(question);
            QuestionItemEntity item = new QuestionItemEntity();
            item.setQuestionId(question.getId());
            item.setContent("Give your response");
            item.setPosition(1);
            questionItems.add(items.saveAndFlush(item));
            MockExamQuestionEntity link = new MockExamQuestionEntity();
            link.setMockExamId(exam.getId());
            link.setQuestionId(question.getId());
            link.setPosition(position);
            links.saveAndFlush(link);
        }
        return new Fixture(user, exam, questionItems);
    }

    private record Fixture(User user, MockExamEntity exam, List<QuestionItemEntity> items) {}
}
