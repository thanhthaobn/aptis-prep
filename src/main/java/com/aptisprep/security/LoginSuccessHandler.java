package com.aptisprep.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final com.aptisprep.repository.UserRepository users;

    public LoginSuccessHandler(com.aptisprep.repository.UserRepository users) {
        this.users = users;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        users.findByEmailIgnoreCase(authentication.getName())
                .ifPresent(
                        user ->
                                request.getSession()
                                        .setAttribute(
                                                CredentialVersion.SESSION_KEY,
                                                CredentialVersion.of(user.getPasswordHash())));

        boolean admin =
                authentication.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        response.sendRedirect(admin ? "/admin/dashboard" : "/student/dashboard");
    }
}
