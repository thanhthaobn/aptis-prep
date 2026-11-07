package com.aptisprep.controller.admin;

import com.aptisprep.controller.ListPagination;
import com.aptisprep.dto.form.FlashcardSetForm;
import com.aptisprep.dto.view.FlashcardViews.SetView;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.learning.FlashcardSetService;
import com.aptisprep.service.learning.LessonService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class AdminFlashcardController {
    private final FlashcardSetService flashcardSetService;

    public AdminFlashcardController(FlashcardSetService flashcardSetService) {
        this.flashcardSetService = flashcardSetService;
    }

    @GetMapping("/admin/flashcards")
    public String showFlashcardList(@RequestParam(defaultValue = "0") int page, Model model) {
        var flashcardPage = ListPagination.slice(flashcardSetService.list(true), page);
        List<SetView> sets = flashcardPage.getContent();
        model.addAttribute("flashcardPage", flashcardPage);
        model.addAttribute("sets", sets);
        model.addAttribute("admin", true);
        model.addAttribute("lessonBranches", FlashcardSetService.groupBySkillAndLevel(sets));
        model.addAttribute(
                "unclassifiedSets",
                sets.stream()
                        .filter(
                                s ->
                                        s.lessonSkill() == null
                                                || s.level() == null
                                                || !LessonService.SKILLS.contains(s.lessonSkill())
                                                || !LessonService.LEVELS.contains(s.level()))
                        .toList());
        return "flashcard/list";
    }

    @GetMapping("/admin/flashcards/create")
    public String showCreateForm(@RequestParam(required = false) Integer lessonId, Model model) {
        FlashcardSetForm form = new FlashcardSetForm();
        form.getRows().add(new FlashcardSetForm.Row());
        flashcardSetService.prepareForLesson(form, lessonId);
        return renderForm(model, form, null);
    }

    @GetMapping("/admin/flashcards/{id}/edit")
    public String showEditForm(@PathVariable Integer id, Model model) {
        return renderForm(model, flashcardSetService.form(id), id);
    }

    @PostMapping("/admin/flashcards")
    public String createFlashcardSet(
            @Valid @ModelAttribute("form") FlashcardSetForm form,
            BindingResult bindingResult,
            Model model) {
        return saveFlashcardSet(null, form, bindingResult, model);
    }

    @PostMapping("/admin/flashcards/{id}")
    public String updateFlashcardSet(
            @PathVariable Integer id,
            @Valid @ModelAttribute("form") FlashcardSetForm form,
            BindingResult bindingResult,
            Model model) {
        return saveFlashcardSet(id, form, bindingResult, model);
    }

    private String saveFlashcardSet(
            Integer id, FlashcardSetForm form, BindingResult bindingResult, Model model) {
        if (!bindingResult.hasErrors()) {
            try {
                flashcardSetService.saveAdmin(id, form);
                return "redirect:/admin/flashcards";
            } catch (BusinessRuleException e) {
                bindingResult.reject("invalid", e.getMessage());
            }
        }
        return renderForm(model, form, id);
    }

    private String renderForm(Model model, FlashcardSetForm form, Integer id) {
        model.addAttribute("form", form);
        model.addAttribute("setId", id);
        model.addAttribute("levels", LessonService.LEVELS);
        model.addAttribute("lessons", flashcardSetService.availableLessons());
        model.addAttribute("personal", false);
        return "flashcard/form";
    }
}
