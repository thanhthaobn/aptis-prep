package com.aptisprep.controller;

import com.aptisprep.entity.User;
import com.aptisprep.enums.Role;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.UserRepository;
import com.aptisprep.service.contract.AiAssessmentContract;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AiAssessmentController {
    private final AiAssessmentContract assessments;
    private final UserRepository userRepository;

    public AiAssessmentController(AiAssessmentContract assessments, UserRepository userRepository) {
        this.assessments = assessments;
        this.userRepository = userRepository;
    }

    @GetMapping("/student/assessments/{key}")
    public String showStudentAssessment(
            @PathVariable String key, Principal principal, Model model) {
        return renderAssessment(key, principal, model);
    }

    @GetMapping("/admin/assessments/{key}")
    public String showAdminAssessment(@PathVariable String key, Principal principal, Model model) {
        return renderAssessment(key, principal, model);
    }

    private String renderAssessment(String key, Principal principal, Model model) {
        User user = user(principal);
        boolean admin = user.getRole() == Role.ADMIN;
        model.addAttribute("assessment", assessments.get(key, user.getId(), admin));
        model.addAttribute(
                "assessmentBasePath", admin ? "/admin/assessments/" : "/student/assessments/");
        return "student/assessment";
    }

    @PostMapping("/student/assessments/{key}/retry")
    public String retryStudentAssessment(
            @PathVariable String key, Principal principal, RedirectAttributes redirect) {
        return retryAssessment(key, principal, redirect);
    }

    @PostMapping("/admin/assessments/{key}/retry")
    public String retryAdminAssessment(
            @PathVariable String key, Principal principal, RedirectAttributes redirect) {
        return retryAssessment(key, principal, redirect);
    }

    private String retryAssessment(String key, Principal principal, RedirectAttributes redirect) {
        User user = user(principal);
        boolean admin = user.getRole() == Role.ADMIN;
        assessments.retry(key, user.getId(), admin);
        redirect.addFlashAttribute("successMessage", "Đã gửi yêu cầu chấm lại phần lỗi.");
        return "redirect:/" + (admin ? "admin" : "student") + "/assessments/" + key;
    }

    private User user(Principal principal) {
        return userRepository
                .findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
    }
}
