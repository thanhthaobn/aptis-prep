package com.aptisprep.service.scoring;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.aptisprep.entity.Certificate;
import com.aptisprep.entity.ScoreBand;
import com.aptisprep.entity.ScoringScaleRule;
import com.aptisprep.entity.ScoringScheme;
import com.aptisprep.enums.RecordStatus;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.repository.ScoreBandRepository;
import com.aptisprep.repository.ScoringScaleRuleRepository;
import com.aptisprep.repository.ScoringSchemeRepository;
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
        ScoringPolicyService service = new ScoringPolicyService(schemes, bands, scaleRules);

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
                new ScoringPolicyService(schemes, bands, scaleRules)
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
        ScoringPolicyService service = new ScoringPolicyService(schemes, bands, scaleRules);

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
                        new ScoringPolicyService(schemes, bands, scaleRules)
                                .validatePolicy(1, "READING"));
    }

    @Test
    void rejectsRawOutsideInternalScale() {
        when(schemes.findById(1)).thenReturn(Optional.of(activeScheme()));
        ScoringScaleRule readingRule = rule(40, 50);
        when(scaleRules.findBySchemeIdAndSkillIgnoreCase(1, "READING"))
                .thenReturn(Optional.of(readingRule));
        ScoringPolicyService service = new ScoringPolicyService(schemes, bands, scaleRules);
        assertThrows(
                BusinessRuleException.class,
                () -> service.scale(1, "READING", new BigDecimal("50.01")));
    }

    @Test
    void convertsObjectiveRawUsingTheSkillSpecificRange() {
        when(schemes.findById(1)).thenReturn(Optional.of(activeScheme()));
        when(scaleRules.findBySchemeIdAndSkillIgnoreCase(1, "READING"))
                .thenReturn(Optional.of(rule(40, 50)));
        ScoringPolicyService service = new ScoringPolicyService(schemes, bands, scaleRules);

        assertEquals(new BigDecimal("30.00"), service.raw(1, "READING", 3, 4));
        assertEquals(new BigDecimal("37.50"), service.scale(1, "READING", new BigDecimal("30.00")));
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
