package com.aptisprep.controller.admin;

import com.aptisprep.dto.form.LessonForm;
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
@RequestMapping("/admin/lessons")
public class AdminLessonController {
    private final LessonService service;
    private final FlashcardSetService flashcards;

    public AdminLessonController(LessonService s, FlashcardSetService f) {
        service = s;
        flashcards = f;
    }

    @GetMapping
    public String list(Model m) {
        m.addAttribute("tree", service.adminTree());
        return "admin/lesson/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model m) {
        m.addAttribute("lesson", service.require(id));
        m.addAttribute("flashcardSet", flashcards.forLesson(id).orElse(null));
        m.addAttribute("questions", service.questions(id));
        m.addAttribute("stats", service.stats(id));
        return "admin/lesson/detail";
    }

    @GetMapping("/create")
    public String create(
            @RequestParam(defaultValue = "Reading") String skill,
            @RequestParam(defaultValue = "A1") String level,
            @RequestParam(defaultValue = "") String topic,
            Model m) {
        if (topic.length() > 160) throw new BusinessRuleException("Tên chủ đề tối đa 160 ký tự");
        var form = service.newForm(skill, level);
        form.setTopic(topic);
        formModel(m, form, false, null);
        return "admin/lesson/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("form") LessonForm f,
            BindingResult b,
            @RequestParam(required = false) MultipartFile audio,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) MultipartFile video,
            Model m,
            RedirectAttributes r) {
        return save(null, f, b, audio, false, image, false, video, false, m, r);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Integer id, Model m) {
        formModel(m, service.form(id), true, id);
        return "admin/lesson/form";
    }

    @PostMapping("/{id}")
    public String edit(
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
                var lesson =
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

    @PostMapping("/{id}/archive")
    public String archive(@PathVariable Integer id, RedirectAttributes r) {
        service.archive(id);
        r.addFlashAttribute("message", "Đã lưu trữ bài học. Lịch sử học vẫn được giữ lại");
        return "redirect:/admin/lessons/" + id;
    }

    @PostMapping("/{id}/duplicate")
    public String duplicate(@PathVariable Integer id, RedirectAttributes r) {
        var copy = service.duplicate(id);
        r.addFlashAttribute("message", "Đã tạo bản sao ở trạng thái Bản nháp");
        return "redirect:/admin/lessons/" + copy.getId() + "/edit";
    }

    @PostMapping("/{id}/move")
    public String move(
            @PathVariable Integer id, @RequestParam int direction, RedirectAttributes r) {
        service.move(id, direction);
        r.addFlashAttribute("message", "Đã thay đổi thứ tự bài học");
        return "redirect:/admin/lessons";
    }

    private void formModel(Model m, LessonForm f, boolean editing, Integer id) {
        m.addAttribute("profile", LessonAuthorProfile.forSkill(f.getSkill()));
        var authorProfiles = new LinkedHashMap<String, LessonAuthorProfile>();
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
