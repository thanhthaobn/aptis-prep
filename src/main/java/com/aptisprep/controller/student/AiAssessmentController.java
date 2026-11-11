package com.aptisprep.controller.student;

import com.aptisprep.service.account.UserService;
import com.aptisprep.service.grading.AiAssessmentService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AiAssessmentController {
    private final AiAssessmentService assessments;
    private final UserService users;

    public AiAssessmentController(AiAssessmentService assessments, UserService users) {
        this.assessments = assessments;
        this.users = users;
    }

    @GetMapping("/student/assessments/{key}")
    public String showStudentAssessment(
            @PathVariable String key, Principal principal, Model model) {
        Integer userId = users.getUserIdByEmail(principal.getName());
        model.addAttribute("assessment", assessments.get(key, userId, false));
        model.addAttribute("assessmentBasePath", "/student/assessments/");
        return "student/assessment";
    }

    @GetMapping("/admin/assessments/{key}")
    public String showAdminAssessment(@PathVariable String key, Principal principal, Model model) {
        Integer userId = users.getUserIdByEmail(principal.getName());
        model.addAttribute("assessment", assessments.get(key, userId, true));
        model.addAttribute("assessmentBasePath", "/admin/assessments/");
        return "student/assessment";
    }

    @PostMapping("/student/assessments/{key}/retry")
    public String retryStudentAssessment(
            @PathVariable String key, Principal principal, RedirectAttributes redirect) {
        Integer userId = users.getUserIdByEmail(principal.getName());
        assessments.retry(key, userId, false);
        redirect.addFlashAttribute("successMessage", "Đã gửi yêu cầu chấm lại phần lỗi.");
        return "redirect:/student/assessments/" + key;
    }

    @PostMapping("/admin/assessments/{key}/retry")
    public String retryAdminAssessment(
            @PathVariable String key, Principal principal, RedirectAttributes redirect) {
        Integer userId = users.getUserIdByEmail(principal.getName());
        assessments.retry(key, userId, true);
        redirect.addFlashAttribute("successMessage", "Đã gửi yêu cầu chấm lại phần lỗi.");
        return "redirect:/admin/assessments/" + key;
    }
}
