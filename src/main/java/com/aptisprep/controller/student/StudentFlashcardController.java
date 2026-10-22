package com.aptisprep.controller.student;
import com.aptisprep.service.learning.FlashcardSetService;
import com.aptisprep.service.learning.LessonService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@Controller @RequestMapping("/student/flashcards")
public class StudentFlashcardController {
 private final FlashcardSetService service;public StudentFlashcardController(FlashcardSetService s){service=s;}
 @GetMapping public String list(@RequestParam(defaultValue="all") String source,@RequestParam(defaultValue="") String q,
   @RequestParam(defaultValue="") String skill,@RequestParam(defaultValue="") String level,@RequestParam(defaultValue="learning") String sort,Principal p,Model m){
  if(!java.util.List.of("all","mine","lesson").contains(source))throw new com.aptisprep.exception.BusinessRuleException("Bộ lọc bộ thẻ không hợp lệ");
  var all=service.listForStudent(p.getName());
  if(q.length()>255||(!skill.isBlank()&&!LessonService.SKILLS.contains(skill))||(!level.isBlank()&&!LessonService.LEVELS.contains(level))||!java.util.List.of("learning","newest").contains(sort))throw new com.aptisprep.exception.BusinessRuleException("Bộ lọc bộ thẻ không hợp lệ");
  var query=fold(q);
  java.util.Comparator<FlashcardSetService.SetView> order="newest".equals(sort)
    ?java.util.Comparator.comparing(FlashcardSetService.SetView::id).reversed()
    :java.util.Comparator.comparingInt((FlashcardSetService.SetView set)->set.personal()?LessonService.SKILLS.size():LessonService.SKILLS.indexOf(set.lessonSkill()))
      .thenComparingInt(set->LessonService.LEVELS.indexOf(set.level())).thenComparing(set->fold(set.title())).thenComparing(FlashcardSetService.SetView::id);
  var visible=all.stream().filter(set->"all".equals(source)||("mine".equals(source)?set.personal():!set.personal()&&set.lessonId()!=null))
    .filter(set->skill.isBlank()||skill.equals(set.lessonSkill())).filter(set->level.isBlank()||level.equals(set.level()))
    .filter(set->query.isBlank()||fold(set.title()+" "+java.util.Objects.toString(set.description(),"")+" "+java.util.Objects.toString(set.lessonTitle(),"")).contains(query)).sorted(order).toList();
  m.addAttribute("sets",visible);m.addAttribute("q",q);m.addAttribute("selectedSkill",skill);m.addAttribute("selectedLevel",level);m.addAttribute("selectedSort",sort);
  m.addAttribute("lessonResultCount",visible.stream().filter(set->!set.personal()).count());
  m.addAttribute("mineResultCount",visible.stream().filter(FlashcardSetService.SetView::personal).count());
  m.addAttribute("lessonBranches",FlashcardSetService.groupBySkillAndLevel(visible));
  m.addAttribute("skills",LessonService.SKILLS);m.addAttribute("levels",LessonService.LEVELS);
  m.addAttribute("totalCards",all.stream().mapToInt(FlashcardSetService.SetView::count).sum());m.addAttribute("visibleCards",visible.stream().mapToInt(FlashcardSetService.SetView::count).sum());
  m.addAttribute("selectedSource",source);m.addAttribute("totalSets",all.size());
  m.addAttribute("mineCount",all.stream().filter(FlashcardSetService.SetView::personal).count());
  m.addAttribute("lessonCount",all.stream().filter(set->!set.personal()&&set.lessonId()!=null).count());
  m.addAttribute("admin",false);return "flashcard/list";
 }
 @GetMapping("/create") public String create(Model m){var f=new com.aptisprep.dto.form.FlashcardSetForm();f.getRows().add(new com.aptisprep.dto.form.FlashcardSetForm.Row());return form(m,f,null);}
 @GetMapping("/{id}/edit") public String edit(@PathVariable Integer id,Principal p,Model m){return form(m,service.personalForm(id,p.getName()),id);}
 @PostMapping public String create(@jakarta.validation.Valid @ModelAttribute("form") com.aptisprep.dto.form.FlashcardSetForm f,org.springframework.validation.BindingResult b,Principal p,Model m){return save(null,f,b,p,m);}
 @PostMapping("/{id}") public String edit(@PathVariable Integer id,@jakarta.validation.Valid @ModelAttribute("form") com.aptisprep.dto.form.FlashcardSetForm f,org.springframework.validation.BindingResult b,Principal p,Model m){return save(id,f,b,p,m);}
 @PostMapping("/{id}/archive") public String archive(@PathVariable Integer id,Principal p,org.springframework.web.servlet.mvc.support.RedirectAttributes r){service.archivePersonal(id,p.getName());r.addFlashAttribute("message","Đã lưu trữ bộ thẻ. Bạn có thể tạo lại khi cần.");return "redirect:/student/flashcards";}
 private static String fold(String value){return java.text.Normalizer.normalize(value,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(java.util.Locale.ROOT).replace('đ','d');}
 private String save(Integer id,com.aptisprep.dto.form.FlashcardSetForm f,org.springframework.validation.BindingResult b,Principal p,Model m){if(!b.hasErrors())try{service.savePersonal(id,f,p.getName());return "redirect:/student/flashcards";}catch(com.aptisprep.exception.BusinessRuleException e){b.reject("invalid",e.getMessage());}return form(m,f,id);}
 private String form(Model m,com.aptisprep.dto.form.FlashcardSetForm f,Integer id){m.addAttribute("form",f);m.addAttribute("setId",id);m.addAttribute("levels",LessonService.LEVELS);m.addAttribute("personal",true);return "flashcard/form";}
 @GetMapping("/{id}") public String study(@PathVariable Integer id,Principal p,Model m){m.addAttribute("cards",service.study(id,p.getName()));m.addAttribute("set",service.studySet(id,p.getName()));return "flashcard/study";}
 @PostMapping("/{setId}/cards/{cardId}/state") @ResponseBody
 public ResponseEntity<Void> mark(@PathVariable Integer setId,@PathVariable Integer cardId,@RequestParam String state,Principal p){service.mark(setId,cardId,state,p.getName());return ResponseEntity.noContent().build();}
}
