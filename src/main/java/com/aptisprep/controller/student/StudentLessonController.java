package com.aptisprep.controller.student;

import com.aptisprep.service.learning.LessonService;
import com.aptisprep.service.learning.LessonTeachingService;
import java.security.Principal;
import java.text.Normalizer;
import com.aptisprep.entity.Lesson;
import java.util.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/student/lessons")
public class StudentLessonController {
    private final LessonService service; private final LessonTeachingService teaching; private final com.aptisprep.service.learning.FlashcardSetService flashcards;
    public StudentLessonController(LessonService s,LessonTeachingService t,com.aptisprep.service.learning.FlashcardSetService f){service=s;teaching=t;flashcards=f;}
    @GetMapping public String list(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String skill,
                                   @RequestParam(defaultValue="") String level,Model m){
        if(q.length()>255||skill.length()>80||level.length()>40) throw new com.aptisprep.exception.BusinessRuleException("Bộ lọc không hợp lệ");
        var query=fold(q); var items=service.publishedLessons().stream().filter(l->query.isBlank()||fold(l.getTitle()+" "+l.getDescription()+" "+topic(l)).contains(query)).filter(l->skill.isBlank()||l.getSkill().equals(skill)).filter(l->level.isBlank()||l.getCefrLevel().equals(level)).toList();
        m.addAttribute("lessonTree",tree(items));
        m.addAttribute("lessonSets",flashcards.lessonSets());
        m.addAttribute("lessons",items);m.addAttribute("q",q);m.addAttribute("selectedSkill",skill);m.addAttribute("selectedLevel",level);m.addAttribute("skills",LessonService.SKILLS);m.addAttribute("levels",LessonService.LEVELS);return "student/lesson/list";}
    @GetMapping("/{id}") public String detail(@PathVariable Integer id,Principal p,Model m){var l=service.requirePublished(id);service.recordOpen(p.getName(),id);m.addAttribute("lesson",l);m.addAttribute("flashcardSet",flashcards.publishedForLesson(id).orElse(null));m.addAttribute("questions",teaching.questions(id));m.addAttribute("savedResponse",teaching.response(id,p.getName()));m.addAttribute("savedWork",teaching.savedWork(id,p.getName()).orElse(null));m.addAttribute("videoId",LessonTeachingService.videoId(l.getMaterial()==null?null:l.getMaterial().getVideoUrl()));m.addAttribute("progress",service.progress(p.getName(),id).orElse(null));m.addAttribute("attempts",teaching.history(id,p.getName()));m.addAttribute("completion",teaching.completion(id,p.getName()));m.addAttribute("neighbors",service.neighbors(l));m.addAttribute("lastSection",teaching.lastSection(id,p.getName()));return "student/lesson/detail";}
    @PostMapping("/{id}/check") public String check(@PathVariable Integer id,@RequestParam java.util.Map<String,String> answers,Principal principal,RedirectAttributes redirect){redirect.addFlashAttribute("feedback",teaching.grade(id,answers,principal.getName()));var skill=service.requirePublished(id).getSkill();return "redirect:/student/lessons/"+id+(java.util.List.of("Writing","Speaking").contains(skill)?"#method":"#practice");}
    @PostMapping("/{id}/response") public String response(@PathVariable Integer id,@RequestParam String response,Principal principal,RedirectAttributes redirect){teaching.saveResponse(id,response,principal.getName());redirect.addFlashAttribute("message","Đã lưu bài thực hành của bạn.");return "redirect:/student/lessons/"+id;}
    @PostMapping("/{id}/assess") public String assess(@PathVariable Integer id,Principal principal,RedirectAttributes redirect){try{return "redirect:/student/assessments/"+teaching.assess(id,principal.getName());}catch(com.aptisprep.exception.BusinessRuleException e){redirect.addFlashAttribute("error",e.getMessage());return "redirect:/student/lessons/"+id;}}
    @PostMapping("/{id}/response/autosave") @ResponseBody public void autosave(@PathVariable Integer id,@RequestParam String response,Principal principal){teaching.saveResponse(id,response,principal.getName());}
    @PostMapping("/{id}/complete") public String complete(@PathVariable Integer id,Principal p,RedirectAttributes r){try{service.complete(p.getName(),id);r.addFlashAttribute("message","Đã ghi nhận hoàn thành bài học");}catch(com.aptisprep.exception.BusinessRuleException e){r.addFlashAttribute("error",e.getMessage());}return "redirect:/student/lessons/"+id;}
    @PostMapping("/{id}/audio-played") @ResponseBody public void audioPlayed(@PathVariable Integer id,Principal p){teaching.audioPlayed(id,p.getName());}
    @PostMapping("/{id}/section") @ResponseBody public void section(@PathVariable Integer id,@RequestParam String section,Principal p){teaching.section(id,section,p.getName());}
    @PostMapping("/{id}/recording") public String recording(@PathVariable Integer id,@RequestParam org.springframework.web.multipart.MultipartFile recording,Principal p,RedirectAttributes r){teaching.saveRecording(id,recording,p.getName());r.addFlashAttribute("message","Đã lưu bản ghi luyện nói");return "redirect:/student/lessons/"+id;}
    public record TopicBranch(String name,boolean independent,List<Lesson> lessons) {}
    public record LevelBranch(String level,int count,List<TopicBranch> topics) {}
    public record SkillBranch(String skill,int count,List<LevelBranch> levels) {}
    private static String topic(Lesson lesson) {
        var material=lesson.getMaterial();
        return material==null||material.getTopic()==null?"":material.getTopic().trim();
    }
    private static List<SkillBranch> tree(List<Lesson> lessons) {
        var result=new ArrayList<SkillBranch>();
        for(var skill:LessonService.SKILLS) {
            var skillLessons=lessons.stream().filter(l->skill.equals(l.getSkill())).toList();
            var levels=new ArrayList<LevelBranch>();
            for(var level:LessonService.LEVELS) {
                var levelLessons=skillLessons.stream().filter(l->level.equals(l.getCefrLevel())).toList();
                if(levelLessons.isEmpty()) continue;
                var groups=new LinkedHashMap<String,List<Lesson>>();
                for(var lesson:levelLessons) groups.computeIfAbsent(topic(lesson),key->new ArrayList<>()).add(lesson);
                var topics=groups.entrySet().stream().map(e->new TopicBranch(
                    e.getKey().isBlank()?"Bài học độc lập":e.getKey(),e.getKey().isBlank(),List.copyOf(e.getValue()))).toList();
                levels.add(new LevelBranch(level,levelLessons.size(),topics));
            }
            if(!levels.isEmpty()) result.add(new SkillBranch(skill,skillLessons.size(),levels));
        }
        return result;
    }
    private static String fold(String value){return Normalizer.normalize(value==null?"":value,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);}
}
