package com.aptisprep.service.scoring;

import com.aptisprep.entity.ScoreBand;
import com.aptisprep.entity.ScoringScheme;
import com.aptisprep.entity.Certificate;
import com.aptisprep.enums.RecordStatus;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.repository.ScoreBandRepository;
import com.aptisprep.repository.ScoringSchemeRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoringPolicyServiceTests {
    @Mock ScoringSchemeRepository schemes;
    @Mock ScoreBandRepository bands;

    @Test
    void roundsRawScoreAndMapsClosedBandBoundary() {
        when(schemes.findById(1)).thenReturn(Optional.of(activeScheme()));
        ScoreBand low = new ScoreBand(); low.setMinScore(new BigDecimal("0.00")); low.setMaxScore(new BigDecimal("9.99")); low.setCefrLevel("A1");
        ScoreBand high = new ScoreBand(); high.setMinScore(new BigDecimal("10.00")); high.setMaxScore(new BigDecimal("19.99")); high.setCefrLevel("A2");
        when(bands.findBySchemeIdAndSkillOrderByMinScoreAsc(1, "READING")).thenReturn(List.of(low, high));
        ScoringPolicyService service = new ScoringPolicyService(schemes, bands);

        assertEquals(new BigDecimal("10.00"), service.scale(1, "Reading", new BigDecimal("9.995")));
        assertEquals("A2", service.estimatedLevel(1, "READING", new BigDecimal("10.00")));
    }

    @Test
    void missingGrammarBandsRemainUnassessed() {
        when(schemes.findById(1)).thenReturn(Optional.of(activeScheme()));
        when(bands.findBySchemeIdAndSkillOrderByMinScoreAsc(1, "GRAMMAR_VOCABULARY")).thenReturn(List.of());
        assertNull(new ScoringPolicyService(schemes, bands).estimatedLevel(1, "GRAMMAR & VOCABULARY", new BigDecimal("30")));
    }

    @Test
    void rejectsRawOutsideInternalScale() {
        when(schemes.findById(1)).thenReturn(Optional.of(activeScheme()));
        ScoringPolicyService service = new ScoringPolicyService(schemes, bands);
        assertThrows(BusinessRuleException.class, () -> service.scale(1, "READING", new BigDecimal("50.01")));
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
