package com.aptisprep.controller;

import com.aptisprep.entity.User;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.UserRepository;
import com.aptisprep.service.contract.AiAssessmentContract;
import java.security.Principal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AiAssessmentController {
    private final AiAssessmentContract assessments;
    private final UserRepository users;
    public AiAssessmentController(AiAssessmentContract assessments, UserRepository users) { this.assessments = assessments; this.users = users; }

    @GetMapping({"/student/assessments/{key}", "/admin/assessments/{key}"})
    public String detail(@PathVariable String key, Principal principal, Authentication authentication, Model model) {
        User user = user(principal); boolean admin = authentication.getAuthorities().stream().anyMatch(value -> value.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("assessment", assessments.get(key, user.getId(), admin));
        model.addAttribute("assessmentBasePath", admin ? "/admin/assessments/" : "/student/assessments/");
        return "student/assessment";
    }

    @PostMapping({"/student/assessments/{key}/retry", "/admin/assessments/{key}/retry"})
    public String retry(@PathVariable String key, Principal principal, Authentication authentication, RedirectAttributes redirect) {
        User user = user(principal); boolean admin = authentication.getAuthorities().stream().anyMatch(value -> value.getAuthority().equals("ROLE_ADMIN"));
        assessments.retry(key, user.getId(), admin);
        redirect.addFlashAttribute("successMessage", "Đã gửi yêu cầu chấm lại phần lỗi.");
        return "redirect:/" + (admin ? "admin" : "student") + "/assessments/" + key;
    }

    private User user(Principal principal) { return users.findByEmailIgnoreCase(principal.getName()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản")); }
}
