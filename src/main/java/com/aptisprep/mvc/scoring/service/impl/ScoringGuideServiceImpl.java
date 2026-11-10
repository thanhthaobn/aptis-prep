package com.aptisprep.mvc.scoring.service.impl;

import com.aptisprep.mvc.catalog.service.ExamCatalogService;
import com.aptisprep.mvc.exam.dto.AttemptResultDTO;
import com.aptisprep.mvc.exam.dto.SkillResultDTO;
import com.aptisprep.mvc.exam.service.ExamAttemptService;
import com.aptisprep.mvc.exam.service.ExamAttemptService.SnapshotSchemeInfo;
import com.aptisprep.mvc.scoring.entity.*;
import com.aptisprep.mvc.scoring.repository.*;
import com.aptisprep.mvc.scoring.service.ScoringGuideService;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public score ranges only; never exposes answer keys or grading context. */
@Service
@Transactional(readOnly = true)
public class ScoringGuideServiceImpl implements ScoringGuideService {
    private final ScoringSchemeRepository schemes;
    private final ScoringScaleRuleRepository rules;
    private final ScoreBandRepository bands;
    private final ExamCatalogService examCatalogService;
    private final ExamAttemptService examAttemptService;

    public ScoringGuideServiceImpl(
            ScoringSchemeRepository schemes,
            ScoringScaleRuleRepository rules,
            ScoreBandRepository bands,
            ExamCatalogService examCatalogService,
            ExamAttemptService examAttemptService) {
        this.schemes = schemes;
        this.rules = rules;
        this.bands = bands;
        this.examCatalogService = examCatalogService;
        this.examAttemptService = examAttemptService;
    }

    public ResultGuideView forResultView(AttemptResultDTO result) {
        List<Guide> scoreGuides = forResult(result);
        Set<String> guidedSkills = new HashSet<>();
        scoreGuides.forEach(guide -> guidedSkills.add(guide.skill()));
        List<SkillResultDTO> feedbackWithoutGuide =
                result.skills().stream()
                        .filter(skill -> !guidedSkills.contains(skill.skill()))
                        .filter(skill -> skill.feedback() != null && !skill.feedback().isBlank())
                        .toList();
        return new ResultGuideView(scoreGuides, feedbackWithoutGuide);
    }

    public List<Guide> forExam(Integer examId) {
        return examCatalogService
                .findMockExamById(examId)
                .map(
                        e ->
                                guides(
                                        e.getScoringSchemeId(),
                                        e.getSkill(),
                                        Map.of(),
                                        "FULL_MOCK".equals(e.getSkill())))
                .orElse(List.of());
    }

    /** Called only after the attempt contract has checked ownership. Uses the original scheme. */
    public List<Guide> forResult(AttemptResultDTO result) {
        var snapshot = examAttemptService.findSnapshotSchemeInfo(result.attemptId());
        Integer scheme =
                snapshot.map(SnapshotSchemeInfo::scoringSchemeId).orElse(result.scoringSchemeId());
        Map<String, String> levels = new HashMap<>();
        result.skills()
                .forEach(
                        s -> {
                            if (s.estimatedLevel() != null)
                                levels.put(s.skill(), s.estimatedLevel());
                        });
        Set<String> skills = new HashSet<>();
        result.skills().forEach(s -> skills.add(s.skill()));
        String snapshotSkill = snapshot.map(SnapshotSchemeInfo::skill).orElse(null);
        if (skills.isEmpty() && snapshotSkill != null)
            return guides(scheme, snapshotSkill, levels, "FULL_MOCK".equals(snapshotSkill));
        return guides(
                        scheme,
                        null,
                        levels,
                        "FULL_MOCK".equals(snapshotSkill) || result.isCombined())
                .stream()
                .filter(g -> skills.contains(g.skill()))
                .toList();
    }

    private List<Guide> guides(
            Integer schemeId, String skill, Map<String, String> levels, boolean fullMock) {
        if (schemeId == null) return List.of();
        var scheme = schemes.findById(schemeId);
        if (scheme.isEmpty()) return List.of();
        return rules.findBySchemeId(schemeId).stream()
                .filter(
                        r ->
                                skill == null
                                        || "FULL_MOCK".equals(skill)
                                        || r.getSkill().equalsIgnoreCase(skill))
                .sorted(Comparator.comparing(r -> r.getSkill()))
                .map(
                        r ->
                                new Guide(
                                        r.getSkill(),
                                        scheme.get().getVersion(),
                                        r.getScaleMin(),
                                        r.getScaleMax(),
                                        fullMock && "GRAMMAR_VOCABULARY".equals(r.getSkill())
                                                ? List.of()
                                                : guideBands(
                                                        schemeId,
                                                        r.getSkill(),
                                                        scheme.get().getCertificate(),
                                                        levels)))
                .toList();
    }

    private List<Band> guideBands(
            Integer schemeId,
            String skill,
            com.aptisprep.mvc.scoring.entity.Certificate certificate,
            Map<String, String> levels) {
        List<Band> configured =
                bands.findBySchemeIdAndSkillOrderByMinScoreAsc(schemeId, skill).stream()
                        .map(
                                b ->
                                        new Band(
                                                b.getMinScore(),
                                                b.getMaxScore(),
                                                certificate.scoringLevel(b.getCefrLevel()),
                                                certificate
                                                        .scoringLevel(b.getCefrLevel())
                                                        .equals(levels.get(skill))))
                        .toList();
        boolean hasBelowA1 = configured.stream().anyMatch(b -> "A0".equals(b.level()));
        if (hasBelowA1 || configured.stream().noneMatch(b -> "A1".equals(b.level())))
            return configured;
        Band a1 = configured.stream().filter(b -> "A1".equals(b.level())).findFirst().orElseThrow();
        if (a1.min().signum() != 0) return configured;
        List<Band> adjusted = new ArrayList<>();
        adjusted.add(new Band(BigDecimal.ZERO, BigDecimal.ZERO, "A0", false));
        for (Band band : configured) {
            adjusted.add(
                    "A1".equals(band.level())
                            ? new Band(
                                    new BigDecimal("0.01"),
                                    band.max(),
                                    band.level(),
                                    band.attained())
                            : band);
        }
        return List.copyOf(adjusted);
    }
}
