package com.aptisprep.service.scoring;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aptisprep.entity.*;
import com.aptisprep.presentation.AttemptResultDTO;
import com.aptisprep.presentation.SkillResultDTO;
import com.aptisprep.repository.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class ScoringGuideServiceTests {
    @Test
    void resultUsesHistoricalSchemeAndHighlightsInclusiveBoundaryOnNonFiftyScale() {
        var schemes = mock(ScoringSchemeRepository.class);
        var rules = mock(ScoringScaleRuleRepository.class);
        var bands = mock(ScoreBandRepository.class);
        var examCatalogService = mock(com.aptisprep.service.admin.ExamCatalogService.class);
        var examAttemptService = mock(com.aptisprep.service.exam.ExamAttemptService.class);
        var service =
                new com.aptisprep.service.scoring.impl.ScoringGuideServiceImpl(
                        schemes, rules, bands, examCatalogService, examAttemptService);
        when(examAttemptService.findSnapshotSchemeInfo(42))
                .thenReturn(
                        Optional.of(
                                new com.aptisprep.service.exam.ExamAttemptService
                                        .SnapshotSchemeInfo(7, "READING")));
        var scheme = new ScoringScheme();
        scheme.setCertificate(new com.aptisprep.entity.Certificate());
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
                new AttemptResultDTO(
                        42,
                        2,
                        9,
                        "PLACEMENT",
                        99,
                        "COMPLETED",
                        List.of(
                                new SkillResultDTO(
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
        verifyNoInteractions(examCatalogService);
    }

    @Test
    void scaleZeroGeneratesBelowA1BandAsAttainedWhileA1IsNotAttained() {
        var schemes = mock(ScoringSchemeRepository.class);
        var rules = mock(ScoringScaleRuleRepository.class);
        var bands = mock(ScoreBandRepository.class);
        var examCatalogService = mock(com.aptisprep.service.admin.ExamCatalogService.class);
        var examAttemptService = mock(com.aptisprep.service.exam.ExamAttemptService.class);
        var service =
                new com.aptisprep.service.scoring.impl.ScoringGuideServiceImpl(
                        schemes, rules, bands, examCatalogService, examAttemptService);
        when(examAttemptService.findSnapshotSchemeInfo(101)).thenReturn(Optional.empty());

        var scheme = new ScoringScheme();
        scheme.setCertificate(new Certificate());
        scheme.setVersion("v1");
        when(schemes.findById(1)).thenReturn(Optional.of(scheme));

        var rule = new ScoringScaleRule();
        rule.setSkill("LISTENING");
        rule.setScaleMin(BigDecimal.ZERO);
        rule.setScaleMax(new BigDecimal("50"));
        when(rules.findBySchemeId(1)).thenReturn(List.of(rule));

        var a1Band = new ScoreBand();
        a1Band.setMinScore(BigDecimal.ZERO);
        a1Band.setMaxScore(new BigDecimal("20"));
        a1Band.setCefrLevel("A1");
        when(bands.findBySchemeIdAndSkillOrderByMinScoreAsc(1, "LISTENING"))
                .thenReturn(List.of(a1Band));

        var result =
                new AttemptResultDTO(
                        101,
                        1,
                        1,
                        "PRACTICE",
                        1,
                        "COMPLETED",
                        List.of(
                                new SkillResultDTO(
                                        "LISTENING",
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO,
                                        "A0",
                                        "COMPLETED",
                                        null)));

        var guide = service.forResult(result).getFirst();
        var guideBands = guide.bands();
        assertThat(guideBands).hasSize(2);

        var a0 = guideBands.get(0);
        assertThat(a0.level()).isEqualTo("A0");
        assertThat(a0.min()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(a0.max()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(a0.attained()).isTrue();

        var a1 = guideBands.get(1);
        assertThat(a1.level()).isEqualTo("A1");
        assertThat(a1.min()).isEqualByComparingTo(new BigDecimal("0.01"));
        assertThat(a1.max()).isEqualByComparingTo(new BigDecimal("20"));
        assertThat(a1.attained()).isFalse();
    }

    @Test
    void levelA1AttainsA1AndNotGeneratedBelowA1Band() {
        var schemes = mock(ScoringSchemeRepository.class);
        var rules = mock(ScoringScaleRuleRepository.class);
        var bands = mock(ScoreBandRepository.class);
        var examCatalogService = mock(com.aptisprep.service.admin.ExamCatalogService.class);
        var examAttemptService = mock(com.aptisprep.service.exam.ExamAttemptService.class);
        var service =
                new com.aptisprep.service.scoring.impl.ScoringGuideServiceImpl(
                        schemes, rules, bands, examCatalogService, examAttemptService);
        when(examAttemptService.findSnapshotSchemeInfo(102)).thenReturn(Optional.empty());

        var scheme = new ScoringScheme();
        scheme.setCertificate(new Certificate());
        scheme.setVersion("v1");
        when(schemes.findById(1)).thenReturn(Optional.of(scheme));

        var rule = new ScoringScaleRule();
        rule.setSkill("LISTENING");
        rule.setScaleMin(BigDecimal.ZERO);
        rule.setScaleMax(new BigDecimal("50"));
        when(rules.findBySchemeId(1)).thenReturn(List.of(rule));

        var a1Band = new ScoreBand();
        a1Band.setMinScore(BigDecimal.ZERO);
        a1Band.setMaxScore(new BigDecimal("20"));
        a1Band.setCefrLevel("A1");
        when(bands.findBySchemeIdAndSkillOrderByMinScoreAsc(1, "LISTENING"))
                .thenReturn(List.of(a1Band));

        var result =
                new AttemptResultDTO(
                        102,
                        1,
                        1,
                        "PRACTICE",
                        1,
                        "COMPLETED",
                        List.of(
                                new SkillResultDTO(
                                        "LISTENING",
                                        new BigDecimal("5"),
                                        new BigDecimal("15"),
                                        "A1",
                                        "COMPLETED",
                                        null)));

        var guide = service.forResult(result).getFirst();
        var guideBands = guide.bands();
        assertThat(guideBands).hasSize(2);

        var a0 = guideBands.get(0);
        assertThat(a0.level()).isEqualTo("A0");
        assertThat(a0.attained()).isFalse();

        var a1 = guideBands.get(1);
        assertThat(a1.level()).isEqualTo("A1");
        assertThat(a1.attained()).isTrue();
    }

    @Test
    void explicitA0BandInSchemeMaintainsCorrectAttainedFlags() {
        var schemes = mock(ScoringSchemeRepository.class);
        var rules = mock(ScoringScaleRuleRepository.class);
        var bands = mock(ScoreBandRepository.class);
        var examCatalogService = mock(com.aptisprep.service.admin.ExamCatalogService.class);
        var examAttemptService = mock(com.aptisprep.service.exam.ExamAttemptService.class);
        var service =
                new com.aptisprep.service.scoring.impl.ScoringGuideServiceImpl(
                        schemes, rules, bands, examCatalogService, examAttemptService);
        when(examAttemptService.findSnapshotSchemeInfo(103)).thenReturn(Optional.empty());

        var scheme = new ScoringScheme();
        scheme.setCertificate(new Certificate());
        scheme.setVersion("v1");
        when(schemes.findById(1)).thenReturn(Optional.of(scheme));

        var rule = new ScoringScaleRule();
        rule.setSkill("SPEAKING");
        rule.setScaleMin(BigDecimal.ZERO);
        rule.setScaleMax(new BigDecimal("50"));
        when(rules.findBySchemeId(1)).thenReturn(List.of(rule));

        var a0Band = new ScoreBand();
        a0Band.setMinScore(BigDecimal.ZERO);
        a0Band.setMaxScore(new BigDecimal("9.99"));
        a0Band.setCefrLevel("A0");

        var a1Band = new ScoreBand();
        a1Band.setMinScore(new BigDecimal("10"));
        a1Band.setMaxScore(new BigDecimal("19.99"));
        a1Band.setCefrLevel("A1");

        when(bands.findBySchemeIdAndSkillOrderByMinScoreAsc(1, "SPEAKING"))
                .thenReturn(List.of(a0Band, a1Band));

        var resultA0 =
                new AttemptResultDTO(
                        103,
                        1,
                        1,
                        "PRACTICE",
                        1,
                        "COMPLETED",
                        List.of(
                                new SkillResultDTO(
                                        "SPEAKING",
                                        BigDecimal.ZERO,
                                        new BigDecimal("5"),
                                        "A0",
                                        "COMPLETED",
                                        null)));

        var guideA0 = service.forResult(resultA0).getFirst();
        var guideBandsA0 = guideA0.bands();
        assertThat(guideBandsA0).hasSize(2);
        assertThat(guideBandsA0.get(0).level()).isEqualTo("A0");
        assertThat(guideBandsA0.get(0).attained()).isTrue();
        assertThat(guideBandsA0.get(1).level()).isEqualTo("A1");
        assertThat(guideBandsA0.get(1).attained()).isFalse();

        var resultA1 =
                new AttemptResultDTO(
                        103,
                        1,
                        1,
                        "PRACTICE",
                        1,
                        "COMPLETED",
                        List.of(
                                new SkillResultDTO(
                                        "SPEAKING",
                                        BigDecimal.TEN,
                                        new BigDecimal("15"),
                                        "A1",
                                        "COMPLETED",
                                        null)));

        var guideA1 = service.forResult(resultA1).getFirst();
        var guideBandsA1 = guideA1.bands();
        assertThat(guideBandsA1).hasSize(2);
        assertThat(guideBandsA1.get(0).level()).isEqualTo("A0");
        assertThat(guideBandsA1.get(0).attained()).isFalse();
        assertThat(guideBandsA1.get(1).level()).isEqualTo("A1");
        assertThat(guideBandsA1.get(1).attained()).isTrue();
    }
}
