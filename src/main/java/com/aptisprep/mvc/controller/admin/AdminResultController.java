package com.aptisprep.mvc.controller.admin;

import com.aptisprep.mvc.enums.ExamType;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.service.exam.ExamAttemptService;
import com.aptisprep.mvc.service.scoring.ScoringGuideService;
import com.aptisprep.mvc.support.contract.*;
import com.aptisprep.mvc.support.contract.IntegrationModels.AdminHistoryFilter;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminResultController {
    private static final List<String> EXAM_TYPES = ExamType.codes();
    private static final List<String> RESULT_STATES =
            List.of("IN_PROGRESS", "WAITING_GRADING", "COMPLETED", "ERROR", "EXPIRED");
    private final ExamAttemptService examAttemptService;
    private final ScoringGuideService scoringGuideService;

    public AdminResultController(
            ExamAttemptService examAttemptService, ScoringGuideService scoringGuideService) {
        this.examAttemptService = examAttemptService;
        this.scoringGuideService = scoringGuideService;
    }

    @GetMapping("/admin/results")
    public String list(
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "skill", defaultValue = "") String skill,
            @RequestParam(name = "examType", defaultValue = "") String examType,
            @RequestParam(name = "state", defaultValue = "") String state,
            @RequestParam(name = "from", defaultValue = "") String from,
            @RequestParam(name = "to", defaultValue = "") String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal,
            Model model) {
        if (q.length() > 255
                || skill.length() > 40
                || examType.length() > 40
                || state.length() > 40
                || page < 0
                || size < 1
                || size > 100) {
            throw new BusinessRuleException("Bộ lọc kết quả không hợp lệ");
        }
        Instant fromValue = null;
        Instant toValue = null;
        try {
            if (from != null && !from.isBlank()) {
                fromValue = LocalDate.parse(from).atStartOfDay(ZoneOffset.UTC).toInstant();
            }
            if (to != null && !to.isBlank()) {
                toValue = LocalDate.parse(to).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            }
        } catch (RuntimeException exception) {
            throw new BusinessRuleException("Ngày lọc không hợp lệ");
        }
        if (fromValue != null && toValue != null && !fromValue.isBefore(toValue)) {
            throw new BusinessRuleException("Ngày bắt đầu phải không sau ngày kết thúc");
        }
        var history =
                examAttemptService.adminHistory(
                        principal.getName(),
                        page,
                        size,
                        new AdminHistoryFilter(q, skill, examType, state, fromValue, toValue));
        model.addAttribute("history", history);
        model.addAttribute("totalPages", (history.totalItems() + size - 1) / size);
        model.addAttribute("q", q);
        model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedExamType", examType);
        model.addAttribute("selectedState", state);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute(
                "skills",
                List.of("GRAMMAR_VOCABULARY", "READING", "LISTENING", "WRITING", "SPEAKING"));
        model.addAttribute("examTypes", EXAM_TYPES);
        model.addAttribute("resultStates", RESULT_STATES);
        return "admin/result/list";
    }

    @GetMapping("/admin/results/{id}")
    public String detail(@PathVariable Integer id, Principal principal, Model model) {
        var result = examAttemptService.adminResult(principal.getName(), id);
        model.addAttribute("attemptResult", result);
        model.addAttribute(
                "overallScaleMax", scoringGuideService.forResultView(result).combinedScaleMax());
        model.addAttribute("review", examAttemptService.adminReview(principal.getName(), id));
        return "admin/result/detail";
    }

    @PostMapping("/admin/results/{id}/delete")
    public String delete(
            @PathVariable Integer id, Principal principal, RedirectAttributes redirectAttributes) {
        examAttemptService.adminDiscard(principal.getName(), id);
        redirectAttributes.addFlashAttribute(
                "successMessage", "Đã loại bỏ lượt thi khỏi danh sách.");
        return "redirect:/admin/results";
    }
}
