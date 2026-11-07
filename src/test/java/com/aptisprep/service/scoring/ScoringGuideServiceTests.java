package com.aptisprep.service.scoring;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aptisprep.entity.*;
import com.aptisprep.repository.*;
import com.aptisprep.service.contract.IntegrationModels.*;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

class ScoringGuideServiceTests {
    @Test
    void resultUsesHistoricalSchemeAndHighlightsInclusiveBoundaryOnNonFiftyScale() {
        var schemes = mock(ScoringSchemeRepository.class);
        var rules = mock(ScoringScaleRuleRepository.class);
        var bands = mock(ScoreBandRepository.class);
        var snapshots = mock(ExamAttemptSnapshotRepository.class);
        var exams = mock(MockExamRepository.class);
        var service = new ScoringGuideService(schemes, rules, bands, exams, snapshots);
        var snapshot = new ExamAttemptSnapshotEntity();
        snapshot.setScoringSchemeId(7);
        snapshot.setSkill("READING");
        when(snapshots.findByAttemptId(42)).thenReturn(Optional.of(snapshot));
        var scheme = new ScoringScheme();
        scheme.setVersion("historical-v1");
        when(schemes.findById(7)).thenReturn(Optional.of(scheme));
        var rule = new ScoringScaleRule();
        rule.setSkill("READING");
        rule.setScaleMin(BigDecimal.ZERO);
        rule.setScaleMax(new BigDecimal("100"));
        when(rules.findBySchemeId(7)).thenReturn(List.of(rule));
        var band = new ScoreBand();
        band.setMinScore(new BigDecimal("60"));
        band.setMaxScore(new BigDecimal("79.99"));
        band.setCefrLevel("B1");
        when(bands.findBySchemeIdAndSkillOrderByMinScoreAsc(7, "READING"))
                .thenReturn(List.of(band));
        var result =
                new AttemptResult(
                        42,
                        2,
                        9,
                        "PLACEMENT",
                        99,
                        "COMPLETED",
                        List.of(
                                new SkillResult(
                                        "READING",
                                        BigDecimal.TEN,
                                        new BigDecimal("60"),
                                        "B1",
                                        "COMPLETED",
                                        null)));
        var guide = service.forResult(result).getFirst();
        assertThat(guide.version()).isEqualTo("historical-v1");
        assertThat(guide.max()).isEqualByComparingTo("100");
        assertThat(guide.bands().getFirst().attained()).isTrue();
        verify(schemes, never()).findById(99);
        verifyNoInteractions(exams);
    }
}
