package com.aptisprep.controller.student;

import com.aptisprep.dto.AttemptResultDTO;
import com.aptisprep.dto.HistoryFilterDTO;
import com.aptisprep.dto.HistoryPageDTO;
import com.aptisprep.dto.StudentCatalogViewDTO;
import com.aptisprep.dto.StudentCatalogViewDTO.Entry;
import com.aptisprep.enums.CefrLevel;
import com.aptisprep.enums.ExamType;
import com.aptisprep.enums.Skill;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.account.AccountAccessService;
import com.aptisprep.service.admin.*;
import com.aptisprep.service.exam.ExamAttemptService;
import com.aptisprep.service.exam.StudentExamCatalogService;
import com.aptisprep.service.exam.StudentExamCatalogService.Catalog;
import com.aptisprep.service.exam.impl.StudentExamCatalogServiceImpl;
import com.aptisprep.service.scoring.ScoringGuideService;
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

/** Student exam catalog pages and ownership-checked result history. */
@Controller
public class StudentExamCatalogController {
    private final ScoringGuideService scoringGuides;

    private final StudentExamCatalogService catalog;
    private final AccountAccessService accounts;
    private final ExamAttemptService attempts;

    public StudentExamCatalogController(
            StudentExamCatalogService catalog,
            AccountAccessService accounts,
            ExamAttemptService attempts,
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
        if (!ExamType.codes().contains(examType)
                || !StudentExamCatalogServiceImpl.SORT_OPTIONS.contains(sort))
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
        model.addAttribute(
                "skills",
                List.of("Grammar & Vocabulary", "Reading", "Listening", "Writing", "Speaking"));
        model.addAttribute("selectedExamType", examType);
        model.addAttribute("selectedSort", sort);
        model.addAttribute("categoryTitle", StudentCatalogViewDTO.categoryLabel(examType));
        model.addAttribute("catalogPage", entries);
        model.addAttribute("size", size);
        model.addAttribute("q", q);
        model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedLevel", level);
        model.addAttribute("selectedState", state);
        model.addAttribute("skillCodes", Skill.codes());
        model.addAttribute("learningLevels", CefrLevel.curriculumLevels());
        return "student/exams/exam-list";
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
        Integer userId = accounts.requireActiveStudent(principal.getName());
        HistoryPageDTO history =
                attempts.history(
                        userId, page, size, new HistoryFilterDTO(q, skill, examType, state));
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
        Entry entry = catalog.entry("exams", id);
        model.addAttribute("fromPlacement", fromPlacement);
        model.addAttribute("entry", entry);
        if (!"CHECKPOINT".equals(entry.examType()) && !"PLACEMENT".equals(entry.examType())) {
            Integer userId = accounts.requireActiveStudent(principal.getName());
            Integer resumableAttemptId = attempts.latestResumableAttemptId(userId, id);
            if (resumableAttemptId != null)
                model.addAttribute("resumableAttemptId", resumableAttemptId);
        }
        model.addAttribute("scoreGuides", scoringGuides.forExam(id));
        return "student/exams/exam-detail";
    }

    @GetMapping("/student/attempts/{id}/result")
    public String result(@PathVariable int id, Principal principal, Model model) {
        Integer userId = accounts.requireActiveStudent(principal.getName());
        AttemptResultDTO result = attempts.result(userId, id);
        if ("IN_PROGRESS".equals(result.state())) return "redirect:/student/attempts/" + id;
        if ("EXPIRED".equals(result.state()) && result.submittedAt() == null)
            return "redirect:/student/results";
        model.addAttribute("attemptResult", result);
        var scoreGuideView = scoringGuides.forResultView(result);
        model.addAttribute("scoreGuides", scoreGuideView.guides());
        model.addAttribute("feedbackWithoutGuide", scoreGuideView.feedbackWithoutGuide());
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
        AttemptResultDTO result = attempts.result(userId, id);
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
}
