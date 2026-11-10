package com.aptisprep.mvc.catalog.controller;

import com.aptisprep.mvc.catalog.dto.AdminCatalogViewDTO.CatalogPage;
import com.aptisprep.mvc.catalog.dto.ExamFormDTO;
import com.aptisprep.mvc.catalog.dto.QuestionFormDTO;
import com.aptisprep.mvc.catalog.service.CatalogService;
import com.aptisprep.mvc.common.exception.BusinessRuleException;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminCatalogController {
    private static final Map<String, String> EXAM_TYPE_LABELS =
            Map.of(
                    "PLACEMENT", "Kiểm tra đầu vào",
                    "CHECKPOINT", "Kiểm tra cuối chặng",
                    "FULL_MOCK", "Thi thử tổng hợp",
                    "PRACTICE", "Bài luyện tập");
    private final CatalogService catalogService;

    public AdminCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/admin/questions")
    public String questions(
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "skill", defaultValue = "") String skill,
            @RequestParam(name = "status", defaultValue = "") String status,
            @RequestParam(name = "kind", defaultValue = "") String kind,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        if (q.length() > 255 || skill.length() > 80 || status.length() > 40 || kind.length() > 80)
            throw new BusinessRuleException("Bộ lọc không hợp lệ");
        addModule("questions", model);
        CatalogPage pageData =
                catalogService.findPage("questions", q, skill, status, kind, page, size);
        model.addAttribute("pageData", pageData);
        model.addAttribute("q", q);
        model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedKind", kind);
        model.addAttribute("examTypeLabels", EXAM_TYPE_LABELS);
        return "admin/catalog/list";
    }

    @GetMapping("/admin/exams")
    public String exams(
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "skill", defaultValue = "") String skill,
            @RequestParam(name = "status", defaultValue = "") String status,
            @RequestParam(name = "kind", defaultValue = "") String kind,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        if (q.length() > 255 || skill.length() > 80 || status.length() > 40 || kind.length() > 80)
            throw new BusinessRuleException("Bộ lọc không hợp lệ");
        addModule("exams", model);
        CatalogPage pageData = catalogService.findPage("exams", q, skill, status, kind, page, size);
        model.addAttribute("pageData", pageData);
        model.addAttribute("q", q);
        model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedKind", kind);
        model.addAttribute("examTypeLabels", EXAM_TYPE_LABELS);
        return "admin/catalog/list";
    }

    @GetMapping("/admin/questions/{id}")
    public String question(@PathVariable int id, Model model) {
        addModule("questions", model);
        model.addAttribute("entry", catalogService.entry("questions", id));
        model.addAttribute("mediaItems", catalogService.mediaItems(id));
        return "admin/catalog/detail";
    }

    @GetMapping("/admin/exams/{id}")
    public String exam(@PathVariable int id, Model model) {
        addModule("exams", model);
        model.addAttribute("entry", catalogService.entry("exams", id));
        model.addAttribute("examPreview", catalogService.examPreview(id));
        return "admin/catalog/detail";
    }

    @GetMapping("/admin/questions/create")
    public String createQuestion(Model model) {
        addModule("questions", model);
        model.addAttribute("editing", false);
        model.addAttribute("questionForm", catalogService.questionForm(null));
        return "admin/catalog/form";
    }

    @GetMapping("/admin/exams/create")
    public String createExam(Model model) {
        addModule("exams", model);
        model.addAttribute("editing", false);
        prepareExamFormDTO(model, catalogService.examForm(null));
        return "admin/catalog/form";
    }

    @GetMapping("/admin/questions/{id}/edit")
    public String editQuestion(@PathVariable int id, Model model) {
        addModule("questions", model);
        model.addAttribute("editing", true);
        model.addAttribute("entry", catalogService.entry("questions", id));
        model.addAttribute("questionForm", catalogService.questionForm(id));
        return "admin/catalog/form";
    }

    @GetMapping("/admin/exams/{id}/edit")
    public String editExam(@PathVariable int id, Model model) {
        addModule("exams", model);
        model.addAttribute("editing", true);
        model.addAttribute("entry", catalogService.entry("exams", id));
        prepareExamFormDTO(model, catalogService.examForm(id));
        return "admin/catalog/form";
    }

    @PostMapping("/admin/questions")
    public String saveQuestion(
            @ModelAttribute QuestionFormDTO form,
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
            @ModelAttribute QuestionFormDTO form,
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
    public String saveExam(@ModelAttribute ExamFormDTO form, BindingResult binding, Model model) {
        String error = "Kiểm tra lại giá trị đã nhập trong đề thi.";
        if (!binding.hasErrors()) {
            try {
                return "redirect:/admin/exams/" + catalogService.saveExam(null, form);
            } catch (BusinessRuleException exception) {
                error = exception.getMessage();
            }
        }
        addModule("exams", model);
        model.addAttribute("editing", false);
        model.addAttribute("examError", error);
        prepareExamFormDTO(model, form);
        return "admin/catalog/form";
    }

    @PostMapping("/admin/exams/{id}/edit")
    public String updateExam(
            @PathVariable int id,
            @ModelAttribute ExamFormDTO form,
            BindingResult binding,
            Model model) {
        String error = "Kiểm tra lại giá trị đã nhập trong đề thi.";
        if (!binding.hasErrors()) {
            try {
                return "redirect:/admin/exams/" + catalogService.saveExam(id, form);
            } catch (BusinessRuleException exception) {
                error = exception.getMessage();
            }
        }
        addModule("exams", model);
        model.addAttribute("editing", true);
        model.addAttribute("entry", catalogService.entry("exams", id));
        model.addAttribute("examError", error);
        prepareExamFormDTO(model, form);
        return "admin/catalog/form";
    }

    @PostMapping("/admin/exams/{id}/publish")
    public String publishExam(@PathVariable int id, RedirectAttributes redirect) {
        try {
            catalogService.publishExam(id);
            redirect.addFlashAttribute("successMessage", "Đã công bố đề thi.");
        } catch (BusinessRuleException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/exams/" + id;
    }

    @PostMapping("/admin/exams/{id}/unpublish")
    public String unpublishExam(@PathVariable int id) {
        catalogService.unpublishExam(id);
        return "redirect:/admin/exams/" + id;
    }

    private void addModule(String key, Model model) {
        model.addAttribute("catalog", catalogService.module(key));
        model.addAttribute("skills", catalogService.skills());
        model.addAttribute("levels", catalogService.levels());
    }

    private void prepareExamFormDTO(Model model, ExamFormDTO form) {
        model.addAttribute("examForm", form);
        model.addAttribute("certificates", catalogService.activeCertificates());
        model.addAttribute("schemes", catalogService.activeSchemes());
        model.addAttribute("publishedQuestions", catalogService.publishedQuestions());
        model.addAttribute("examTypes", catalogService.examTypes());
    }

    private void prepareQuestionError(
            Model model, QuestionFormDTO form, boolean editing, String message, Integer id) {
        addModule("questions", model);
        model.addAttribute("editing", editing);
        model.addAttribute("questionForm", form);
        model.addAttribute("formError", message);
        if (id != null) model.addAttribute("entry", catalogService.entry("questions", id));
    }
}
