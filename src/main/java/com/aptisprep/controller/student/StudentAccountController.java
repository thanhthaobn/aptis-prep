package com.aptisprep.controller.student;

import com.aptisprep.controller.ListPagination;
import com.aptisprep.dto.form.GoalForm;
import com.aptisprep.dto.form.ProfileForm;
import com.aptisprep.dto.view.AccountSnapshot;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.account.AccountProfileService;
import com.aptisprep.service.account.LearningActivityService;
import com.aptisprep.service.contract.IntegrationModels.AttemptHandle;
import com.aptisprep.service.placement.PlacementService;
import com.aptisprep.service.scoring.ScoringGuideService;
import com.aptisprep.service.learning.LessonService;
import com.aptisprep.enums.Skill;
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
    private final ScoringGuideService scoringGuides;

    private final PlacementService placement;
    private final AccountProfileService accounts;
    private final LearningActivityService activity;

    public StudentAccountController(
            PlacementService placement,
            AccountProfileService accounts,
            LearningActivityService activity,
            ScoringGuideService scoringGuides) {
        this.placement = placement;
        this.accounts = accounts;
        this.scoringGuides = scoringGuides;
        this.activity = activity;
    }

    @GetMapping("/student/profile")
    public String profile(Principal principal, Model model) {
        AccountSnapshot snapshot = accounts.snapshot(principal.getName());
        model.addAttribute("account", snapshot);
        model.addAttribute("activity", activity.activity(principal.getName()));
        model.addAttribute("skillCodes", Skill.codes());
        if (!model.containsAttribute("profileForm")) {
            ProfileForm form = new ProfileForm();
            form.setFullName(snapshot.fullName());
            model.addAttribute("profileForm", form);
        }
        return "student/account/profile";
    }

    @GetMapping("/student/profile/edit")
    public String profileEdit() {
        return "redirect:/student/profile";
    }

    @PostMapping("/student/profile/edit")
    public String updateProfile(
            Principal principal,
            @Valid @ModelAttribute("profileForm") ProfileForm profileForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/student/profile";
        }
        accounts.updateProfile(principal.getName(), profileForm);
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật hồ sơ.");
        return "redirect:/student/profile";
    }

    @GetMapping("/student/goals")
    public String goals(Principal principal, Model model) {
        model.addAttribute("certificates", accounts.activeCertificates());
        model.addAttribute("learningLevels", LessonService.LEVELS);
        if (!model.containsAttribute("goalForm"))
            model.addAttribute(
                    "goalForm", accounts.formFrom(accounts.snapshot(principal.getName())));
        return "student/account/goals";
    }

    @PostMapping("/student/goals")
    public String saveGoal(
            Principal principal,
            @Valid @ModelAttribute("goalForm") GoalForm goalForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("certificates", accounts.activeCertificates());
            model.addAttribute("learningLevels", LessonService.LEVELS);
            return "student/account/goals";
        }
        try {
            accounts.saveGoal(principal.getName(), goalForm);
        } catch (BusinessRuleException exception) {
            bindingResult.reject("goal.invalid", exception.getMessage());
            model.addAttribute("certificates", accounts.activeCertificates());
            model.addAttribute("learningLevels", LessonService.LEVELS);
            return "student/account/goals";
        }
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Đã lưu mục tiêu. Lộ trình sẽ được cập nhật sau khi lưu thành công.");
        return "redirect:/student/goals";
    }

    @GetMapping("/student/placement")
    public String placement(
            @RequestParam(required = false) String skill,
            @RequestParam(defaultValue = "0") int page,
            Principal principal,
            Model model) {
        if (skill == null || skill.isBlank()) skill = "FULL_MOCK";
        if (!placementSkills().contains(skill))
            throw new BusinessRuleException("Kỹ năng Placement không hợp lệ");
        model.addAttribute("skill", skill);
        var placementPage =
                ListPagination.slice(placement.available(principal.getName(), skill), page);
        model.addAttribute("placementPage", placementPage);
        model.addAttribute("placementExams", placementPage.getContent());
        return "student/account/placement";
    }

    @PostMapping("/student/placement/start")
    public String start(
            @RequestParam String skill,
            @RequestParam Integer examId,
            Principal principal,
            RedirectAttributes redirect) {
        try {
            AttemptHandle handle = placement.start(principal.getName(), skill, examId);
            return "redirect:/student/attempts/"
                    + handle.attemptId()
                    + ("IN_PROGRESS".equals(handle.state()) ? "" : "/result");
        } catch (BusinessRuleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            redirect.addAttribute("skill", skill);
            return "redirect:/student/placement";
        }
    }

    @GetMapping("/student/placement/result")
    public String result(@RequestParam Integer attemptId, Principal principal, Model model) {
        var result = placement.result(principal.getName(), attemptId);
        model.addAttribute("placementResult", result);
        var scoreGuideView = scoringGuides.forResultView(result);
        model.addAttribute("scoreGuides", scoreGuideView.guides());
        model.addAttribute("feedbackWithoutGuide", scoreGuideView.feedbackWithoutGuide());
        return "student/account/placement-result";
    }

    private List<String> placementSkills() {
        return List.of(
                "FULL_MOCK", "GRAMMAR_VOCABULARY", "READING", "LISTENING", "WRITING", "SPEAKING");
    }
}
