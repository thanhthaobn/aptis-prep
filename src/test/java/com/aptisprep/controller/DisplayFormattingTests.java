package com.aptisprep.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.aptisprep.enums.RecordStatus;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class DisplayFormattingTests {
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
