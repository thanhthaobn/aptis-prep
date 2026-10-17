package com.aptisprep.controller.student;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/student")
public class StudentDashboardController {

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        model.addAttribute("email", principal.getName());
        return "student/dashboard";
    }

    /** Read-only orientation preview: no goals, attempts or progress are created. */
    @GetMapping("/orientation")
    public String orientation() {
        return "student/learning/orientation";
    }
}
