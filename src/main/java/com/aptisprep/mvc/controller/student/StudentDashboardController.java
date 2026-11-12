package com.aptisprep.mvc.controller.student;
import com.aptisprep.mvc.support.contract.*;

import com.aptisprep.mvc.dto.UserGoalDTO;
import com.aptisprep.mvc.dto.UserSkillProfileDTO;
import com.aptisprep.mvc.enums.Skill;
import com.aptisprep.mvc.service.account.AccountProfileService;
import com.aptisprep.mvc.service.account.LearningActivityService;
import com.aptisprep.mvc.service.exam.AttemptResultReader;
import com.aptisprep.mvc.support.contract.IntegrationModels.AttemptResult;
import com.aptisprep.mvc.support.contract.IntegrationModels.HistoryFilter;
import com.aptisprep.mvc.support.contract.IntegrationModels.PathProgress;
import com.aptisprep.mvc.service.learning.LearningPathService;
import com.aptisprep.mvc.service.learning.LessonRecommendationService;
import com.aptisprep.mvc.service.learning.LessonRecommendationService.Recommendation;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StudentDashboardController {
    private final LearningPathService learningPathService;
    private final AttemptResultReader attemptResultReader;
    private final AccountProfileService accountProfileService;
    private final LearningActivityService learningActivityService;
    private final LessonRecommendationService lessonRecommendationService;

    public StudentDashboardController(
            LearningPathService learningPathService,
            AttemptResultReader attemptResultReader,
            AccountProfileService accountProfileService,
            LearningActivityService learningActivityService,
            LessonRecommendationService lessonRecommendationService) {
        this.learningPathService = learningPathService;
        this.attemptResultReader = attemptResultReader;
        this.accountProfileService = accountProfileService;
        this.learningActivityService = learningActivityService;
        this.lessonRecommendationService = lessonRecommendationService;
    }

    @GetMapping("/student/dashboard")
    public String dashboard(Principal principal, Model model) {
        List<PathProgress> paths = learningPathService.ensureAndSummaries(principal.getName());
        boolean hasLearningContent = paths.stream().anyMatch(path -> path.totalLessons() > 0);
        Recommendation recommendation =
                lessonRecommendationService.recommend(learningPathService.userId(principal.getName()), paths);
        model.addAttribute("email", principal.getName());
        model.addAttribute("paths", paths);
        model.addAttribute("hasLearningContent", hasLearningContent);
        UserGoalDTO goalDto = accountProfileService.goal(principal.getName());
        List<UserSkillProfileDTO> profiles = accountProfileService.skillProfiles(principal.getName());
        model.addAttribute("account", accountProfileService.profile(principal.getName()));
        model.addAttribute("goal", goalDto);
        model.addAttribute("profiles", profiles);
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
                    profiles.stream()
                            .filter(profile -> code.equals(profile.getSkill()))
                            .map(streamValue -> streamValue.getCurrentLevel())
                            .filter(streamValue -> Objects.nonNull(streamValue))
                            .findFirst()
                            .orElse("—");
            String goalValue =
                    goalDto == null
                            ? null
                            : switch (code) {
                                case "GRAMMAR_VOCABULARY" -> goalDto.getGrammarVocabulary();
                                case "READING" -> goalDto.getReading();
                                case "LISTENING" -> goalDto.getListening();
                                case "WRITING" -> goalDto.getWriting();
                                case "SPEAKING" -> goalDto.getSpeaking();
                                default -> null;
                            };
            String goal = goalValue == null || goalValue.isBlank() ? "chưa đặt" : goalValue;
            skillOverview.add(new SkillOverview(label, current, goal));
        }
        model.addAttribute("skillOverview", skillOverview);
        var activityView = learningActivityService.activity(principal.getName());
        model.addAttribute("activity", activityView);
        AttemptResult latestResult =
                attemptResultReader
                        .history(
                                learningPathService.userId(principal.getName()),
                                0,
                                1,
                                HistoryFilter.empty())
                        .items()
                        .stream()
                        .findFirst()
                        .orElse(null);
        boolean newcomer =
                activityView.totalActivities() == 0
                        && goalDto == null
                        && profiles.isEmpty()
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

    public record NextAction(String title, String url, String label) {}

    public record SkillOverview(String label, String current, String goal) {}

    /** Preview giới thiệu chỉ đọc: không tạo mục tiêu, lượt thi hoặc tiến độ. */
    @GetMapping("/student/orientation")
    public String orientation() {
        return "redirect:/student/learning-path";
    }
}







