package com.aptisprep.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Shared navbar state follows the route, not the signed-in user's role. */
@ControllerAdvice
public class NavigationModelAdvice {
    @ModelAttribute("navSection")
    public String navSection(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.equals("/")) return "home";
        if (path.equals("/student") || path.startsWith("/student/")) return "student";
        if (path.equals("/admin") || path.startsWith("/admin/")) return "admin";
        if (path.equals("/login")) return "login";
        if (path.equals("/register")) return "register";
        return "other";
    }
}
