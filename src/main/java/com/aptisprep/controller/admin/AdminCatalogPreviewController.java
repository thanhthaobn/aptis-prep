package com.aptisprep.controller.admin;

import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.service.admin.AdminCatalogPreviewService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/** GET-only UI scaffolding. No repositories, mutation routes or AI calls. */
@Controller
@RequestMapping("/admin")
public class AdminCatalogPreviewController {
    private final AdminCatalogPreviewService preview;
    public AdminCatalogPreviewController(AdminCatalogPreviewService preview) { this.preview = preview; }

    @GetMapping("/{module:lessons|questions|exams|results}")
    public String list(@PathVariable String module, @RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "") String skill, @RequestParam(defaultValue = "") String status,
                       @RequestParam(defaultValue = "") String kind, Model model) {
        if (q.length() > 255 || skill.length() > 80 || status.length() > 40 || kind.length() > 80)
            throw new BusinessRuleException("Bộ lọc không hợp lệ");
        addModule(module, model);
        model.addAttribute("entries", preview.find(module, q, skill, status, kind));
        model.addAttribute("q", q); model.addAttribute("selectedSkill", skill);
        model.addAttribute("selectedStatus", status); model.addAttribute("selectedKind", kind);
        return "admin/catalog/list";
    }

    @GetMapping("/{module:lessons|questions|exams|results}/{id:\\d+}")
    public String detail(@PathVariable String module, @PathVariable int id, Model model) {
        addModule(module, model); model.addAttribute("entry", preview.entry(module, id));
        return "admin/catalog/detail";
    }

    @GetMapping("/{module:lessons|questions|exams}/create")
    public String create(@PathVariable String module, Model model) {
        addModule(module, model); model.addAttribute("editing", false);
        return "admin/catalog/form";
    }

    @GetMapping("/{module:lessons|questions|exams}/{id:\\d+}/edit")
    public String edit(@PathVariable String module, @PathVariable int id, Model model) {
        if (!preview.module(module).editable()) throw new ResourceNotFoundException("Không tìm thấy nội dung");
        addModule(module, model); model.addAttribute("entry", preview.entry(module, id));
        model.addAttribute("editing", true); return "admin/catalog/form";
    }

    private void addModule(String key, Model model) {
        model.addAttribute("catalog", preview.module(key));
        model.addAttribute("skills", List.of("Grammar & Vocabulary", "Reading", "Listening", "Writing", "Speaking", "Tổng hợp"));
    }
}
