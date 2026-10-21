package com.aptisprep.service.placement;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.aptisprep.event.AttemptCompletedEvent;
import com.aptisprep.service.account.AccountAccessService;
import com.aptisprep.service.contract.LearningPathContract;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class PlacementOrchestrationServiceTests {
    @Test
    void placementUpdatesProfileBeforeLearningPath() {
        AccountAccessService accounts = mock(AccountAccessService.class);
        LearningPathContract paths = mock(LearningPathContract.class);
        @SuppressWarnings("unchecked") ObjectProvider<LearningPathContract> provider = mock(ObjectProvider.class);
        org.mockito.Mockito.when(provider.getIfAvailable()).thenReturn(paths);
        PlacementOrchestrationService service = new PlacementOrchestrationService(accounts, provider);

        service.onCompleted(new AttemptCompletedEvent(7, 91, "PLACEMENT"));

        var order = inOrder(accounts, paths);
        order.verify(accounts).applyAssessment(7, 91);
        order.verify(paths).ensurePaths(7);
    }

    @Test
    void nonPlacementDoesNotUpdateProfileOrPath() {
        AccountAccessService accounts = mock(AccountAccessService.class);
        @SuppressWarnings("unchecked") ObjectProvider<LearningPathContract> provider = mock(ObjectProvider.class);
        PlacementOrchestrationService service = new PlacementOrchestrationService(accounts, provider);

        service.onCompleted(new AttemptCompletedEvent(7, 91, "PRACTICE"));

        verifyNoInteractions(accounts, provider);
    }
}
