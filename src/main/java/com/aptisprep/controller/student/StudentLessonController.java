package com.aptisprep.controller.student;

import com.aptisprep.entity.Lesson;
import com.aptisprep.entity.LessonMaterial;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.learning.FlashcardSetService;
import com.aptisprep.service.learning.LessonListeningService;
import com.aptisprep.service.learning.LessonService;
import com.aptisprep.service.learning.LessonTeachingService;
import java.security.Principal;
import java.text.Normalizer;
import java.util.*;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StudentLessonController {
    @Autowired private LessonListeningService listening;

    private final LessonService service;
    private final LessonTeachingService teaching;
    private final FlashcardSetService flashcards;

    public StudentLessonController(
            LessonService s, LessonTeachingService t, FlashcardSetService f) {
        service = s;
        teaching = t;
        flashcards = f;
    }

    @GetMapping("/student/lessons")
    public String list(
            @RequestParam(name = "q", defaultValue = "") String q,
            @RequestParam(name = "skill", defaultValue = "") String skill,
            @RequestParam(name = "level", defaultValue = "") String level,
            Principal principal,
            Model m) {
        String queryParam = q == null ? "" : q;
        String skillParam = skill == null ? "" : skill;
        String levelParam = level == null ? "" : level;
        if (queryParam.length() > 255 || skillParam.length() > 80 || levelParam.length() > 40)
            throw new BusinessRuleException("Bộ lọc không hợp lệ");
        String query = fold(queryParam);
        List<Lesson> items =
                service.publishedLessons().stream()
                        .filter(
                                l ->
                                        query.isBlank()
                                                || fold(l.getTitle()
                                                                + " "
                                                                + l.getDescription()
                                                                + " "
                                                                + topic(l))
                                                        .contains(query))
                        .filter(l -> skillParam.isBlank() || l.getSkill().equals(skillParam))
                        .filter(l -> levelParam.isBlank() || l.getCefrLevel().equals(levelParam))
                        .toList();
        m.addAttribute("lessonTree", tree(items));
        m.addAttribute(
                "lessonStatuses",
                service.lessonStatuses(
                        principal.getName(), items.stream().map(Lesson::getId).toList()));
        m.addAttribute("lessonSets", flashcards.lessonSets());
        m.addAttribute("lessons", items);
        m.addAttribute("q", q);
        m.addAttribute("selectedSkill", skill);
        m.addAttribute("selectedLevel", level);
        m.addAttribute("skills", LessonService.SKILLS);
        m.addAttribute("levels", LessonService.LEVELS);
        return "student/lesson/list";
    }

    @GetMapping("/student/lessons/{id}")
    public String detail(@PathVariable Integer id, Principal p, Model m) {
        Lesson l = service.requirePublished(id);
        service.recordOpen(p.getName(), id);
        m.addAttribute("release", teaching.releaseState(id, p.getName()));
        m.addAttribute("canAssess", teaching.canAssess(id, p.getName()));
        m.addAttribute("aiAvailable", teaching.aiAvailable());
        m.addAttribute("lesson", l);
        m.addAttribute("flashcardSet", flashcards.publishedForLesson(id));
        m.addAttribute("questions", teaching.questions(id));
        m.addAttribute("savedResponse", teaching.response(id, p.getName()));
        m.addAttribute("savedWork", teaching.savedWork(id, p.getName()));
        m.addAttribute(
                "videoId",
                LessonTeachingService.videoId(
                        l.getMaterial() == null ? null : l.getMaterial().getVideoUrl()));
        m.addAttribute("progress", service.progress(p.getName(), id));
        m.addAttribute("attempts", teaching.history(id, p.getName()));
        m.addAttribute("submissions", teaching.submissionHistory(id, p.getName()));
        m.addAttribute("aiSubmissions", teaching.assessableSubmissions(id, p.getName()));
        m.addAttribute("completion", teaching.completion(id, p.getName()));
        m.addAttribute("neighbors", service.neighbors(l));
        m.addAttribute("lastSection", teaching.lastSection(id, p.getName()));
        return "student/lesson/detail";
    }

    @PostMapping("/student/lessons/{id}/check")
    public String check(
            @PathVariable Integer id,
            @RequestParam Map<String, String> answers,
            Principal principal,
            RedirectAttributes redirect) {
        redirect.addFlashAttribute("feedback", teaching.grade(id, answers, principal.getName()));
        String skill = service.requirePublished(id).getSkill();
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
            teaching.submitResponse(id, response, principal.getName());
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
                    + teaching.assess(id, submissionId, principal.getName());
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
        teaching.saveResponse(id, response, principal.getName());
    }

    @PostMapping("/student/lessons/{id}/complete")
    public String complete(@PathVariable Integer id, Principal p, RedirectAttributes r) {
        try {
            service.complete(p.getName(), id);
            r.addFlashAttribute("message", "Đã ghi nhận hoàn thành bài học");
        } catch (BusinessRuleException e) {
            r.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/lessons/" + id;
    }

    @PostMapping("/student/lessons/{id}/listening/start")
    @ResponseBody
    public LessonListeningService.Playback startListening(
            @PathVariable Integer id,
            @RequestParam(name = "position") double position,
            Principal principal) {
        return listening.start(id, principal.getName(), position);
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
        return listening.heartbeat(
                id, principal.getName(), token, sequence, position, playing, discontinuity);
    }

    @PostMapping("/student/lessons/{id}/audio-played")
    @ResponseBody
    public void audioPlayed(@PathVariable Integer id) {
        throw new BusinessRuleException(
                "Hãy phát audio và nghe đủ 90% qua phiên nghe được xác thực");
    }

    @PostMapping("/student/lessons/{id}/section")
    @ResponseBody
    public void section(
            @PathVariable Integer id, @RequestParam(name = "section") String section, Principal p) {
        teaching.section(id, section, p.getName());
    }

    @PostMapping("/student/lessons/{id}/recording")
    public String recording(
            @PathVariable Integer id,
            @RequestParam(name = "recording") MultipartFile recording,
            Principal p,
            RedirectAttributes r) {
        teaching.saveRecording(id, recording, p.getName());
        r.addFlashAttribute("message", "Đã lưu bản ghi luyện nói");
        return "redirect:/student/lessons/" + id;
    }

    public record TopicBranch(String name, boolean independent, List<Lesson> lessons) {}

    public record LevelBranch(String level, int count, List<TopicBranch> topics) {}

    public record SkillBranch(String skill, int count, List<LevelBranch> levels) {}

    private static String topic(Lesson lesson) {
        LessonMaterial material = lesson.getMaterial();
        return material == null || material.getTopic() == null ? "" : material.getTopic().trim();
    }

    private static List<SkillBranch> tree(List<Lesson> lessons) {
        ArrayList<SkillBranch> result = new ArrayList<SkillBranch>();
        for (String skill : LessonService.SKILLS) {
            List<Lesson> skillLessons =
                    lessons.stream().filter(l -> skill.equals(l.getSkill())).toList();
            ArrayList<LevelBranch> levels = new ArrayList<LevelBranch>();
            for (String level : LessonService.LEVELS) {
                List<Lesson> levelLessons =
                        skillLessons.stream().filter(l -> level.equals(l.getCefrLevel())).toList();
                if (levelLessons.isEmpty()) continue;
                LinkedHashMap<String, List<Lesson>> groups =
                        new LinkedHashMap<String, List<Lesson>>();
                for (Lesson lesson : levelLessons)
                    groups.computeIfAbsent(topic(lesson), key -> new ArrayList<>()).add(lesson);
                List<TopicBranch> topics =
                        groups.entrySet().stream()
                                .map(
                                        e ->
                                                new TopicBranch(
                                                        e.getKey().isBlank()
                                                                ? "Bài học độc lập"
                                                                : e.getKey(),
                                                        e.getKey().isBlank(),
                                                        List.copyOf(e.getValue())))
                                .toList();
                levels.add(new LevelBranch(level, levelLessons.size(), topics));
            }
            if (!levels.isEmpty()) result.add(new SkillBranch(skill, skillLessons.size(), levels));
        }
        return result;
    }

    private static String fold(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
