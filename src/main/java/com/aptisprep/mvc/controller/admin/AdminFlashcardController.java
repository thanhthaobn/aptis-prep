package com.aptisprep.mvc.controller.admin;

import com.aptisprep.mvc.dto.FlashcardDTO;
import com.aptisprep.mvc.dto.FlashcardSetDTO;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.service.learning.FlashcardSetService;
import com.aptisprep.mvc.service.learning.LessonService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class AdminFlashcardController {
    private final FlashcardSetService flashcardSetService;
    private final LessonService lessonService;

    public AdminFlashcardController(
            FlashcardSetService flashcardSetService, LessonService lessonService) {
        this.flashcardSetService = flashcardSetService;
        this.lessonService = lessonService;
    }

    @GetMapping("/admin/flashcards")
    public String showFlashcardList(Model model) {
        List<FlashcardSetDTO> sets = flashcardSetService.list(true);
        model.addAttribute("sets", sets);
        model.addAttribute("lessonBranches", flashcardSetService.groupBySkillAndLevel(sets));
        model.addAttribute(
                "unclassifiedSets",
                sets.stream()
                        .filter(
                                s ->
                                        s.getLessonSkill() == null
                                                || s.getLevel() == null
                                                || !lessonService
                                                        .getSkills()
                                                        .contains(s.getLessonSkill())
                                                || !lessonService
                                                        .getLevels()
                                                        .contains(s.getLevel()))
                        .toList());
        return "admin/flashcards/list";
    }

    @GetMapping("/admin/flashcards/create")
    public String showCreateForm(@RequestParam(required = false) Integer lessonId, Model model) {
        FlashcardSetDTO form = new FlashcardSetDTO();
        form.getFlashcards().add(new FlashcardDTO());
        flashcardSetService.prepareForLesson(form, lessonId);
        model.addAttribute("form", form);
        model.addAttribute("setId", null);
        model.addAttribute("levels", lessonService.getLevels());
        model.addAttribute("lessons", flashcardSetService.availableLessonViews());
        return "admin/flashcards/form";
    }

    @GetMapping("/admin/flashcards/{id}/edit")
    public String showEditForm(@PathVariable Integer id, Model model) {
        model.addAttribute("form", flashcardSetService.form(id));
        model.addAttribute("setId", id);
        model.addAttribute("levels", lessonService.getLevels());
        model.addAttribute("lessons", flashcardSetService.availableLessonViews());
        return "admin/flashcards/form";
    }

    @PostMapping("/admin/flashcards")
    public String createFlashcardSet(
            @Valid @ModelAttribute("form") FlashcardSetDTO form,
            BindingResult bindingResult,
            @RequestParam(name = "formAction", defaultValue = "SAVE") String formAction,
            @RequestParam(name = "bulkImport", required = false) String bulkImport,
            Model model) {
        if (!"SAVE".equals(formAction)) {
            changeForm(form, formAction, bulkImport);
            populateFormModel(model, form, null);
            return "admin/flashcards/form";
        }
        if (!bindingResult.hasErrors()) {
            try {
                flashcardSetService.saveAdmin(null, form);
                return "redirect:/admin/flashcards";
            } catch (BusinessRuleException exception) {
                bindingResult.reject("invalid", exception.getMessage());
            }
        }
        populateFormModel(model, form, null);
        return "admin/flashcards/form";
    }

    @PostMapping("/admin/flashcards/{id}")
    public String updateFlashcardSet(
            @PathVariable Integer id,
            @Valid @ModelAttribute("form") FlashcardSetDTO form,
            BindingResult bindingResult,
            @RequestParam(name = "formAction", defaultValue = "SAVE") String formAction,
            @RequestParam(name = "bulkImport", required = false) String bulkImport,
            Model model) {
        if (!"SAVE".equals(formAction)) {
            changeForm(form, formAction, bulkImport);
            populateFormModel(model, form, id);
            return "admin/flashcards/form";
        }
        if (!bindingResult.hasErrors()) {
            try {
                flashcardSetService.saveAdmin(id, form);
                return "redirect:/admin/flashcards";
            } catch (BusinessRuleException exception) {
                bindingResult.reject("invalid", exception.getMessage());
            }
        }
        populateFormModel(model, form, id);
        return "admin/flashcards/form";
    }

    private void changeForm(FlashcardSetDTO form, String action, String bulkImport) {
        form.setFlashcards(
                form.getFlashcards() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(form.getFlashcards()));
        if ("ADD_CARD".equals(action)) {
            form.getFlashcards().add(new FlashcardDTO());
        } else if (action.startsWith("REMOVE_CARD_")) {
            try {
                int index = Integer.parseInt(action.substring("REMOVE_CARD_".length()));
                if (form.getFlashcards().size() > 1
                        && index >= 0
                        && index < form.getFlashcards().size()) {
                    form.getFlashcards().remove(index);
                }
            } catch (NumberFormatException ignored) {
                // Render the unchanged form for a malformed UI action.
            }
        } else if ("IMPORT_CARDS".equals(action) && bulkImport != null) {
            for (String line : bulkImport.split("\\R")) {
                String value = line.trim();
                if (value.isEmpty()) continue;
                String[] pair = value.split("\\s*[|\\t,]\\s*", 2);
                if (pair.length == 2 && !pair[0].isBlank() && !pair[1].isBlank()) {
                    FlashcardDTO card = new FlashcardDTO();
                    card.setFrontContent(pair[0].trim());
                    card.setBackContent(pair[1].trim());
                    form.getFlashcards().add(card);
                }
            }
        }
        if (form.getFlashcards().isEmpty()) form.getFlashcards().add(new FlashcardDTO());
    }

    private void populateFormModel(Model model, FlashcardSetDTO form, Integer id) {
        model.addAttribute("form", form);
        model.addAttribute("setId", id);
        model.addAttribute("levels", lessonService.getLevels());
        model.addAttribute("lessons", flashcardSetService.availableLessonViews());
    }
}
