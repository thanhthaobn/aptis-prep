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
 @GetMapping public String list(Principal p,Model m){m.addAttribute("sets",service.listForStudent(p.getName()));m.addAttribute("admin",false);return "flashcard/list";}
 @GetMapping("/create") public String create(Model m){var f=new com.aptisprep.dto.form.FlashcardSetForm();f.getRows().add(new com.aptisprep.dto.form.FlashcardSetForm.Row());return form(m,f,null);}
 @GetMapping("/{id}/edit") public String edit(@PathVariable Integer id,Principal p,Model m){return form(m,service.personalForm(id,p.getName()),id);}
 @PostMapping public String create(@jakarta.validation.Valid @ModelAttribute("form") com.aptisprep.dto.form.FlashcardSetForm f,org.springframework.validation.BindingResult b,Principal p,Model m){return save(null,f,b,p,m);}
 @PostMapping("/{id}") public String edit(@PathVariable Integer id,@jakarta.validation.Valid @ModelAttribute("form") com.aptisprep.dto.form.FlashcardSetForm f,org.springframework.validation.BindingResult b,Principal p,Model m){return save(id,f,b,p,m);}
 private String save(Integer id,com.aptisprep.dto.form.FlashcardSetForm f,org.springframework.validation.BindingResult b,Principal p,Model m){if(!b.hasErrors())try{service.savePersonal(id,f,p.getName());return "redirect:/student/flashcards";}catch(com.aptisprep.exception.BusinessRuleException e){b.reject("invalid",e.getMessage());}return form(m,f,id);}
 private String form(Model m,com.aptisprep.dto.form.FlashcardSetForm f,Integer id){m.addAttribute("form",f);m.addAttribute("setId",id);m.addAttribute("levels",LessonService.LEVELS);m.addAttribute("personal",true);return "flashcard/form";}
 @GetMapping("/{id}") public String study(@PathVariable Integer id,Principal p,Model m){m.addAttribute("cards",service.study(id,p.getName()));m.addAttribute("set",service.requireForStudent(id,p.getName()));return "flashcard/study";}
 @PostMapping("/{setId}/cards/{cardId}/state") @ResponseBody
 public ResponseEntity<Void> mark(@PathVariable Integer setId,@PathVariable Integer cardId,@RequestParam String state,Principal p){service.mark(setId,cardId,state,p.getName());return ResponseEntity.noContent().build();}
}
