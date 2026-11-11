package com.aptisprep.controller.student;

import com.aptisprep.controller.ListPagination;
import com.aptisprep.enums.Skill;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.presentation.AccountSnapshotDTO;
import com.aptisprep.presentation.AttemptHandleDTO;
import com.aptisprep.presentation.form.GoalFormDTO;
import com.aptisprep.presentation.form.ProfileFormDTO;
import com.aptisprep.service.account.AccountAccessService;
import com.aptisprep.service.account.AccountProfileService;
import com.aptisprep.service.account.LearningActivityService;
import com.aptisprep.service.exam.ExamAttemptService;
import com.aptisprep.service.exam.PlacementService;
import com.aptisprep.service.learning.LessonService;
import com.aptisprep.service.scoring.ScoringGuideService;
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
    private final ExamAttemptService attempts;
    private final AccountAccessService access;

    public StudentAccountController(
            PlacementService placement,
            AccountProfileService accounts,
            LearningActivityService activity,
            ScoringGuideService scoringGuides,
            ExamAttemptService attempts,
            AccountAccessService access) {
        this.placement = placement;
        this.accounts = accounts;
        this.scoringGuides = scoringGuides;
        this.activity = activity;
        this.attempts = attempts;
        this.access = access;
    }

    @GetMapping("/student/profile")
    public String profile(Principal principal, Model model) {
        AccountSnapshotDTO snapshot = accounts.snapshot(principal.getName());
        model.addAttribute("account", snapshot);
        model.addAttribute("activity", activity.activity(principal.getName()));
        model.addAttribute("skillCodes", Skill.codes());
        if (!model.containsAttribute("profileForm")) {
            ProfileFormDTO form = new ProfileFormDTO();
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
            @Valid @ModelAttribute("profileForm") ProfileFormDTO profileForm,
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
        model.addAttribute("learningLevels", LessonService.LEVELS);
        if (!model.containsAttribute("goalForm"))
            model.addAttribute(
                    "goalForm", accounts.formFrom(accounts.snapshot(principal.getName())));
        return "student/account/goals";
    }

    @PostMapping("/student/goals")
    public String saveGoal(
            Principal principal,
            @Valid @ModelAttribute("goalForm") GoalFormDTO goalForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("learningLevels", LessonService.LEVELS);
            return "student/account/goals";
        }
        try {
            accounts.saveGoal(principal.getName(), goalForm);
        } catch (BusinessRuleException exception) {
            bindingResult.reject("goal.invalid", exception.getMessage());
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
        model.addAttribute("skill", "FULL_MOCK");
        var placementPage =
                ListPagination.slice(placement.availableForStudent(principal.getName()), page);
        model.addAttribute("placementPage", placementPage);
        model.addAttribute("placementExams", placementPage.getContent());
        return "student/account/placement";
    }

    @PostMapping("/student/placement/start")
    public String start(
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) Integer examId,
            Principal principal,
            RedirectAttributes redirect) {
        try {
            AttemptHandleDTO handle =
                    skill != null || examId != null
                            ? placement.start(principal.getName(), skill, examId)
                            : placement.start(principal.getName());
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
        if ("IN_PROGRESS".equals(result.state())) return "redirect:/student/attempts/" + attemptId;
        if ("EXPIRED".equals(result.state()) && result.submittedAt() == null)
            return "redirect:/student/placement";
        model.addAttribute("attemptResult", result);
        model.addAttribute("fromPlacement", true);
        Integer userId = access.requireActiveStudent(principal.getName());
        boolean terminalResult =
                "COMPLETED".equalsIgnoreCase(result.state())
                        || "EXPIRED".equalsIgnoreCase(result.state())
                        || "ERROR".equalsIgnoreCase(result.state());
        if ("PLACEMENT".equalsIgnoreCase(result.examType()) && terminalResult) {
            access.applyAssessment(userId, attemptId);
        }
        var scoreGuideView = scoringGuides.forResultView(result);
        model.addAttribute("scoreGuides", scoreGuideView.guides());
        model.addAttribute("feedbackWithoutGuide", scoreGuideView.feedbackWithoutGuide());
        return "student/attempt-result";
    }

    private List<String> placementSkills() {
        return List.of(
                "FULL_MOCK", "GRAMMAR_VOCABULARY", "READING", "LISTENING", "WRITING", "SPEAKING");
    }
}
