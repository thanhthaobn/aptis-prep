package com.aptisprep.mvc.placement.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aptisprep.mvc.account.service.AccountAccessService;
import com.aptisprep.mvc.common.event.AttemptCompletedEvent;
import com.aptisprep.mvc.exam.service.AttemptOutboxService;
import com.aptisprep.mvc.learning.service.LearningPathService;
import com.aptisprep.mvc.placement.service.impl.PlacementOrchestrationListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

class PlacementOrchestrationServiceTests {
    @Test
    void placementUpdatesProfileBeforeLearningPath() {
        AccountAccessService accounts = mock(AccountAccessService.class);
        LearningPathService paths = mock(LearningPathService.class);
        AttemptOutboxService outbox = mock(AttemptOutboxService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<LearningPathService> provider = mock(ObjectProvider.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        org.mockito.Mockito.when(provider.getIfAvailable()).thenReturn(paths);
        PlacementOrchestrationListener service =
                new PlacementOrchestrationListener(accounts, provider, outbox, transactionManager);

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
        ObjectProvider<LearningPathService> provider = mock(ObjectProvider.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        PlacementOrchestrationListener service =
                new PlacementOrchestrationListener(accounts, provider, outbox, transactionManager);

        service.onCompleted(new AttemptCompletedEvent(7, 91, "PRACTICE"));

        verifyNoInteractions(accounts, provider);
    }

    @Test
    void failedProfileUpdateDoesNotAckCompletedEvent() {
        AccountAccessService accounts = mock(AccountAccessService.class);
        AttemptOutboxService outbox = mock(AttemptOutboxService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<LearningPathService> provider = mock(ObjectProvider.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        doThrow(new IllegalStateException("profile failure")).when(accounts).applyAssessment(7, 91);
        PlacementOrchestrationListener service =
                new PlacementOrchestrationListener(accounts, provider, outbox, transactionManager);

        assertThrows(
                IllegalStateException.class,
                () -> service.onCompleted(new AttemptCompletedEvent(7, 91, "PLACEMENT")));

        verifyNoInteractions(outbox, provider);
    }
}
