package com.aptisprep.controller.student;

import com.aptisprep.dto.form.FlashcardSetFormDTO;
import com.aptisprep.dto.view.FlashcardViewsDTO;
import com.aptisprep.dto.view.FlashcardViewsDTO.SetView;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.learning.FlashcardSetService;
import com.aptisprep.service.learning.LessonService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StudentFlashcardController {
    private final FlashcardSetService service;

    public StudentFlashcardController(FlashcardSetService s) {
        service = s;
    }

    @GetMapping("/student/flashcards")
    public String list(
            @RequestParam(defaultValue = "all") String source,
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "skill", defaultValue = "") String skill,
            @RequestParam(name = "level", defaultValue = "") String level,
            @RequestParam(defaultValue = "learning") String sort,
            Principal p,
            Model m) {
        if (!List.of("all", "mine", "lesson").contains(source))
            throw new BusinessRuleException("Bộ lọc bộ thẻ không hợp lệ");
        List<SetView> all = service.listForStudent(p.getName());
        if (q.length() > 255
                || (!skill.isBlank() && !LessonService.SKILLS.contains(skill))
                || (!level.isBlank() && !LessonService.LEVELS.contains(level))
                || !List.of("learning", "newest").contains(sort))
            throw new BusinessRuleException("Bộ lọc bộ thẻ không hợp lệ");
        String query = fold(q);
        Comparator<FlashcardViewsDTO.SetView> order =
                "newest".equals(sort)
                        ? Comparator.comparing(FlashcardViewsDTO.SetView::id).reversed()
                        : Comparator.comparingInt(
                                        (FlashcardViewsDTO.SetView set) ->
                                                set.personal()
                                                        ? LessonService.SKILLS.size()
                                                        : LessonService.SKILLS.indexOf(
                                                                set.lessonSkill()))
                                .thenComparingInt(set -> LessonService.LEVELS.indexOf(set.level()))
                                .thenComparing(set -> fold(set.title()))
                                .thenComparing(FlashcardViewsDTO.SetView::id);
        List<SetView> visible =
                all.stream()
                        .filter(
                                set ->
                                        "all".equals(source)
                                                || ("mine".equals(source)
                                                        ? set.personal()
                                                        : !set.personal()
                                                                && set.lessonId() != null))
                        .filter(set -> skill.isBlank() || skill.equals(set.lessonSkill()))
                        .filter(set -> level.isBlank() || level.equals(set.level()))
                        .filter(
                                set ->
                                        query.isBlank()
                                                || fold(set.title()
                                                                + " "
                                                                + Objects.toString(
                                                                        set.description(), "")
                                                                + " "
                                                                + Objects.toString(
                                                                        set.lessonTitle(), ""))
                                                        .contains(query))
                        .sorted(order)
                        .toList();
        m.addAttribute("sets", visible);
        m.addAttribute("q", q);
        m.addAttribute("selectedSkill", skill);
        m.addAttribute("selectedLevel", level);
        m.addAttribute("selectedSort", sort);
        m.addAttribute(
                "lessonResultCount", visible.stream().filter(set -> !set.personal()).count());
        m.addAttribute(
                "mineResultCount",
                visible.stream().filter(FlashcardViewsDTO.SetView::personal).count());
        m.addAttribute("lessonBranches", service.groupBySkillAndLevel(visible));
        m.addAttribute("skills", LessonService.SKILLS);
        m.addAttribute("levels", LessonService.LEVELS);
        m.addAttribute("totalCards", all.stream().mapToInt(FlashcardViewsDTO.SetView::count).sum());
        m.addAttribute(
                "visibleCards", visible.stream().mapToInt(FlashcardViewsDTO.SetView::count).sum());
        m.addAttribute("selectedSource", source);
        m.addAttribute("totalSets", all.size());
        m.addAttribute(
                "mineCount", all.stream().filter(FlashcardViewsDTO.SetView::personal).count());
        m.addAttribute(
                "lessonCount",
                all.stream().filter(set -> !set.personal() && set.lessonId() != null).count());
        return "student/flashcards/list";
    }

    @GetMapping("/student/flashcards/create")
    public String showCreateForm(Model m) {
        FlashcardSetFormDTO f = new FlashcardSetFormDTO();
        f.getRows().add(new FlashcardSetFormDTO.Row());
        m.addAttribute("form", f);
        m.addAttribute("setId", null);
        m.addAttribute("levels", LessonService.LEVELS);
        return "student/flashcards/form";
    }

    @GetMapping("/student/flashcards/{id}/edit")
    public String showEditForm(@PathVariable Integer id, Principal p, Model m) {
        FlashcardSetFormDTO f = service.personalForm(id, p.getName());
        m.addAttribute("form", f);
        m.addAttribute("setId", id);
        m.addAttribute("levels", LessonService.LEVELS);
        return "student/flashcards/form";
    }

    @PostMapping("/student/flashcards")
    public String createPersonalSet(
            @Valid @ModelAttribute("form") FlashcardSetFormDTO f,
            BindingResult b,
            Principal p,
            Model m) {
        if (!b.hasErrors())
            try {
                service.savePersonal(null, f, p.getName());
                return "redirect:/student/flashcards";
            } catch (BusinessRuleException e) {
                b.reject("invalid", e.getMessage());
            }
        m.addAttribute("form", f);
        m.addAttribute("setId", null);
        m.addAttribute("levels", LessonService.LEVELS);
        return "student/flashcards/form";
    }

    @PostMapping("/student/flashcards/{id}")
    public String updatePersonalSet(
            @PathVariable Integer id,
            @Valid @ModelAttribute("form") FlashcardSetFormDTO f,
            BindingResult b,
            Principal p,
            Model m) {
        if (!b.hasErrors())
            try {
                service.savePersonal(id, f, p.getName());
                return "redirect:/student/flashcards";
            } catch (BusinessRuleException e) {
                b.reject("invalid", e.getMessage());
            }
        m.addAttribute("form", f);
        m.addAttribute("setId", id);
        m.addAttribute("levels", LessonService.LEVELS);
        return "student/flashcards/form";
    }

    @PostMapping("/student/flashcards/{id}/archive")
    public String archive(@PathVariable Integer id, Principal p, RedirectAttributes r) {
        service.archivePersonal(id, p.getName());
        r.addFlashAttribute("message", "Đã lưu trữ bộ thẻ. Bạn có thể tạo lại khi cần.");
        return "redirect:/student/flashcards";
    }

    private static String fold(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd');
    }

    @GetMapping("/student/flashcards/{id}")
    public String study(@PathVariable Integer id, Principal p, Model m) {
        m.addAttribute("cards", service.study(id, p.getName()));
        m.addAttribute("set", service.studySet(id, p.getName()));
        return "student/flashcards/study";
    }

    @PostMapping("/student/flashcards/{setId}/cards/{cardId}/state")
    @ResponseBody
    public ResponseEntity<Void> mark(
            @PathVariable Integer setId,
            @PathVariable Integer cardId,
            @RequestParam(name = "state") String state,
            @RequestParam(name = "reviewKey") String reviewKey,
            Principal p) {
        service.mark(setId, cardId, state, p.getName(), reviewKey);
        return ResponseEntity.noContent().build();
    }
}
