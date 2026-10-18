package com.aptisprep.controller.admin;

import com.aptisprep.service.admin.StudentManagementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {
    private final StudentManagementService students;
    public AdminDashboardController(StudentManagementService students) { this.students = students; }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        model.addAttribute("email", principal.getName());
        model.addAttribute("studentCount", students.totalStudents());
        return "admin/dashboard";
    }
}
