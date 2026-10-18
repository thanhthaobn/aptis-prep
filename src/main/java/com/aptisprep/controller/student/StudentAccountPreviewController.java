package com.aptisprep.controller.student;

import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.placement.PlacementService;
import java.security.Principal;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class StudentAccountPreviewController {
    private final PlacementService placement;
    public StudentAccountPreviewController(PlacementService placement) { this.placement = placement; }
    @GetMapping("/student/profile")
    public String profile(Principal principal, Model model) {
        model.addAttribute("email", principal.getName());
        return "student/account/profile";
    }
    @GetMapping("/student/placement")
    public String placement(@RequestParam(defaultValue = "READING") String skill, Model model) {
        if (!placementSkills().contains(skill)) throw new BusinessRuleException("Kỹ năng Placement không hợp lệ");
        model.addAttribute("skill", skill); model.addAttribute("placementExams", placement.available(skill)); return "student/account/placement";
    }

    @PostMapping("/student/placement/start")
    public String start(@RequestParam String skill, Principal principal, org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        try { var handle = placement.start(principal.getName(), skill); return "redirect:/student/placement/result?attemptId=" + handle.attemptId(); }
        catch (BusinessRuleException e) { redirect.addFlashAttribute("error", e.getMessage()); return "redirect:/student/placement?skill=" + skill; }
    }

    @GetMapping("/student/placement/result")
    public String result(@RequestParam Integer attemptId, Principal principal, Model model) {
        model.addAttribute("placementResult", placement.result(principal.getName(), attemptId)); return "student/account/placement-result";
    }

    private List<String> placementSkills() { return List.of("GRAMMAR_VOCABULARY", "READING", "LISTENING", "WRITING", "SPEAKING"); }
}
