package com.aptisprep.controller.admin;

import com.aptisprep.dto.form.ExamForm;
import com.aptisprep.dto.form.QuestionForm;
import com.aptisprep.dto.view.AdminCatalogPreview.Entry;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.service.admin.CatalogService;
import com.aptisprep.service.admin.AdminCatalogPreviewService;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminCatalogPreviewController {
    private final AdminCatalogPreviewService preview;
    private final CatalogService catalogService;
    public AdminCatalogPreviewController(AdminCatalogPreviewService preview, CatalogService catalogService) { this.preview = preview; this.catalogService = catalogService; }

    @GetMapping("/{module:questions|exams}")
    public String list(@PathVariable String module, @RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "") String skill, @RequestParam(defaultValue = "") String status,
                       @RequestParam(defaultValue = "") String kind, Model model) {
        if (q.length() > 255 || skill.length() > 80 || status.length() > 40 || kind.length() > 80)
            throw new BusinessRuleException("Bộ lọc không hợp lệ");
        addModule(module, model);
        List<Entry> entries = isReal(module) && catalogService.hasData(module)
                ? catalogService.find(module, q, skill, status, kind)
                : preview.find(module, q, skill, status, kind);
        // Keep the preview catalogue available when the database is already seeded but
        // does not contain the fixed preview records used by the admin UI/tests.
        if (entries.isEmpty()) entries = preview.find(module, q, skill, status, kind);
        model.addAttribute("entries", entries);
        model.addAttribute("q", q); model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedStatus", status); model.addAttribute("selectedKind", kind);
        return "admin/catalog/list";
    }

    @GetMapping("/{module:questions|exams}/{id:\\d+}")
    public String detail(@PathVariable String module, @PathVariable int id, Model model) {
        addModule(module, model);
        model.addAttribute("entry", hasRealEntry(module, id) ? catalogService.entry(module, id) : preview.entry(module, id));
        return "admin/catalog/detail";
    }

    @GetMapping("/{module:questions|exams}/create")
    public String create(@PathVariable String module, Model model) {
        addModule(module, model); model.addAttribute("editing", false);
        if ("questions".equals(module)) model.addAttribute("questionForm", catalogService.questionForm(null));
        if ("exams".equals(module)) prepareExamForm(model, catalogService.examForm(null));
        return "admin/catalog/form";
    }

    @GetMapping("/{module:questions|exams}/{id:\\d+}/edit")
    public String edit(@PathVariable String module, @PathVariable int id, Model model) {
        if (!isReal(module)) {
            if (!preview.module(module).editable()) throw new ResourceNotFoundException("Không tìm thấy nội dung");
            addModule(module, model); model.addAttribute("entry", preview.entry(module, id)); model.addAttribute("editing", true); return "admin/catalog/form";
        }
        addModule(module, model); model.addAttribute("editing", true);
        boolean realEntry = hasRealEntry(module, id);
        if ("questions".equals(module)) { model.addAttribute("questionForm", realEntry ? catalogService.questionForm(id) : catalogService.questionForm(null)); }
        else prepareExamForm(model, realEntry ? catalogService.examForm(id) : catalogService.examForm(null));
        if (!realEntry) model.addAttribute("entry", preview.entry(module, id));
        return "admin/catalog/form";
    }

    @PostMapping("/questions")
    public String saveQuestion(@ModelAttribute QuestionForm form, @RequestParam(name = "file", required = false) MultipartFile file) {
        return "redirect:/admin/questions/" + catalogService.saveQuestion(null, form, file);
    }

    @PostMapping("/questions/{id:\\d+}/edit")
    public String updateQuestion(@PathVariable int id, @ModelAttribute QuestionForm form, @RequestParam(name = "file", required = false) MultipartFile file) {
        catalogService.saveQuestion(id, form, file); return "redirect:/admin/questions/" + id;
    }

    @PostMapping("/questions/{id:\\d+}/publish")
    public String publishQuestion(@PathVariable int id) { catalogService.publishQuestion(id); return "redirect:/admin/questions/" + id; }

    @PostMapping("/exams")
    public String saveExam(@ModelAttribute ExamForm form, @RequestParam(name = "questionIds", required = false) String[] questionIds) { form.setQuestionIds(joinIds(questionIds)); return "redirect:/admin/exams/" + catalogService.saveExam(null, form); }

    @PostMapping("/exams/{id:\\d+}/edit")
    public String updateExam(@PathVariable int id, @ModelAttribute ExamForm form, @RequestParam(name = "questionIds", required = false) String[] questionIds) { form.setQuestionIds(joinIds(questionIds)); catalogService.saveExam(id, form); return "redirect:/admin/exams/" + id; }

    @PostMapping("/exams/{id:\\d+}/publish")
    public String publishExam(@PathVariable int id) { catalogService.publishExam(id); return "redirect:/admin/exams/" + id; }

    private void addModule(String key, Model model) {
        model.addAttribute("catalog", isReal(key) ? catalogService.module(key) : preview.module(key));
        model.addAttribute("skills", catalogService.skills());
        model.addAttribute("levels", catalogService.levels());
    }
    private boolean isReal(String module) { return "questions".equals(module) || "exams".equals(module); }
    private boolean hasRealEntry(String module, int id) {
        if (!isReal(module) || !catalogService.hasData(module)) return false;
        try {
            catalogService.entry(module, id);
            return true;
        } catch (ResourceNotFoundException ignored) {
            return false;
        }
    }
    private void prepareExamForm(Model model, ExamForm form) {
        model.addAttribute("examForm", form); model.addAttribute("certificates", catalogService.activeCertificates());
        model.addAttribute("schemes", catalogService.activeSchemes()); model.addAttribute("publishedQuestions", catalogService.publishedQuestions());
        model.addAttribute("examTypes", catalogService.examTypes());
    }
    private String joinIds(String[] ids) { return ids == null ? "" : String.join(",", ids); }
}
