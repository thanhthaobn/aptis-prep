package com.aptisprep.mvc.controller.student;

import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.exception.ResourceNotFoundException;
import com.aptisprep.mvc.service.learning.FlashcardSetService;
import com.aptisprep.mvc.service.learning.LearningPathService;
import com.aptisprep.mvc.service.learning.LessonService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StudentLearningPathController {
    private final LearningPathService learningPathService;
    private final LessonService lessonService;
    private final FlashcardSetService flashcardSetService;

    public StudentLearningPathController(
            LearningPathService learningPathService, LessonService lessonService, FlashcardSetService flashcardSetService) {
        this.learningPathService = learningPathService;
        this.lessonService = lessonService;
        this.flashcardSetService = flashcardSetService;
    }

    @PostMapping("/student/learning-path/refresh")
    public String refresh(Principal principal, RedirectAttributes redirect) {
        int changed = learningPathService.refreshPaths(principal.getName());
        redirect.addFlashAttribute(
                "message",
                changed == 0
                        ? "Lộ trình đã cập nhật mới nhất."
                        : "Đã cập nhật lộ trình. Các bài đã hoàn thành được giữ lại.");
        return "redirect:/student/learning-path";
    }

    @GetMapping("/student/learning-path")
    public String paths(Principal principal, Model model) {
        learningPathService.refreshPaths(principal.getName());
        var stages = learningPathService.stages(principal.getName(), null);
        boolean hasAssessment = stages.stream().anyMatch(stage -> stage.currentLevel() != null);
        model.addAttribute("stages", stages);
        model.addAttribute("hasAssessment", hasAssessment);
        model.addAttribute("learningLevels", lessonService.getLevels());
        return "student/learning/path";
    }

    @GetMapping("/student/learning-path/{skill}")
    public String skill(@PathVariable String skill, Principal principal, Model model) {
        String selected =
                lessonService.getSkills().stream()
                        .filter(value -> value.equalsIgnoreCase(skill))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỹ năng"));
        model.addAttribute("selectedSkill", selected);
        model.addAttribute("lessonSets", flashcardSetService.lessonSets());
        model.addAttribute(
                "paths",
                learningPathService.ensureAndSummaries(principal.getName()).stream()
                        .filter(path -> path.skill().equals(selected))
                        .toList());
        model.addAttribute("stages", learningPathService.stages(principal.getName(), selected));
        model.addAttribute("learningLevels", lessonService.getLevels());
        return "student/learning/skill";
    }

    @PostMapping("/student/learning-path/{pathId}/checkpoint/start")
    public String startCheckpoint(
            @PathVariable Integer pathId, Principal principal, RedirectAttributes redirect) {
        try {
            Integer user = learningPathService.userId(principal.getName());
            Integer attemptId = learningPathService.startCheckpoint(user, pathId, null);
            var result = learningPathService.checkpointResult(principal.getName(), attemptId);
            String destination =
                    "/student/attempts/"
                            + attemptId
                            + ("IN_PROGRESS".equals(result.state()) ? "" : "/result");
            return "redirect:" + destination;
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/student/learning-path";
        }
    }

    @PostMapping("/student/attempts/{attemptId}/checkpoint/retry")
    public String retryCheckpoint(
            @PathVariable Integer attemptId, Principal principal, RedirectAttributes redirect) {
        try {
            Integer user = learningPathService.userId(principal.getName());
            Integer retryAttemptId = learningPathService.retryCheckpoint(user, attemptId);
            var result = learningPathService.checkpointResult(principal.getName(), retryAttemptId);
            String destination =
                    "/student/attempts/"
                            + retryAttemptId
                            + ("IN_PROGRESS".equals(result.state()) ? "" : "/result");
            return "redirect:" + destination;
        } catch (BusinessRuleException | ResourceNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/student/attempts/" + attemptId + "/result";
        }
    }
}







