package com.aptisprep.mvc.controller.student;

import com.aptisprep.mvc.dto.*;
import com.aptisprep.mvc.dto.UserGoalDTO;
import com.aptisprep.mvc.enums.Skill;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.service.account.AccountAccessService;
import com.aptisprep.mvc.service.account.AccountProfileService;
import com.aptisprep.mvc.service.account.LearningActivityService;
import com.aptisprep.mvc.service.exam.ExamAttemptService;
import com.aptisprep.mvc.service.learning.LessonService;
import com.aptisprep.mvc.service.placement.PlacementService;
import com.aptisprep.mvc.service.scoring.ScoringGuideService;
import com.aptisprep.mvc.support.contract.*;
import com.aptisprep.mvc.support.contract.IntegrationModels.AttemptHandle;
import com.aptisprep.mvc.support.pagination.ListPagination;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StudentAccountController {
    private final ScoringGuideService scoringGuideService;

    private final PlacementService placementService;
    private final AccountProfileService accountProfileService;
    private final LearningActivityService learningActivityService;
    private final ExamAttemptService examAttemptContract;
    private final AccountAccessService accountAccessService;
    private final LessonService lessonService;

    public StudentAccountController(
            PlacementService placementService,
            AccountProfileService accountProfileService,
            LearningActivityService learningActivityService,
            ScoringGuideService scoringGuideService,
            ExamAttemptService examAttemptContract,
            AccountAccessService accountAccessService,
            LessonService lessonService) {
        this.placementService = placementService;
        this.accountProfileService = accountProfileService;
        this.scoringGuideService = scoringGuideService;
        this.learningActivityService = learningActivityService;
        this.examAttemptContract = examAttemptContract;
        this.accountAccessService = accountAccessService;
        this.lessonService = lessonService;
    }

    @GetMapping("/student/profile")
    public String profile(Principal principal, Model model) {
        UserDTO account = accountProfileService.profile(principal.getName());
        model.addAttribute("account", account);
        model.addAttribute("goal", accountProfileService.goal(principal.getName()));
        model.addAttribute("profiles", accountProfileService.skillProfiles(principal.getName()));
        model.addAttribute("activity", learningActivityService.activity(principal.getName()));
        model.addAttribute("skillCodes", Skill.codes());
        if (!model.containsAttribute("profileForm")) {
            UserDTO form = new UserDTO();
            form.setFullName(account.getFullName());
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
            @Valid @ModelAttribute("profileForm") UserDTO profileForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/student/profile";
        }
        accountProfileService.updateProfile(principal.getName(), profileForm);
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật hồ sơ.");
        return "redirect:/student/profile";
    }

    @GetMapping("/student/goals")
    public String goals(Principal principal, Model model) {
        model.addAttribute("learningLevels", lessonService.getLevels());
        if (!model.containsAttribute("goalForm"))
            model.addAttribute(
                    "goalForm",
                    accountProfileService.goal(principal.getName()) == null
                            ? new UserGoalDTO()
                            : accountProfileService.goal(principal.getName()));
        return "student/account/goals";
    }

    @PostMapping("/student/goals")
    public String saveGoal(
            Principal principal,
            @Valid @ModelAttribute("goalForm") UserGoalDTO goalForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("learningLevels", lessonService.getLevels());
            return "student/account/goals";
        }
        try {
            accountProfileService.saveGoal(principal.getName(), goalForm);
        } catch (BusinessRuleException exception) {
            bindingResult.reject("goal.invalid", exception.getMessage());
            model.addAttribute("learningLevels", lessonService.getLevels());
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
                ListPagination.slice(
                        placementService.availableForStudent(principal.getName()), page);
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
            AttemptHandle handle =
                    skill != null || examId != null
                            ? placementService.start(principal.getName(), skill, examId)
                            : placementService.start(principal.getName());
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
        var result = placementService.result(principal.getName(), attemptId);
        if ("IN_PROGRESS".equals(result.state())) return "redirect:/student/attempts/" + attemptId;
        if ("EXPIRED".equals(result.state()) && result.submittedAt() == null)
            return "redirect:/student/placement";
        model.addAttribute("attemptResult", result);
        model.addAttribute("fromPlacement", true);
        Integer userId = accountAccessService.requireActiveStudent(principal.getName());
        boolean terminalResult =
                "COMPLETED".equalsIgnoreCase(result.state())
                        || "EXPIRED".equalsIgnoreCase(result.state())
                        || "ERROR".equalsIgnoreCase(result.state());
        if ("PLACEMENT".equalsIgnoreCase(result.examType()) && terminalResult) {
            accountAccessService.applyAssessment(userId, attemptId);
        }
        var scoreGuideView = scoringGuideService.forResultView(result);
        model.addAttribute("scoreGuides", scoreGuideView.guides());
        model.addAttribute("overallScaleMax", scoreGuideView.combinedScaleMax());
        model.addAttribute("feedbackWithoutGuide", scoreGuideView.feedbackWithoutGuide());
        return "student/attempt/result";
    }
}
