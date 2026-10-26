package com.aptisprep.controller.student;

import com.aptisprep.dto.view.AccountSnapshot.SkillView;
import com.aptisprep.enums.Skill;
import com.aptisprep.service.account.AccountProfileService;
import com.aptisprep.service.account.LearningActivityService;
import com.aptisprep.service.contract.ExamAttemptContract;
import com.aptisprep.service.contract.IntegrationModels.PathProgress;
import com.aptisprep.service.learning.LearningPathService;
import com.aptisprep.service.learning.LessonRecommendationService;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/student")
public class StudentDashboardController {
    private final LearningPathService learningPaths;
    private final ObjectProvider<ExamAttemptContract> attempts;
    private final AccountProfileService accounts;
    private final LearningActivityService activity;
    private final LessonRecommendationService recommendations;

    public StudentDashboardController(
            LearningPathService learningPaths,
            ObjectProvider<ExamAttemptContract> attempts,
            AccountProfileService accounts,
            LearningActivityService activity,
            LessonRecommendationService recommendations) {
        this.learningPaths = learningPaths;
        this.attempts = attempts;
        this.accounts = accounts;
        this.activity = activity;
        this.recommendations = recommendations;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        List<PathProgress> paths = learningPaths.ensureAndSummaries(principal.getName());
        int completedLessons = paths.stream().mapToInt(PathProgress::completedLessons).sum();
        int totalLessons = paths.stream().mapToInt(PathProgress::totalLessons).sum();
        var recommendation =
                recommendations.recommend(learningPaths.userId(principal.getName()), paths);
        model.addAttribute("email", principal.getName());
        model.addAttribute("paths", paths);
        model.addAttribute("completedLessons", completedLessons);
        model.addAttribute("totalLessons", totalLessons);
        model.addAttribute("recommendation", recommendation);
        model.addAttribute("hasLearningContent", totalLessons > 0);
        var account = accounts.snapshot(principal.getName());
        model.addAttribute("account", account);
        var skillOverview = new ArrayList<SkillOverview>();
        for (var skill : Skill.values()) {
            String code = skill.name();
            String label =
                    switch (skill) {
                        case GRAMMAR_VOCABULARY -> "Ngữ pháp & từ vựng";
                        case READING -> "Reading";
                        case LISTENING -> "Listening";
                        case WRITING -> "Writing";
                        case SPEAKING -> "Speaking";
                    };
            String current =
                    account.profiles().stream()
                            .filter(profile -> code.equals(profile.skill()))
                            .map(SkillView::currentLevel)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse("—");
            String goal =
                    account.goal() == null
                            ? "Chưa đặt mục tiêu"
                            : account.goal().targets().getOrDefault(code, "Chưa đặt mục tiêu");
            skillOverview.add(new SkillOverview(label, current, goal));
        }
        model.addAttribute("skillOverview", skillOverview);
        model.addAttribute("activity", activity.activity(principal.getName()));
        ExamAttemptContract attemptService = attempts.getIfAvailable();
        var latestResult =
                attemptService == null
                        ? null
                        : attemptService
                                .history(learningPaths.userId(principal.getName()), 0, 1)
                                .items()
                                .stream()
                                .findFirst()
                                .orElse(null);
        model.addAttribute("latestResult", latestResult);
        return "student/dashboard";
    }

    public record SkillOverview(String label, String current, String goal) {}

    /** Read-only orientation preview: no goals, attempts or progress are created. */
    @GetMapping("/orientation")
    public String orientation() {
        return "redirect:/student/learning-path";
    }
}
