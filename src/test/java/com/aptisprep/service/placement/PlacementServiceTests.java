package com.aptisprep.service.placement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.contract.*;
import com.aptisprep.service.contract.IntegrationModels.AttemptHandle;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class PlacementServiceTests {
    @Test
    @SuppressWarnings("unchecked")
    void availableExamsUseAnUnambiguousActionForTheCurrentAttemptState() {
        var catalog = mock(PlacementExamCatalogService.class);
        var published = mock(ExamCatalogContract.class);
        var engine = mock(ExamAttemptContract.class);
        var accounts = mock(AccountAccessContract.class);
        ObjectProvider<ExamAttemptContract> engines = mock(ObjectProvider.class);
        ObjectProvider<AccountAccessContract> access = mock(ObjectProvider.class);
        when(engines.getIfAvailable()).thenReturn(engine);
        when(access.getIfAvailable()).thenReturn(accounts);
        when(accounts.requireActiveStudent("learner@test.local")).thenReturn(9);
        when(catalog.findPublished("LISTENING"))
                .thenReturn(
                        List.of(
                                new PlacementExamCatalogService.PlacementExam(
                                        10, "Listening placement", "")));
        var service = new PlacementService(catalog, published, engines, access);

        assertFalse(service.available("learner@test.local", "LISTENING").getFirst().resumable());

        when(engine.hasInProgressPlacement(9, "LISTENING")).thenReturn(true);
        assertTrue(service.available("learner@test.local", "LISTENING").getFirst().resumable());
    }

    @Test
    @SuppressWarnings("unchecked")
    void selectionUsesIntegerValuesBeyondTheJvmCacheAndRejectsOtherIds() {
        var catalog = mock(PlacementExamCatalogService.class);
        var published = mock(ExamCatalogContract.class);
        var engine = mock(ExamAttemptContract.class);
        var accounts = mock(AccountAccessContract.class);
        ObjectProvider<ExamAttemptContract> engines = mock(ObjectProvider.class);
        ObjectProvider<AccountAccessContract> access = mock(ObjectProvider.class);
        when(engines.getIfAvailable()).thenReturn(engine);
        when(access.getIfAvailable()).thenReturn(accounts);
        when(accounts.requireActiveStudent("learner@test.local")).thenReturn(9);
        when(catalog.findPublished("READING"))
                .thenReturn(
                        List.of(
                                new PlacementExamCatalogService.PlacementExam(
                                        Integer.valueOf("1000"), "Placement", "")));
        when(engine.startPlacement(eq(9), eq(1000), anyString()))
                .thenReturn(new AttemptHandle(17, "IN_PROGRESS", null));
        var service = new PlacementService(catalog, published, engines, access);
        assertEquals(
                17,
                service.start("learner@test.local", "READING", Integer.valueOf("1000"))
                        .attemptId());
        assertThrows(
                BusinessRuleException.class,
                () -> service.start("learner@test.local", "READING", 1001));
        assertThrows(
                BusinessRuleException.class,
                () -> service.start("learner@test.local", "READING", -1));
        verify(engine, times(1)).startPlacement(eq(9), eq(1000), anyString());
        verify(engine, never()).start(any(), any(), any());
    }
}
