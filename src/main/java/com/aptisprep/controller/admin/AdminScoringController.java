package com.aptisprep.controller.admin;

import com.aptisprep.controller.DisplayFormatting;
import com.aptisprep.controller.ListPagination;
import com.aptisprep.dto.form.*;
import com.aptisprep.enums.CefrLevel;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.service.scoring.ScoringAdminService;
import com.aptisprep.service.scoring.ScoringViews;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminScoringController {
    private static final List<String> EXAM_TYPES =
            List.of("PLACEMENT", "PRACTICE", "CHECKPOINT", "FULL_MOCK");
    private final ScoringAdminService scoring;
    private final MockExamRepository exams;
    private final DisplayFormatting display;

    public AdminScoringController(
            ScoringAdminService scoring, MockExamRepository exams, DisplayFormatting display) {
        this.scoring = scoring;
        this.exams = exams;
        this.display = display;
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
        return "admin/settings/certificates";
    }

    @GetMapping("/admin/scoring-schemes")
    public String showScoringSchemes(
            @RequestParam(defaultValue = "0") int schemePage,
            @RequestParam(defaultValue = "") String examType,
            @RequestParam(required = false) Integer certificateId,
            Model model) {
        if (!examType.isBlank()
                && !"UNASSIGNED".equals(examType)
                && !EXAM_TYPES.contains(examType))
            throw new BusinessRuleException("Loại đề không hợp lệ");
        var examTypes =
                exams.findScoringSchemeExamTypes().stream()
                        .collect(
                                Collectors.groupingBy(
                                        MockExamRepository.SchemeExamType::getSchemeId,
                                        Collectors.mapping(
                                                MockExamRepository.SchemeExamType::getExamType,
                                                Collectors.toCollection(TreeSet::new))));
        var schemes =
                scoring.schemeViews().stream()
                        .filter(
                                scheme ->
                                        certificateId == null
                                                || certificateId.equals(scheme.certificateId()))
                        .filter(
                                scheme ->
                                        examType.isBlank()
                                                || ("UNASSIGNED".equals(examType)
                                                        ? !examTypes.containsKey(scheme.id())
                                                        : examType.equals(scheme.examType())
                                                                || examTypes.containsKey(
                                                                                scheme.id())
                                                                        && examTypes
                                                                                .get(scheme.id())
                                                                                .contains(
                                                                                        examType)))
                        .toList();
        model.addAttribute("schemePage", ListPagination.slice(schemes, schemePage));
        model.addAttribute("schemeExamTypes", examTypes);
        model.addAttribute("examType", examType);
        model.addAttribute("certificateId", certificateId);
        model.addAttribute("certificates", scoring.certificateViews());
        model.addAttribute("examTypes", EXAM_TYPES);
        return "admin/settings/schemes";
    }

    @GetMapping("/admin/certificates/create")
    public String createCertificate(Model model) {
        model.addAttribute("editing", false);
        model.addAttribute("readOnly", false);
        model.addAttribute("certificateForm", new CertificateForm());
        return "admin/settings/certificate";
    }

    @GetMapping("/admin/certificates/{id}")
    public String certificate(@PathVariable int id, Model model) {
        ScoringViews.CertificateView value = scoring.certificate(id);
        model.addAttribute("certificate", value);
        model.addAttribute("certificateForm", certificateForm(value));
        model.addAttribute("editing", true);
        model.addAttribute("readOnly", true);
        return "admin/settings/certificate";
    }

    @GetMapping("/admin/certificates/{id}/edit")
    public String editCertificate(@PathVariable int id, Model model) {
        certificate(id, model);
        model.addAttribute("readOnly", false);
        return "admin/settings/certificate";
    }

    @PostMapping("/admin/certificates")
    public String saveCertificate(
            Principal principal,
            @Valid @ModelAttribute("certificateForm") CertificateForm form,
            BindingResult result,
            Model model,
            RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("editing", false);
            model.addAttribute("readOnly", false);
            return "admin/settings/certificate";
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
            return "admin/settings/certificate";
        }
    }

    @PostMapping("/admin/certificates/{id}")
    public String updateCertificate(
            @PathVariable int id,
            Principal principal,
            @Valid @ModelAttribute("certificateForm") CertificateForm form,
            BindingResult result,
            Model model,
            RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("certificate", scoring.certificate(id));
            model.addAttribute("editing", true);
            model.addAttribute("readOnly", false);
            return "admin/settings/certificate";
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
            return "admin/settings/certificate";
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

    @GetMapping("/admin/scoring-schemes/create")
    public String createScheme(Model model) {
        schemeOptions(model);
        model.addAttribute("editing", false);
        model.addAttribute("readOnly", false);
        model.addAttribute("schemeForm", new ScoringSchemeForm());
        model.addAttribute("selectedSkill", "Reading");
        model.addAttribute("bands", List.of());
        model.addAttribute("bandForm", new ScoreBandForm());
        return "admin/settings/scheme";
    }

    @GetMapping("/admin/scoring-schemes/{id}")
    public String scheme(
            @PathVariable int id,
            @RequestParam(defaultValue = "Reading") String skill,
            @RequestParam(required = false) Integer bandId,
            Model model) {
        return loadScheme(id, skill, bandId, true, model);
    }

    @GetMapping("/admin/scoring-schemes/{id}/edit")
    public String editScheme(
            @PathVariable int id,
            @RequestParam(defaultValue = "Reading") String skill,
            @RequestParam(required = false) Integer bandId,
            @RequestParam(defaultValue = "false") boolean addBand,
            Model model) {
        model.addAttribute("addBand", addBand);
        return loadScheme(id, skill, bandId, false, model);
    }

    @PostMapping("/admin/scoring-schemes")
    public String saveScheme(
            Principal principal,
            @Valid @ModelAttribute("schemeForm") ScoringSchemeForm form,
            BindingResult result,
            Model model,
            RedirectAttributes redirect) {
        if (form.getExamType() == null || form.getExamType().isBlank())
            result.rejectValue("examType", "scheme.examType", "Chọn loại đề thi");
        if (result.hasErrors()) {
            schemeOptions(model);
            model.addAttribute("editing", false);
            model.addAttribute("readOnly", false);
            model.addAttribute("bandForm", new ScoreBandForm());
            return "admin/settings/scheme";
        }
        try {
            form.setStatus("DRAFT");
            Integer id = scoring.saveSchemeWithScores(principal.getName(), null, form);
            redirect.addFlashAttribute(
                    "successMessage",
                    "Đã lưu bản nháp bộ chấm điểm. Kích hoạt khi cấu hình đầy đủ.");
            return "redirect:/admin/scoring-schemes/" + id;
        } catch (BusinessRuleException exception) {
            result.reject("scheme.invalid", exception.getMessage());
            schemeOptions(model);
            model.addAttribute("editing", false);
            model.addAttribute("readOnly", false);
            model.addAttribute("bandForm", new ScoreBandForm());
            return "admin/settings/scheme";
        }
    }

    @PostMapping("/admin/scoring-schemes/{id}")
    public String updateScheme(
            @PathVariable int id,
            Principal principal,
            @Valid @ModelAttribute("schemeForm") ScoringSchemeForm form,
            BindingResult result,
            Model model,
            RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return loadScheme(id, "Reading", null, false, model);
        }
        try {
            Integer nextId = scoring.saveSchemeWithScores(principal.getName(), id, form);
            redirect.addFlashAttribute(
                    "successMessage",
                    nextId.equals(id)
                            ? "Đã cập nhật bộ chấm điểm."
                            : "Đã tạo phiên bản mới để bảo toàn lịch sử.");
            return "redirect:/admin/scoring-schemes/" + nextId;
        } catch (BusinessRuleException exception) {
            result.reject("scheme.invalid", exception.getMessage());
            return loadScheme(id, "Reading", null, false, model);
        }
    }

    @PostMapping("/admin/scoring-schemes/{id}/disable")
    public String disableScheme(
            @PathVariable int id, Principal principal, RedirectAttributes redirect) {
        scoring.disableScheme(principal.getName(), id);
        redirect.addFlashAttribute("successMessage", "Đã ngừng sử dụng bộ chấm điểm.");
        return "redirect:/admin/scoring-schemes/" + id;
    }

    @PostMapping("/admin/scoring-schemes/{id}/activate")
    public String activateScheme(
            @PathVariable int id, Principal principal, RedirectAttributes redirect) {
        try {
            scoring.activateScheme(principal.getName(), id);
            redirect.addFlashAttribute("successMessage", "Đã kích hoạt bộ chấm điểm.");
        } catch (BusinessRuleException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/scoring-schemes/" + id;
    }

    @PostMapping("/admin/scoring-schemes/{schemeId}/bands")
    public String saveBand(
            @PathVariable int schemeId,
            Principal principal,
            @Valid @ModelAttribute("bandForm") ScoreBandForm form,
            BindingResult result,
            @RequestParam(defaultValue = "Reading") String skill,
            Model model,
            RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return loadScheme(schemeId, skill, form.getId(), false, model);
        }
        try {
            scoring.saveBand(principal.getName(), schemeId, form.getId(), form);
            redirect.addFlashAttribute("successMessage", "Đã lưu khoảng điểm.");
            return "redirect:/admin/scoring-schemes/" + schemeId + "?skill=" + skill;
        } catch (BusinessRuleException exception) {
            result.reject("band.invalid", exception.getMessage());
            return loadScheme(schemeId, skill, form.getId(), false, model);
        }
    }

    @PostMapping("/admin/scoring-schemes/{schemeId}/bands/{bandId}/delete")
    public String deleteBand(
            @PathVariable int schemeId,
            @PathVariable int bandId,
            Principal principal,
            @RequestParam(defaultValue = "Reading") String skill,
            RedirectAttributes redirect) {
        scoring.deleteBand(principal.getName(), schemeId, bandId);
        redirect.addFlashAttribute("successMessage", "Đã xóa khoảng điểm.");
        return "redirect:/admin/scoring-schemes/" + schemeId + "?skill=" + skill;
    }

    private String loadScheme(int id, String skill, Integer bandId, boolean readOnly, Model model) {
        skill =
                switch (skill.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_")) {
                    case "GRAMMAR_VOCABULARY" -> "Grammar & Vocabulary";
                    case "READING" -> "Reading";
                    case "LISTENING" -> "Listening";
                    case "WRITING" -> "Writing";
                    case "SPEAKING" -> "Speaking";
                    default -> throw new BusinessRuleException("Kỹ năng không hợp lệ");
                };
        ScoringViews.SchemeView value = scoring.scheme(id);
        if (value.scopeSkill() != null) skill = display.catalogSkill(value.scopeSkill());
        schemeOptions(model);
        if (value.scopeSkill() != null) model.addAttribute("skills", List.of(skill));
        model.addAttribute("scheme", value);
        if (!model.containsAttribute("schemeForm")) {
            ScoringSchemeForm form = schemeForm(value);
            scoring.populateScoreConfiguration(id, form);
            model.addAttribute("schemeForm", form);
        }
        model.addAttribute("editing", true);
        model.addAttribute("readOnly", readOnly);
        model.addAttribute("selectedSkill", skill);
        model.addAttribute("bands", scoring.bandViews(id, skill));
        if (!model.containsAttribute("scaleRuleForm"))
            model.addAttribute("scaleRuleForm", scoring.scaleRule(id, skill));
        if (!model.containsAttribute("bandForm")) {
            ScoreBandForm bandForm = new ScoreBandForm();
            if (bandId != null) {
                ScoringViews.BandView band = scoring.band(id, bandId);
                bandForm.setId(band.id());
                bandForm.setSkill(band.skill());
                bandForm.setCefrLevel(band.cefrLevel());
                bandForm.setMinScore(band.minScore());
                bandForm.setMaxScore(band.maxScore());
            } else bandForm.setSkill(skill);
            model.addAttribute("bandForm", bandForm);
        }
        return "admin/settings/scheme";
    }

    @PostMapping("/admin/scoring-schemes/{schemeId}/scale-rule")
    public String saveScaleRule(
            @PathVariable int schemeId,
            @RequestParam String skill,
            Principal principal,
            @Valid @ModelAttribute("scaleRuleForm") ScoringScaleRuleForm form,
            BindingResult result,
            Model model,
            RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                scoring.saveScaleRule(principal.getName(), schemeId, skill, form);
                redirect.addFlashAttribute("successMessage", "Đã lưu quy tắc quy đổi điểm.");
                return "redirect:/admin/scoring-schemes/" + schemeId + "/edit?skill=" + skill;
            } catch (BusinessRuleException exception) {
                result.reject("scale.invalid", exception.getMessage());
            }
        }
        return loadScheme(schemeId, skill, null, false, model);
    }

    private void schemeOptions(Model model) {
        model.addAttribute("certificates", scoring.certificateViews());
        model.addAttribute("examTypes", EXAM_TYPES);
        model.addAttribute("scoringLevels", CefrLevel.codes());
        model.addAttribute("fullMockScoringLevels", CefrLevel.scoringCodes());
        model.addAttribute(
                "skills",
                List.of("Grammar & Vocabulary", "Reading", "Listening", "Writing", "Speaking"));
    }

    private CertificateForm certificateForm(ScoringViews.CertificateView value) {
        CertificateForm form = new CertificateForm();
        form.setName(value.name());
        form.setCode(value.code());
        form.setDescription(value.description());
        form.setStatus(value.status());
        return form;
    }

    private ScoringSchemeForm schemeForm(ScoringViews.SchemeView value) {
        ScoringSchemeForm form = new ScoringSchemeForm();
        form.setExamType(value.examType());
        form.setScopeSkill(value.scopeSkill());
        form.setCertificateId(value.certificateId());
        form.setName(value.name());
        form.setVersion(value.version());
        form.setStatus(value.status());
        return form;
    }
}
