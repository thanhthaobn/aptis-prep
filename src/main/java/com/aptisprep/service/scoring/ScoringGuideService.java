package com.aptisprep.service.scoring;

import com.aptisprep.repository.*;
import com.aptisprep.service.contract.IntegrationModels.AttemptResult;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public score ranges only; never exposes answer keys or grading context. */
@Service
@Transactional(readOnly = true)
public class ScoringGuideService {
    private final ScoringSchemeRepository schemes;
    private final ScoringScaleRuleRepository rules;
    private final ScoreBandRepository bands;
    private final MockExamRepository exams;
    private final ExamAttemptSnapshotRepository snapshots;

    public ScoringGuideService(
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

    public record Band(BigDecimal min, BigDecimal max, String level, boolean attained) {}

    public record Guide(
            String skill, String version, BigDecimal min, BigDecimal max, List<Band> bands) {}

    public List<Guide> forExam(Integer examId) {
        return exams.findById(examId)
                .map(e -> guides(e.getScoringSchemeId(), e.getSkill(), Map.of()))
                .orElse(List.of());
    }

    /** Called only after the attempt contract has checked ownership. Uses the original scheme. */
    public List<Guide> forResult(AttemptResult result) {
        var snapshot = snapshots.findByAttemptId(result.attemptId());
        Integer scheme = snapshot.map(s -> s.getScoringSchemeId()).orElse(result.scoringSchemeId());
        Map<String, BigDecimal> scores = new HashMap<>();
        result.skills()
                .forEach(
                        s -> {
                            if (s.scaleScore() != null) scores.put(s.skill(), s.scaleScore());
                        });
        Set<String> skills = new HashSet<>();
        result.skills().forEach(s -> skills.add(s.skill()));
        String snapshotSkill = snapshot.map(s -> s.getSkill()).orElse(null);
        if (skills.isEmpty() && snapshotSkill != null) return guides(scheme, snapshotSkill, scores);
        return guides(scheme, null, scores).stream()
                .filter(g -> skills.contains(g.skill()))
                .toList();
    }

    private List<Guide> guides(Integer schemeId, String skill, Map<String, BigDecimal> scores) {
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
                                        bands
                                                .findBySchemeIdAndSkillOrderByMinScoreAsc(
                                                        schemeId, r.getSkill())
                                                .stream()
                                                .map(
                                                        b ->
                                                                new Band(
                                                                        b.getMinScore(),
                                                                        b.getMaxScore(),
                                                                        b.getCefrLevel(),
                                                                        scores.containsKey(
                                                                                        r
                                                                                                .getSkill())
                                                                                && scores.get(
                                                                                                        r
                                                                                                                .getSkill())
                                                                                                .compareTo(
                                                                                                        b
                                                                                                                .getMinScore())
                                                                                        >= 0
                                                                                && scores.get(
                                                                                                        r
                                                                                                                .getSkill())
                                                                                                .compareTo(
                                                                                                        b
                                                                                                                .getMaxScore())
                                                                                        <= 0))
                                                .toList()))
                .toList();
    }
}
