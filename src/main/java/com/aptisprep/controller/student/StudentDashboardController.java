package com.aptisprep.controller.student;

import com.aptisprep.dto.view.AccountSnapshotDTO;
import com.aptisprep.dto.view.AccountSnapshotDTO.SkillViewDTO;
import com.aptisprep.dto.view.AttemptResultDTO;
import com.aptisprep.dto.view.LessonRecommendationDTO;
import com.aptisprep.dto.view.PathProgressDTO;
import com.aptisprep.dto.view.StudentDashboardViewDTO.NextAction;
import com.aptisprep.dto.view.StudentDashboardViewDTO.SkillOverview;
import com.aptisprep.enums.Skill;
import com.aptisprep.service.account.AccountProfileService;
import com.aptisprep.service.account.LearningActivityService;
import com.aptisprep.service.exam.ExamAttemptService;
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

@Controller
public class StudentDashboardController {
    private final LearningPathService learningPaths;
    private final ObjectProvider<ExamAttemptService> attempts;
    private final AccountProfileService accounts;
    private final LearningActivityService activity;
    private final LessonRecommendationService recommendations;

    public StudentDashboardController(
            LearningPathService learningPaths,
            ObjectProvider<ExamAttemptService> attempts,
            AccountProfileService accounts,
            LearningActivityService activity,
            LessonRecommendationService recommendations) {
        this.learningPaths = learningPaths;
        this.attempts = attempts;
        this.accounts = accounts;
        this.activity = activity;
        this.recommendations = recommendations;
    }

    @GetMapping("/student/dashboard")
    public String dashboard(Principal principal, Model model) {
        List<PathProgressDTO> paths = learningPaths.ensureAndSummaries(principal.getName());
        boolean hasLearningContent = paths.stream().anyMatch(path -> path.totalLessons() > 0);
        LessonRecommendationDTO recommendation =
                recommendations.recommend(learningPaths.userId(principal.getName()), paths);
        model.addAttribute("email", principal.getName());
        model.addAttribute("paths", paths);
        model.addAttribute("hasLearningContent", hasLearningContent);
        AccountSnapshotDTO account = accounts.snapshot(principal.getName());
        model.addAttribute("account", account);
        ArrayList<SkillOverview> skillOverview = new ArrayList<SkillOverview>();
        for (Skill skill : Skill.values()) {
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
                            .map(SkillViewDTO::currentLevel)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse("—");
            String goal =
                    account.goal() == null
                            ? "chưa đặt"
                            : account.goal().targets().getOrDefault(code, "chưa đặt");
            skillOverview.add(new SkillOverview(label, current, goal));
        }
        model.addAttribute("skillOverview", skillOverview);
        var activityView = activity.activity(principal.getName());
        model.addAttribute("activity", activityView);
        ExamAttemptService attemptService = attempts.getIfAvailable();
        AttemptResultDTO latestResult =
                attemptService == null
                        ? null
                        : attemptService
                                .history(learningPaths.userId(principal.getName()), 0, 1)
                                .items()
                                .stream()
                                .findFirst()
                                .orElse(null);
        boolean newcomer =
                activityView.totalActivities() == 0
                        && account.goal() == null
                        && account.profiles().isEmpty()
                        && latestResult == null;
        model.addAttribute("newLearner", newcomer);
        NextAction nextAction = null;
        if (latestResult != null && "IN_PROGRESS".equals(latestResult.state()))
            nextAction =
                    new NextAction(
                            "Bài thi đang làm",
                            "/student/attempts/" + latestResult.attemptId(),
                            "Tiếp tục làm bài");
        else if (latestResult != null
                && ("WAITING_GRADING".equals(latestResult.state())
                        || "SUBMITTED".equals(latestResult.state())))
            nextAction =
                    new NextAction(
                            "Bài đã nộp đang được chấm",
                            "/student/attempts/" + latestResult.attemptId() + "/result",
                            "Xem trạng thái chấm");
        if (nextAction == null) {
            var checkpoint =
                    paths.stream()
                            .filter(
                                    p ->
                                            p.totalRequiredLessons() > 0
                                                    && p.completedRequiredLessons()
                                                            == p.totalRequiredLessons()
                                                    && !"COMPLETED".equals(p.pathStatus())
                                                    && !"not_configured"
                                                            .equals(p.checkpointState()))
                            .findFirst();
            if (checkpoint.isPresent())
                nextAction =
                        new NextAction(
                                "Kiểm tra cuối chặng · " + checkpoint.get().skill(),
                                "/student/learning-path/" + checkpoint.get().skill(),
                                "Xem kiểm tra cuối chặng");
            else if (!newcomer && recommendation != null)
                nextAction =
                        new NextAction(
                                recommendation.title(),
                                "/student/lessons/" + recommendation.lessonId(),
                                "Học tiếp ngay");
        }
        model.addAttribute("nextAction", nextAction);
        return "student/dashboard";
    }

    /** Read-only orientation preview: no goals, attempts or progress are created. */
    @GetMapping("/student/orientation")
    public String orientation() {
        return "redirect:/student/learning-path";
    }
}
