package com.aptisprep.mvc.student.controller;

import com.aptisprep.mvc.student.dto.AdminDashboardViewDTO;
import com.aptisprep.mvc.student.service.AdminDashboardService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDashboardController {
    private final AdminDashboardService dashboardService;

    public AdminDashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Principal principal, Model model) {
        AdminDashboardViewDTO page = dashboardService.dashboard(principal.getName());
        model.addAttribute("email", page.email());
        model.addAttribute("studentCount", page.studentCount());
        model.addAttribute("lessonCount", page.lessonCount());
        model.addAttribute("questionCount", page.questionCount());
        model.addAttribute("publishedExamCount", page.publishedExamCount());
        model.addAttribute("recentContent", page.recentContent());
        model.addAttribute("recentAttempts", page.recentAttempts());
        return "admin/dashboard";
    }
}
