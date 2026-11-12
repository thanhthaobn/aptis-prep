package com.aptisprep.mvc.controller.student;

import com.aptisprep.mvc.dto.LessonDTO;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.service.learning.FlashcardSetService;
import com.aptisprep.mvc.service.learning.LessonListeningService;
import com.aptisprep.mvc.service.learning.LessonService;
import com.aptisprep.mvc.service.learning.LessonTeachingService;
import java.security.Principal;
import java.util.*;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StudentLessonController {
    private final LessonListeningService lessonListeningService;
    private final LessonService lessonService;
    private final LessonTeachingService lessonTeachingService;
    private final FlashcardSetService flashcardSetService;

    public StudentLessonController(
            LessonListeningService lessonListeningService,
            LessonService lessonService, LessonTeachingService lessonTeachingService, FlashcardSetService flashcardSetService) {
        this.lessonListeningService = lessonListeningService;
        this.lessonService = lessonService;
        this.lessonTeachingService = lessonTeachingService;
        this.flashcardSetService = flashcardSetService;
    }

    @GetMapping("/student/lessons")
    public String list(
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "skill", defaultValue = "") String skill,
            @RequestParam(name = "level", defaultValue = "") String level,
            Principal principal,
            Model model) {
        String queryParam = q == null ? "" : q;
        String skillParam = skill == null ? "" : skill;
        String levelParam = level == null ? "" : level;
        if (queryParam.length() > 255 || skillParam.length() > 80 || levelParam.length() > 40)
            throw new BusinessRuleException("Bộ lọc không hợp lệ");
        List<LessonDTO> items = lessonService.filterPublishedLessonDtos(queryParam, skillParam, levelParam);
        Map<String, List<LessonDTO>> lessonGroups = new LinkedHashMap<>();
        Map<Integer, Integer> lessonNumbers = new HashMap<>();
        for (int index = 0; index < items.size(); index++) {
            LessonDTO lesson = items.get(index);
            String topic =
                    lesson.getTopic() == null || lesson.getTopic().isBlank()
                            ? "Bài học độc lập"
                            : lesson.getTopic();
            lessonGroups.computeIfAbsent(topic, key -> new ArrayList<>()).add(lesson);
            lessonNumbers.put(lesson.getId(), index + 1);
        }
        model.addAttribute("lessons", items);
        model.addAttribute("lessonGroups", lessonGroups);
        model.addAttribute("lessonNumbers", lessonNumbers);
        model.addAttribute(
                "lessonStatuses",
                lessonService.lessonStatuses(
                        principal.getName(), items.stream().map(streamValue -> streamValue.getId()).toList()));
        model.addAttribute("lessonSets", flashcardSetService.lessonSets());
        model.addAttribute("lessons", items);
        model.addAttribute("q", q);
        model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedLevel", level);
        model.addAttribute("skills", lessonService.getSkills());
        model.addAttribute("levels", lessonService.getLevels());
        return "student/lesson/list";
    }

    @GetMapping("/student/lessons/{id}")
    public String detail(@PathVariable Integer id, Principal principal, Model model) {
        LessonDTO lesson = lessonService.requirePublishedView(id);
        lessonService.recordOpen(principal.getName(), id);
        model.addAttribute("release", lessonTeachingService.releaseState(id, principal.getName()));
        model.addAttribute("canAssess", lessonTeachingService.canAssess(id, principal.getName()));
        model.addAttribute("aiAvailable", lessonTeachingService.aiAvailable());
        model.addAttribute("lesson", lesson);
        model.addAttribute("flashcardSet", flashcardSetService.publishedForLessonView(id));
        model.addAttribute("questions", lessonTeachingService.questions(id));
        model.addAttribute("savedResponse", lessonTeachingService.response(id, principal.getName()));
        model.addAttribute("savedWork", lessonTeachingService.savedWork(id, principal.getName()));
        model.addAttribute(
                "videoId",
                lessonTeachingService.videoId(lesson.getVideoUrl()));
        model.addAttribute("progress", lessonService.progressView(principal.getName(), id));
        model.addAttribute("submissions", lessonTeachingService.submissionHistory(id, principal.getName()));
        model.addAttribute("aiSubmissions", lessonTeachingService.assessableSubmissions(id, principal.getName()));
        model.addAttribute("completion", lessonTeachingService.completion(id, principal.getName()));
        model.addAttribute("previousLesson", lessonService.previousLessonView(id));
        model.addAttribute("nextLesson", lessonService.nextLessonView(id));
        model.addAttribute("lastSection", lessonTeachingService.lastSection(id, principal.getName()));
        return "student/lesson/detail";
    }

    @PostMapping("/student/lessons/{id}/check")
    public String check(
            @PathVariable Integer id,
            @RequestParam Map<String, String> answers,
            Principal principal,
            RedirectAttributes redirect) {
        redirect.addFlashAttribute("feedback", lessonTeachingService.grade(id, answers, principal.getName()));
        String skill = lessonService.requirePublishedView(id).getSkill();
        return "redirect:/student/lessons/"
                + id
                + (List.of("Writing", "Speaking").contains(skill) ? "#method" : "#practice");
    }

    @PostMapping("/student/lessons/{id}/response")
    public String response(
            @PathVariable Integer id,
            @RequestParam(name = "response") String response,
            Principal principal,
            RedirectAttributes redirect) {
        try {
            lessonTeachingService.submitResponse(id, response, principal.getName());
            redirect.addFlashAttribute("message", "Đã lưu bài thực hành vào lịch sử.");
        } catch (BusinessRuleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/lessons/" + id;
    }

    @PostMapping("/student/lessons/{id}/assess")
    public String assess(
            @PathVariable Integer id,
            @RequestParam(name = "submissionId") Integer submissionId,
            Principal principal,
            RedirectAttributes redirect) {
        try {
            return "redirect:/student/assessments/"
                    + lessonTeachingService.assess(id, submissionId, principal.getName());
        } catch (BusinessRuleException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/student/lessons/" + id;
        }
    }

    @PostMapping("/student/lessons/{id}/response/autosave")
    @ResponseBody
    public void autosave(
            @PathVariable Integer id,
            @RequestParam(name = "response") String response,
            Principal principal) {
        lessonTeachingService.saveResponse(id, response, principal.getName());
    }

    @PostMapping("/student/lessons/{id}/complete")
    public String complete(@PathVariable Integer id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            lessonService.complete(principal.getName(), id);
            redirectAttributes.addFlashAttribute("message", "Đã ghi nhận hoàn thành bài học");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/lessons/" + id;
    }

    @PostMapping("/student/lessons/{id}/listening/start")
    @ResponseBody
    public LessonListeningService.Playback startListening(
            @PathVariable Integer id,
            @RequestParam(name = "position") double position,
            Principal principal) {
        return lessonListeningService.start(id, principal.getName(), position);
    }

    @PostMapping("/student/lessons/{id}/listening/heartbeat")
    @ResponseBody
    public LessonListeningService.Playback listeningHeartbeat(
            @PathVariable Integer id,
            @RequestParam(name = "token") String token,
            @RequestParam(name = "sequence") int sequence,
            @RequestParam(name = "position") double position,
            @RequestParam(name = "playing") boolean playing,
            @RequestParam(name = "discontinuity", defaultValue = "false") boolean discontinuity,
            Principal principal) {
        return lessonListeningService.heartbeat(
                id, principal.getName(), token, sequence, position, playing, discontinuity);
    }

    @PostMapping("/student/lessons/{id}/audio-played")
    public String audioPlayed(
            @PathVariable Integer id,
            @RequestParam(name = "duration", required = false) Double duration,
            @RequestParam(name = "listened", required = false) Double listened,
            Principal principal,
            RedirectAttributes redirect) {
        if (duration != null
                && listened != null
                && (!Double.isFinite(duration)
                        || duration <= 0
                        || !Double.isFinite(listened)
                        || listened < duration * .9)) {
            throw new BusinessRuleException("Cần nghe ít nhất 90% thời lượng audio");
        }
        lessonListeningService.confirmManualListening(id, principal.getName());
        redirect.addFlashAttribute(
                "message", "Đã ghi nhận bạn hoàn thành phần nghe. Transcript có thể được mở theo cấu hình bài học.");
        return "redirect:/student/lessons/" + id;
    }

    @PostMapping("/student/lessons/{id}/section")
    @ResponseBody
    public void section(
            @PathVariable Integer id, @RequestParam(name = "section") String section, Principal principal) {
        lessonTeachingService.section(id, section, principal.getName());
    }

    @PostMapping("/student/lessons/{id}/recording")
    public String recording(
            @PathVariable Integer id,
            @RequestParam(name = "recording") MultipartFile recording,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        lessonTeachingService.saveRecording(id, recording, principal.getName());
        redirectAttributes.addFlashAttribute("message", "Đã lưu bản ghi luyện nói");
        return "redirect:/student/lessons/" + id;
    }

}







