package com.aptisprep.controller.student;

import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.student.StudentContentPreviewService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/** GET-only scaffolding. No attempt creation, submissions, tracking or grading. */
@Controller
@RequestMapping("/student")
public class StudentContentPreviewController {
    private final StudentContentPreviewService preview;
    public StudentContentPreviewController(StudentContentPreviewService preview) { this.preview = preview; }

    @GetMapping("/{module:lessons|exams|results}")
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
    @GetMapping("/{module:lessons|exams}/{id:\\d+}")
    public String detail(@PathVariable String module, @PathVariable int id, Model model) {
        common(module, model); model.addAttribute("entry", preview.entry(module, id));
        return "student/content/detail";
    }
    @GetMapping("/attempts/{id:\\d+}/result")
    public String result(@PathVariable int id, Model model) {
        common("results", model); model.addAttribute("entry", preview.entry("results", id));
        return "student/content/detail";
    }
    private void common(String module, Model model) {
        model.addAttribute("module", module);
        model.addAttribute("title", switch(module) { case "lessons" -> "Bài học"; case "exams" -> "Đề thi"; default -> "Kết quả"; });
        model.addAttribute("skills", List.of("Grammar & Vocabulary", "Reading", "Listening", "Writing", "Speaking", "Tổng hợp"));
    }
}
