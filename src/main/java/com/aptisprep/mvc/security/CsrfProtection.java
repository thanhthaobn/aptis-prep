package com.aptisprep.mvc.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class CsrfProtection implements HandlerInterceptor {
    public static final String PARAMETER = "_csrf";
    public static final String HEADER = "X-CSRF-TOKEN";
    private static final String SESSION_ATTRIBUTE = CsrfProtection.class.getName();
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws java.io.IOException {
        if (!(handler instanceof HandlerMethod)) return true;
        if (!isSafe(request.getMethod())) {
            HttpSession session = request.getSession(false);
            String expected =
                    session == null ? null : (String) session.getAttribute(SESSION_ATTRIBUTE);
            String supplied = request.getHeader(HEADER);
            if (supplied == null) supplied = request.getParameter(PARAMETER);
            if (expected == null
                    || supplied == null
                    || !MessageDigest.isEqual(
                            expected.getBytes(StandardCharsets.UTF_8),
                            supplied.getBytes(StandardCharsets.UTF_8))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
        }
        return true;
    }

    @Override
    public void postHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            ModelAndView view) {
        if (view != null
                && view.getViewName() != null
                && !view.getViewName().startsWith("redirect:")) {
            request.setAttribute(PARAMETER, new ViewToken(token(request.getSession())));
        }
    }

    public static boolean isSafe(String method) {
        return SAFE_METHODS.contains(method);
    }

    public static String token(HttpSession session) {
        synchronized (session) {
            String existing = (String) session.getAttribute(SESSION_ATTRIBUTE);
            if (existing != null) return existing;
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(SESSION_ATTRIBUTE, token);
            return token;
        }
    }

    public record ViewToken(String token) {
        public String getToken() {
            return token;
        }

        public String getHeaderName() {
            return HEADER;
        }
    }
}
