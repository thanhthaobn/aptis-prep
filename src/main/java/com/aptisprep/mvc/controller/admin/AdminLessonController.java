package com.aptisprep.mvc.controller.admin;

import com.aptisprep.mvc.dto.LessonCheckDTO;
import com.aptisprep.mvc.dto.LessonDTO;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.service.learning.FlashcardSetService;
import com.aptisprep.mvc.service.learning.LessonService;
import com.aptisprep.mvc.support.learning.*;
import com.aptisprep.mvc.support.learning.LessonAuthorProfile;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminLessonController {
    private final LessonService lessonService;
    private final FlashcardSetService flashcardSetService;

    public AdminLessonController(
            LessonService lessonService, FlashcardSetService flashcardSetService) {
        this.lessonService = lessonService;
        this.flashcardSetService = flashcardSetService;
    }

    @GetMapping("/admin/lessons")
    public String list(
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "skill", defaultValue = "") String skill,
            @RequestParam(name = "level", defaultValue = "") String level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        var pageData = lessonService.searchAdminLessonDtos(q, skill, level, page, size);
        model.addAttribute("pageData", pageData);
        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("selectedSkill", skill == null ? "" : skill.trim());
        model.addAttribute("selectedLevel", level == null ? "" : level.trim());
        model.addAttribute("skills", lessonService.getSkills());
        model.addAttribute("levels", lessonService.getLevels());
        return "admin/lesson/list";
    }

    @GetMapping("/admin/lessons/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("lesson", lessonService.requireView(id));
        model.addAttribute("flashcardSet", flashcardSetService.forLessonView(id));
        model.addAttribute("questions", lessonService.questions(id));
        model.addAttribute("stats", lessonService.stats(id));
        return "admin/lesson/detail";
    }

    @GetMapping("/admin/lessons/create")
    public String showCreateForm(
            @RequestParam(defaultValue = "Reading") String skill,
            @RequestParam(defaultValue = "A1") String level,
            @RequestParam(name = "topic", defaultValue = "") String topic,
            Model model) {
        topic = topic == null ? "" : topic;
        if (topic.length() > 160) throw new BusinessRuleException("Tên chủ đề tối đa 160 ký tự");
        LessonDTO form = lessonService.newForm(skill, level);
        form.setTopic(topic);
        model.addAttribute("profile", LessonAuthorProfile.forSkill(form.getSkill()));
        LinkedHashMap<String, LessonAuthorProfile> authorProfiles = new LinkedHashMap<>();
        lessonService
                .getSkills()
                .forEach(item -> authorProfiles.put(item, LessonAuthorProfile.forSkill(item)));
        model.addAttribute("authorProfiles", authorProfiles);
        model.addAttribute("form", form);
        model.addAttribute("editing", false);
        model.addAttribute("lessonId", null);
        model.addAttribute("lesson", null);
        model.addAttribute("skills", lessonService.getSkills());
        model.addAttribute("levels", lessonService.getLevels());
        model.addAttribute("statuses", lessonService.getStatuses());
        return "admin/lesson/form";
    }

    @PostMapping("/admin/lessons")
    public String createLesson(
            @Valid @ModelAttribute("form") LessonDTO lessonForm,
            BindingResult bindingResult,
            @RequestParam(required = false) MultipartFile audio,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) MultipartFile video,
            @RequestParam(name = "formAction", defaultValue = "SAVE") String formAction,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!"SAVE".equals(formAction)) {
            changeForm(lessonForm, formAction);
            model.addAttribute("showValidationErrors", false);
            populateFormModel(model, lessonForm, false, null, null);
            return "admin/lesson/form";
        }
        if (!bindingResult.hasErrors()) {
            try {
                LessonDTO lesson =
                        lessonService.saveView(
                                null, lessonForm, audio, false, image, false, video, false);
                redirectAttributes.addFlashAttribute("message", "Đã tạo bài học");
                return "redirect:/admin/lessons/" + lesson.getId();
            } catch (BusinessRuleException exception) {
                bindingResult.reject("lesson.invalid", exception.getMessage());
            }
        }
        model.addAttribute("profile", LessonAuthorProfile.forSkill(lessonForm.getSkill()));
        LinkedHashMap<String, LessonAuthorProfile> authorProfiles = new LinkedHashMap<>();
        lessonService
                .getSkills()
                .forEach(item -> authorProfiles.put(item, LessonAuthorProfile.forSkill(item)));
        model.addAttribute("authorProfiles", authorProfiles);
        model.addAttribute("form", lessonForm);
        model.addAttribute("editing", false);
        model.addAttribute("lessonId", null);
        model.addAttribute("lesson", null);
        model.addAttribute("skills", lessonService.getSkills());
        model.addAttribute("levels", lessonService.getLevels());
        model.addAttribute("statuses", lessonService.getStatuses());
        return "admin/lesson/form";
    }

    @GetMapping("/admin/lessons/{id}/edit")
    public String showEditForm(@PathVariable Integer id, Model model) {
        LessonDTO form = lessonService.form(id);
        model.addAttribute("profile", LessonAuthorProfile.forSkill(form.getSkill()));
        LinkedHashMap<String, LessonAuthorProfile> authorProfiles = new LinkedHashMap<>();
        lessonService
                .getSkills()
                .forEach(item -> authorProfiles.put(item, LessonAuthorProfile.forSkill(item)));
        model.addAttribute("authorProfiles", authorProfiles);
        model.addAttribute("form", form);
        model.addAttribute("editing", true);
        model.addAttribute("lessonId", id);
        model.addAttribute("lesson", lessonService.requireView(id));
        model.addAttribute("skills", lessonService.getSkills());
        model.addAttribute("levels", lessonService.getLevels());
        model.addAttribute("statuses", lessonService.getStatuses());
        return "admin/lesson/form";
    }

    @PostMapping("/admin/lessons/{id}")
    public String updateLesson(
            @PathVariable Integer id,
            @Valid @ModelAttribute("form") LessonDTO lessonForm,
            BindingResult bindingResult,
            @RequestParam(required = false) MultipartFile audio,
            @RequestParam(defaultValue = "false") boolean removeAudio,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(defaultValue = "false") boolean removeImage,
            @RequestParam(required = false) MultipartFile video,
            @RequestParam(defaultValue = "false") boolean removeVideo,
            @RequestParam(name = "formAction", defaultValue = "SAVE") String formAction,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!"SAVE".equals(formAction)) {
            changeForm(lessonForm, formAction);
            model.addAttribute("showValidationErrors", false);
            populateFormModel(model, lessonForm, true, id, lessonService.requireView(id));
            return "admin/lesson/form";
        }
        if (!bindingResult.hasErrors()) {
            try {
                LessonDTO lesson =
                        lessonService.saveView(
                                id,
                                lessonForm,
                                audio,
                                removeAudio,
                                image,
                                removeImage,
                                video,
                                removeVideo);
                redirectAttributes.addFlashAttribute("message", "Đã cập nhật bài học");
                return "redirect:/admin/lessons/" + lesson.getId();
            } catch (BusinessRuleException exception) {
                bindingResult.reject("lesson.invalid", exception.getMessage());
            }
        }
        model.addAttribute("profile", LessonAuthorProfile.forSkill(lessonForm.getSkill()));
        LinkedHashMap<String, LessonAuthorProfile> authorProfiles = new LinkedHashMap<>();
        lessonService
                .getSkills()
                .forEach(item -> authorProfiles.put(item, LessonAuthorProfile.forSkill(item)));
        model.addAttribute("authorProfiles", authorProfiles);
        model.addAttribute("form", lessonForm);
        model.addAttribute("editing", true);
        model.addAttribute("lessonId", id);
        model.addAttribute("lesson", lessonService.requireView(id));
        model.addAttribute("skills", lessonService.getSkills());
        model.addAttribute("levels", lessonService.getLevels());
        model.addAttribute("statuses", lessonService.getStatuses());
        return "admin/lesson/form";
    }

    private void changeForm(LessonDTO form, String action) {
        form.setChecks(
                form.getChecks() == null ? new ArrayList<>() : new ArrayList<>(form.getChecks()));
        if ("ADD_CHECK".equals(action)) {
            LessonCheckDTO check = new LessonCheckDTO();
            check.setPosition(form.getChecks().size() + 1);
            check.setCorrectOption(0);
            form.getChecks().add(check);
        } else if (action.startsWith("REMOVE_CHECK_")) {
            try {
                int index = Integer.parseInt(action.substring("REMOVE_CHECK_".length()));
                if (index >= 0 && index < form.getChecks().size()) form.getChecks().remove(index);
            } catch (NumberFormatException ignored) {
                // Render the unchanged form for a malformed UI action.
            }
        } else if (action.startsWith("REMOVE_OPTION_")) {
            String[] parts = action.substring("REMOVE_OPTION_".length()).split("_", 2);
            if (parts.length == 2) {
                try {
                    LessonCheckDTO check = form.getChecks().get(Integer.parseInt(parts[0]));
                    switch (parts[1]) {
                        case "C" -> check.setOptionC(null);
                        case "D" -> check.setOptionD(null);
                        case "E" -> check.setOptionE(null);
                        case "F" -> check.setOptionF(null);
                        default -> {}
                    }
                } catch (RuntimeException ignored) {
                    // Render the unchanged form for a malformed UI action.
                }
            }
        }
        for (int index = 0; index < form.getChecks().size(); index++) {
            form.getChecks().get(index).setPosition(index + 1);
        }
    }

    private void populateFormModel(
            Model model, LessonDTO form, boolean editing, Integer lessonId, LessonDTO lesson) {
        model.addAttribute("profile", LessonAuthorProfile.forSkill(form.getSkill()));
        LinkedHashMap<String, LessonAuthorProfile> authorProfiles = new LinkedHashMap<>();
        lessonService
                .getSkills()
                .forEach(item -> authorProfiles.put(item, LessonAuthorProfile.forSkill(item)));
        model.addAttribute("authorProfiles", authorProfiles);
        model.addAttribute("form", form);
        model.addAttribute("editing", editing);
        model.addAttribute("lessonId", lessonId);
        model.addAttribute("lesson", lesson);
        model.addAttribute("skills", lessonService.getSkills());
        model.addAttribute("levels", lessonService.getLevels());
        model.addAttribute("statuses", lessonService.getStatuses());
    }

    @PostMapping("/admin/lessons/{id}/archive")
    public String archive(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        lessonService.archive(id);
        redirectAttributes.addFlashAttribute(
                "message", "Đã lưu trữ bài học. Lịch sử học vẫn được giữ lại");
        return "redirect:/admin/lessons/" + id;
    }

    @PostMapping("/admin/lessons/{id}/duplicate")
    public String duplicate(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        LessonDTO copy = lessonService.duplicateView(id);
        redirectAttributes.addFlashAttribute("message", "Đã tạo bản sao ở trạng thái Bản nháp");
        return "redirect:/admin/lessons/" + copy.getId() + "/edit";
    }

    @PostMapping("/admin/lessons/{id}/move")
    public String move(
            @PathVariable Integer id,
            @RequestParam int direction,
            RedirectAttributes redirectAttributes) {
        lessonService.move(id, direction);
        redirectAttributes.addFlashAttribute("message", "Đã thay đổi thứ tự bài học");
        return "redirect:/admin/lessons";
    }
}
