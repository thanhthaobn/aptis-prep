package com.aptisprep.controller.admin;

import com.aptisprep.dto.form.LessonForm;
import com.aptisprep.entity.Lesson;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.learning.FlashcardSetService;
import com.aptisprep.service.learning.LessonAuthorProfile;
import com.aptisprep.service.learning.LessonService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminLessonController {
    private final LessonService service;
    private final FlashcardSetService flashcards;

    public AdminLessonController(LessonService s, FlashcardSetService f) {
        service = s;
        flashcards = f;
    }

    @GetMapping("/admin/lessons")
    public String list(Model m) {
        m.addAttribute("tree", service.adminTree(service.adminLessons()));
        return "admin/lesson/list";
    }

    @GetMapping("/admin/lessons/{id}")
    public String detail(@PathVariable Integer id, Model m) {
        m.addAttribute("lesson", service.require(id));
        m.addAttribute("flashcardSet", flashcards.forLesson(id));
        m.addAttribute("questions", service.questions(id));
        m.addAttribute("stats", service.stats(id));
        return "admin/lesson/detail";
    }

    @GetMapping("/admin/lessons/create")
    public String showCreateForm(
            @RequestParam(defaultValue = "Reading") String skill,
            @RequestParam(defaultValue = "A1") String level,
               @RequestParam(name = "topic", defaultValue = "") String topic,
            Model m) {
        topic = topic == null ? "" : topic;
        if (topic.length() > 160) throw new BusinessRuleException("Tên chủ đề tối đa 160 ký tự");
        LessonForm form = service.newForm(skill, level);
        form.setTopic(topic);
        formModel(m, form, false, null);
        return "admin/lesson/form";
    }

    @PostMapping("/admin/lessons")
    public String createLesson(
            @Valid @ModelAttribute("form") LessonForm f,
            BindingResult b,
            @RequestParam(required = false) MultipartFile audio,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) MultipartFile video,
            Model m,
            RedirectAttributes r) {
        return save(null, f, b, audio, false, image, false, video, false, m, r);
    }

    @GetMapping("/admin/lessons/{id}/edit")
    public String showEditForm(@PathVariable Integer id, Model m) {
        formModel(m, service.form(id), true, id);
        return "admin/lesson/form";
    }

    @PostMapping("/admin/lessons/{id}")
    public String updateLesson(
            @PathVariable Integer id,
            @Valid @ModelAttribute("form") LessonForm f,
            BindingResult b,
            @RequestParam(required = false) MultipartFile audio,
            @RequestParam(defaultValue = "false") boolean removeAudio,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(defaultValue = "false") boolean removeImage,
            @RequestParam(required = false) MultipartFile video,
            @RequestParam(defaultValue = "false") boolean removeVideo,
            Model m,
            RedirectAttributes r) {
        return save(id, f, b, audio, removeAudio, image, removeImage, video, removeVideo, m, r);
    }

    private String save(
            Integer id,
            LessonForm f,
            BindingResult b,
            MultipartFile audio,
            boolean removeAudio,
            MultipartFile image,
            boolean removeImage,
            MultipartFile video,
            boolean removeVideo,
            Model m,
            RedirectAttributes r) {
        if (!b.hasErrors()) {
            try {
                Lesson lesson =
                        service.save(
                                id, f, audio, removeAudio, image, removeImage, video, removeVideo);
                r.addFlashAttribute(
                        "message", id == null ? "Đã tạo bài học" : "Đã cập nhật bài học");
                return "redirect:/admin/lessons/" + lesson.getId();
            } catch (BusinessRuleException e) {
                b.reject("lesson.invalid", e.getMessage());
            }
        }
        formModel(m, f, id != null, id);
        return "admin/lesson/form";
    }

    @PostMapping("/admin/lessons/{id}/archive")
    public String archive(@PathVariable Integer id, RedirectAttributes r) {
        service.archive(id);
        r.addFlashAttribute("message", "Đã lưu trữ bài học. Lịch sử học vẫn được giữ lại");
        return "redirect:/admin/lessons/" + id;
    }

    @PostMapping("/admin/lessons/{id}/duplicate")
    public String duplicate(@PathVariable Integer id, RedirectAttributes r) {
        Lesson copy = service.duplicate(id);
        r.addFlashAttribute("message", "Đã tạo bản sao ở trạng thái Bản nháp");
        return "redirect:/admin/lessons/" + copy.getId() + "/edit";
    }

    @PostMapping("/admin/lessons/{id}/move")
    public String move(
            @PathVariable Integer id, @RequestParam int direction, RedirectAttributes r) {
        service.move(id, direction);
        r.addFlashAttribute("message", "Đã thay đổi thứ tự bài học");
        return "redirect:/admin/lessons";
    }

    private void formModel(Model m, LessonForm f, boolean editing, Integer id) {
        m.addAttribute("profile", LessonAuthorProfile.forSkill(f.getSkill()));
        LinkedHashMap<String, LessonAuthorProfile> authorProfiles =
                new LinkedHashMap<String, LessonAuthorProfile>();
        LessonService.SKILLS.forEach(
                skill -> authorProfiles.put(skill, LessonAuthorProfile.forSkill(skill)));
        m.addAttribute("authorProfiles", authorProfiles);
        m.addAttribute("form", f);
        m.addAttribute("editing", editing);
        m.addAttribute("lessonId", id);
        m.addAttribute("lesson", id == null ? null : service.require(id));
        m.addAttribute("skills", LessonService.SKILLS);
        m.addAttribute("levels", LessonService.LEVELS);
        m.addAttribute("statuses", LessonService.STATUSES);
    }
}
