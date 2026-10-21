package com.aptisprep.controller.admin;
import com.aptisprep.service.learning.*;
import com.aptisprep.dto.form.FlashcardSetForm;
import com.aptisprep.exception.BusinessRuleException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
@Controller @RequestMapping("/admin/flashcards")
public class AdminFlashcardController {
 private final FlashcardSetService service;
 public AdminFlashcardController(FlashcardSetService s){service=s;}
 @GetMapping public String list(Model m){m.addAttribute("sets",service.list(true));m.addAttribute("admin",true);return "flashcard/list";}
 @GetMapping("/create") public String create(@RequestParam(required=false) Integer lessonId,Model m){var f=new FlashcardSetForm();f.getRows().add(new FlashcardSetForm.Row());service.prepareForLesson(f,lessonId);return form(m,f,null);}
 @GetMapping("/{id}/edit") public String edit(@PathVariable Integer id,Model m){return form(m,service.form(id),id);}
 @PostMapping public String create(@Valid @ModelAttribute("form") FlashcardSetForm f,BindingResult b,Model m){return save(null,f,b,m);}
 @PostMapping("/{id}") public String edit(@PathVariable Integer id,@Valid @ModelAttribute("form") FlashcardSetForm f,BindingResult b,Model m){return save(id,f,b,m);}
 private String save(Integer id,FlashcardSetForm f,BindingResult b,Model m){if(!b.hasErrors()){try{service.saveAdmin(id,f);return "redirect:/admin/flashcards";}catch(BusinessRuleException e){b.reject("invalid",e.getMessage());}}return form(m,f,id);}
 private String form(Model m,FlashcardSetForm f,Integer id){m.addAttribute("form",f);m.addAttribute("setId",id);m.addAttribute("levels",LessonService.LEVELS);m.addAttribute("lessons",service.availableLessons());m.addAttribute("personal",false);return "flashcard/form";}
}
