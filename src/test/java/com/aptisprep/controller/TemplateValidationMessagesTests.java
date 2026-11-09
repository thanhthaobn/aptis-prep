package com.aptisprep.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class TemplateValidationMessagesTests {
    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void actualFormSummariesSelectAndEscapeLocalizedErrorsAndHideWhenValid() {
        MockServletContext servletContext = new MockServletContext();
        try (StaticWebApplicationContext application = new StaticWebApplicationContext()) {
            application.setServletContext(servletContext);
            application
                    .getStaticMessageSource()
                    .addMessage("test.global", Locale.ENGLISH, "Global <script>alert(1)</script>");
            application.refresh();
            servletContext.setAttribute(
                    WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, application);
            MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
            request.addPreferredLocale(Locale.ENGLISH);
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
            DisplayFormatting display = new DisplayFormatting();
            BeanPropertyBindingResult errors =
                    new BeanPropertyBindingResult(new TestForm(), "form");
            request.setAttribute("org.springframework.validation.BindingResult.form", errors);
            request.setAttribute(
                    "org.springframework.validation.BindingResult.registerForm", errors);
            assertTrue(display.allErrors("form").isEmpty());
            var flashcardForms = List.of("student/flashcards/form", "admin/flashcards/form");
            for (String form : flashcardForms) {
                assertFalse(renderSummary(form, display).contains("alert-danger"));
            }
            errors.rejectValue("title", "test.field", "Field <b>required</b>");
            errors.reject("test.global", "Fallback");
            assertEquals(2, display.allErrors("form").size());
            assertEquals(1, display.globalErrors("form").size());
            for (String form : flashcardForms) {
                String all = renderSummary(form, display);
                assertTrue(all.contains("Field &lt;b&gt;required&lt;/b&gt;"));
                assertTrue(all.contains("Global &lt;script&gt;alert(1)&lt;/script&gt;"));
                assertFalse(all.contains("<script>"));
            }
            String global = renderSummary("auth/register", display);
            assertTrue(global.contains("Global &lt;script&gt;"));
            assertFalse(global.contains("Field"));
            assertTrue(display.allErrors("missingForm").isEmpty());
        }
    }

    private String renderSummary(String template, DisplayFormatting display) {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        engine.setTemplateResolver(resolver);
        Context context = new Context(Locale.ENGLISH, Map.of("display", display));
        return engine.process(
                new TemplateSpec(template, Set.of(".alert-danger"), TemplateMode.HTML, Map.of()),
                context);
    }

    public static class TestForm {
        public String getTitle() {
            return "";
        }
    }
}
