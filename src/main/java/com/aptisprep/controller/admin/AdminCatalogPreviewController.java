package com.aptisprep.controller.admin;

import com.aptisprep.dto.form.ExamForm;
import com.aptisprep.dto.form.QuestionForm;
import com.aptisprep.dto.view.AdminCatalogPreview.Entry;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.service.admin.CatalogService;
import com.aptisprep.service.admin.AdminCatalogPreviewService;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

@Controller
@RequestMapping("/admin")
public class AdminCatalogPreviewController {
    private final AdminCatalogPreviewService preview;
    private final CatalogService catalogService;
    @Value("${app.demo.catalog-fallback:false}") private boolean catalogFallback;
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
                : catalogFallback ? preview.find(module, q, skill, status, kind) : List.of();
        model.addAttribute("entries", entries);
        model.addAttribute("q", q); model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedStatus", status); model.addAttribute("selectedKind", kind);
        return "admin/catalog/list";
    }

    @GetMapping("/{module:questions|exams}/{id:\\d+}")
    public String detail(@PathVariable String module, @PathVariable int id, Model model) {
        if (isReal(module) && !catalogService.hasData(module) && !catalogFallback) {
            throw new ResourceNotFoundException("Chưa có dữ liệu nội dung");
        }
        addModule(module, model);
        model.addAttribute("entry", isReal(module) && catalogService.hasData(module)
                ? catalogService.entry(module, id) : preview.entry(module, id));
        if ("questions".equals(module) && isReal(module) && catalogService.hasData(module)) {
            model.addAttribute("mediaItems", catalogService.mediaItems(id));
        }
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
        if (!catalogService.hasData(module) && !catalogFallback) {
            throw new ResourceNotFoundException("Chưa có dữ liệu nội dung");
        }
        if (!catalogService.hasData(module)) {
            if (!preview.module(module).editable()) throw new ResourceNotFoundException("Không tìm thấy nội dung");
            addModule(module, model); model.addAttribute("entry", preview.entry(module, id)); model.addAttribute("editing", true);
            if ("questions".equals(module)) model.addAttribute("questionForm", catalogService.questionForm(null));
            else prepareExamForm(model, catalogService.examForm(null));
            return "admin/catalog/form";
        }
        addModule(module, model); model.addAttribute("editing", true);
        boolean realEntry = hasRealEntry(module, id);
        if (!realEntry) throw new ResourceNotFoundException("Không tìm thấy nội dung");
        model.addAttribute("entry", catalogService.entry(module, id));
        if ("questions".equals(module)) model.addAttribute("questionForm", catalogService.questionForm(id));
        else prepareExamForm(model, catalogService.examForm(id));
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

    @PostMapping("/questions/{id:\\d+}/media")
    public String addMedia(@PathVariable int id, @RequestParam(name = "files", required = false) MultipartFile[] files) {
        catalogService.addMedia(id, files); return "redirect:/admin/questions/" + id;
    }

    @PostMapping("/questions/{id:\\d+}/media/{mediaId:\\d+}/delete")
    public String deleteMedia(@PathVariable int id, @PathVariable int mediaId) {
        catalogService.deleteMedia(id, mediaId); return "redirect:/admin/questions/" + id;
    }

    @PostMapping("/questions/{id:\\d+}/media/{mediaId:\\d+}")
    public String updateMedia(@PathVariable int id, @PathVariable int mediaId,
            @RequestParam(required = false) String altText, @RequestParam(required = false) String transcript,
            @RequestParam(required = false) Integer maxPlayCount) {
        catalogService.updateMedia(id, mediaId, altText, transcript, maxPlayCount);
        return "redirect:/admin/questions/" + id;
    }

    @PostMapping("/questions/{id:\\d+}/media/{mediaId:\\d+}/move")
    public String moveMedia(@PathVariable int id, @PathVariable int mediaId, @RequestParam int direction) {
        catalogService.moveMedia(id, mediaId, direction); return "redirect:/admin/questions/" + id;
    }

    @GetMapping("/questions/{id:\\d+}/media/{mediaId:\\d+}/content")
    public ResponseEntity<Resource> mediaContent(@PathVariable int id, @PathVariable int mediaId) {
        CatalogService.MediaContent value = catalogService.mediaContent(id, mediaId);
        MediaType type;
        try { type = value.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(value.contentType()); }
        catch (IllegalArgumentException exception) { type = MediaType.APPLICATION_OCTET_STREAM; }
        return ResponseEntity.ok().contentType(type).body(value.resource());
    }

    @PostMapping("/questions/{id:\\d+}/archive")
    public String archiveQuestion(@PathVariable int id) { catalogService.archiveQuestion(id); return "redirect:/admin/questions/" + id; }

    @PostMapping("/questions/{id:\\d+}/restore")
    public String restoreQuestion(@PathVariable int id) { catalogService.restoreQuestion(id); return "redirect:/admin/questions/" + id; }

    @PostMapping("/exams")
    public String saveExam(@ModelAttribute ExamForm form) { return "redirect:/admin/exams/" + catalogService.saveExam(null, form); }

    @PostMapping("/exams/{id:\\d+}/edit")
    public String updateExam(@PathVariable int id, @ModelAttribute ExamForm form) { catalogService.saveExam(id, form); return "redirect:/admin/exams/" + id; }

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
}
