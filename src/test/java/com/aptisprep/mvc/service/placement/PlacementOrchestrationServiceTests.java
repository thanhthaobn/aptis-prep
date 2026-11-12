package com.aptisprep.mvc.service.placement;

import com.aptisprep.mvc.service.exam.AttemptOutboxService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aptisprep.mvc.event.AttemptCompletedEvent;
import com.aptisprep.mvc.service.account.AccountAccessService;
import com.aptisprep.mvc.service.learning.LearningPathService;
import com.aptisprep.mvc.service.exam.impl.AttemptOutboxServiceImpl;
import com.aptisprep.mvc.service.placement.impl.PlacementOrchestrationServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

class PlacementOrchestrationServiceTests {
    @Test
    void placementUpdatesProfileBeforeLearningPath() {
        AccountAccessService accounts = mock(AccountAccessService.class);
        LearningPathService paths = mock(LearningPathService.class);
        AttemptOutboxService outbox = mock(AttemptOutboxService.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        PlacementOrchestrationService service =
                new PlacementOrchestrationServiceImpl(accounts, paths, outbox, transactionManager);

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
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        PlacementOrchestrationService service =
                new PlacementOrchestrationServiceImpl(
                        accounts, mock(LearningPathService.class), outbox, transactionManager);

        service.onCompleted(new AttemptCompletedEvent(7, 91, "PRACTICE"));

        verifyNoInteractions(accounts);
    }

    @Test
    void failedProfileUpdateDoesNotAckCompletedEvent() {
        AccountAccessService accounts = mock(AccountAccessService.class);
        AttemptOutboxService outbox = mock(AttemptOutboxService.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        doThrow(new IllegalStateException("profile failure")).when(accounts).applyAssessment(7, 91);
        PlacementOrchestrationService service =
                new PlacementOrchestrationServiceImpl(
                        accounts, mock(LearningPathService.class), outbox, transactionManager);

        assertThrows(
                IllegalStateException.class,
                () -> service.onCompleted(new AttemptCompletedEvent(7, 91, "PLACEMENT")));

        verifyNoInteractions(outbox);
    }
}










