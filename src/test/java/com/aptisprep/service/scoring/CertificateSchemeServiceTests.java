package com.aptisprep.service.scoring;

import com.aptisprep.entity.Certificate;
import com.aptisprep.entity.ScoreBand;
import com.aptisprep.entity.ScoringScheme;
import com.aptisprep.enums.RecordStatus;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.CertificateRepository;
import com.aptisprep.repository.ScoreBandRepository;
import com.aptisprep.repository.ScoringSchemeRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CertificateSchemeServiceTests {
    @Mock CertificateRepository certificates;
    @Mock ScoringSchemeRepository schemes;
    @Mock ScoreBandRepository bands;

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
        service = new CertificateSchemeService(certificates, schemes, bands);
    }

    @Test
    void requiresMatchingActiveCertificateAndConfiguredSkill() {
        when(schemes.findByIdAndCertificateId(2, 1)).thenReturn(Optional.of(scheme));
        ScoreBand band = mock(ScoreBand.class);
        when(band.getSkill()).thenReturn("READING");
        when(bands.findBySchemeIdAndSkillOrderByMinScoreAsc(2, "READING")).thenReturn(List.of(band));
        when(bands.findBySchemeIdOrderBySkillAscMinScoreAsc(2)).thenReturn(List.of(band));

        var option = service.requireAvailableScheme(1, 2, "Reading");

        assertEquals(2, option.id());
        assertEquals(List.of("READING"), option.skills());
    }

    @Test
    void rejectsWrongCertificateAndMissingSkill() {
        when(schemes.findByIdAndCertificateId(2, 9)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.requireAvailableScheme(9, 2, "Reading"));

        when(schemes.findByIdAndCertificateId(2, 1)).thenReturn(Optional.of(scheme));
        when(bands.findBySchemeIdAndSkillOrderByMinScoreAsc(2, "GRAMMAR_VOCABULARY")).thenReturn(List.of());
        assertThrows(BusinessRuleException.class, () -> service.requireAvailableScheme(1, 2, "Grammar & Vocabulary"));
    }
}
