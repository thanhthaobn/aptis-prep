package com.aptisprep.mvc.controller.student;

import com.aptisprep.mvc.dto.MockExamDTO;
import com.aptisprep.mvc.enums.Skill;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.service.account.AccountAccessService;
import com.aptisprep.mvc.service.exam.ExamAttemptService;
import com.aptisprep.mvc.service.learning.LessonService;
import com.aptisprep.mvc.service.scoring.ScoringGuideService;
import com.aptisprep.mvc.service.student.StudentExamCatalogService;
import com.aptisprep.mvc.support.contract.*;
import com.aptisprep.mvc.support.contract.IntegrationModels.AttemptResult;
import com.aptisprep.mvc.support.contract.IntegrationModels.HistoryFilter;
import com.aptisprep.mvc.support.contract.IntegrationModels.HistoryPage;
import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Các trang catalog đề thi và lịch sử kết quả đã kiểm tra quyền sở hữu. */
@Controller
public class StudentExamCatalogController {
    private final ScoringGuideService scoringGuideService;

    private final StudentExamCatalogService examCatalogService;
    private final AccountAccessService accountAccessService;
    private final ExamAttemptService examAttemptContract;
    private final LessonService lessonService;

    public StudentExamCatalogController(
            StudentExamCatalogService examCatalogService,
            AccountAccessService accountAccessService,
            ExamAttemptService examAttemptContract,
            ScoringGuideService scoringGuideService,
            LessonService lessonService) {
        this.examCatalogService = examCatalogService;
        this.accountAccessService = accountAccessService;
        this.scoringGuideService = scoringGuideService;
        this.examAttemptContract = examAttemptContract;
        this.lessonService = lessonService;
    }

    @GetMapping("/student/exams")
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "skill", defaultValue = "") String skill,
            @RequestParam(name = "level", defaultValue = "") String level,
            @RequestParam(name = "state", defaultValue = "") String state,
            @RequestParam(defaultValue = "PRACTICE") String examType,
            @RequestParam(defaultValue = "level") String sort,
            Model model) {
        if (page < 0 || size < 1 || size > 50)
            throw new BusinessRuleException("Phân trang không hợp lệ");
        if (q.length() > 255 || skill.length() > 80 || level.length() > 40 || state.length() > 40)
            throw new BusinessRuleException("Bộ lọc không hợp lệ");
        if ("CHECKPOINT".equalsIgnoreCase(examType)) return "redirect:/student/learning-path";
        if (!examCatalogService.examTypes().contains(examType)
                || !examCatalogService.sortOptions().contains(sort))
            throw new BusinessRuleException("Phân loại hoặc sắp xếp đề thi không hợp lệ");
        Page<MockExamDTO> entries =
                examCatalogService.browse(
                        q, skill, level, state, examType, sort, PageRequest.of(page, size));
        LinkedHashMap<String, List<MockExamDTO>> groups = new LinkedHashMap<>();
        entries.getContent()
                .forEach(
                        entry ->
                                groups.computeIfAbsent(entry.getSkill(), key -> new ArrayList<>())
                                        .add(entry));
        model.addAttribute("entries", entries.getContent());
        model.addAttribute("examGroups", groups);
        model.addAttribute(
                "examCategories", examCatalogService.categoryCounts(q, skill, level, state));
        model.addAttribute(
                "skills",
                List.of("Grammar & Vocabulary", "Reading", "Listening", "Writing", "Speaking"));
        model.addAttribute("selectedExamType", examType);
        model.addAttribute("selectedSort", sort);
        model.addAttribute("categoryTitle", examCatalogService.categoryLabel(examType));
        model.addAttribute("catalogPage", entries);
        model.addAttribute("size", size);
        model.addAttribute("q", q);
        model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedLevel", level);
        model.addAttribute("selectedState", state);
        model.addAttribute("skillCodes", Skill.codes());
        model.addAttribute("learningLevels", lessonService.getLevels());
        return "student/exams/list";
    }

    @GetMapping("/student/results")
    public String results(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "skill", defaultValue = "") String skill,
            @RequestParam(name = "examType", defaultValue = "") String examType,
            @RequestParam(name = "state", defaultValue = "") String state,
            Principal principal,
            Model model) {
        if (page < 0 || size < 1 || size > 50)
            throw new BusinessRuleException("Phân trang không hợp lệ");
        if (q.length() > 255
                || skill.length() > 80
                || examType.length() > 40
                || state.length() > 40) throw new BusinessRuleException("Bộ lọc không hợp lệ");
        Integer userId = accountAccessService.requireActiveStudent(principal.getName());
        HistoryPage history =
                examAttemptContract.history(
                        userId, page, size, new HistoryFilter(q, skill, examType, state));
        model.addAttribute("history", history);
        model.addAttribute(
                "totalPages", (history.totalItems() + history.size() - 1) / history.size());
        model.addAttribute(
                "inProgressCount",
                history.items().stream()
                        .filter(item -> "IN_PROGRESS".equals(item.state()))
                        .count());
        model.addAttribute(
                "completedCount",
                history.items().stream().filter(item -> "COMPLETED".equals(item.state())).count());
        model.addAttribute("q", q);
        model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedExamType", examType);
        model.addAttribute("selectedState", state);
        return "student/results/list";
    }

    @GetMapping("/student/exams/{id}")
    public String detail(
            @PathVariable int id,
            @RequestParam(defaultValue = "false") boolean fromPlacement,
            Principal principal,
            Model model) {
        model.addAttribute("module", "exams");
        MockExamDTO entry = examCatalogService.entry("exams", id);
        model.addAttribute("fromPlacement", fromPlacement);
        model.addAttribute("entry", entry);
        if (!"CHECKPOINT".equals(entry.getExamType()) && !"PLACEMENT".equals(entry.getExamType())) {
            Integer userId = accountAccessService.requireActiveStudent(principal.getName());
            Integer resumableAttemptId = examAttemptContract.latestResumableAttemptId(userId, id);
            if (resumableAttemptId != null)
                model.addAttribute("resumableAttemptId", resumableAttemptId);
        }
        model.addAttribute("scoreGuides", scoringGuideService.forExam(id));
        return "student/exams/detail";
    }

    @GetMapping("/student/attempts/{id}/result")
    public String result(@PathVariable int id, Principal principal, Model model) {
        Integer userId = accountAccessService.requireActiveStudent(principal.getName());
        AttemptResult result = examAttemptContract.result(userId, id);
        if ("IN_PROGRESS".equals(result.state())) return "redirect:/student/attempts/" + id;
        if ("EXPIRED".equals(result.state()) && result.submittedAt() == null)
            return "redirect:/student/results";
        model.addAttribute("attemptResult", result);
        var scoreGuideView = scoringGuideService.forResultView(result);
        model.addAttribute("scoreGuides", scoreGuideView.guides());
        model.addAttribute("feedbackWithoutGuide", scoreGuideView.feedbackWithoutGuide());
        model.addAttribute("canRegradeEmpty", examAttemptContract.canRegradeEmpty(userId, id));
        return "student/attempt/result";
    }

    @PostMapping("/student/attempts/{id}/regrade-empty")
    public String regradeEmpty(
            @PathVariable int id, Principal principal, RedirectAttributes redirect) {
        Integer userId = accountAccessService.requireActiveStudent(principal.getName());
        try {
            examAttemptContract.regradeEmpty(userId, id);
            redirect.addFlashAttribute(
                    "successMessage", "Đã tính lại phần bỏ trống thành 0 điểm từ bài làm đã lưu.");
        } catch (BusinessRuleException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/student/attempts/" + id + "/result";
    }

    @PostMapping("/student/attempts/{id}/retry-grading")
    public String retryGrading(
            @PathVariable int id, Principal principal, RedirectAttributes redirect) {
        Integer userId = accountAccessService.requireActiveStudent(principal.getName());
        AttemptResult result = examAttemptContract.result(userId, id);
        if (!"ERROR".equals(result.state())
                || result.skills().stream().noneMatch(skill -> skill.retryable()))
            throw new BusinessRuleException("Lượt thi không có phần chấm lỗi có thể thử lại");
        try {
            examAttemptContract.retryFailedGrading(userId, id, UUID.randomUUID().toString());
            redirect.addFlashAttribute(
                    "successMessage", "Đã gửi yêu cầu chấm lại phần lỗi trên bài làm đã lưu.");
        } catch (BusinessRuleException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/student/attempts/" + id + "/result";
    }
}
