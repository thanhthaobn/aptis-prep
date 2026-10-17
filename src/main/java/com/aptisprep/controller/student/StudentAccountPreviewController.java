package com.aptisprep.controller.student;

import com.aptisprep.dto.form.GoalForm;
import com.aptisprep.dto.form.ProfileForm;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.account.AccountProfileService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StudentAccountPreviewController {
    private final AccountProfileService accounts;
    public StudentAccountPreviewController(AccountProfileService accounts) { this.accounts = accounts; }

    @GetMapping("/student/profile")
    public String profile(Principal principal, Model model) {
        var snapshot = accounts.snapshot(principal.getName());
        model.addAttribute("account", snapshot);
        if (!model.containsAttribute("profileForm")) {
            ProfileForm form = new ProfileForm(); form.setFullName(snapshot.fullName()); model.addAttribute("profileForm", form);
        }
        return "student/account/profile";
    }

    @GetMapping("/student/profile/edit")
    public String profileEdit() { return "redirect:/student/profile"; }

    @PostMapping("/student/profile/edit")
    public String updateProfile(Principal principal, @Valid @ModelAttribute("profileForm") ProfileForm profileForm,
            BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/student/profile";
        }
        accounts.updateProfile(principal.getName(), profileForm);
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật hồ sơ.");
        return "redirect:/student/profile";
    }

    @GetMapping("/student/goals")
    public String goals(Principal principal, Model model) {
        var snapshot = accounts.snapshot(principal.getName());
        model.addAttribute("account", snapshot);
        model.addAttribute("certificates", accounts.activeCertificates());
        if (!model.containsAttribute("goalForm")) model.addAttribute("goalForm", accounts.formFrom(snapshot));
        return "student/account/goals";
    }

    @PostMapping("/student/goals")
    public String saveGoal(Principal principal, @Valid @ModelAttribute("goalForm") GoalForm goalForm,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("account", accounts.snapshot(principal.getName()));
            model.addAttribute("certificates", accounts.activeCertificates());
            return "student/account/goals";
        }
        try {
            accounts.saveGoal(principal.getName(), goalForm);
        } catch (BusinessRuleException exception) {
            bindingResult.reject("goal.invalid", exception.getMessage());
            model.addAttribute("account", accounts.snapshot(principal.getName()));
            model.addAttribute("certificates", accounts.activeCertificates());
            return "student/account/goals";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đã lưu mục tiêu. Lộ trình sẽ được cập nhật sau khi lưu thành công.");
        return "redirect:/student/goals";
    }

    @GetMapping("/student/placement")
    public String placement() { return "student/account/placement"; }
}
