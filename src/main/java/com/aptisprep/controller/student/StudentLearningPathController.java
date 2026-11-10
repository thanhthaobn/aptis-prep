package com.aptisprep.controller.student;

import com.aptisprep.mvc.common.exception.BusinessRuleException;
import com.aptisprep.mvc.common.exception.ResourceNotFoundException;
import com.aptisprep.mvc.flashcard.service.FlashcardSetService;
import com.aptisprep.service.learning.LearningPathService;
import com.aptisprep.service.learning.LessonService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StudentLearningPathController {
    private final LearningPathService service;
    private final LessonService lessons;
    private final FlashcardSetService flashcards;

    public StudentLearningPathController(
            LearningPathService s, LessonService l, FlashcardSetService f) {
        service = s;
        lessons = l;
        flashcards = f;
    }

    @PostMapping("/student/learning-path/refresh")
    public String refresh(Principal principal, RedirectAttributes redirect) {
        int changed = service.refreshPaths(principal.getName());
        redirect.addFlashAttribute(
                "message",
                changed == 0
                        ? "Lộ trình đã cập nhật mới nhất."
                        : "Đã cập nhật lộ trình. Các bài đã hoàn thành được giữ lại.");
        return "redirect:/student/learning-path";
    }

    @GetMapping("/student/learning-path")
    public String paths(Principal principal, Model model) {
        service.refreshPaths(principal.getName());
        var stages = service.stages(principal.getName(), null);
        boolean hasAssessment = stages.stream().anyMatch(stage -> stage.currentLevel() != null);
        model.addAttribute("stages", stages);
        model.addAttribute("hasAssessment", hasAssessment);
        model.addAttribute("learningLevels", LessonService.LEVELS);
        return "student/learning/path";
    }

    @GetMapping("/student/learning-path/{skill}")
    public String skill(@PathVariable String skill, Principal principal, Model model) {
        String selected =
                LessonService.SKILLS.stream()
                        .filter(value -> value.equalsIgnoreCase(skill))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỹ năng"));
        model.addAttribute("selectedSkill", selected);
        model.addAttribute("lessonSets", flashcards.lessonSets());
        model.addAttribute(
                "paths",
                service.ensureAndSummaries(principal.getName()).stream()
                        .filter(path -> path.skill().equals(selected))
                        .toList());
        model.addAttribute("stages", service.stages(principal.getName(), selected));
        model.addAttribute("learningLevels", LessonService.LEVELS);
        return "student/learning/skill";
    }

    @PostMapping("/student/learning-path/{pathId}/checkpoint/start")
    public String startCheckpoint(
            @PathVariable Integer pathId, Principal principal, RedirectAttributes redirect) {
        try {
            Integer user = service.userId(principal.getName());
            Integer attemptId = service.startCheckpoint(user, pathId, null);
            return "redirect:" + service.checkpointDestination(principal.getName(), attemptId);
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/student/learning-path";
        }
    }
}
