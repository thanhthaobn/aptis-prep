package com.aptisprep.controller.admin;

import com.aptisprep.dto.form.ExamForm;
import com.aptisprep.dto.form.QuestionForm;
import com.aptisprep.dto.view.AdminCatalogView.CatalogPage;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.admin.CatalogService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class AdminCatalogController {
    private final CatalogService catalogService;

    public AdminCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/admin/questions")
    public String questions(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String skill,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String kind,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        return list("questions", q, skill, status, kind, page, size, model);
    }

    @GetMapping("/admin/exams")
    public String exams(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String skill,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String kind,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        return list("exams", q, skill, status, kind, page, size, model);
    }

    private String list(
            String module,
            String q,
            String skill,
            String status,
            String kind,
            int page,
            int size,
            Model model) {
        if (q.length() > 255 || skill.length() > 80 || status.length() > 40 || kind.length() > 80)
            throw new BusinessRuleException("Bộ lọc không hợp lệ");
        addModule(module, model);
        CatalogPage pageData = catalogService.findPage(module, q, skill, status, kind, page, size);
        model.addAttribute("pageData", pageData);
        model.addAttribute("entries", pageData.items());
        model.addAttribute("q", q);
        model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedKind", kind);
        return "admin/catalog/list";
    }

    @GetMapping("/admin/questions/{id}")
    public String question(@PathVariable int id, Model model) {
        return detail("questions", id, model);
    }

    @GetMapping("/admin/exams/{id}")
    public String exam(@PathVariable int id, Model model) {
        return detail("exams", id, model);
    }

    private String detail(String module, int id, Model model) {
        addModule(module, model);
        model.addAttribute("entry", catalogService.entry(module, id));
        if ("questions".equals(module)) {
            model.addAttribute("mediaItems", catalogService.mediaItems(id));
        }
        return "admin/catalog/detail";
    }

    @GetMapping("/admin/questions/create")
    public String createQuestion(Model model) {
        return create("questions", model);
    }

    @GetMapping("/admin/exams/create")
    public String createExam(Model model) {
        return create("exams", model);
    }

    private String create(String module, Model model) {
        addModule(module, model);
        model.addAttribute("editing", false);
        if ("questions".equals(module))
            model.addAttribute("questionForm", catalogService.questionForm(null));
        if ("exams".equals(module)) prepareExamForm(model, catalogService.examForm(null));
        return "admin/catalog/form";
    }

    @GetMapping("/admin/questions/{id}/edit")
    public String editQuestion(@PathVariable int id, Model model) {
        return edit("questions", id, model);
    }

    @GetMapping("/admin/exams/{id}/edit")
    public String editExam(@PathVariable int id, Model model) {
        return edit("exams", id, model);
    }

    private String edit(String module, int id, Model model) {
        addModule(module, model);
        model.addAttribute("editing", true);
        model.addAttribute("entry", catalogService.entry(module, id));
        if ("questions".equals(module))
            model.addAttribute("questionForm", catalogService.questionForm(id));
        else prepareExamForm(model, catalogService.examForm(id));
        return "admin/catalog/form";
    }

    @PostMapping("/admin/questions")
    public String saveQuestion(
            @ModelAttribute QuestionForm form,
            @RequestParam(name = "file", required = false) MultipartFile file,
            Model model) {
        try {
            return "redirect:/admin/questions/" + catalogService.saveQuestion(null, form, file);
        } catch (BusinessRuleException exception) {
            prepareQuestionError(model, form, false, exception.getMessage(), null);
            return "admin/catalog/form";
        }
    }

    @PostMapping("/admin/questions/{id}/edit")
    public String updateQuestion(
            @PathVariable int id,
            @ModelAttribute QuestionForm form,
            @RequestParam(name = "file", required = false) MultipartFile file,
            Model model) {
        try {
            catalogService.saveQuestion(id, form, file);
            return "redirect:/admin/questions/" + id;
        } catch (BusinessRuleException exception) {
            prepareQuestionError(model, form, true, exception.getMessage(), id);
            return "admin/catalog/form";
        }
    }

    @PostMapping("/admin/questions/{id}/publish")
    public String publishQuestion(@PathVariable int id) {
        catalogService.publishQuestion(id);
        return "redirect:/admin/questions/" + id;
    }

    @PostMapping("/admin/questions/{id}/media")
    public String addMedia(
            @PathVariable int id,
            @RequestParam(name = "files", required = false) MultipartFile[] files) {
        catalogService.addMedia(id, files);
        return "redirect:/admin/questions/" + id;
    }

    @PostMapping("/admin/questions/{id}/media/{mediaId}/delete")
    public String deleteMedia(@PathVariable int id, @PathVariable int mediaId) {
        catalogService.deleteMedia(id, mediaId);
        return "redirect:/admin/questions/" + id;
    }

    @PostMapping("/admin/questions/{id}/media/{mediaId}")
    public String updateMedia(
            @PathVariable int id,
            @PathVariable int mediaId,
            @RequestParam(required = false) String altText,
            @RequestParam(required = false) String transcript,
            @RequestParam(required = false) Integer maxPlayCount) {
        catalogService.updateMedia(id, mediaId, altText, transcript, maxPlayCount);
        return "redirect:/admin/questions/" + id;
    }

    @PostMapping("/admin/questions/{id}/media/{mediaId}/move")
    public String moveMedia(
            @PathVariable int id, @PathVariable int mediaId, @RequestParam int direction) {
        catalogService.moveMedia(id, mediaId, direction);
        return "redirect:/admin/questions/" + id;
    }

    @GetMapping("/admin/questions/{id}/media/{mediaId}/content")
    public ResponseEntity<Resource> mediaContent(@PathVariable int id, @PathVariable int mediaId) {
        CatalogService.MediaContent value = catalogService.mediaContent(id, mediaId);
        MediaType type;
        try {
            type =
                    value.contentType() == null
                            ? MediaType.APPLICATION_OCTET_STREAM
                            : MediaType.parseMediaType(value.contentType());
        } catch (IllegalArgumentException exception) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok().contentType(type).body(value.resource());
    }

    @PostMapping("/admin/questions/{id}/archive")
    public String archiveQuestion(@PathVariable int id) {
        catalogService.archiveQuestion(id);
        return "redirect:/admin/questions/" + id;
    }

    @PostMapping("/admin/questions/{id}/restore")
    public String restoreQuestion(@PathVariable int id) {
        catalogService.restoreQuestion(id);
        return "redirect:/admin/questions/" + id;
    }

    @PostMapping("/admin/exams")
    public String saveExam(@ModelAttribute ExamForm form) {
        return "redirect:/admin/exams/" + catalogService.saveExam(null, form);
    }

    @PostMapping("/admin/exams/{id}/edit")
    public String updateExam(@PathVariable int id, @ModelAttribute ExamForm form) {
        catalogService.saveExam(id, form);
        return "redirect:/admin/exams/" + id;
    }

    @PostMapping("/admin/exams/{id}/publish")
    public String publishExam(@PathVariable int id) {
        catalogService.publishExam(id);
        return "redirect:/admin/exams/" + id;
    }

    private void addModule(String key, Model model) {
        model.addAttribute("catalog", catalogService.module(key));
        model.addAttribute("skills", catalogService.skills());
        model.addAttribute("levels", catalogService.levels());
    }

    private void prepareExamForm(Model model, ExamForm form) {
        model.addAttribute("examForm", form);
        model.addAttribute("certificates", catalogService.activeCertificates());
        model.addAttribute("schemes", catalogService.activeSchemes());
        model.addAttribute("publishedQuestions", catalogService.publishedQuestions());
        model.addAttribute("examTypes", catalogService.examTypes());
    }

    private void prepareQuestionError(
            Model model, QuestionForm form, boolean editing, String message, Integer id) {
        addModule("questions", model);
        model.addAttribute("editing", editing);
        model.addAttribute("questionForm", form);
        model.addAttribute("formError", message);
        if (id != null) model.addAttribute("entry", catalogService.entry("questions", id));
    }
}
