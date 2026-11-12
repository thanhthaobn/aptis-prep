package com.aptisprep.mvc.controller;

import com.aptisprep.mvc.dto.UserDTO;
import com.aptisprep.mvc.enums.Role;
import com.aptisprep.mvc.service.account.AccountAccessService;
import com.aptisprep.mvc.service.grading.AiAssessmentService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AiAssessmentController {
    private final AiAssessmentService aiAssessmentContract;
    private final AccountAccessService accountAccessService;

    public AiAssessmentController(
            AiAssessmentService aiAssessmentContract, AccountAccessService accountAccessService) {
        this.aiAssessmentContract = aiAssessmentContract;
        this.accountAccessService = accountAccessService;
    }

    @GetMapping("/student/assessments/{key}")
    public String showStudentAssessment(
            @PathVariable String key, Principal principal, Model model) {
        UserDTO user = accountAccessService.requireAccountView(principal.getName());
        model.addAttribute("assessment", aiAssessmentContract.get(key, user.getId(), false));
        model.addAttribute("assessmentBasePath", "/student/assessments/");
        return "student/assessment";
    }

    @GetMapping("/admin/assessments/{key}")
    public String showAdminAssessment(@PathVariable String key, Principal principal, Model model) {
        UserDTO user = accountAccessService.requireAccountView(principal.getName());
        model.addAttribute("assessment", aiAssessmentContract.get(key, user.getId(), true));
        model.addAttribute("assessmentBasePath", "/admin/assessments/");
        return "student/assessment";
    }

    @PostMapping("/student/assessments/{key}/retry")
    public String retryStudentAssessment(
            @PathVariable String key, Principal principal, RedirectAttributes redirect) {
        UserDTO user = accountAccessService.requireAccountView(principal.getName());
        aiAssessmentContract.retry(key, user.getId(), false);
        redirect.addFlashAttribute("successMessage", "Đã gửi yêu cầu chấm lại phần lỗi.");
        return "redirect:/student/assessments/" + key;
    }

    @PostMapping("/admin/assessments/{key}/retry")
    public String retryAdminAssessment(
            @PathVariable String key, Principal principal, RedirectAttributes redirect) {
        UserDTO user = accountAccessService.requireAccountView(principal.getName());
        aiAssessmentContract.retry(key, user.getId(), true);
        redirect.addFlashAttribute("successMessage", "Đã gửi yêu cầu chấm lại phần lỗi.");
        return "redirect:/admin/assessments/" + key;
    }
}







