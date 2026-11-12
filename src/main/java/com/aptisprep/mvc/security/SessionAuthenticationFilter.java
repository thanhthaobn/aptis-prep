package com.aptisprep.mvc.security;

import com.aptisprep.mvc.dto.UserDTO;
import com.aptisprep.mvc.enums.Role;
import com.aptisprep.mvc.enums.UserStatus;
import com.aptisprep.mvc.service.auth.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {
    private static final List<PathPattern> ASSETS = paths("/css/**", "/images/**", "/demo/**");
    private static final List<PathPattern> PUBLIC =
            paths("/", "/login", "/register", "/account-locked", "/error");
    private static final List<PathPattern> ADMIN = paths("/admin/**");
    private static final List<PathPattern> STUDENT =
            paths("/student/**", "/lessons/**", "/exams/**", "/attempts/**");
    private static final List<PathPattern> UPLOADS = paths("/uploads/**");

    private final UserService users;

    public SessionAuthenticationFilter(UserService users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        PathContainer path = ServletRequestPathUtils.parse(request).pathWithinApplication();
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        if (request.isSecure()) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        if (matches(UPLOADS, path)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (matches(ASSETS, path)) {
            chain.doFilter(request, response);
            return;
        }
        response.setHeader("Cache-Control", "no-store");

        UserDTO user = null;
        String loginMessage = "";
        LoginSession login = LoginSession.current(request);
        if (login != null) {
            user = users.findAuthenticatedById(login.userId());
            if (user != null && !UserStatus.ACTIVE.name().equals(user.getStatus())) {
                loginMessage = "?locked";
                user = null;
            } else if (user != null
                    && !login.credentialVersion()
                            .equals(LoginSession.credentialVersion(user.getPasswordHash()))) {
                loginMessage = "?passwordChanged";
                user = null;
            }
            if (user == null) LoginSession.end(request);
        }

        if (user == null && !matches(PUBLIC, path)) {
            response.sendRedirect(request.getContextPath() + "/login" + loginMessage);
            return;
        }
        if (user != null
                && ((matches(ADMIN, path) && user.getRole() != Role.ADMIN)
                        || (matches(STUDENT, path) && user.getRole() != Role.STUDENT))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(user == null ? request : authenticated(request, user), response);
    }

    private static HttpServletRequest authenticated(HttpServletRequest request, UserDTO user) {
        request.setAttribute("signedIn", true);
        request.setAttribute("isAdmin", user.getRole() == Role.ADMIN);
        request.setAttribute("isStudent", user.getRole() == Role.STUDENT);
        Principal principal = user::getEmail;
        return new HttpServletRequestWrapper(request) {
            @Override
            public Principal getUserPrincipal() {
                return principal;
            }

            @Override
            public String getRemoteUser() {
                return principal.getName();
            }

            @Override
            public boolean isUserInRole(String role) {
                return user.getRole().name().equals(role);
            }
        };
    }

    private static List<PathPattern> paths(String... patterns) {
        return Arrays.stream(patterns)
                .map(streamValue -> PathPatternParser.defaultInstance.parse(streamValue))
                .toList();
    }

    private static boolean matches(List<PathPattern> patterns, PathContainer path) {
        return patterns.stream().anyMatch(pattern -> pattern.matches(path));
    }
}
