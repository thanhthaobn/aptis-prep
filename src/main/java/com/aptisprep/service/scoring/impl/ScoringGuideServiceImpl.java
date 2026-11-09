package com.aptisprep.service.scoring.impl;

import com.aptisprep.service.scoring.ScoringGuideService;

import com.aptisprep.repository.*;
import com.aptisprep.service.contract.IntegrationModels.AttemptResult;
import com.aptisprep.service.contract.IntegrationModels.SkillResult;
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
    private final MockExamRepository exams;
    private final ExamAttemptSnapshotRepository snapshots;

    public ScoringGuideServiceImpl(
            ScoringSchemeRepository schemes,
            ScoringScaleRuleRepository rules,
            ScoreBandRepository bands,
            MockExamRepository exams,
            ExamAttemptSnapshotRepository snapshots) {
        this.schemes = schemes;
        this.rules = rules;
        this.bands = bands;
        this.exams = exams;
        this.snapshots = snapshots;
    }


    public ResultGuideView forResultView(AttemptResult result) {
        List<Guide> scoreGuides = forResult(result);
        Set<String> guidedSkills = new HashSet<>();
        scoreGuides.forEach(guide -> guidedSkills.add(guide.skill()));
        List<SkillResult> feedbackWithoutGuide =
                result.skills().stream()
                        .filter(skill -> !guidedSkills.contains(skill.skill()))
                        .filter(skill -> skill.feedback() != null && !skill.feedback().isBlank())
                        .toList();
        return new ResultGuideView(scoreGuides, feedbackWithoutGuide);
    }

    public List<Guide> forExam(Integer examId) {
        return exams.findById(examId)
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
    public List<Guide> forResult(AttemptResult result) {
        var snapshot = snapshots.findByAttemptId(result.attemptId());
        Integer scheme = snapshot.map(s -> s.getScoringSchemeId()).orElse(result.scoringSchemeId());
        Map<String, String> levels = new HashMap<>();
        result.skills()
                .forEach(
                        s -> {
                            if (s.estimatedLevel() != null)
                                levels.put(s.skill(), s.estimatedLevel());
                        });
        Set<String> skills = new HashSet<>();
        result.skills().forEach(s -> skills.add(s.skill()));
        String snapshotSkill = snapshot.map(s -> s.getSkill()).orElse(null);
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
                                                : bands
                                                        .findBySchemeIdAndSkillOrderByMinScoreAsc(
                                                                schemeId, r.getSkill())
                                                        .stream()
                                                        .map(
                                                                b ->
                                                                        new Band(
                                                                                b.getMinScore(),
                                                                                b.getMaxScore(),
                                                                                scheme.get()
                                                                                        .getCertificate()
                                                                                        .scoringLevel(
                                                                                                b
                                                                                                        .getCefrLevel()),
                                                                                scheme.get()
                                                                                        .getCertificate()
                                                                                        .scoringLevel(
                                                                                                b
                                                                                                        .getCefrLevel())
                                                                                        .equals(
                                                                                                levels
                                                                                                        .get(
                                                                                                                r
                                                                                                                        .getSkill()))))
                                                        .toList()))
                .toList();
    }
}


