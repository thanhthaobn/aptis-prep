package com.aptisprep.service.scoring;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.aptisprep.entity.Certificate;
import com.aptisprep.entity.ScoreBand;
import com.aptisprep.entity.ScoringScaleRule;
import com.aptisprep.entity.ScoringScheme;
import com.aptisprep.mvc.common.enums.RecordStatus;
import com.aptisprep.mvc.common.exception.BusinessRuleException;
import com.aptisprep.repository.ScoreBandRepository;
import com.aptisprep.repository.ScoringScaleRuleRepository;
import com.aptisprep.repository.ScoringSchemeRepository;
import com.aptisprep.service.contract.ScoringPolicyContract.SkillScore;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScoringPolicyServiceTests {
    @Mock ScoringSchemeRepository schemes;
    @Mock ScoreBandRepository bands;
    @Mock ScoringScaleRuleRepository scaleRules;

    @Test
    void roundsRawScoreAndMapsClosedBandBoundary() {
        when(schemes.findById(1)).thenReturn(Optional.of(activeScheme()));
        ScoreBand low = new ScoreBand();
        low.setMinScore(new BigDecimal("0.00"));
        low.setMaxScore(new BigDecimal("9.99"));
        low.setCefrLevel("A1");
        ScoreBand high = new ScoreBand();
        high.setMinScore(new BigDecimal("10.00"));
        high.setMaxScore(new BigDecimal("50.00"));
        high.setCefrLevel("A2");
        when(bands.findBySchemeIdAndSkillOrderByMinScoreAsc(1, "READING"))
                .thenReturn(List.of(low, high));
        when(scaleRules.findBySchemeIdAndSkillIgnoreCase(1, "READING"))
                .thenReturn(Optional.of(rule(50, 50)));
        ScoringPolicyService service =
                new com.aptisprep.service.scoring.impl.ScoringPolicyServiceImpl(
                        schemes, bands, scaleRules);

        assertEquals(new BigDecimal("10.00"), service.scale(1, "Reading", new BigDecimal("9.995")));
        assertEquals("A2", service.estimatedLevel(1, "READING", new BigDecimal("10.00")));
    }

    @Test
    void missingGrammarBandsRemainUnassessed() {
        when(schemes.findById(1)).thenReturn(Optional.of(activeScheme()));
        when(bands.findBySchemeIdAndSkillOrderByMinScoreAsc(1, "GRAMMAR_VOCABULARY"))
                .thenReturn(List.of());
        when(scaleRules.findBySchemeIdAndSkillIgnoreCase(1, "GRAMMAR_VOCABULARY"))
                .thenReturn(Optional.of(rule(50, 50)));
        assertNull(
                new com.aptisprep.service.scoring.impl.ScoringPolicyServiceImpl(
                                schemes, bands, scaleRules)
                        .estimatedLevel(1, "GRAMMAR & VOCABULARY", new BigDecimal("30")));
    }

    @Test
    void rejectsGapAndTailOutsideCeFrCoverageBeforeExamStarts() {
        when(schemes.findById(1)).thenReturn(Optional.of(activeScheme()));
        when(scaleRules.findBySchemeIdAndSkillIgnoreCase(1, "READING"))
                .thenReturn(Optional.of(rule(50, 50)));
        ScoreBand first = band("0.00", "24.99", "A1");
        ScoreBand gap = band("25.01", "49.99", "A2");
        when(bands.findBySchemeIdAndSkillOrderByMinScoreAsc(1, "READING"))
                .thenReturn(List.of(first, gap));
        ScoringPolicyService service =
                new com.aptisprep.service.scoring.impl.ScoringPolicyServiceImpl(
                        schemes, bands, scaleRules);

        assertThrows(BusinessRuleException.class, () -> service.validatePolicy(1, "READING"));
    }

    @Test
    void rejectsBandSetThatDoesNotReachScaleMaximum() {
        when(schemes.findById(1)).thenReturn(Optional.of(activeScheme()));
        when(scaleRules.findBySchemeIdAndSkillIgnoreCase(1, "READING"))
                .thenReturn(Optional.of(rule(50, 50)));
        when(bands.findBySchemeIdAndSkillOrderByMinScoreAsc(1, "READING"))
                .thenReturn(List.of(band("0.00", "49.99", "A1")));

        assertThrows(
                BusinessRuleException.class,
                () ->
                        new com.aptisprep.service.scoring.impl.ScoringPolicyServiceImpl(
                                        schemes, bands, scaleRules)
                                .validatePolicy(1, "READING"));
    }

    @Test
    void rejectsRawOutsideInternalScale() {
        when(schemes.findById(1)).thenReturn(Optional.of(activeScheme()));
        ScoringScaleRule readingRule = rule(40, 50);
        when(scaleRules.findBySchemeIdAndSkillIgnoreCase(1, "READING"))
                .thenReturn(Optional.of(readingRule));
        ScoringPolicyService service =
                new com.aptisprep.service.scoring.impl.ScoringPolicyServiceImpl(
                        schemes, bands, scaleRules);
        assertThrows(
                BusinessRuleException.class,
                () -> service.scale(1, "READING", new BigDecimal("50.01")));
    }

    @Test
    void convertsObjectiveRawUsingTheSkillSpecificRange() {
        when(schemes.findById(1)).thenReturn(Optional.of(activeScheme()));
        when(scaleRules.findBySchemeIdAndSkillIgnoreCase(1, "READING"))
                .thenReturn(Optional.of(rule(40, 50)));
        ScoringPolicyService service =
                new com.aptisprep.service.scoring.impl.ScoringPolicyServiceImpl(
                        schemes, bands, scaleRules);

        assertEquals(new BigDecimal("30.00"), service.raw(1, "READING", 3, 4));
        assertEquals(new BigDecimal("37.50"), service.scale(1, "READING", new BigDecimal("30.00")));
    }

    @Test
    void fullMockNormalizesFourScalesAndUsesCoreOnlyAtTheNextBoundary() {
        when(schemes.findById(1)).thenReturn(Optional.of(activeScheme()));
        when(scaleRules.findBySchemeIdAndSkillIgnoreCase(eq(1), anyString()))
                .thenAnswer(
                        call ->
                                Optional.of(
                                        rule(
                                                50,
                                                "READING".equals(call.getArgument(1)) ? 100 : 50)));
        when(bands.findBySchemeIdAndSkillOrderByMinScoreAsc(1, "READING"))
                .thenReturn(List.of(band("60", "79.99", "B2")));
        when(bands.findBySchemeIdAndSkillOrderByMinScoreAsc(1, "LISTENING"))
                .thenReturn(List.of(band("40", "50", "B2")));
        ScoringPolicyService service =
                new com.aptisprep.service.scoring.impl.ScoringPolicyServiceImpl(
                        schemes, bands, scaleRules);
        var result =
                service.fullMockScore(
                        1,
                        List.of(
                                new SkillScore("READING", new BigDecimal("59"), "B1"),
                                new SkillScore("LISTENING", new BigDecimal("35"), "B1"),
                                new SkillScore("WRITING", new BigDecimal("30"), "B1"),
                                new SkillScore("SPEAKING", new BigDecimal("45"), "C1"),
                                new SkillScore("GRAMMAR_VOCABULARY", new BigDecimal("50"), "C1")));

        assertEquals(new BigDecimal("139.50"), result.total());
        assertEquals("B2", result.cefrLevel());
        assertEquals("B2", result.skills().get(0).cefrLevel());
        assertEquals("B1", result.skills().get(1).cefrLevel());
        assertNull(result.skills().get(4).cefrLevel());
    }

    @Test
    void fullMockRoundsCefrMidpointUpAsInAptisScoringSystem() {
        when(schemes.findById(1)).thenReturn(Optional.of(activeScheme()));
        when(scaleRules.findBySchemeIdAndSkillIgnoreCase(eq(1), anyString()))
                .thenReturn(Optional.of(rule(50, 50)));
        ScoringPolicyService service =
                new com.aptisprep.service.scoring.impl.ScoringPolicyServiceImpl(
                        schemes, bands, scaleRules);

        var result =
                service.fullMockScore(
                        1,
                        List.of(
                                new SkillScore("READING", BigDecimal.valueOf(40), "B2"),
                                new SkillScore("LISTENING", BigDecimal.valueOf(36), "B2"),
                                new SkillScore("WRITING", BigDecimal.valueOf(30), "B1"),
                                new SkillScore("SPEAKING", BigDecimal.valueOf(30), "B1"),
                                new SkillScore(
                                        "GRAMMAR_VOCABULARY", BigDecimal.valueOf(30), "B1")));

        assertEquals("B2", result.cefrLevel());
    }

    @Test
    void fullMockRetainsBelowA1AndIncludesItInOverallLevel() {
        when(schemes.findById(1)).thenReturn(Optional.of(activeScheme()));
        when(scaleRules.findBySchemeIdAndSkillIgnoreCase(eq(1), anyString()))
                .thenReturn(Optional.of(rule(50, 50)));
        var service =
                new com.aptisprep.service.scoring.impl.ScoringPolicyServiceImpl(
                        schemes, bands, scaleRules);
        var result =
                service.fullMockScore(
                        1,
                        List.of(
                                new SkillScore("READING", new BigDecimal("7.99"), "A0"),
                                new SkillScore("LISTENING", new BigDecimal("7.99"), "A0"),
                                new SkillScore("WRITING", new BigDecimal("5.99"), "A0"),
                                new SkillScore("SPEAKING", new BigDecimal("3.99"), "A0"),
                                new SkillScore(
                                        "GRAMMAR_VOCABULARY", BigDecimal.valueOf(50), "C1")));
        assertEquals("A0", result.cefrLevel());
        assertEquals("A0", result.skills().getFirst().cefrLevel());
        assertEquals(new BigDecimal("25.96"), result.total());
    }

    private ScoringScaleRule rule(int rawMax, int scaleMax) {
        ScoringScaleRule rule = new ScoringScaleRule();
        rule.setRawMin(BigDecimal.ZERO);
        rule.setRawMax(BigDecimal.valueOf(rawMax));
        rule.setScaleMin(BigDecimal.ZERO);
        rule.setScaleMax(BigDecimal.valueOf(scaleMax));
        return rule;
    }

    private ScoreBand band(String min, String max, String cefr) {
        ScoreBand band = new ScoreBand();
        band.setMinScore(new BigDecimal(min));
        band.setMaxScore(new BigDecimal(max));
        band.setCefrLevel(cefr);
        return band;
    }

    private ScoringScheme activeScheme() {
        Certificate certificate = new Certificate();
        certificate.setStatus(RecordStatus.ACTIVE);
        ScoringScheme scheme = new ScoringScheme();
        scheme.setCertificate(certificate);
        scheme.setStatus(RecordStatus.ACTIVE);
        return scheme;
    }
}
