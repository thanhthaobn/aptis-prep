package com.aptisprep.mvc.controller.admin;

import com.aptisprep.mvc.dto.MockExamDTO;
import com.aptisprep.mvc.dto.MockExamSectionDTO;
import com.aptisprep.mvc.dto.QuestionDTO;
import com.aptisprep.mvc.dto.QuestionItemDTO;
import com.aptisprep.mvc.dto.QuestionOptionDTO;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.service.admin.CatalogService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
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
        if (q.length() > 255
                || skill.length() > 80
                || status.length() > 40
                || kind.length() > 80
                || page < 0
                || size < 1
                || size > 50) {
            throw new BusinessRuleException("Bộ lọc không hợp lệ");
        }
        model.addAttribute("catalog", catalogService.module("questions"));
        model.addAttribute("skills", catalogService.skills());
        model.addAttribute("levels", catalogService.levels());
        model.addAttribute(
                "pageData", catalogService.questionPage(q, skill, status, kind, page, size));
        model.addAttribute("q", q);
        model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedKind", kind);
        return "admin/catalog/list";
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
        if (q.length() > 255
                || skill.length() > 80
                || status.length() > 40
                || kind.length() > 80
                || page < 0
                || size < 1
                || size > 50) {
            throw new BusinessRuleException("Bộ lọc không hợp lệ");
        }
        model.addAttribute("catalog", catalogService.module("exams"));
        model.addAttribute("skills", catalogService.skills());
        model.addAttribute("levels", catalogService.levels());
        model.addAttribute("pageData", catalogService.examPage(q, skill, status, kind, page, size));
        model.addAttribute("q", q);
        model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedKind", kind);
        return "admin/catalog/list";
    }

    @GetMapping("/admin/questions/{id}")
    public String question(@PathVariable int id, Model model) {
        model.addAttribute("catalog", catalogService.module("questions"));
        model.addAttribute("skills", catalogService.skills());
        model.addAttribute("levels", catalogService.levels());
        model.addAttribute("entry", catalogService.question(id));
        model.addAttribute("mediaItems", catalogService.mediaItemViews(id));
        return "admin/catalog/detail";
    }

    @GetMapping("/admin/exams/{id}")
    public String exam(@PathVariable int id, Model model) {
        model.addAttribute("catalog", catalogService.module("exams"));
        model.addAttribute("skills", catalogService.skills());
        model.addAttribute("levels", catalogService.levels());
        model.addAttribute("entry", catalogService.exam(id));
        model.addAttribute("examPreview", catalogService.examPreview(id));
        return "admin/catalog/detail";
    }

    @GetMapping("/admin/questions/create")
    public String createQuestion(Model model) {
        model.addAttribute("catalog", catalogService.module("questions"));
        model.addAttribute("skills", catalogService.skills());
        model.addAttribute("levels", catalogService.levels());
        model.addAttribute("editing", false);
        model.addAttribute("questionForm", catalogService.questionForm(null));
        return "admin/catalog/form";
    }

    @GetMapping("/admin/exams/create")
    public String createExam(Model model) {
        renderExamForm(model, catalogService.examForm(null), false, null, null);
        return "admin/catalog/form";
    }

    @GetMapping("/admin/questions/{id}/edit")
    public String editQuestion(@PathVariable int id, Model model) {
        model.addAttribute("catalog", catalogService.module("questions"));
        model.addAttribute("skills", catalogService.skills());
        model.addAttribute("levels", catalogService.levels());
        model.addAttribute("editing", true);
        model.addAttribute("entry", catalogService.question(id));
        model.addAttribute("questionForm", catalogService.questionForm(id));
        return "admin/catalog/form";
    }

    @GetMapping("/admin/exams/{id}/edit")
    public String editExam(@PathVariable int id, Model model) {
        renderExamForm(model, catalogService.examForm(id), true, id, null);
        return "admin/catalog/form";
    }

    @PostMapping("/admin/questions")
    public String saveQuestion(
            @Valid @ModelAttribute QuestionDTO form,
            BindingResult binding,
            @RequestParam(name = "file", required = false) MultipartFile file,
            @RequestParam(name = "formAction", defaultValue = "SAVE") String formAction,
            Model model) {
        if (!"SAVE".equals(formAction)) {
            changeQuestionForm(form, formAction);
            renderQuestionForm(model, form, false, null, null);
            return "admin/catalog/form";
        }
        if (binding.hasErrors()) {
            String message =
                    binding.getFieldError() != null
                            ? binding.getFieldError().getDefaultMessage()
                            : binding.getGlobalError() != null
                                    ? binding.getGlobalError().getDefaultMessage()
                                    : "Kiểm tra lại giá trị đã nhập.";
            model.addAttribute("catalog", catalogService.module("questions"));
            model.addAttribute("skills", catalogService.skills());
            model.addAttribute("levels", catalogService.levels());
            model.addAttribute("editing", false);
            model.addAttribute("questionForm", form);
            model.addAttribute("formError", message);
            return "admin/catalog/form";
        }
        try {
            return "redirect:/admin/questions/" + catalogService.saveQuestion(null, form, file);
        } catch (BusinessRuleException exception) {
            model.addAttribute("catalog", catalogService.module("questions"));
            model.addAttribute("skills", catalogService.skills());
            model.addAttribute("levels", catalogService.levels());
            model.addAttribute("editing", false);
            model.addAttribute("questionForm", form);
            model.addAttribute("formError", exception.getMessage());
            return "admin/catalog/form";
        }
    }

    @PostMapping("/admin/questions/{id}/edit")
    public String updateQuestion(
            @PathVariable int id,
            @Valid @ModelAttribute QuestionDTO form,
            BindingResult binding,
            @RequestParam(name = "file", required = false) MultipartFile file,
            @RequestParam(name = "formAction", defaultValue = "SAVE") String formAction,
            Model model) {
        if (!"SAVE".equals(formAction)) {
            changeQuestionForm(form, formAction);
            renderQuestionForm(model, form, true, id, null);
            return "admin/catalog/form";
        }
        if (binding.hasErrors()) {
            String message =
                    binding.getFieldError() != null
                            ? binding.getFieldError().getDefaultMessage()
                            : binding.getGlobalError() != null
                                    ? binding.getGlobalError().getDefaultMessage()
                                    : "Kiểm tra lại giá trị đã nhập.";
            model.addAttribute("catalog", catalogService.module("questions"));
            model.addAttribute("skills", catalogService.skills());
            model.addAttribute("levels", catalogService.levels());
            model.addAttribute("editing", true);
            model.addAttribute("questionForm", form);
            model.addAttribute("formError", message);
            model.addAttribute("entry", catalogService.question(id));
            return "admin/catalog/form";
        }
        try {
            catalogService.saveQuestion(id, form, file);
            return "redirect:/admin/questions/" + id;
        } catch (BusinessRuleException exception) {
            model.addAttribute("catalog", catalogService.module("questions"));
            model.addAttribute("skills", catalogService.skills());
            model.addAttribute("levels", catalogService.levels());
            model.addAttribute("editing", true);
            model.addAttribute("questionForm", form);
            model.addAttribute("formError", exception.getMessage());
            model.addAttribute("entry", catalogService.question(id));
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

    private void changeQuestionForm(QuestionDTO form, String action) {
        form.setItems(
                form.getItems() == null ? new ArrayList<>() : new ArrayList<>(form.getItems()));
        if (action.startsWith("REMOVE_ITEM_")) {
            try {
                int index = Integer.parseInt(action.substring("REMOVE_ITEM_".length()));
                if (form.getItems().size() > 1 && index >= 0 && index < form.getItems().size())
                    form.getItems().remove(index);
            } catch (NumberFormatException ignored) {
                // Ignore a malformed UI action; the unchanged form is rendered again.
            }
        } else if ("ADD_ITEM".equals(action)) {
            form.getItems().add(new QuestionItemDTO());
        }
        if (form.getItems().isEmpty()) form.getItems().add(new QuestionItemDTO());
        if ("WRITING".equals(form.getSkill())) form.setQuestionType("WRITING_TEXT");
        else if ("SPEAKING".equals(form.getSkill())) form.setQuestionType("SPEAKING_AUDIO");
        else if (!java.util.Set.of("SINGLE_CHOICE", "MATCHING", "ORDERING")
                .contains(form.getQuestionType())) form.setQuestionType("SINGLE_CHOICE");
        if (action.startsWith("ADD_OPTION_") || action.startsWith("REMOVE_OPTION_")) {
            int separator = action.lastIndexOf('_');
            try {
                int itemIndex = Integer.parseInt(action.substring(separator + 1));
                if (itemIndex >= 0 && itemIndex < form.getItems().size()) {
                    int optionIndex =
                            action.startsWith("ADD_OPTION_")
                                    ? -1
                                    : Integer.parseInt(
                                            action.substring("REMOVE_OPTION_".length(), separator));
                    QuestionItemDTO item = form.getItems().get(itemIndex);
                    List<QuestionOptionDTO> options =
                            item.getOptions() == null
                                    ? new ArrayList<>()
                                    : new ArrayList<>(item.getOptions());
                    if (action.startsWith("ADD_OPTION_") && options.size() < 26) {
                        options.add(
                                new QuestionOptionDTO(
                                        null,
                                        null,
                                        null,
                                        String.valueOf((char) ('A' + options.size())),
                                        false,
                                        options.size() + 1));
                    } else if (action.startsWith("REMOVE_OPTION_")
                            && options.size() > 2
                            && optionIndex >= 0
                            && optionIndex < options.size()) {
                        options.remove(optionIndex);
                        for (int index = 0; index < options.size(); index++) {
                            options.get(index).setOptionCode(String.valueOf((char) ('A' + index)));
                            options.get(index).setPosition(index + 1);
                        }
                        if (item.getCorrectPosition() != null
                                && item.getCorrectPosition() > options.size())
                            item.setCorrectPosition(null);
                    }
                    item.setOptions(options);
                }
            } catch (NumberFormatException ignored) {
                // Ignore malformed option actions and render the unchanged form.
            }
        }
    }

    private void renderQuestionForm(
            Model model, QuestionDTO form, boolean editing, Integer id, String error) {
        model.addAttribute("catalog", catalogService.module("questions"));
        model.addAttribute("skills", catalogService.skills());
        model.addAttribute("levels", catalogService.levels());
        model.addAttribute("editing", editing);
        model.addAttribute("questionForm", form);
        if (editing && id != null) model.addAttribute("entry", catalogService.question(id));
        if (error != null) model.addAttribute("formError", error);
    }

    private void changeExamForm(MockExamDTO form, String action) {
        form.setSections(
                form.getSections() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(form.getSections()));
        if (action.startsWith("REMOVE_SECTION_")) {
            try {
                int index = Integer.parseInt(action.substring("REMOVE_SECTION_".length()));
                if (form.getSections().size() > 1
                        && index >= 0
                        && index < form.getSections().size()) form.getSections().remove(index);
            } catch (NumberFormatException ignored) {
                // Ignore a malformed UI action; the unchanged form is rendered again.
            }
        } else if ("ADD_SECTION".equals(action)) {
            MockExamSectionDTO section = new MockExamSectionDTO();
            section.setSkill("READING");
            section.setDisplayOrder(form.getSections().size() + 1);
            section.setDurationMinutes(35);
            section.setQuestionIds("");
            form.getSections().add(section);
        }
        if (form.getSections().isEmpty()) {
            MockExamSectionDTO section = new MockExamSectionDTO();
            section.setSkill("READING");
            section.setDisplayOrder(1);
            section.setDurationMinutes(35);
            section.setQuestionIds("");
            form.getSections().add(section);
        }
    }

    private void normalizeExamQuestionIds(MockExamDTO form) {
        if (form.getSections() == null) return;
        for (MockExamSectionDTO section : form.getSections()) {
            if (section.getSelectedQuestionIds() == null
                    || section.getSelectedQuestionIds().isEmpty()) continue;
            section.setQuestionIds(
                    section.getSelectedQuestionIds().stream()
                            .filter(java.util.Objects::nonNull)
                            .map(String::valueOf)
                            .distinct()
                            .collect(java.util.stream.Collectors.joining(",")));
        }
    }

    private void renderExamForm(
            Model model, MockExamDTO form, boolean editing, Integer id, String error) {
        model.addAttribute("catalog", catalogService.module("exams"));
        model.addAttribute("skills", catalogService.skills());
        model.addAttribute("levels", catalogService.levels());
        model.addAttribute("editing", editing);
        model.addAttribute("examForm", form);
        model.addAttribute("certificates", catalogService.activeCertificateViews());
        model.addAttribute("schemes", catalogService.activeSchemeViews());
        model.addAttribute("publishedQuestions", catalogService.publishedQuestionViews());
        model.addAttribute("examTypes", catalogService.examTypes());
        if (editing && id != null) model.addAttribute("entry", catalogService.exam(id));
        if (error != null) model.addAttribute("examError", error);
    }

    @PostMapping("/admin/exams")
    public String saveExam(
            @Valid @ModelAttribute MockExamDTO form,
            BindingResult binding,
            @RequestParam(name = "formAction", defaultValue = "SAVE") String formAction,
            Model model) {
        if (!"SAVE".equals(formAction)) {
            changeExamForm(form, formAction);
            normalizeExamQuestionIds(form);
            renderExamForm(model, form, false, null, null);
            return "admin/catalog/form";
        }
        String error = "Kiểm tra lại giá trị đã nhập trong đề thi.";
        normalizeExamQuestionIds(form);
        if (binding.hasErrors()) {
            error =
                    binding.getFieldError() != null
                            ? binding.getFieldError().getDefaultMessage()
                            : binding.getGlobalError() != null
                                    ? binding.getGlobalError().getDefaultMessage()
                                    : "Kiểm tra lại giá trị đã nhập.";
        } else {
            try {
                return "redirect:/admin/exams/" + catalogService.saveExam(null, form);
            } catch (BusinessRuleException exception) {
                error = exception.getMessage();
            }
        }
        renderExamForm(model, form, false, null, error);
        return "admin/catalog/form";
    }

    @PostMapping("/admin/exams/{id}/edit")
    public String updateExam(
            @PathVariable int id,
            @Valid @ModelAttribute MockExamDTO form,
            BindingResult binding,
            @RequestParam(name = "formAction", defaultValue = "SAVE") String formAction,
            Model model) {
        if (!"SAVE".equals(formAction)) {
            changeExamForm(form, formAction);
            normalizeExamQuestionIds(form);
            renderExamForm(model, form, true, id, null);
            return "admin/catalog/form";
        }
        String error = "Kiểm tra lại giá trị đã nhập trong đề thi.";
        normalizeExamQuestionIds(form);
        if (binding.hasErrors()) {
            error =
                    binding.getFieldError() != null
                            ? binding.getFieldError().getDefaultMessage()
                            : binding.getGlobalError() != null
                                    ? binding.getGlobalError().getDefaultMessage()
                                    : "Kiểm tra lại giá trị đã nhập.";
        } else {
            try {
                return "redirect:/admin/exams/" + catalogService.saveExam(id, form);
            } catch (BusinessRuleException exception) {
                error = exception.getMessage();
            }
        }
        renderExamForm(model, form, true, id, error);
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

    @PostMapping("/admin/exams/{id}/restore")
    public String restoreExam(@PathVariable int id) {
        catalogService.restoreExam(id);
        return "redirect:/admin/exams/" + id;
    }
}
