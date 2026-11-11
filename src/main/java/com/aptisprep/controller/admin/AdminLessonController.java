package com.aptisprep.controller.admin;

import com.aptisprep.dto.LessonAuthorProfileDTO;
import com.aptisprep.dto.LessonDTO;
import com.aptisprep.dto.LessonEditorForm;
import com.aptisprep.enums.CefrLevel;
import com.aptisprep.enums.ContentStatus;
import com.aptisprep.enums.Skill;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.learning.FlashcardSetService;
import com.aptisprep.service.learning.LessonService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminLessonController {
    private final LessonService lessonService;
    private final FlashcardSetService flashcardService;

    public AdminLessonController(
            LessonService lessonService, FlashcardSetService flashcardService) {
        this.lessonService = lessonService;
        this.flashcardService = flashcardService;
    }

    @GetMapping("/admin/lessons")
    public String showLessonList(Model model) {
        model.addAttribute("tree", lessonService.getAdminTree());
        return "admin/lesson/list";
    }

    @GetMapping("/admin/lessons/{id}")
    public String getLessonDetail(@PathVariable Integer id, Model model) {
        model.addAttribute("lesson", lessonService.getLessonDtoById(id));
        model.addAttribute("flashcardSet", flashcardService.forLesson(id));
        model.addAttribute("questions", lessonService.getQuestionsByLessonId(id));
        model.addAttribute("stats", lessonService.getLessonStatistics(id));
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
        LessonEditorForm lessonForm = lessonService.createNewForm(skill, level);
        lessonForm.setTopic(topic);
        populateLessonFormModel(model, lessonForm, false, null);
        return "admin/lesson/form";
    }

    @PostMapping("/admin/lessons")
    public String createLesson(
            @Valid @ModelAttribute("form") LessonEditorForm lessonForm,
            BindingResult bindingResult,
            @RequestParam(required = false) MultipartFile audio,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) MultipartFile video,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                LessonDTO lesson =
                        lessonService.saveDto(
                                null, lessonForm, audio, false, image, false, video, false);
                redirectAttributes.addFlashAttribute("message", "Đã tạo bài học");
                return "redirect:/admin/lessons/" + lesson.getId();
            } catch (BusinessRuleException exception) {
                bindingResult.reject("lesson.invalid", exception.getMessage());
            }
        }
        populateLessonFormModel(model, lessonForm, false, null);
        return "admin/lesson/form";
    }

    @GetMapping("/admin/lessons/{id}/edit")
    public String showEditForm(@PathVariable Integer id, Model model) {
        populateLessonFormModel(model, lessonService.getLessonFormById(id), true, id);
        return "admin/lesson/form";
    }

    @PostMapping("/admin/lessons/{id}")
    public String updateLesson(
            @PathVariable Integer id,
            @Valid @ModelAttribute("form") LessonEditorForm lessonForm,
            BindingResult bindingResult,
            @RequestParam(required = false) MultipartFile audio,
            @RequestParam(defaultValue = "false") boolean removeAudio,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(defaultValue = "false") boolean removeImage,
            @RequestParam(required = false) MultipartFile video,
            @RequestParam(defaultValue = "false") boolean removeVideo,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                LessonDTO lesson =
                        lessonService.saveDto(
                                id,
                                lessonForm,
                                audio,
                                removeAudio,
                                image,
                                removeImage,
                                video,
                                removeVideo);
                redirectAttributes.addFlashAttribute(
                        "message", id == null ? "Đã tạo bài học" : "Đã cập nhật bài học");
                return "redirect:/admin/lessons/" + lesson.getId();
            } catch (BusinessRuleException exception) {
                bindingResult.reject("lesson.invalid", exception.getMessage());
            }
        }
        populateLessonFormModel(model, lessonForm, true, id);
        return "admin/lesson/form";
    }

    @PostMapping("/admin/lessons/{id}/archive")
    public String archiveLesson(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        lessonService.archiveLesson(id);
        redirectAttributes.addFlashAttribute(
                "message", "Đã lưu trữ bài học. Lịch sử học vẫn được giữ lại");
        return "redirect:/admin/lessons/" + id;
    }

    @PostMapping("/admin/lessons/{id}/duplicate")
    public String duplicateLesson(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        LessonDTO copy = lessonService.duplicateLessonDto(id);
        redirectAttributes.addFlashAttribute("message", "Đã tạo bản sao ở trạng thái Bản nháp");
        return "redirect:/admin/lessons/" + copy.getId() + "/edit";
    }

    @PostMapping("/admin/lessons/{id}/move")
    public String moveLesson(
            @PathVariable Integer id,
            @RequestParam int direction,
            RedirectAttributes redirectAttributes) {
        lessonService.moveLesson(id, direction);
        redirectAttributes.addFlashAttribute("message", "Đã thay đổi thứ tự bài học");
        return "redirect:/admin/lessons";
    }

    private void populateLessonFormModel(
            Model model, LessonEditorForm lessonForm, boolean editing, Integer id) {
        model.addAttribute("profile", LessonAuthorProfileDTO.forSkill(lessonForm.getSkill()));
        LinkedHashMap<String, LessonAuthorProfileDTO> authorProfiles = new LinkedHashMap<>();
        for (String skill : Skill.labels()) {
            authorProfiles.put(skill, LessonAuthorProfileDTO.forSkill(skill));
        }
        model.addAttribute("authorProfiles", authorProfiles);
        model.addAttribute("skills", Skill.labels());
        model.addAttribute("levels", CefrLevel.curriculumLevels());
        model.addAttribute("statuses", ContentStatus.codes());
        model.addAttribute("editing", editing);
        model.addAttribute("lessonId", id);
        model.addAttribute("lesson", id == null ? null : lessonService.getLessonDtoById(id));
        model.addAttribute("form", lessonForm);
    }
}
