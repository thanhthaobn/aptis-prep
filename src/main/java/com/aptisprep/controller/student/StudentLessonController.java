package com.aptisprep.controller.student;

import com.aptisprep.service.learning.LessonService;
import com.aptisprep.service.learning.LessonTeachingService;
import java.security.Principal;
import java.text.Normalizer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/student/lessons")
public class StudentLessonController {
    private final LessonService service; private final LessonTeachingService teaching;
    public StudentLessonController(LessonService s,LessonTeachingService t){service=s;teaching=t;}
    @GetMapping public String list(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String skill,
                                   @RequestParam(defaultValue="") String level,Model m){
        if(q.length()>255||skill.length()>80||level.length()>40) throw new com.aptisprep.exception.BusinessRuleException("Bộ lọc không hợp lệ");
        var query=fold(q); var items=service.publishedLessons().stream().filter(l->query.isBlank()||fold(l.getTitle()+" "+l.getDescription()).contains(query)).filter(l->skill.isBlank()||l.getSkill().equals(skill)).filter(l->level.isBlank()||l.getCefrLevel().equals(level)).toList();
        m.addAttribute("lessons",items);m.addAttribute("q",q);m.addAttribute("selectedSkill",skill);m.addAttribute("selectedLevel",level);m.addAttribute("skills",LessonService.SKILLS);m.addAttribute("levels",LessonService.LEVELS);return "student/lesson/list";}
    @GetMapping("/{id}") public String detail(@PathVariable Integer id,Principal p,Model m){var l=service.requirePublished(id);service.recordOpen(p.getName(),id);m.addAttribute("lesson",l);m.addAttribute("questions",teaching.questions(id));m.addAttribute("savedResponse",teaching.response(id,p.getName()));m.addAttribute("savedWork",teaching.savedWork(id,p.getName()).orElse(null));m.addAttribute("videoId",LessonTeachingService.videoId(l.getMaterial()==null?null:l.getMaterial().getVideoUrl()));m.addAttribute("progress",service.progress(p.getName(),id).orElse(null));m.addAttribute("attempts",teaching.history(id,p.getName()));m.addAttribute("completion",teaching.completion(id,p.getName()));m.addAttribute("neighbors",service.neighbors(l));m.addAttribute("lastSection",teaching.lastSection(id,p.getName()));return "student/lesson/detail";}
    @PostMapping("/{id}/check") public String check(@PathVariable Integer id,@RequestParam java.util.Map<String,String> answers,Principal principal,RedirectAttributes redirect){redirect.addFlashAttribute("feedback",teaching.grade(id,answers,principal.getName()));return "redirect:/student/lessons/"+id+"#practice";}
    @PostMapping("/{id}/response") public String response(@PathVariable Integer id,@RequestParam String response,Principal principal,RedirectAttributes redirect){teaching.saveResponse(id,response,principal.getName());redirect.addFlashAttribute("message","Đã lưu bài thực hành của bạn.");return "redirect:/student/lessons/"+id;}
    @PostMapping("/{id}/assess") public String assess(@PathVariable Integer id,Principal principal,RedirectAttributes redirect){try{return "redirect:/student/assessments/"+teaching.assess(id,principal.getName());}catch(com.aptisprep.exception.BusinessRuleException e){redirect.addFlashAttribute("error",e.getMessage());return "redirect:/student/lessons/"+id;}}
    @PostMapping("/{id}/response/autosave") @ResponseBody public void autosave(@PathVariable Integer id,@RequestParam String response,Principal principal){teaching.saveResponse(id,response,principal.getName());}
    @PostMapping("/{id}/complete") public String complete(@PathVariable Integer id,Principal p,RedirectAttributes r){try{service.complete(p.getName(),id);r.addFlashAttribute("message","Đã ghi nhận hoàn thành bài học");}catch(com.aptisprep.exception.BusinessRuleException e){r.addFlashAttribute("error",e.getMessage());}return "redirect:/student/lessons/"+id;}
    @PostMapping("/{id}/audio-played") @ResponseBody public void audioPlayed(@PathVariable Integer id,Principal p){teaching.audioPlayed(id,p.getName());}
    @PostMapping("/{id}/section") @ResponseBody public void section(@PathVariable Integer id,@RequestParam String section,Principal p){teaching.section(id,section,p.getName());}
    @PostMapping("/{id}/recording") public String recording(@PathVariable Integer id,@RequestParam org.springframework.web.multipart.MultipartFile recording,Principal p,RedirectAttributes r){teaching.saveRecording(id,recording,p.getName());r.addFlashAttribute("message","Đã lưu bản ghi luyện nói");return "redirect:/student/lessons/"+id;}
    @PostMapping("/{lessonId}/flashcards/{cardId}/review") public String review(@PathVariable Integer lessonId,@PathVariable Integer cardId,Principal p,RedirectAttributes r){service.review(p.getName(),lessonId,cardId);r.addFlashAttribute("message","Đã ghi nhận ôn Flashcard");return "redirect:/student/lessons/"+lessonId;}
    private static String fold(String value){return Normalizer.normalize(value==null?"":value,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase();}
}
