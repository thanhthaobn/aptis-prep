package com.aptisprep.security;

import com.aptisprep.enums.UserStatus;
import com.aptisprep.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Rechecks a logged-in account on every request so a lock takes effect for an existing session. */
@Component
public class ActiveAccountFilter extends OncePerRequestFilter {
    private final UserRepository users;

    public ActiveAccountFilter(UserRepository users) { this.users = users; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getName() != null && !"/login".equals(request.getServletPath())) {
            users.findByEmailIgnoreCase(authentication.getName()).ifPresent(user -> {
                if (user.getStatus() != UserStatus.ACTIVE) {
                    SecurityContextHolder.clearContext();
                    var session = request.getSession(false);
                    if (session != null) session.invalidate();
                    try { response.sendRedirect(request.getContextPath() + "/login?locked"); }
                    catch (IOException exception) { throw new RuntimeException(exception); }
                }
            });
            if (response.isCommitted()) return;
        }
        filterChain.doFilter(request, response);
    }
}
