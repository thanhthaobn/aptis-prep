package com.aptisprep.service.placement;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aptisprep.mvc.account.service.AccountAccessService;
import com.aptisprep.mvc.common.event.AttemptCompletedEvent;
import com.aptisprep.service.contract.LearningPathContract;
import com.aptisprep.service.exam.AttemptOutboxService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

class PlacementOrchestrationServiceTests {
    @Test
    void placementUpdatesProfileBeforeLearningPath() {
        AccountAccessService accounts = mock(AccountAccessService.class);
        LearningPathContract paths = mock(LearningPathContract.class);
        AttemptOutboxService outbox = mock(AttemptOutboxService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<LearningPathContract> provider = mock(ObjectProvider.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        org.mockito.Mockito.when(provider.getIfAvailable()).thenReturn(paths);
        PlacementOrchestrationService service =
                new PlacementOrchestrationService(accounts, provider, outbox, transactionManager);

        service.onCompleted(new AttemptCompletedEvent(7, 91, "PLACEMENT"));

        var order = inOrder(accounts, paths);
        order.verify(accounts).applyAssessment(7, 91);
        order.verify(paths).ensurePaths(7);
        org.mockito.Mockito.verify(outbox)
                .markDelivered(new AttemptCompletedEvent(7, 91, "PLACEMENT"));
    }

    @Test
    void nonPlacementDoesNotUpdateProfileOrPath() {
        AccountAccessService accounts = mock(AccountAccessService.class);
        AttemptOutboxService outbox = mock(AttemptOutboxService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<LearningPathContract> provider = mock(ObjectProvider.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        PlacementOrchestrationService service =
                new PlacementOrchestrationService(accounts, provider, outbox, transactionManager);

        service.onCompleted(new AttemptCompletedEvent(7, 91, "PRACTICE"));

        verifyNoInteractions(accounts, provider);
    }

    @Test
    void failedProfileUpdateDoesNotAckCompletedEvent() {
        AccountAccessService accounts = mock(AccountAccessService.class);
        AttemptOutboxService outbox = mock(AttemptOutboxService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<LearningPathContract> provider = mock(ObjectProvider.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        doThrow(new IllegalStateException("profile failure")).when(accounts).applyAssessment(7, 91);
        PlacementOrchestrationService service =
                new PlacementOrchestrationService(accounts, provider, outbox, transactionManager);

        assertThrows(
                IllegalStateException.class,
                () -> service.onCompleted(new AttemptCompletedEvent(7, 91, "PLACEMENT")));

        verifyNoInteractions(outbox, provider);
    }
}
