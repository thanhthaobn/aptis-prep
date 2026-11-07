package com.aptisprep.controller.student;

import com.aptisprep.dto.view.StudentCatalogView.Entry;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.account.AccountAccessService;
import com.aptisprep.service.contract.ExamAttemptContract;
import com.aptisprep.service.contract.IntegrationModels.AttemptResult;
import com.aptisprep.service.contract.IntegrationModels.HistoryFilter;
import com.aptisprep.service.contract.IntegrationModels.HistoryPage;
import com.aptisprep.service.scoring.ScoringGuideService;
import com.aptisprep.service.student.StudentExamCatalogService;
import com.aptisprep.service.student.StudentExamCatalogService.Catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/** Published exams from the database; results use the ownership-checked attempt contract. */
@Controller
public class StudentContentController {
    private final ScoringGuideService scoringGuides;

    private final StudentExamCatalogService catalog;
    private final AccountAccessService accounts;
    private final ExamAttemptContract attempts;

    public StudentContentController(
            StudentExamCatalogService catalog,
            AccountAccessService accounts,
            ExamAttemptContract attempts,
            ScoringGuideService scoringGuides) {
        this.catalog = catalog;
        this.accounts = accounts;
        this.scoringGuides = scoringGuides;
        this.attempts = attempts;
    }

    @GetMapping("/student/exams")
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String skill,
            @RequestParam(defaultValue = "") String level,
            @RequestParam(defaultValue = "") String state,
            @RequestParam(defaultValue = "PRACTICE") String examType,
            @RequestParam(defaultValue = "level") String sort,
            Model model) {
        if (page < 0 || size < 1 || size > 50)
            throw new BusinessRuleException("Phân trang không hợp lệ");
        if (q.length() > 255 || skill.length() > 80 || level.length() > 40 || state.length() > 40)
            throw new BusinessRuleException("Bộ lọc không hợp lệ");
        examModel(model);
        if (!StudentExamCatalogService.EXAM_TYPES.contains(examType)
                || !StudentExamCatalogService.SORT_OPTIONS.contains(sort))
            throw new BusinessRuleException("Phân loại hoặc sắp xếp đề thi không hợp lệ");
        Catalog content =
                catalog.browse(q, skill, level, state, examType, sort, PageRequest.of(page, size));
        Page<Entry> entries = content.page();
        LinkedHashMap<String, List<Entry>> groups = new LinkedHashMap<String, List<Entry>>();
        entries.getContent()
                .forEach(
                        entry ->
                                groups.computeIfAbsent(entry.skill(), key -> new ArrayList<>())
                                        .add(entry));
        model.addAttribute("entries", entries.getContent());
        model.addAttribute("examGroups", groups);
        model.addAttribute("examCategories", content.categories());
        model.addAttribute("selectedExamType", examType);
        model.addAttribute("selectedSort", sort);
        model.addAttribute("categoryTitle", StudentExamCatalogService.categoryLabel(examType));
        model.addAttribute("catalogPage", entries);
        model.addAttribute("size", size);
        model.addAttribute("q", q);
        model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedLevel", level);
        model.addAttribute("selectedState", state);
        return "student/content/list";
    }

    @GetMapping("/student/results")
    public String results(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String skill,
            @RequestParam(defaultValue = "") String examType,
            @RequestParam(defaultValue = "") String state,
            Principal principal,
            Model model) {
        if (page < 0 || size < 1 || size > 50)
            throw new BusinessRuleException("Phân trang không hợp lệ");
        if (q.length() > 255
                || skill.length() > 80
                || examType.length() > 40
                || state.length() > 40) throw new BusinessRuleException("Bộ lọc không hợp lệ");
        Integer userId = accounts.requireActiveStudent(principal.getName());
        HistoryPage history =
                attempts.history(userId, page, size, new HistoryFilter(q, skill, examType, state));
        model.addAttribute("history", history);
        model.addAttribute(
                "totalPages", (history.totalItems() + history.size() - 1) / history.size());
        model.addAttribute("page", history.page());
        model.addAttribute("size", size);
        model.addAttribute("q", q);
        model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedExamType", examType);
        model.addAttribute("selectedState", state);
        return "student/results/list";
    }

    @GetMapping("/student/exams/{id}")
    public String detail(@PathVariable int id, Model model) {
        examModel(model);
        model.addAttribute("entry", catalog.entry("exams", id));
        model.addAttribute("scoreGuides", scoringGuides.forExam(id));
        return "student/content/detail";
    }

    @GetMapping("/student/attempts/{id}/result")
    public String result(@PathVariable int id, Principal principal, Model model) {
        Integer userId = accounts.requireActiveStudent(principal.getName());
        AttemptResult result = attempts.result(userId, id);
        model.addAttribute("attemptResult", result);
        model.addAttribute("scoreGuides", scoringGuides.forResult(result));
        model.addAttribute("canRegradeEmpty", attempts.canRegradeEmpty(userId, id));
        return "student/attempt-result";
    }

    @PostMapping("/student/attempts/{id}/regrade-empty")
    public String regradeEmpty(
            @PathVariable int id, Principal principal, RedirectAttributes redirect) {
        Integer userId = accounts.requireActiveStudent(principal.getName());
        try {
            attempts.regradeEmpty(userId, id);
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
        Integer userId = accounts.requireActiveStudent(principal.getName());
        AttemptResult result = attempts.result(userId, id);
        if (!"ERROR".equals(result.state())
                || result.skills().stream().noneMatch(skill -> skill.retryable()))
            throw new BusinessRuleException("Lượt thi không có phần chấm lỗi có thể thử lại");
        try {
            attempts.retryFailedGrading(userId, id, UUID.randomUUID().toString());
            redirect.addFlashAttribute(
                    "successMessage", "Đã gửi yêu cầu chấm lại phần lỗi trên bài làm đã lưu.");
        } catch (BusinessRuleException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/student/attempts/" + id + "/result";
    }

    private void examModel(Model model) {
        model.addAttribute("module", "exams");
        model.addAttribute("title", "Đề thi");
        model.addAttribute(
                "skills",
                List.of("Grammar & Vocabulary", "Reading", "Listening", "Writing", "Speaking"));
    }
}
