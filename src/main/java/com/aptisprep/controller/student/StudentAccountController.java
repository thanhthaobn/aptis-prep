package com.aptisprep.controller.student;

import com.aptisprep.dto.form.GoalForm;
import com.aptisprep.dto.form.ProfileForm;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.account.AccountProfileService;
import com.aptisprep.service.account.LearningActivityService;
import com.aptisprep.service.placement.PlacementService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StudentAccountController {
    private final PlacementService placement;
    private final AccountProfileService accounts;
    private final LearningActivityService activity;
    public StudentAccountController(PlacementService placement, AccountProfileService accounts, LearningActivityService activity) {
        this.placement = placement; this.accounts = accounts; this.activity = activity;
    }
    @GetMapping("/student/profile")
    public String profile(Principal principal, Model model) {
        var snapshot = accounts.snapshot(principal.getName());
        model.addAttribute("account", snapshot);
        model.addAttribute("activity", activity.activity(principal.getName()));
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
    public String placement(@RequestParam(defaultValue = "READING") String skill, Model model) {
        if (!placementSkills().contains(skill)) throw new BusinessRuleException("Kỹ năng Placement không hợp lệ");
        model.addAttribute("skill", skill); model.addAttribute("placementExams", placement.available(skill)); return "student/account/placement";
    }

    @PostMapping("/student/placement/start")
    public String start(@RequestParam String skill, @RequestParam Integer examId, Principal principal, RedirectAttributes redirect) {
        try { var handle = placement.start(principal.getName(), skill, examId); return "redirect:/student/attempts/" + handle.attemptId(); }
        catch (BusinessRuleException e) { redirect.addFlashAttribute("error", e.getMessage()); return "redirect:/student/placement?skill=" + skill; }
    }

    @GetMapping("/student/placement/result")
    public String result(@RequestParam Integer attemptId, Principal principal, Model model) {
        model.addAttribute("placementResult", placement.result(principal.getName(), attemptId)); return "student/account/placement-result";
    }

    private List<String> placementSkills() { return List.of("GRAMMAR_VOCABULARY", "READING", "LISTENING", "WRITING", "SPEAKING"); }
}
