package com.aptisprep.mvc.controller.admin;

import com.aptisprep.mvc.dto.CertificateDTO;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.service.scoring.ScoringAdminService;
import com.aptisprep.mvc.support.pagination.ListPagination;
import com.aptisprep.mvc.support.scoring.ScoringViews;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Certificate administration endpoints extracted from the scoring controller. */
@Controller
public class AdminCertificateController {
    private final ScoringAdminService scoring;

    public AdminCertificateController(ScoringAdminService scoring) {
        this.scoring = scoring;
    }

    @GetMapping("/admin/settings")
    public String settings() {
        return "redirect:/admin/certificates";
    }

    @GetMapping("/admin/certificates")
    public String showCertificates(
            @RequestParam(defaultValue = "0") int certificatePage, Model model) {
        model.addAttribute(
                "certificatePage",
                ListPagination.slice(scoring.certificateViews(), certificatePage));
        return "admin/settings/certificate/list";
    }

    @GetMapping("/admin/certificates/create")
    public String createCertificate(Model model) {
        model.addAttribute("editing", false);
        model.addAttribute("readOnly", false);
        model.addAttribute("certificateForm", new CertificateDTO());
        return "admin/settings/certificate/form";
    }

    @GetMapping("/admin/certificates/{id}")
    public String certificate(@PathVariable int id, Model model) {
        ScoringViews.CertificateView value = scoring.certificate(id);
        model.addAttribute("certificate", value);
        model.addAttribute(
                "certificateForm",
                new CertificateDTO(
                        value.name(),
                        value.code(),
                        value.description(),
                        value.status(),
                        value.id(),
                        null,
                        null,
                        null));
        model.addAttribute("editing", true);
        model.addAttribute("readOnly", true);
        return "admin/settings/certificate/form";
    }

    @GetMapping("/admin/certificates/{id}/edit")
    public String editCertificate(@PathVariable int id, Model model) {
        certificate(id, model);
        model.addAttribute("readOnly", false);
        return "admin/settings/certificate/form";
    }

    @PostMapping("/admin/certificates")
    public String saveCertificate(
            Principal principal,
            @Valid @ModelAttribute("certificateForm") CertificateDTO form,
            BindingResult result,
            Model model,
            RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("editing", false);
            model.addAttribute("readOnly", false);
            return "admin/settings/certificate/form";
        }
        try {
            form.setStatus("DRAFT");
            scoring.createCertificate(principal.getName(), form);
            redirect.addFlashAttribute(
                    "successMessage", "Đã lưu bản nháp chứng chỉ. Kích hoạt khi sẵn sàng.");
            return "redirect:/admin/certificates";
        } catch (BusinessRuleException exception) {
            result.reject("certificate.invalid", exception.getMessage());
            model.addAttribute("editing", false);
            model.addAttribute("readOnly", false);
            return "admin/settings/certificate/form";
        }
    }

    @PostMapping("/admin/certificates/{id}")
    public String updateCertificate(
            @PathVariable int id,
            Principal principal,
            @Valid @ModelAttribute("certificateForm") CertificateDTO form,
            BindingResult result,
            Model model,
            RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("certificate", scoring.certificate(id));
            model.addAttribute("editing", true);
            model.addAttribute("readOnly", false);
            return "admin/settings/certificate/form";
        }
        try {
            scoring.updateCertificate(principal.getName(), id, form);
            redirect.addFlashAttribute("successMessage", "Đã cập nhật chứng chỉ.");
            return "redirect:/admin/certificates";
        } catch (BusinessRuleException exception) {
            result.reject("certificate.invalid", exception.getMessage());
            model.addAttribute("certificate", scoring.certificate(id));
            model.addAttribute("editing", true);
            model.addAttribute("readOnly", false);
            return "admin/settings/certificate/form";
        }
    }

    @PostMapping("/admin/certificates/{id}/disable")
    public String disableCertificate(
            @PathVariable int id, Principal principal, RedirectAttributes redirect) {
        scoring.disableCertificate(principal.getName(), id);
        redirect.addFlashAttribute("successMessage", "Đã ngừng sử dụng chứng chỉ.");
        return "redirect:/admin/certificates";
    }

    @PostMapping("/admin/certificates/{id}/activate")
    public String activateCertificate(
            @PathVariable int id, Principal principal, RedirectAttributes redirect) {
        scoring.activateCertificate(principal.getName(), id);
        redirect.addFlashAttribute("successMessage", "Đã kích hoạt chứng chỉ.");
        return "redirect:/admin/certificates/" + id;
    }
}
