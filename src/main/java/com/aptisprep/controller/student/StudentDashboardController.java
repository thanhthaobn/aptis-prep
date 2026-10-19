package com.aptisprep.controller.student;

import com.aptisprep.service.contract.IntegrationModels.PathProgress;
import com.aptisprep.service.learning.LearningPathService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/student")
public class StudentDashboardController {
    private final LearningPathService learningPaths;

    public StudentDashboardController(LearningPathService learningPaths) {
        this.learningPaths = learningPaths;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        List<PathProgress> paths = learningPaths.ensureAndSummaries(principal.getName());
        int completedLessons = paths.stream().mapToInt(PathProgress::completedLessons).sum();
        int totalLessons = paths.stream().mapToInt(PathProgress::totalLessons).sum();
        PathProgress nextPath = paths.stream().filter(path -> path.nextLessonId() != null).findFirst().orElse(null);
        model.addAttribute("email", principal.getName());
        model.addAttribute("paths", paths);
        model.addAttribute("completedLessons", completedLessons);
        model.addAttribute("totalLessons", totalLessons);
        model.addAttribute("nextPath", nextPath);
        model.addAttribute("hasLearningContent", totalLessons > 0);
        return "student/dashboard";
    }

    /** Read-only orientation preview: no goals, attempts or progress are created. */
    @GetMapping("/orientation")
    public String orientation() {
        return "redirect:/student/learning-path";
    }
}
