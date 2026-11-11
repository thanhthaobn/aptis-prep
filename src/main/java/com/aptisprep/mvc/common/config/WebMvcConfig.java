package com.aptisprep.mvc.common.config;

import com.aptisprep.mvc.common.security.CsrfProtection;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.support.RequestDataValueProcessor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    // uploads is private. Media is served only by authorized MVC controllers.
    private final CsrfProtection csrf;

    public WebMvcConfig(CsrfProtection csrf) {
        this.csrf = csrf;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(csrf).excludePathPatterns("/error");
    }

    @Bean
    RequestDataValueProcessor requestDataValueProcessor() {
        return new RequestDataValueProcessor() {
            private static final String FORM_METHOD = "aptis.csrf.formMethod";

            @Override
            public String processAction(HttpServletRequest request, String action, String method) {
                request.setAttribute(FORM_METHOD, method);
                return action;
            }

            @Override
            public String processFormFieldValue(
                    HttpServletRequest request, String name, String value, String type) {
                return value;
            }

            @Override
            public Map<String, String> getExtraHiddenFields(HttpServletRequest request) {
                String method = (String) request.getAttribute(FORM_METHOD);
                request.removeAttribute(FORM_METHOD);
                if (method != null
                        && !CsrfProtection.isSafe(method.toUpperCase(java.util.Locale.ROOT))
                        && request.getAttribute(CsrfProtection.PARAMETER)
                                instanceof CsrfProtection.ViewToken token) {
                    return Map.of(CsrfProtection.PARAMETER, token.getToken());
                }
                return Map.of();
            }

            @Override
            public String processUrl(HttpServletRequest request, String url) {
                return url;
            }
        };
    }
}
