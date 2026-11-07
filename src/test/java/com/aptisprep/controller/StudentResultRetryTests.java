package com.aptisprep.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.aptisprep.controller.student.StudentContentController;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.account.AccountAccessService;
import com.aptisprep.service.contract.ExamAttemptContract;
import com.aptisprep.service.contract.IntegrationModels.*;
import com.aptisprep.service.student.StudentExamCatalogService;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

class StudentResultRetryTests {
    @Test
    void retryDelegatesOnlyAnOwnersRetryableErrorToTheExistingGradingContract() {
        var accounts = mock(AccountAccessService.class);
        var attempts = mock(ExamAttemptContract.class);
        var controller =
                new StudentContentController(
                        mock(StudentExamCatalogService.class),
                        accounts,
                        attempts,
                        mock(com.aptisprep.service.scoring.ScoringGuideService.class));
        when(accounts.requireActiveStudent("learner@test.local")).thenReturn(12);
        when(attempts.result(12, 99))
                .thenReturn(
                        new AttemptResult(
                                99,
                                12,
                                5,
                                "PRACTICE",
                                1,
                                "ERROR",
                                List.of(
                                        new SkillResult(
                                                "WRITING", null, null, null, "ERROR", null, false,
                                                true))));
        assertThat(
                        controller.retryGrading(
                                99, () -> "learner@test.local", new RedirectAttributesModelMap()))
                .isEqualTo("redirect:/student/attempts/99/result");
        verify(attempts).retryFailedGrading(eq(12), eq(99), anyString());
        when(attempts.result(12, 99))
                .thenReturn(new AttemptResult(99, 12, 5, "PRACTICE", 1, "COMPLETED", List.of()));
        assertThatThrownBy(
                        () ->
                                controller.retryGrading(
                                        99,
                                        () -> "learner@test.local",
                                        new RedirectAttributesModelMap()))
                .isInstanceOf(BusinessRuleException.class);
        verify(attempts, times(1)).retryFailedGrading(anyInt(), anyInt(), anyString());
    }
}
