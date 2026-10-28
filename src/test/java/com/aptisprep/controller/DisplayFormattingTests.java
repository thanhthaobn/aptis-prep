package com.aptisprep.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.aptisprep.enums.RecordStatus;
import com.aptisprep.service.contract.IntegrationModels.SkillResult;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.expression.Numbers;
import org.thymeleaf.expression.Strings;
import org.thymeleaf.expression.Temporals;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class DisplayFormattingTests {
    private final DisplayFormatting display = new DisplayFormatting();

    @Test
    void gradingBannersAndRetryKeepTheirIndependentConditions() {
        SkillResult pending = new SkillResult("WRITING", null, null, null, "PENDING", null);
        SkillResult failed =
                new SkillResult("SPEAKING", null, null, null, "ERROR", null, false, true);
        assertFalse(display.hasPendingGrades(null));
        assertFalse(display.hasGradingErrors(List.of()));
        assertFalse(display.hasRetryableGrades(List.of(pending)));
        assertTrue(display.hasPendingGrades(List.of(pending, failed)));
        assertTrue(display.hasGradingErrors(List.of(pending, failed)));
        assertTrue(display.hasRetryableGrades(List.of(pending, failed)));
        assertFalse(display.hasPendingGrades(List.of(failed)));
    }

    @Test
    void datesRetainThePreviousFormattingForLocalDatesAndInstants() {
        Temporals previous = new Temporals(Locale.getDefault(), ZoneId.systemDefault());
        LocalDate day = LocalDate.of(2026, 10, 27);
        LocalDateTime local = day.atTime(9, 5, 7);
        Instant instant = Instant.parse("2026-10-26T23:59:07Z");
        assertEquals(previous.format(day, "dd/MM/yyyy"), display.date(day));
        assertEquals(
                previous.format(local, "dd/MM/yyyy HH:mm:ss"), display.dateTimeWithSeconds(local));
        assertEquals(previous.format(instant, "dd/MM/yyyy HH:mm"), display.dateTime(instant));
        assertEquals(previous.format(instant, "HH:mm dd/MM/yyyy"), display.timeAndDate(instant));
        assertNull(display.dateTime(null));
    }

    @Test
    void paginationAndGoalLabelsKeepTheirExistingValues() {
        Numbers previous = new Numbers(Locale.getDefault());
        assertEquals(Arrays.asList(previous.sequence(0, 4)), display.pageNumbers(0, 4));
        assertEquals(Arrays.asList(previous.sequence(2, 2)), display.pageNumbers(2, 2));
        assertEquals(Arrays.asList(previous.sequence(5, 3)), display.pageNumbers(5, 3));
        List<String> targets = List.of("A2", "B1", "C1");
        assertEquals(
                new Strings(Locale.getDefault()).listJoin(targets, ", "),
                display.join(targets, ", "));
        assertNull(display.join(null, ", "));
    }

    @Test
    void statusFragmentHandlesStringsEnumsNullAndEscapedFallback() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        engine.setTemplateResolver(resolver);
        Context context = new Context();
        TemplateSpec fragment =
                new TemplateSpec(
                        "fragments/status-badge",
                        Set.of("statusBadge"),
                        org.thymeleaf.templatemode.TemplateMode.HTML,
                        Map.of());
        context.setVariable("status", "COMPLETED");
        String complete = engine.process(fragment, context);
        assertTrue(complete.contains("Đã hoàn thành"));
        assertFalse(complete.contains("Đang chấm điểm"));
        context.setVariable("status", RecordStatus.ACTIVE);
        assertTrue(engine.process(fragment, context).contains("Đang hoạt động"));
        context.setVariable("status", null);
        assertFalse(engine.process(fragment, context).contains("class=\"badge"));
        context.setVariable("status", "<script>unknown</script>");
        String fallback = engine.process(fragment, context);
        assertTrue(fallback.contains("&lt;script&gt;unknown&lt;/script&gt;"));
        assertFalse(fallback.contains("<script>"));
    }
}
