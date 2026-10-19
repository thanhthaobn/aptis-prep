package com.aptisprep.controller.student;

import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.account.AccountAccessService;
import com.aptisprep.service.exam.ExamAttemptService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StudentExamController {
    private final AccountAccessService accounts;
    private final ExamAttemptService attempts;

    public StudentExamController(AccountAccessService accounts, ExamAttemptService attempts) {
        this.accounts = accounts; this.attempts = attempts;
    }

    @PostMapping("/student/exams/{id:\\d+}/start")
    public String start(@PathVariable Integer id, Principal principal, RedirectAttributes redirect) {
        try {
            Integer userId = accounts.requireActiveStudent(principal.getName());
            return "redirect:/student/attempts/" + attempts.start(userId, id, "exam:" + userId + ":" + id).attemptId();
        } catch (BusinessRuleException exception) {
            redirect.addFlashAttribute("error", exception.getMessage()); return "redirect:/student/exams/" + id;
        }
    }

    @GetMapping("/student/attempts/{id:\\d+}")
    public String attempt(@PathVariable Integer id, Principal principal, Model model) {
        Integer userId = accounts.requireActiveStudent(principal.getName());
        model.addAttribute("attempt", attempts.page(userId, id));
        return "student/attempt";
    }

    @PostMapping("/student/attempts/{id:\\d+}/answers")
    public String answer(@PathVariable Integer id, @RequestParam Integer questionId,
            @RequestParam(required = false) Integer itemId, @RequestParam(required = false) Integer selectedOptionId,
            @RequestParam(required = false) Integer userPosition, @RequestParam(required = false) String textAnswer,
            @RequestParam(required = false) String audioUrl,
            Principal principal, RedirectAttributes redirect) {
        try {
            Integer userId = accounts.requireActiveStudent(principal.getName());
            attempts.saveAnswer(userId, id, questionId, itemId, selectedOptionId, userPosition, textAnswer, audioUrl);
        } catch (BusinessRuleException exception) { redirect.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/student/attempts/" + id;
    }

    @PostMapping("/student/attempts/{id:\\d+}/submit")
    public String submit(@PathVariable Integer id, Principal principal, RedirectAttributes redirect) {
        try {
            Integer userId = accounts.requireActiveStudent(principal.getName());
            attempts.submit(userId, id, "submit:" + userId + ":" + id);
            return "redirect:/student/attempts/" + id + "/result";
        } catch (BusinessRuleException exception) {
            redirect.addFlashAttribute("error", exception.getMessage()); return "redirect:/student/attempts/" + id;
        }
    }
}
