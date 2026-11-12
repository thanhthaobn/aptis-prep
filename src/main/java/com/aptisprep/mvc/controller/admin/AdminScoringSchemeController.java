package com.aptisprep.mvc.controller.admin;

import com.aptisprep.mvc.dto.*;
import com.aptisprep.mvc.enums.CefrLevel;
import com.aptisprep.mvc.enums.ExamType;
import com.aptisprep.mvc.enums.Skill;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.service.scoring.ScoringAdminService;
import com.aptisprep.mvc.support.contract.*;
import com.aptisprep.mvc.support.grading.*;
import com.aptisprep.mvc.support.pagination.ListPagination;
import com.aptisprep.mvc.support.scoring.*;
import com.aptisprep.mvc.support.scoring.ScoringViews;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminScoringSchemeController {
    private static final List<String> EXAM_TYPES = ExamType.codes();
    private static final Map<String, String> EXAM_TYPE_LABELS = ExamType.labels();
    private static final Map<String, String> SKILL_LABELS =
            Map.of(
                    "GRAMMAR_VOCABULARY", "Grammar & Vocabulary",
                    "READING", "Reading",
                    "LISTENING", "Listening",
                    "WRITING", "Writing",
                    "SPEAKING", "Speaking");
    private final ScoringAdminService scoring;

    public AdminScoringSchemeController(ScoringAdminService scoring) {
        this.scoring = scoring;
    }

    @GetMapping("/admin/scoring-schemes")
    public String showScoringSchemes(
            @RequestParam(defaultValue = "0") int schemePage,
            @RequestParam(name = "examType", defaultValue = "") String examType,
            @RequestParam(required = false) Integer certificateId,
            Model model) {
        String normalizedExamType = examType == null ? "" : examType;
        if (!normalizedExamType.isBlank()
                && !"UNASSIGNED".equals(normalizedExamType)
                && !EXAM_TYPES.contains(normalizedExamType))
            throw new BusinessRuleException("Loại đề không hợp lệ");
        var catalog = scoring.filteredSchemes(certificateId, normalizedExamType);
        model.addAttribute("schemePage", ListPagination.slice(catalog.schemes(), schemePage));
        model.addAttribute("schemeExamTypes", catalog.examTypesByScheme());
        model.addAttribute("examType", normalizedExamType);
        model.addAttribute("certificateId", certificateId);
        model.addAttribute("certificates", scoring.certificateViews());
        model.addAttribute("examTypes", EXAM_TYPES);
        model.addAttribute("examTypeLabels", EXAM_TYPE_LABELS);
        model.addAttribute("skillLabels", SKILL_LABELS);
        return "admin/settings/scheme/list";
    }

    @GetMapping("/admin/scoring-schemes/create")
    public String createScheme(
            @RequestParam(defaultValue = "") String examType,
            @RequestParam(defaultValue = "") String scopeSkill,
            Model model) {
        {
            model.addAttribute("certificates", scoring.certificateViews());
            model.addAttribute("examTypes", EXAM_TYPES);
            model.addAttribute("scoringLevels", CefrLevel.codes());
            model.addAttribute("fullMockScoringLevels", CefrLevel.scoringCodes());
            model.addAttribute("skillCodes", Skill.codes());
            model.addAttribute("examTypeLabels", EXAM_TYPE_LABELS);
            model.addAttribute("skillLabels", SKILL_LABELS);
            model.addAttribute(
                    "skills",
                    List.of("Grammar & Vocabulary", "Reading", "Listening", "Writing", "Speaking"));
        }
        String selectedExamType = examType.trim().toUpperCase(Locale.ROOT);
        if (!selectedExamType.isBlank() && !EXAM_TYPES.contains(selectedExamType))
            throw new BusinessRuleException("Loại đề không hợp lệ");
        boolean singleSkill =
                "PRACTICE".equals(selectedExamType) || "CHECKPOINT".equals(selectedExamType);
        String selectedScopeSkill = scopeSkill.trim().toUpperCase(Locale.ROOT);
        if (!selectedScopeSkill.isBlank()
                && (!singleSkill || !Skill.codes().contains(selectedScopeSkill)))
            throw new BusinessRuleException("Kỹ năng không hợp lệ");
        ScoringSchemeDTO form = new ScoringSchemeDTO();
        form.setExamType(selectedExamType.isBlank() ? null : selectedExamType);
        form.setScopeSkill(selectedScopeSkill.isBlank() ? null : selectedScopeSkill);
        model.addAttribute("editing", false);
        model.addAttribute("readOnly", false);
        model.addAttribute("schemeForm", form);
        model.addAttribute("selectedSkill", "Reading");
        model.addAttribute("bands", List.of());
        model.addAttribute("bandForm", new ScoreBandDTO());
        return "admin/settings/scheme/form";
    }

    @GetMapping("/admin/scoring-schemes/{id}")
    public String scheme(
            @PathVariable int id,
            @RequestParam(defaultValue = "Reading") String skill,
            @RequestParam(required = false) Integer bandId,
            Model model) {
        {
            int schemePageId1 = id;
            String schemePageSkill1 = skill;
            Integer schemePageBandId1 = bandId;
            boolean schemePageReadOnly1 = true;
            Model schemePageModel1 = model;
            schemePageSkill1 =
                    switch (schemePageSkill1
                            .toUpperCase(Locale.ROOT)
                            .replaceAll("[^A-Z0-9]+", "_")) {
                        case "GRAMMAR_VOCABULARY" -> "Grammar & Vocabulary";
                        case "READING" -> "Reading";
                        case "LISTENING" -> "Listening";
                        case "WRITING" -> "Writing";
                        case "SPEAKING" -> "Speaking";
                        default -> throw new BusinessRuleException("Kỹ năng không hợp lệ");
                    };
            ScoringViews.SchemeView value = scoring.scheme(schemePageId1);
            if (value.scopeSkill() != null)
                schemePageSkill1 =
                        SKILL_LABELS.getOrDefault(value.scopeSkill(), value.scopeSkill());
            {
                schemePageModel1.addAttribute("certificates", scoring.certificateViews());
                schemePageModel1.addAttribute("examTypes", EXAM_TYPES);
                schemePageModel1.addAttribute("scoringLevels", CefrLevel.codes());
                schemePageModel1.addAttribute("fullMockScoringLevels", CefrLevel.scoringCodes());
                schemePageModel1.addAttribute("skillCodes", Skill.codes());
                schemePageModel1.addAttribute("examTypeLabels", EXAM_TYPE_LABELS);
                schemePageModel1.addAttribute("skillLabels", SKILL_LABELS);
                schemePageModel1.addAttribute(
                        "skills",
                        List.of(
                                "Grammar & Vocabulary",
                                "Reading",
                                "Listening",
                                "Writing",
                                "Speaking"));
            }
            if (value.scopeSkill() != null)
                schemePageModel1.addAttribute("skills", List.of(schemePageSkill1));
            schemePageModel1.addAttribute("scheme", value);
            if (!schemePageModel1.containsAttribute("schemeForm")) {
                ScoringSchemeDTO pageForm =
                        new ScoringSchemeDTO(
                                value.id(),
                                value.certificateId(),
                                null,
                                value.name(),
                                value.version(),
                                value.status(),
                                value.examType(),
                                value.scopeSkill(),
                                value.effectiveFrom(),
                                null);
                scoring.populateScoreConfiguration(schemePageId1, pageForm);
                schemePageModel1.addAttribute("schemeForm", pageForm);
            }
            schemePageModel1.addAttribute("editing", true);
            schemePageModel1.addAttribute("readOnly", schemePageReadOnly1);
            schemePageModel1.addAttribute("selectedSkill", schemePageSkill1);
            schemePageModel1.addAttribute(
                    "bands", scoring.bandViews(schemePageId1, schemePageSkill1));
            Object bandErrors =
                    schemePageModel1.getAttribute(BindingResult.MODEL_KEY_PREFIX + "bandForm");
            schemePageModel1.addAttribute(
                    "bandFormHasErrors",
                    bandErrors instanceof BindingResult bandResult && bandResult.hasErrors());
            if (!schemePageModel1.containsAttribute("scaleRuleForm"))
                schemePageModel1.addAttribute(
                        "scaleRuleForm", scoring.scaleRule(schemePageId1, schemePageSkill1));
            if (!schemePageModel1.containsAttribute("bandForm")) {
                ScoreBandDTO bandForm = new ScoreBandDTO();
                if (schemePageBandId1 != null) {
                    ScoringViews.BandView band = scoring.band(schemePageId1, schemePageBandId1);
                    bandForm.setId(band.id());
                    bandForm.setSkill(band.skill());
                    bandForm.setCefrLevel(band.cefrLevel());
                    bandForm.setMinScore(band.minScore());
                    bandForm.setMaxScore(band.maxScore());
                } else bandForm.setSkill(schemePageSkill1);
                schemePageModel1.addAttribute("bandForm", bandForm);
            }
        }
        return "admin/settings/scheme/form";
    }

    @GetMapping("/admin/scoring-schemes/{id}/edit")
    public String editScheme(
            @PathVariable int id,
            @RequestParam(defaultValue = "Reading") String skill,
            @RequestParam(required = false) Integer bandId,
            @RequestParam(defaultValue = "false") boolean addBand,
            Model model) {
        model.addAttribute("addBand", addBand);
        {
            int schemePageId2 = id;
            String schemePageSkill2 = skill;
            Integer schemePageBandId2 = bandId;
            boolean schemePageReadOnly2 = false;
            Model schemePageModel2 = model;
            schemePageSkill2 =
                    switch (schemePageSkill2
                            .toUpperCase(Locale.ROOT)
                            .replaceAll("[^A-Z0-9]+", "_")) {
                        case "GRAMMAR_VOCABULARY" -> "Grammar & Vocabulary";
                        case "READING" -> "Reading";
                        case "LISTENING" -> "Listening";
                        case "WRITING" -> "Writing";
                        case "SPEAKING" -> "Speaking";
                        default -> throw new BusinessRuleException("Kỹ năng không hợp lệ");
                    };
            ScoringViews.SchemeView value = scoring.scheme(schemePageId2);
            if (value.scopeSkill() != null)
                schemePageSkill2 =
                        SKILL_LABELS.getOrDefault(value.scopeSkill(), value.scopeSkill());
            {
                schemePageModel2.addAttribute("certificates", scoring.certificateViews());
                schemePageModel2.addAttribute("examTypes", EXAM_TYPES);
                schemePageModel2.addAttribute("scoringLevels", CefrLevel.codes());
                schemePageModel2.addAttribute("fullMockScoringLevels", CefrLevel.scoringCodes());
                schemePageModel2.addAttribute("skillCodes", Skill.codes());
                schemePageModel2.addAttribute("examTypeLabels", EXAM_TYPE_LABELS);
                schemePageModel2.addAttribute("skillLabels", SKILL_LABELS);
                schemePageModel2.addAttribute(
                        "skills",
                        List.of(
                                "Grammar & Vocabulary",
                                "Reading",
                                "Listening",
                                "Writing",
                                "Speaking"));
            }
            if (value.scopeSkill() != null)
                schemePageModel2.addAttribute("skills", List.of(schemePageSkill2));
            schemePageModel2.addAttribute("scheme", value);
            if (!schemePageModel2.containsAttribute("schemeForm")) {
                ScoringSchemeDTO pageForm =
                        new ScoringSchemeDTO(
                                value.id(),
                                value.certificateId(),
                                null,
                                value.name(),
                                value.version(),
                                value.status(),
                                value.examType(),
                                value.scopeSkill(),
                                value.effectiveFrom(),
                                null);
                scoring.populateScoreConfiguration(schemePageId2, pageForm);
                schemePageModel2.addAttribute("schemeForm", pageForm);
            }
            schemePageModel2.addAttribute("editing", true);
            schemePageModel2.addAttribute("readOnly", schemePageReadOnly2);
            schemePageModel2.addAttribute("selectedSkill", schemePageSkill2);
            schemePageModel2.addAttribute(
                    "bands", scoring.bandViews(schemePageId2, schemePageSkill2));
            Object bandErrors =
                    schemePageModel2.getAttribute(BindingResult.MODEL_KEY_PREFIX + "bandForm");
            schemePageModel2.addAttribute(
                    "bandFormHasErrors",
                    bandErrors instanceof BindingResult bandResult && bandResult.hasErrors());
            if (!schemePageModel2.containsAttribute("scaleRuleForm"))
                schemePageModel2.addAttribute(
                        "scaleRuleForm", scoring.scaleRule(schemePageId2, schemePageSkill2));
            if (!schemePageModel2.containsAttribute("bandForm")) {
                ScoreBandDTO bandForm = new ScoreBandDTO();
                if (schemePageBandId2 != null) {
                    ScoringViews.BandView band = scoring.band(schemePageId2, schemePageBandId2);
                    bandForm.setId(band.id());
                    bandForm.setSkill(band.skill());
                    bandForm.setCefrLevel(band.cefrLevel());
                    bandForm.setMinScore(band.minScore());
                    bandForm.setMaxScore(band.maxScore());
                } else bandForm.setSkill(schemePageSkill2);
                schemePageModel2.addAttribute("bandForm", bandForm);
            }
        }
        return "admin/settings/scheme/form";
    }

    @PostMapping("/admin/scoring-schemes")
    public String saveScheme(
            Principal principal,
            @Valid @ModelAttribute("schemeForm") ScoringSchemeDTO form,
            BindingResult result,
            Model model,
            RedirectAttributes redirect) {
        if (form.getExamType() == null || form.getExamType().isBlank())
            result.rejectValue("examType", "scheme.examType", "Chọn loại đề thi");
        if (result.hasErrors()) {
            {
                model.addAttribute("certificates", scoring.certificateViews());
                model.addAttribute("examTypes", EXAM_TYPES);
                model.addAttribute("scoringLevels", CefrLevel.codes());
                model.addAttribute("fullMockScoringLevels", CefrLevel.scoringCodes());
                model.addAttribute("skillCodes", Skill.codes());
                model.addAttribute("examTypeLabels", EXAM_TYPE_LABELS);
                model.addAttribute("skillLabels", SKILL_LABELS);
                model.addAttribute(
                        "skills",
                        List.of(
                                "Grammar & Vocabulary",
                                "Reading",
                                "Listening",
                                "Writing",
                                "Speaking"));
            }
            model.addAttribute("editing", false);
            model.addAttribute("readOnly", false);
            model.addAttribute("bandForm", new ScoreBandDTO());
            return "admin/settings/scheme/form";
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
            {
                model.addAttribute("certificates", scoring.certificateViews());
                model.addAttribute("examTypes", EXAM_TYPES);
                model.addAttribute("scoringLevels", CefrLevel.codes());
                model.addAttribute("fullMockScoringLevels", CefrLevel.scoringCodes());
                model.addAttribute("skillCodes", Skill.codes());
                model.addAttribute("examTypeLabels", EXAM_TYPE_LABELS);
                model.addAttribute("skillLabels", SKILL_LABELS);
                model.addAttribute(
                        "skills",
                        List.of(
                                "Grammar & Vocabulary",
                                "Reading",
                                "Listening",
                                "Writing",
                                "Speaking"));
            }
            model.addAttribute("editing", false);
            model.addAttribute("readOnly", false);
            model.addAttribute("bandForm", new ScoreBandDTO());
            return "admin/settings/scheme/form";
        }
    }

    @PostMapping("/admin/scoring-schemes/{id}")
    public String updateScheme(
            @PathVariable int id,
            Principal principal,
            @Valid @ModelAttribute("schemeForm") ScoringSchemeDTO form,
            BindingResult result,
            Model model,
            RedirectAttributes redirect) {
        if (result.hasErrors()) {
            {
                int schemePageId3 = id;
                String schemePageSkill3 = "Reading";
                Integer schemePageBandId3 = null;
                boolean schemePageReadOnly3 = false;
                Model schemePageModel3 = model;
                schemePageSkill3 =
                        switch (schemePageSkill3
                                .toUpperCase(Locale.ROOT)
                                .replaceAll("[^A-Z0-9]+", "_")) {
                            case "GRAMMAR_VOCABULARY" -> "Grammar & Vocabulary";
                            case "READING" -> "Reading";
                            case "LISTENING" -> "Listening";
                            case "WRITING" -> "Writing";
                            case "SPEAKING" -> "Speaking";
                            default -> throw new BusinessRuleException("Kỹ năng không hợp lệ");
                        };
                ScoringViews.SchemeView value = scoring.scheme(schemePageId3);
                if (value.scopeSkill() != null)
                    schemePageSkill3 =
                            SKILL_LABELS.getOrDefault(value.scopeSkill(), value.scopeSkill());
                {
                    schemePageModel3.addAttribute("certificates", scoring.certificateViews());
                    schemePageModel3.addAttribute("examTypes", EXAM_TYPES);
                    schemePageModel3.addAttribute("scoringLevels", CefrLevel.codes());
                    schemePageModel3.addAttribute(
                            "fullMockScoringLevels", CefrLevel.scoringCodes());
                    schemePageModel3.addAttribute("skillCodes", Skill.codes());
                    schemePageModel3.addAttribute("examTypeLabels", EXAM_TYPE_LABELS);
                    schemePageModel3.addAttribute("skillLabels", SKILL_LABELS);
                    schemePageModel3.addAttribute(
                            "skills",
                            List.of(
                                    "Grammar & Vocabulary",
                                    "Reading",
                                    "Listening",
                                    "Writing",
                                    "Speaking"));
                }
                if (value.scopeSkill() != null)
                    schemePageModel3.addAttribute("skills", List.of(schemePageSkill3));
                schemePageModel3.addAttribute("scheme", value);
                if (!schemePageModel3.containsAttribute("schemeForm")) {
                    ScoringSchemeDTO pageForm =
                            new ScoringSchemeDTO(
                                    value.id(),
                                    value.certificateId(),
                                    null,
                                    value.name(),
                                    value.version(),
                                    value.status(),
                                    value.examType(),
                                    value.scopeSkill(),
                                    value.effectiveFrom(),
                                    null);
                    scoring.populateScoreConfiguration(schemePageId3, pageForm);
                    schemePageModel3.addAttribute("schemeForm", pageForm);
                }
                schemePageModel3.addAttribute("editing", true);
                schemePageModel3.addAttribute("readOnly", schemePageReadOnly3);
                schemePageModel3.addAttribute("selectedSkill", schemePageSkill3);
                schemePageModel3.addAttribute(
                        "bands", scoring.bandViews(schemePageId3, schemePageSkill3));
                Object bandErrors =
                        schemePageModel3.getAttribute(BindingResult.MODEL_KEY_PREFIX + "bandForm");
                schemePageModel3.addAttribute(
                        "bandFormHasErrors",
                        bandErrors instanceof BindingResult bandResult && bandResult.hasErrors());
                if (!schemePageModel3.containsAttribute("scaleRuleForm"))
                    schemePageModel3.addAttribute(
                            "scaleRuleForm", scoring.scaleRule(schemePageId3, schemePageSkill3));
                if (!schemePageModel3.containsAttribute("bandForm")) {
                    ScoreBandDTO bandForm = new ScoreBandDTO();
                    if (schemePageBandId3 != null) {
                        ScoringViews.BandView band = scoring.band(schemePageId3, schemePageBandId3);
                        bandForm.setId(band.id());
                        bandForm.setSkill(band.skill());
                        bandForm.setCefrLevel(band.cefrLevel());
                        bandForm.setMinScore(band.minScore());
                        bandForm.setMaxScore(band.maxScore());
                    } else bandForm.setSkill(schemePageSkill3);
                    schemePageModel3.addAttribute("bandForm", bandForm);
                }
            }
            return "admin/settings/scheme/form";
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
            {
                int schemePageId4 = id;
                String schemePageSkill4 = "Reading";
                Integer schemePageBandId4 = null;
                boolean schemePageReadOnly4 = false;
                Model schemePageModel4 = model;
                schemePageSkill4 =
                        switch (schemePageSkill4
                                .toUpperCase(Locale.ROOT)
                                .replaceAll("[^A-Z0-9]+", "_")) {
                            case "GRAMMAR_VOCABULARY" -> "Grammar & Vocabulary";
                            case "READING" -> "Reading";
                            case "LISTENING" -> "Listening";
                            case "WRITING" -> "Writing";
                            case "SPEAKING" -> "Speaking";
                            default -> throw new BusinessRuleException("Kỹ năng không hợp lệ");
                        };
                ScoringViews.SchemeView value = scoring.scheme(schemePageId4);
                if (value.scopeSkill() != null)
                    schemePageSkill4 =
                            SKILL_LABELS.getOrDefault(value.scopeSkill(), value.scopeSkill());
                {
                    schemePageModel4.addAttribute("certificates", scoring.certificateViews());
                    schemePageModel4.addAttribute("examTypes", EXAM_TYPES);
                    schemePageModel4.addAttribute("scoringLevels", CefrLevel.codes());
                    schemePageModel4.addAttribute(
                            "fullMockScoringLevels", CefrLevel.scoringCodes());
                    schemePageModel4.addAttribute("skillCodes", Skill.codes());
                    schemePageModel4.addAttribute("examTypeLabels", EXAM_TYPE_LABELS);
                    schemePageModel4.addAttribute("skillLabels", SKILL_LABELS);
                    schemePageModel4.addAttribute(
                            "skills",
                            List.of(
                                    "Grammar & Vocabulary",
                                    "Reading",
                                    "Listening",
                                    "Writing",
                                    "Speaking"));
                }
                if (value.scopeSkill() != null)
                    schemePageModel4.addAttribute("skills", List.of(schemePageSkill4));
                schemePageModel4.addAttribute("scheme", value);
                if (!schemePageModel4.containsAttribute("schemeForm")) {
                    ScoringSchemeDTO pageForm =
                            new ScoringSchemeDTO(
                                    value.id(),
                                    value.certificateId(),
                                    null,
                                    value.name(),
                                    value.version(),
                                    value.status(),
                                    value.examType(),
                                    value.scopeSkill(),
                                    value.effectiveFrom(),
                                    null);
                    scoring.populateScoreConfiguration(schemePageId4, pageForm);
                    schemePageModel4.addAttribute("schemeForm", pageForm);
                }
                schemePageModel4.addAttribute("editing", true);
                schemePageModel4.addAttribute("readOnly", schemePageReadOnly4);
                schemePageModel4.addAttribute("selectedSkill", schemePageSkill4);
                schemePageModel4.addAttribute(
                        "bands", scoring.bandViews(schemePageId4, schemePageSkill4));
                Object bandErrors =
                        schemePageModel4.getAttribute(BindingResult.MODEL_KEY_PREFIX + "bandForm");
                schemePageModel4.addAttribute(
                        "bandFormHasErrors",
                        bandErrors instanceof BindingResult bandResult && bandResult.hasErrors());
                if (!schemePageModel4.containsAttribute("scaleRuleForm"))
                    schemePageModel4.addAttribute(
                            "scaleRuleForm", scoring.scaleRule(schemePageId4, schemePageSkill4));
                if (!schemePageModel4.containsAttribute("bandForm")) {
                    ScoreBandDTO bandForm = new ScoreBandDTO();
                    if (schemePageBandId4 != null) {
                        ScoringViews.BandView band = scoring.band(schemePageId4, schemePageBandId4);
                        bandForm.setId(band.id());
                        bandForm.setSkill(band.skill());
                        bandForm.setCefrLevel(band.cefrLevel());
                        bandForm.setMinScore(band.minScore());
                        bandForm.setMaxScore(band.maxScore());
                    } else bandForm.setSkill(schemePageSkill4);
                    schemePageModel4.addAttribute("bandForm", bandForm);
                }
            }
            return "admin/settings/scheme/form";
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
            @Valid @ModelAttribute("bandForm") ScoreBandDTO form,
            BindingResult result,
            @RequestParam(defaultValue = "Reading") String skill,
            Model model,
            RedirectAttributes redirect) {
        if (result.hasErrors()) {
            {
                int schemePageId5 = schemeId;
                String schemePageSkill5 = skill;
                Integer schemePageBandId5 = form.getId();
                boolean schemePageReadOnly5 = false;
                Model schemePageModel5 = model;
                schemePageSkill5 =
                        switch (schemePageSkill5
                                .toUpperCase(Locale.ROOT)
                                .replaceAll("[^A-Z0-9]+", "_")) {
                            case "GRAMMAR_VOCABULARY" -> "Grammar & Vocabulary";
                            case "READING" -> "Reading";
                            case "LISTENING" -> "Listening";
                            case "WRITING" -> "Writing";
                            case "SPEAKING" -> "Speaking";
                            default -> throw new BusinessRuleException("Kỹ năng không hợp lệ");
                        };
                ScoringViews.SchemeView value = scoring.scheme(schemePageId5);
                if (value.scopeSkill() != null)
                    schemePageSkill5 =
                            SKILL_LABELS.getOrDefault(value.scopeSkill(), value.scopeSkill());
                {
                    schemePageModel5.addAttribute("certificates", scoring.certificateViews());
                    schemePageModel5.addAttribute("examTypes", EXAM_TYPES);
                    schemePageModel5.addAttribute("scoringLevels", CefrLevel.codes());
                    schemePageModel5.addAttribute(
                            "fullMockScoringLevels", CefrLevel.scoringCodes());
                    schemePageModel5.addAttribute("skillCodes", Skill.codes());
                    schemePageModel5.addAttribute("examTypeLabels", EXAM_TYPE_LABELS);
                    schemePageModel5.addAttribute("skillLabels", SKILL_LABELS);
                    schemePageModel5.addAttribute(
                            "skills",
                            List.of(
                                    "Grammar & Vocabulary",
                                    "Reading",
                                    "Listening",
                                    "Writing",
                                    "Speaking"));
                }
                if (value.scopeSkill() != null)
                    schemePageModel5.addAttribute("skills", List.of(schemePageSkill5));
                schemePageModel5.addAttribute("scheme", value);
                if (!schemePageModel5.containsAttribute("schemeForm")) {
                    ScoringSchemeDTO pageForm =
                            new ScoringSchemeDTO(
                                    value.id(),
                                    value.certificateId(),
                                    null,
                                    value.name(),
                                    value.version(),
                                    value.status(),
                                    value.examType(),
                                    value.scopeSkill(),
                                    value.effectiveFrom(),
                                    null);
                    scoring.populateScoreConfiguration(schemePageId5, pageForm);
                    schemePageModel5.addAttribute("schemeForm", pageForm);
                }
                schemePageModel5.addAttribute("editing", true);
                schemePageModel5.addAttribute("readOnly", schemePageReadOnly5);
                schemePageModel5.addAttribute("selectedSkill", schemePageSkill5);
                schemePageModel5.addAttribute(
                        "bands", scoring.bandViews(schemePageId5, schemePageSkill5));
                Object bandErrors =
                        schemePageModel5.getAttribute(BindingResult.MODEL_KEY_PREFIX + "bandForm");
                schemePageModel5.addAttribute(
                        "bandFormHasErrors",
                        bandErrors instanceof BindingResult bandResult && bandResult.hasErrors());
                if (!schemePageModel5.containsAttribute("scaleRuleForm"))
                    schemePageModel5.addAttribute(
                            "scaleRuleForm", scoring.scaleRule(schemePageId5, schemePageSkill5));
                if (!schemePageModel5.containsAttribute("bandForm")) {
                    ScoreBandDTO bandForm = new ScoreBandDTO();
                    if (schemePageBandId5 != null) {
                        ScoringViews.BandView band = scoring.band(schemePageId5, schemePageBandId5);
                        bandForm.setId(band.id());
                        bandForm.setSkill(band.skill());
                        bandForm.setCefrLevel(band.cefrLevel());
                        bandForm.setMinScore(band.minScore());
                        bandForm.setMaxScore(band.maxScore());
                    } else bandForm.setSkill(schemePageSkill5);
                    schemePageModel5.addAttribute("bandForm", bandForm);
                }
            }
            return "admin/settings/scheme/form";
        }
        try {
            scoring.saveBand(principal.getName(), schemeId, form.getId(), form);
            redirect.addFlashAttribute("successMessage", "Đã lưu khoảng điểm.");
            return "redirect:/admin/scoring-schemes/" + schemeId + "?skill=" + skill;
        } catch (BusinessRuleException exception) {
            result.reject("band.invalid", exception.getMessage());
            {
                int schemePageId6 = schemeId;
                String schemePageSkill6 = skill;
                Integer schemePageBandId6 = form.getId();
                boolean schemePageReadOnly6 = false;
                Model schemePageModel6 = model;
                schemePageSkill6 =
                        switch (schemePageSkill6
                                .toUpperCase(Locale.ROOT)
                                .replaceAll("[^A-Z0-9]+", "_")) {
                            case "GRAMMAR_VOCABULARY" -> "Grammar & Vocabulary";
                            case "READING" -> "Reading";
                            case "LISTENING" -> "Listening";
                            case "WRITING" -> "Writing";
                            case "SPEAKING" -> "Speaking";
                            default -> throw new BusinessRuleException("Kỹ năng không hợp lệ");
                        };
                ScoringViews.SchemeView value = scoring.scheme(schemePageId6);
                if (value.scopeSkill() != null)
                    schemePageSkill6 =
                            SKILL_LABELS.getOrDefault(value.scopeSkill(), value.scopeSkill());
                {
                    schemePageModel6.addAttribute("certificates", scoring.certificateViews());
                    schemePageModel6.addAttribute("examTypes", EXAM_TYPES);
                    schemePageModel6.addAttribute("scoringLevels", CefrLevel.codes());
                    schemePageModel6.addAttribute(
                            "fullMockScoringLevels", CefrLevel.scoringCodes());
                    schemePageModel6.addAttribute("skillCodes", Skill.codes());
                    schemePageModel6.addAttribute("examTypeLabels", EXAM_TYPE_LABELS);
                    schemePageModel6.addAttribute("skillLabels", SKILL_LABELS);
                    schemePageModel6.addAttribute(
                            "skills",
                            List.of(
                                    "Grammar & Vocabulary",
                                    "Reading",
                                    "Listening",
                                    "Writing",
                                    "Speaking"));
                }
                if (value.scopeSkill() != null)
                    schemePageModel6.addAttribute("skills", List.of(schemePageSkill6));
                schemePageModel6.addAttribute("scheme", value);
                if (!schemePageModel6.containsAttribute("schemeForm")) {
                    ScoringSchemeDTO pageForm =
                            new ScoringSchemeDTO(
                                    value.id(),
                                    value.certificateId(),
                                    null,
                                    value.name(),
                                    value.version(),
                                    value.status(),
                                    value.examType(),
                                    value.scopeSkill(),
                                    value.effectiveFrom(),
                                    null);
                    scoring.populateScoreConfiguration(schemePageId6, pageForm);
                    schemePageModel6.addAttribute("schemeForm", pageForm);
                }
                schemePageModel6.addAttribute("editing", true);
                schemePageModel6.addAttribute("readOnly", schemePageReadOnly6);
                schemePageModel6.addAttribute("selectedSkill", schemePageSkill6);
                schemePageModel6.addAttribute(
                        "bands", scoring.bandViews(schemePageId6, schemePageSkill6));
                Object bandErrors =
                        schemePageModel6.getAttribute(BindingResult.MODEL_KEY_PREFIX + "bandForm");
                schemePageModel6.addAttribute(
                        "bandFormHasErrors",
                        bandErrors instanceof BindingResult bandResult && bandResult.hasErrors());
                if (!schemePageModel6.containsAttribute("scaleRuleForm"))
                    schemePageModel6.addAttribute(
                            "scaleRuleForm", scoring.scaleRule(schemePageId6, schemePageSkill6));
                if (!schemePageModel6.containsAttribute("bandForm")) {
                    ScoreBandDTO bandForm = new ScoreBandDTO();
                    if (schemePageBandId6 != null) {
                        ScoringViews.BandView band = scoring.band(schemePageId6, schemePageBandId6);
                        bandForm.setId(band.id());
                        bandForm.setSkill(band.skill());
                        bandForm.setCefrLevel(band.cefrLevel());
                        bandForm.setMinScore(band.minScore());
                        bandForm.setMaxScore(band.maxScore());
                    } else bandForm.setSkill(schemePageSkill6);
                    schemePageModel6.addAttribute("bandForm", bandForm);
                }
            }
            return "admin/settings/scheme/form";
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

    @PostMapping("/admin/scoring-schemes/{schemeId}/scale-rule")
    public String saveScaleRule(
            @PathVariable int schemeId,
            @RequestParam String skill,
            Principal principal,
            @Valid @ModelAttribute("scaleRuleForm") ScoringScaleRuleDTO form,
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
        {
            int schemePageId7 = schemeId;
            String schemePageSkill7 = skill;
            Integer schemePageBandId7 = null;
            boolean schemePageReadOnly7 = false;
            Model schemePageModel7 = model;
            schemePageSkill7 =
                    switch (schemePageSkill7
                            .toUpperCase(Locale.ROOT)
                            .replaceAll("[^A-Z0-9]+", "_")) {
                        case "GRAMMAR_VOCABULARY" -> "Grammar & Vocabulary";
                        case "READING" -> "Reading";
                        case "LISTENING" -> "Listening";
                        case "WRITING" -> "Writing";
                        case "SPEAKING" -> "Speaking";
                        default -> throw new BusinessRuleException("Kỹ năng không hợp lệ");
                    };
            ScoringViews.SchemeView value = scoring.scheme(schemePageId7);
            if (value.scopeSkill() != null)
                schemePageSkill7 =
                        SKILL_LABELS.getOrDefault(value.scopeSkill(), value.scopeSkill());
            {
                schemePageModel7.addAttribute("certificates", scoring.certificateViews());
                schemePageModel7.addAttribute("examTypes", EXAM_TYPES);
                schemePageModel7.addAttribute("scoringLevels", CefrLevel.codes());
                schemePageModel7.addAttribute("fullMockScoringLevels", CefrLevel.scoringCodes());
                schemePageModel7.addAttribute("skillCodes", Skill.codes());
                schemePageModel7.addAttribute("examTypeLabels", EXAM_TYPE_LABELS);
                schemePageModel7.addAttribute("skillLabels", SKILL_LABELS);
                schemePageModel7.addAttribute(
                        "skills",
                        List.of(
                                "Grammar & Vocabulary",
                                "Reading",
                                "Listening",
                                "Writing",
                                "Speaking"));
            }
            if (value.scopeSkill() != null)
                schemePageModel7.addAttribute("skills", List.of(schemePageSkill7));
            schemePageModel7.addAttribute("scheme", value);
            if (!schemePageModel7.containsAttribute("schemeForm")) {
                ScoringSchemeDTO pageForm =
                        new ScoringSchemeDTO(
                                value.id(),
                                value.certificateId(),
                                null,
                                value.name(),
                                value.version(),
                                value.status(),
                                value.examType(),
                                value.scopeSkill(),
                                value.effectiveFrom(),
                                null);
                scoring.populateScoreConfiguration(schemePageId7, pageForm);
                schemePageModel7.addAttribute("schemeForm", pageForm);
            }
            schemePageModel7.addAttribute("editing", true);
            schemePageModel7.addAttribute("readOnly", schemePageReadOnly7);
            schemePageModel7.addAttribute("selectedSkill", schemePageSkill7);
            schemePageModel7.addAttribute(
                    "bands", scoring.bandViews(schemePageId7, schemePageSkill7));
            Object bandErrors =
                    schemePageModel7.getAttribute(BindingResult.MODEL_KEY_PREFIX + "bandForm");
            schemePageModel7.addAttribute(
                    "bandFormHasErrors",
                    bandErrors instanceof BindingResult bandResult && bandResult.hasErrors());
            if (!schemePageModel7.containsAttribute("scaleRuleForm"))
                schemePageModel7.addAttribute(
                        "scaleRuleForm", scoring.scaleRule(schemePageId7, schemePageSkill7));
            if (!schemePageModel7.containsAttribute("bandForm")) {
                ScoreBandDTO bandForm = new ScoreBandDTO();
                if (schemePageBandId7 != null) {
                    ScoringViews.BandView band = scoring.band(schemePageId7, schemePageBandId7);
                    bandForm.setId(band.id());
                    bandForm.setSkill(band.skill());
                    bandForm.setCefrLevel(band.cefrLevel());
                    bandForm.setMinScore(band.minScore());
                    bandForm.setMaxScore(band.maxScore());
                } else bandForm.setSkill(schemePageSkill7);
                schemePageModel7.addAttribute("bandForm", bandForm);
            }
        }
        return "admin/settings/scheme/form";
    }
}
