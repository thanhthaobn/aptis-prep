package com.aptisprep.security;

import com.aptisprep.enums.UserStatus;
import com.aptisprep.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rechecks a logged-in account on every request so a lock takes effect for an existing session.
 */
@Component
public class ActiveAccountFilter extends OncePerRequestFilter {
    private final UserRepository userRepository;

    public ActiveAccountFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getName() != null
                && !"/login".equals(request.getServletPath())) {
            userRepository
                    .findByEmailIgnoreCase(authentication.getName())
                    .ifPresent(
                            user -> {
                                HttpSession currentSession = request.getSession(false);
                                String version = CredentialVersion.of(user.getPasswordHash());
                                Object previous =
                                        currentSession == null
                                                ? null
                                                : currentSession.getAttribute(
                                                        CredentialVersion.SESSION_KEY);
                                boolean passwordChanged =
                                        previous != null && !version.equals(previous);
                                if (currentSession != null && previous == null)
                                    currentSession.setAttribute(
                                            CredentialVersion.SESSION_KEY, version);
                                if (user.getStatus() != UserStatus.ACTIVE || passwordChanged) {
                                    SecurityContextHolder.clearContext();
                                    HttpSession session = request.getSession(false);
                                    if (session != null) session.invalidate();
                                    try {
                                        response.sendRedirect(
                                                request.getContextPath()
                                                        + (passwordChanged
                                                                ? "/login?passwordChanged"
                                                                : "/login?locked"));
                                    } catch (IOException exception) {
                                        throw new RuntimeException(exception);
                                    }
                                }
                            });
            if (response.isCommitted()) return;
        }
        filterChain.doFilter(request, response);
    }
}
