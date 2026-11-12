package com.aptisprep.mvc.service.placement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.service.account.AccountAccessService;
import com.aptisprep.mvc.service.catalog.PublishedExamCatalogService;
import com.aptisprep.mvc.service.exam.ExamAttemptCommandService;
import com.aptisprep.mvc.service.exam.ExamAttemptQueryService;
import com.aptisprep.mvc.support.contract.*;
import com.aptisprep.mvc.support.contract.IntegrationModels.AttemptHandle;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlacementServiceTests {
    @Test
    @SuppressWarnings("unchecked")
    void availableExamsUseAnUnambiguousActionForTheCurrentAttemptState() {
        var catalog = mock(PlacementExamCatalogService.class);
        var published = mock(PublishedExamCatalogService.class);
        var engine = mock(ExamAttemptCommandService.class);
        var accounts = mock(AccountAccessService.class);
        var queries = mock(ExamAttemptQueryService.class);
        when(accounts.requireActiveStudent("learner@test.local")).thenReturn(9);
        when(catalog.findPublished("LISTENING"))
                .thenReturn(
                        List.of(
                                new PlacementExamCatalogService.PlacementExam(
                                        10, "Listening placement", "", 15, List.of())));
        var service =
                new com.aptisprep.mvc.service.placement.impl.PlacementServiceImpl(
                        catalog, published, engine, queries, accounts);

        assertFalse(service.available("learner@test.local", "LISTENING").getFirst().resumable());

        when(queries.hasInProgressPlacement(9, "LISTENING")).thenReturn(true);
        assertTrue(service.available("learner@test.local", "LISTENING").getFirst().resumable());
    }

    @Test
    @SuppressWarnings("unchecked")
    void selectionUsesIntegerValuesBeyondTheJvmCacheAndRejectsOtherIds() {
        var catalog = mock(PlacementExamCatalogService.class);
        var published = mock(PublishedExamCatalogService.class);
        var engine = mock(ExamAttemptCommandService.class);
        var accounts = mock(AccountAccessService.class);
        var queries = mock(ExamAttemptQueryService.class);
        when(accounts.requireActiveStudent("learner@test.local")).thenReturn(9);
        when(catalog.findPublished("READING"))
                .thenReturn(
                        List.of(
                                new PlacementExamCatalogService.PlacementExam(
                                        Integer.valueOf("1000"), "Placement", "", 15, List.of())));
        when(engine.startPlacement(eq(9), eq(1000), anyString()))
                .thenReturn(new AttemptHandle(17, "IN_PROGRESS", null));
        var service =
                new com.aptisprep.mvc.service.placement.impl.PlacementServiceImpl(
                        catalog, published, engine, queries, accounts);
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
