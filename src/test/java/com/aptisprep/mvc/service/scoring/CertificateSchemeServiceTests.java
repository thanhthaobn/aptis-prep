package com.aptisprep.mvc.service.scoring;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aptisprep.mvc.entity.Certificate;
import com.aptisprep.mvc.entity.ScoreBand;
import com.aptisprep.mvc.entity.ScoringScaleRule;
import com.aptisprep.mvc.entity.ScoringScheme;
import com.aptisprep.mvc.enums.RecordStatus;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.exception.ResourceNotFoundException;
import com.aptisprep.mvc.repository.CertificateRepository;
import com.aptisprep.mvc.repository.ScoreBandRepository;
import com.aptisprep.mvc.repository.ScoringSchemeRepository;
import com.aptisprep.mvc.service.scoring.impl.CertificateSchemeServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CertificateSchemeServiceTests {
    @Mock CertificateRepository certificates;
    @Mock ScoringSchemeRepository schemes;
    @Mock ScoreBandRepository bands;
    @Mock com.aptisprep.mvc.repository.ScoringScaleRuleRepository scaleRules;

    private Certificate certificate;
    private ScoringScheme scheme;
    private CertificateSchemeService service;

    @BeforeEach
    void setUp() {
        certificate = mock(Certificate.class);
        lenient().when(certificate.getId()).thenReturn(1);
        lenient().when(certificate.getStatus()).thenReturn(RecordStatus.ACTIVE);
        scheme = mock(ScoringScheme.class);
        lenient().when(scheme.getId()).thenReturn(2);
        lenient().when(scheme.getCertificate()).thenReturn(certificate);
        lenient().when(scheme.getStatus()).thenReturn(RecordStatus.ACTIVE);
        lenient().when(scheme.getVersion()).thenReturn("1.0");
        lenient().when(scheme.getName()).thenReturn("Test scheme");
        service = new CertificateSchemeServiceImpl(certificates, schemes, bands, scaleRules);
    }

    @Test
    void requiresMatchingActiveCertificateAndConfiguredSkill() {
        when(schemes.findByIdAndCertificateId(2, 1)).thenReturn(Optional.of(scheme));
        ScoreBand band = new ScoreBand();
        band.setSkill("READING");
        band.setCefrLevel("A1");
        band.setMinScore(java.math.BigDecimal.ZERO);
        band.setMaxScore(java.math.BigDecimal.valueOf(50));
        when(bands.findBySchemeIdAndSkillOrderByMinScoreAsc(2, "READING"))
                .thenReturn(List.of(band));
        when(bands.findBySchemeIdOrderBySkillAscMinScoreAsc(2)).thenReturn(List.of(band));
        ScoringScaleRule rule = new ScoringScaleRule();
        rule.setScaleMin(java.math.BigDecimal.ZERO);
        rule.setScaleMax(java.math.BigDecimal.valueOf(50));
        when(scaleRules.findBySchemeIdAndSkillIgnoreCase(2, "READING"))
                .thenReturn(Optional.of(rule));

        var option = service.requireAvailableScheme(1, 2, "Reading");

        assertEquals(2, option.getId());
        assertEquals(List.of("READING"), option.getSkills());
    }

    @Test
    void rejectsWrongCertificateAndMissingSkill() {
        when(schemes.findByIdAndCertificateId(2, 9)).thenReturn(Optional.empty());
        assertThrows(
                ResourceNotFoundException.class,
                () -> service.requireAvailableScheme(9, 2, "Reading"));

        when(schemes.findByIdAndCertificateId(2, 1)).thenReturn(Optional.of(scheme));
        ScoringScaleRule rule = new ScoringScaleRule();
        rule.setScaleMin(java.math.BigDecimal.ZERO);
        rule.setScaleMax(java.math.BigDecimal.valueOf(50));
        when(scaleRules.findBySchemeIdAndSkillIgnoreCase(2, "GRAMMAR_VOCABULARY"))
                .thenReturn(Optional.of(rule));
        when(bands.findBySchemeIdAndSkillOrderByMinScoreAsc(2, "GRAMMAR_VOCABULARY"))
                .thenReturn(List.of());
        assertThrows(
                BusinessRuleException.class,
                () -> service.requireAvailableScheme(1, 2, "Grammar & Vocabulary"));
    }

    @Test
    void rejectsMissingScaleRuleBeforeCheckingBands() {
        when(schemes.findByIdAndCertificateId(2, 1)).thenReturn(Optional.of(scheme));
        when(scaleRules.findBySchemeIdAndSkillIgnoreCase(2, "READING"))
                .thenReturn(Optional.empty());

        var error =
                assertThrows(
                        BusinessRuleException.class,
                        () -> service.requireAvailableScheme(1, 2, "Reading"));

        assertTrue(error.getMessage().contains("raw-to-scale"));
        verifyNoInteractions(bands);
    }
}
