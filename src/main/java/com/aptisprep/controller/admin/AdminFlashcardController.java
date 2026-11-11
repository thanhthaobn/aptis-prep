package com.aptisprep.controller.admin;

import com.aptisprep.dto.form.FlashcardSetFormDTO;
import com.aptisprep.dto.view.FlashcardViewsDTO.SetView;
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
    public String showFlashcardList(Model model) {
        List<SetView> sets = flashcardSetService.list(true);
        model.addAttribute("sets", sets);
        model.addAttribute("lessonBranches", flashcardSetService.groupBySkillAndLevel(sets));
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
        return "admin/flashcards/list";
    }

    @GetMapping("/admin/flashcards/create")
    public String showCreateForm(@RequestParam(required = false) Integer lessonId, Model model) {
        FlashcardSetFormDTO form = new FlashcardSetFormDTO();
        form.getRows().add(new FlashcardSetFormDTO.Row());
        flashcardSetService.prepareForLesson(form, lessonId);
        model.addAttribute("form", form);
        model.addAttribute("setId", null);
        model.addAttribute("levels", LessonService.LEVELS);
        model.addAttribute("lessons", flashcardSetService.availableLessons());
        return "admin/flashcards/form";
    }

    @GetMapping("/admin/flashcards/{id}/edit")
    public String showEditForm(@PathVariable Integer id, Model model) {
        FlashcardSetFormDTO form = flashcardSetService.form(id);
        model.addAttribute("form", form);
        model.addAttribute("setId", id);
        model.addAttribute("levels", LessonService.LEVELS);
        model.addAttribute("lessons", flashcardSetService.availableLessons());
        return "admin/flashcards/form";
    }

    @PostMapping("/admin/flashcards")
    public String createFlashcardSet(
            @Valid @ModelAttribute("form") FlashcardSetFormDTO form,
            BindingResult bindingResult,
            Model model) {
        if (!bindingResult.hasErrors()) {
            try {
                flashcardSetService.saveAdmin(null, form);
                return "redirect:/admin/flashcards";
            } catch (BusinessRuleException exception) {
                bindingResult.reject("invalid", exception.getMessage());
            }
        }
        model.addAttribute("form", form);
        model.addAttribute("setId", null);
        model.addAttribute("levels", LessonService.LEVELS);
        model.addAttribute("lessons", flashcardSetService.availableLessons());
        return "admin/flashcards/form";
    }

    @PostMapping("/admin/flashcards/{id}")
    public String updateFlashcardSet(
            @PathVariable Integer id,
            @Valid @ModelAttribute("form") FlashcardSetFormDTO form,
            BindingResult bindingResult,
            Model model) {
        if (!bindingResult.hasErrors()) {
            try {
                flashcardSetService.saveAdmin(id, form);
                return "redirect:/admin/flashcards";
            } catch (BusinessRuleException e) {
                bindingResult.reject("invalid", e.getMessage());
            }
        }
        model.addAttribute("form", form);
        model.addAttribute("setId", id);
        model.addAttribute("levels", LessonService.LEVELS);
        model.addAttribute("lessons", flashcardSetService.availableLessons());
        return "admin/flashcards/form";
    }
}
