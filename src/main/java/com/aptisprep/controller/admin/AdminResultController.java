package com.aptisprep.controller.admin;

import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.contract.ExamAttemptContract;
import com.aptisprep.service.contract.IntegrationModels.AdminHistoryFilter;
import java.security.Principal;
import java.time.*;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/results")
public class AdminResultController {
    private final ExamAttemptContract attempts;

    public AdminResultController(ExamAttemptContract attempts) {
        this.attempts = attempts;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String skill,
            @RequestParam(defaultValue = "") String examType,
            @RequestParam(defaultValue = "") String state,
            @RequestParam(defaultValue = "") String from,
            @RequestParam(defaultValue = "") String to,
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
                || size > 100) throw new BusinessRuleException("Bộ lọc kết quả không hợp lệ");
        Instant fromValue = parseDate(from, false), toValue = parseDate(to, true);
        if (fromValue != null && toValue != null && !fromValue.isBefore(toValue))
            throw new BusinessRuleException("Ngày bắt đầu phải không sau ngày kết thúc");
        model.addAttribute(
                "history",
                attempts.adminHistory(
                        principal.getName(),
                        page,
                        size,
                        new AdminHistoryFilter(q, skill, examType, state, fromValue, toValue)));
        model.addAttribute("q", q);
        model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedExamType", examType);
        model.addAttribute("selectedState", state);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute(
                "skills",
                List.of("GRAMMAR_VOCABULARY", "READING", "LISTENING", "WRITING", "SPEAKING"));
        return "admin/result/list";
    }

    @GetMapping("/{id:\\d+}")
    public String detail(@PathVariable Integer id, Principal principal, Model model) {
        model.addAttribute("attemptResult", attempts.adminResult(principal.getName(), id));
        return "admin/result/detail";
    }

    private Instant parseDate(String value, boolean nextDay) {
        if (value == null || value.isBlank()) return null;
        try {
            LocalDate date = LocalDate.parse(value);
            if (nextDay) date = date.plusDays(1);
            return date.atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (Exception e) {
            throw new BusinessRuleException("Ngày lọc kết quả không hợp lệ");
        }
    }
}
