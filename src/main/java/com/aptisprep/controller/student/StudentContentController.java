package com.aptisprep.controller.student;

import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.account.AccountAccessService;
import com.aptisprep.service.contract.ExamAttemptContract;
import com.aptisprep.service.contract.IntegrationModels.HistoryFilter;
import com.aptisprep.service.student.StudentExamCatalogService;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Published exams from the database; results use the ownership-checked attempt contract. */
@Controller
@RequestMapping("/student")
public class StudentContentController {
    private final StudentExamCatalogService catalog;
    private final AccountAccessService accounts;
    private final ExamAttemptContract attempts;

    public StudentContentController(
            StudentExamCatalogService catalog,
            AccountAccessService accounts,
            ExamAttemptContract attempts) {
        this.catalog = catalog;
        this.accounts = accounts;
        this.attempts = attempts;
    }

    @GetMapping("/exams")
    public String list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String skill,
            @RequestParam(defaultValue = "") String level,
            @RequestParam(defaultValue = "") String state,
            Model model) {
        if (q.length() > 255 || skill.length() > 80 || level.length() > 40 || state.length() > 40)
            throw new BusinessRuleException("Bộ lọc không hợp lệ");
        examModel(model);
        model.addAttribute("entries", catalog.find("exams", q, skill, level, state));
        model.addAttribute("q", q);
        model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedLevel", level);
        model.addAttribute("selectedState", state);
        return "student/content/list";
    }

    @GetMapping("/results")
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
        model.addAttribute(
                "history",
                attempts.history(userId, page, size, new HistoryFilter(q, skill, examType, state)));
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("q", q);
        model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedExamType", examType);
        model.addAttribute("selectedState", state);
        return "student/results/list";
    }

    @GetMapping("/exams/{id}")
    public String detail(@PathVariable int id, Model model) {
        examModel(model);
        model.addAttribute("entry", catalog.entry("exams", id));
        return "student/content/detail";
    }

    @GetMapping("/attempts/{id}/result")
    public String result(@PathVariable int id, Principal principal, Model model) {
        Integer userId = accounts.requireActiveStudent(principal.getName());
        model.addAttribute("attemptResult", attempts.result(userId, id));
        return "student/attempt-result";
    }

    @PostMapping("/attempts/{id}/retry-grading")
    public String retryGrading(
            @PathVariable int id, Principal principal, RedirectAttributes redirect) {
        Integer userId = accounts.requireActiveStudent(principal.getName());
        var result = attempts.result(userId, id);
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
                List.of(
                        "Grammar & Vocabulary",
                        "Reading",
                        "Listening",
                        "Writing",
                        "Speaking",
                        "Tổng hợp"));
    }
}
