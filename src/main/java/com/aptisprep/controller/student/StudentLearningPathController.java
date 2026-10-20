package com.aptisprep.controller.student;

import com.aptisprep.service.learning.LearningPathService;
import com.aptisprep.service.learning.LessonService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/student/learning-path")
public class StudentLearningPathController {
    private final LearningPathService service; private final LessonService lessons;
    public StudentLearningPathController(LearningPathService s,LessonService l){service=s;lessons=l;}
    @PostMapping("/refresh")
    public String refresh(Principal principal, RedirectAttributes redirect) {
        int changed = service.refreshPaths(principal.getName());
        redirect.addFlashAttribute("message", changed == 0 ? "Lộ trình đã cập nhật mới nhất."
                : "Đã cập nhật lộ trình. Các bài đã hoàn thành được giữ lại.");
        return "redirect:/student/learning-path";
    }
    @GetMapping public String paths(Principal principal,Model model){model.addAttribute("paths",service.ensureAndSummaries(principal.getName()));model.addAttribute("lessons",lessons.publishedLessons());model.addAttribute("levels",LessonService.LEVELS);return "student/learning/path";}
    @GetMapping("/{skill}")
    public String skill(@PathVariable String skill, Principal principal, Model model) {
        String selected = LessonService.SKILLS.stream().filter(value -> value.equalsIgnoreCase(skill))
                .findFirst().orElseThrow(() -> new com.aptisprep.exception.ResourceNotFoundException("Không tìm thấy kỹ năng"));
        model.addAttribute("selectedSkill", selected);
        model.addAttribute("paths", service.ensureAndSummaries(principal.getName()).stream()
                .filter(path -> path.skill().equals(selected)).toList());
        var published = lessons.publishedLessons().stream().filter(lesson -> lesson.getSkill().equals(selected)).toList();
        model.addAttribute("lessons", published);
        model.addAttribute("levels", LessonService.LEVELS);
        model.addAttribute("curriculum",LessonService.LEVELS.stream().map(level -> new LessonService.LevelBranch(level,
                published.stream().filter(lesson -> level.equals(lesson.getCefrLevel())).toList())).toList());
        return "student/learning/skill";
    }
    @PostMapping("/{pathId}/checkpoint/start")
    public String startCheckpoint(@PathVariable Integer pathId,Principal principal,RedirectAttributes redirect){try{var user=service.userId(principal.getName());Integer attemptId=service.startCheckpoint(user,pathId,java.util.UUID.randomUUID().toString());return "redirect:/student/attempts/"+attemptId+"/result";}catch(com.aptisprep.exception.BusinessRuleException|com.aptisprep.exception.ResourceNotFoundException e){redirect.addFlashAttribute("error",e.getMessage());return "redirect:/student/learning-path";}}
}
