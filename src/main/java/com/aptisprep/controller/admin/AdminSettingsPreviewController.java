package com.aptisprep.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import java.util.List;
import com.aptisprep.service.admin.ScoringPreviewService;
import com.aptisprep.exception.BusinessRuleException;

/** Read-only settings layout. Never exposes configuration secrets or writes properties. */
@Controller
public class AdminSettingsPreviewController {
    private final ScoringPreviewService preview;
    public AdminSettingsPreviewController(ScoringPreviewService preview) { this.preview = preview; }
    @GetMapping("/admin/settings")
    public String settings() { return "redirect:/admin/certificates"; }
    @GetMapping({"/admin/certificates", "/admin/scoring-schemes"})
    public String index(Model model) {
        model.addAttribute("certificates", preview.certificates()); model.addAttribute("schemes", preview.schemes());
        return "admin/settings/index";
    }
    @GetMapping("/admin/certificates/create")
    public String createCertificate(Model model) {
        model.addAttribute("editing", false); model.addAttribute("readOnly", false); return "admin/settings/certificate";
    }
    @GetMapping("/admin/certificates/{id:\\d+}")
    public String certificate(@PathVariable int id, Model model) {
        model.addAttribute("certificate", preview.certificate(id)); model.addAttribute("editing", true); model.addAttribute("readOnly", true);
        return "admin/settings/certificate";
    }
    @GetMapping("/admin/certificates/{id:\\d+}/edit")
    public String editCertificate(@PathVariable int id, Model model) {
        certificate(id, model); model.addAttribute("readOnly", false); return "admin/settings/certificate";
    }
    @GetMapping("/admin/scoring-schemes/create")
    public String createScheme(Model model) {
        schemeOptions(model); model.addAttribute("editing", false); model.addAttribute("readOnly", false); model.addAttribute("selectedSkill", "Reading"); model.addAttribute("bands", List.of());
        return "admin/settings/scheme";
    }
    @GetMapping("/admin/scoring-schemes/{id:\\d+}")
    public String scheme(@PathVariable int id, @RequestParam(defaultValue="Reading") String skill, Model model) {
        schemeOptions(model);
        if (!skills().contains(skill)) throw new BusinessRuleException("Kỹ năng không hợp lệ");
        model.addAttribute("scheme", preview.scheme(id)); model.addAttribute("editing", true); model.addAttribute("readOnly", true); model.addAttribute("selectedSkill", skill);
        model.addAttribute("bands", skill.equals("Grammar & Vocabulary") ? List.of() : preview.bands());
        return "admin/settings/scheme";
    }
    @GetMapping("/admin/scoring-schemes/{id:\\d+}/edit")
    public String editScheme(@PathVariable int id, @RequestParam(defaultValue="Reading") String skill, Model model) {
        scheme(id, skill, model); model.addAttribute("readOnly", false); return "admin/settings/scheme";
    }
    private List<String> skills() { return List.of("Grammar & Vocabulary", "Reading", "Listening", "Writing", "Speaking"); }
    private void schemeOptions(Model model) { model.addAttribute("certificates", preview.certificates()); model.addAttribute("skills", skills()); }
}
