package com.aptisprep.mvc.controller.student;

import com.aptisprep.mvc.dto.FlashcardSetDTO;
import com.aptisprep.mvc.dto.FlashcardDTO;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.service.learning.FlashcardSetService;
import com.aptisprep.mvc.service.learning.LessonService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.security.Principal;
import java.util.UUID;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StudentFlashcardController {
    private final FlashcardSetService flashcardSetService;
    private final LessonService lessonService;

    public StudentFlashcardController(
            FlashcardSetService flashcardSetService,
            LessonService lessonService) {
        this.flashcardSetService = flashcardSetService;
        this.lessonService = lessonService;
    }

    @GetMapping("/student/flashcards")
    public String list(
            @RequestParam(defaultValue = "all") String source,
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "skill", defaultValue = "") String skill,
            @RequestParam(name = "level", defaultValue = "") String level,
            @RequestParam(defaultValue = "learning") String sort,
            Principal principal,
            Model model) {
        List<FlashcardSetDTO> all = flashcardSetService.listForStudent(principal.getName());
        List<FlashcardSetDTO> visible =
                flashcardSetService.filteredListForStudent(principal.getName(), source, q, skill, level, sort);
        model.addAttribute("sets", visible);
        model.addAttribute("q", q);
        model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedLevel", level);
        model.addAttribute("selectedSort", sort);
        model.addAttribute(
                "lessonResultCount",
                visible.stream()
                        .filter(set -> !set.isPersonal())
                        .count());
        model.addAttribute(
                "mineResultCount",
                visible.stream().filter(streamValue -> streamValue.isPersonal()).count());
        model.addAttribute("lessonBranches", flashcardSetService.groupBySkillAndLevel(visible));
        model.addAttribute("skills", lessonService.getSkills());
        model.addAttribute("levels", lessonService.getLevels());
        model.addAttribute(
                "totalCards",
                all.stream()
                        .mapToInt(
                                streamValue -> streamValue.getCount())
                        .sum());
        model.addAttribute(
                "visibleCards",
                visible.stream()
                        .mapToInt(
                                streamValue -> streamValue.getCount())
                        .sum());
        model.addAttribute("selectedSource", source);
        model.addAttribute("totalSets", all.size());
        model.addAttribute(
                "mineCount",
                all.stream().filter(streamValue -> streamValue.isPersonal()).count());
        model.addAttribute(
                "lessonCount",
                all.stream()
                        .filter(
                                set -> !set.isPersonal() && set.getLessonId() != null)
                        .count());
        return "student/flashcards/list";
    }

    @GetMapping("/student/flashcards/create")
    public String showCreateForm(Model model) {
        FlashcardSetDTO flashcardSetForm = new FlashcardSetDTO();
        flashcardSetForm.getFlashcards().add(new FlashcardDTO());
        model.addAttribute("form", flashcardSetForm);
        model.addAttribute("setId", null);
        model.addAttribute("levels", lessonService.getLevels());
        return "student/flashcards/form";
    }

    @GetMapping("/student/flashcards/{id}/edit")
    public String showEditForm(@PathVariable Integer id, Principal principal, Model model) {
        model.addAttribute("form", flashcardSetService.personalForm(id, principal.getName()));
        model.addAttribute("setId", id);
        model.addAttribute("levels", lessonService.getLevels());
        return "student/flashcards/form";
    }

    @PostMapping("/student/flashcards")
    public String createPersonalSet(
            @Valid @ModelAttribute("form") FlashcardSetDTO flashcardSetForm,
            BindingResult bindingResult,
            @RequestParam(name = "formAction", defaultValue = "SAVE") String formAction,
            @RequestParam(name = "bulkImport", required = false) String bulkImport,
            Principal principal,
            Model model) {
        if (!"SAVE".equals(formAction)) {
            changeForm(flashcardSetForm, formAction, bulkImport);
            populateFormModel(model, flashcardSetForm, null);
            return "student/flashcards/form";
        }
        if (!bindingResult.hasErrors())
            try {
                flashcardSetService.savePersonal(null, flashcardSetForm, principal.getName());
                return "redirect:/student/flashcards";
            } catch (BusinessRuleException exception) {
                bindingResult.reject("invalid", exception.getMessage());
            }
        populateFormModel(model, flashcardSetForm, null);
        return "student/flashcards/form";
    }

    @PostMapping("/student/flashcards/{id}")
    public String updatePersonalSet(
            @PathVariable Integer id,
            @Valid @ModelAttribute("form") FlashcardSetDTO flashcardSetForm,
            BindingResult bindingResult,
            @RequestParam(name = "formAction", defaultValue = "SAVE") String formAction,
            @RequestParam(name = "bulkImport", required = false) String bulkImport,
            Principal principal,
            Model model) {
        if (!"SAVE".equals(formAction)) {
            changeForm(flashcardSetForm, formAction, bulkImport);
            populateFormModel(model, flashcardSetForm, id);
            return "student/flashcards/form";
        }
        if (!bindingResult.hasErrors())
            try {
                flashcardSetService.savePersonal(id, flashcardSetForm, principal.getName());
                return "redirect:/student/flashcards";
            } catch (BusinessRuleException exception) {
                bindingResult.reject("invalid", exception.getMessage());
            }
        populateFormModel(model, flashcardSetForm, id);
        return "student/flashcards/form";
    }

    @PostMapping("/student/flashcards/{id}/archive")
    public String archive(@PathVariable Integer id, Principal principal, RedirectAttributes redirectAttributes) {
        flashcardSetService.archivePersonal(id, principal.getName());
        redirectAttributes.addFlashAttribute("message", "Đã lưu trữ bộ thẻ. Bạn có thể tạo lại khi cần.");
        return "redirect:/student/flashcards";
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
    }

    @GetMapping("/student/flashcards/{id}")
    public String study(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "0") int card,
            @RequestParam(defaultValue = "false") boolean unlearned,
            Principal principal,
            Model model) {
        List<FlashcardDTO> allCards = flashcardSetService.study(id, principal.getName());
        List<FlashcardDTO> queue =
                unlearned
                        ? allCards.stream().filter(item -> !"MASTERED".equals(item.getState())).toList()
                        : allCards;
        int safeCard = queue.isEmpty() ? 0 : Math.max(0, Math.min(card, queue.size() - 1));
        model.addAttribute("cards", allCards);
        model.addAttribute("studyCards", queue);
        model.addAttribute("cardIndex", safeCard);
        model.addAttribute("currentCard", queue.isEmpty() ? null : queue.get(safeCard));
        model.addAttribute("previousCard", safeCard > 0 ? safeCard - 1 : null);
        model.addAttribute("nextCard", safeCard + 1 < queue.size() ? safeCard + 1 : null);
        model.addAttribute("unlearnedOnly", unlearned);
        model.addAttribute("reviewKey", UUID.randomUUID().toString());
        model.addAttribute("set", flashcardSetService.studySet(id, principal.getName()));
        return "student/flashcards/study";
    }

    @PostMapping("/student/flashcards/{setId}/cards/{cardId}/state")
    @ResponseBody
    public ResponseEntity<Void> mark(
            @PathVariable Integer setId,
            @PathVariable Integer cardId,
            @RequestParam(name = "state") String state,
            @RequestParam(name = "reviewKey") String reviewKey,
            Principal principal) {
        flashcardSetService.mark(setId, cardId, state, principal.getName(), reviewKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/student/flashcards/{setId}/cards/{cardId}/state/page")
    public String markAndContinue(
            @PathVariable Integer setId,
            @PathVariable Integer cardId,
            @RequestParam String state,
            @RequestParam String reviewKey,
            @RequestParam(defaultValue = "0") int nextCard,
            @RequestParam(defaultValue = "false") boolean unlearned,
            Principal principal) {
        flashcardSetService.mark(setId, cardId, state, principal.getName(), reviewKey);
        return "redirect:/student/flashcards/"
                + setId
                + "?card="
                + Math.max(0, nextCard)
                + "&unlearned="
                + unlearned;
    }
}







