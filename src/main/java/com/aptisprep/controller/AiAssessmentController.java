package com.aptisprep.controller;

import com.aptisprep.entity.User;
import com.aptisprep.service.contract.AiAssessmentContract;
import com.aptisprep.service.auth.UserService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AiAssessmentController {
    private final AiAssessmentContract assessments;
    private final UserService users;

    public AiAssessmentController(AiAssessmentContract assessments, UserService users) {
        this.assessments = assessments;
        this.users = users;
    }

    @GetMapping("/student/assessments/{key}")
    public String showStudentAssessment(
            @PathVariable String key, Principal principal, Model model) {
        User user = user(principal);
        model.addAttribute("assessment", assessments.get(key, user.getId(), false));
        model.addAttribute("assessmentBasePath", "/student/assessments/");
        return "student/assessment";
    }

    @GetMapping("/admin/assessments/{key}")
    public String showAdminAssessment(@PathVariable String key, Principal principal, Model model) {
        User user = user(principal);
        model.addAttribute("assessment", assessments.get(key, user.getId(), true));
        model.addAttribute("assessmentBasePath", "/admin/assessments/");
        return "student/assessment";
    }

    @PostMapping("/student/assessments/{key}/retry")
    public String retryStudentAssessment(
            @PathVariable String key, Principal principal, RedirectAttributes redirect) {
        User user = user(principal);
        assessments.retry(key, user.getId(), false);
        redirect.addFlashAttribute("successMessage", "Đã gửi yêu cầu chấm lại phần lỗi.");
        return "redirect:/student/assessments/" + key;
    }

    @PostMapping("/admin/assessments/{key}/retry")
    public String retryAdminAssessment(
            @PathVariable String key, Principal principal, RedirectAttributes redirect) {
        User user = user(principal);
        assessments.retry(key, user.getId(), true);
        redirect.addFlashAttribute("successMessage", "Đã gửi yêu cầu chấm lại phần lỗi.");
        return "redirect:/admin/assessments/" + key;
    }

    private User user(Principal principal) {
        return users.requireByEmail(principal.getName());
    }
}
