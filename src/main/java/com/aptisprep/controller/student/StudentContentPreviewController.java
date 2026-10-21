package com.aptisprep.controller.student;

import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.account.AccountAccessService;
import com.aptisprep.service.contract.ExamAttemptContract;
import com.aptisprep.service.contract.IntegrationModels.HistoryFilter;
import java.security.Principal;
import com.aptisprep.service.student.StudentContentPreviewService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/** Exam preview routes belong to Trâm; result presentation delegates to the safe attempt contract. */
@Controller
@RequestMapping("/student")
public class StudentContentPreviewController {
    private final StudentContentPreviewService preview;
    private final AccountAccessService accounts;
    private final ExamAttemptContract attempts;
    public StudentContentPreviewController(StudentContentPreviewService preview, AccountAccessService accounts, ExamAttemptContract attempts) {
        this.preview = preview; this.accounts = accounts; this.attempts = attempts;
    }

    @GetMapping("/{module:exams}")
    public String list(@PathVariable String module, @RequestParam(defaultValue="") String q,
                       @RequestParam(defaultValue="") String skill, @RequestParam(defaultValue="") String level,
                       @RequestParam(defaultValue="") String state, Model model) {
        if (q.length() > 255 || skill.length() > 80 || level.length() > 40 || state.length() > 40)
            throw new BusinessRuleException("Bộ lọc không hợp lệ");
        common(module, model);
        model.addAttribute("entries", preview.find(module, q, skill, level, state));
        model.addAttribute("q", q); model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedLevel", level); model.addAttribute("selectedState", state);
        return "student/content/list";
    }

    @GetMapping("/results")
    public String results(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(defaultValue = "") String q,
                          @RequestParam(defaultValue = "") String skill,
                          @RequestParam(defaultValue = "") String examType,
                          @RequestParam(defaultValue = "") String state,
                          Principal principal, Model model) {
        if (page < 0 || size < 1 || size > 50) throw new BusinessRuleException("Phân trang không hợp lệ");
        if (q.length() > 255 || skill.length() > 80 || examType.length() > 40 || state.length() > 40)
            throw new BusinessRuleException("Bộ lọc không hợp lệ");
        Integer userId = accounts.requireActiveStudent(principal.getName());
        model.addAttribute("history", attempts.history(userId, page, size, new HistoryFilter(q, skill, examType, state)));
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("q", q);
        model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedExamType", examType);
        model.addAttribute("selectedState", state);
        return "student/results/list";
    }
    @GetMapping("/{module:exams}/{id:\\d+}")
    public String detail(@PathVariable String module, @PathVariable int id, Model model) {
        common(module, model); model.addAttribute("entry", preview.entry(module, id));
        return "student/content/detail";
    }
    @GetMapping("/attempts/{id:\\d+}/result")
    public String result(@PathVariable int id, Principal principal, Model model) {
        Integer userId = accounts.requireActiveStudent(principal.getName());
        model.addAttribute("attemptResult", attempts.result(userId, id));
        return "student/attempt-result";
    }
    @PostMapping("/attempts/{id:\\d+}/retry-grading")
    public String retryGrading(@PathVariable int id, Principal principal,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        Integer userId = accounts.requireActiveStudent(principal.getName());
        var result = attempts.result(userId, id);
        if (!"ERROR".equals(result.state()) || result.skills().stream().noneMatch(skill -> skill.retryable()))
            throw new BusinessRuleException("Lượt thi không có phần chấm lỗi có thể thử lại");
        try {
            attempts.retryFailedGrading(userId, id, java.util.UUID.randomUUID().toString());
            redirect.addFlashAttribute("successMessage", "Đã gửi yêu cầu chấm lại phần lỗi trên bài làm đã lưu.");
        } catch (BusinessRuleException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/student/attempts/" + id + "/result";
    }
    private void common(String module, Model model) {
        model.addAttribute("module", module);
        model.addAttribute("title", switch(module) { case "lessons" -> "Bài học"; case "exams" -> "Đề thi"; default -> "Kết quả"; });
        model.addAttribute("skills", List.of("Grammar & Vocabulary", "Reading", "Listening", "Writing", "Speaking", "Tổng hợp"));
    }
}
