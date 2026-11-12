package com.aptisprep.mvc.service.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.aptisprep.mvc.dto.ScoreBandDTO;
import com.aptisprep.mvc.dto.ScoringScaleRuleDTO;
import com.aptisprep.mvc.dto.ScoringSchemeDTO;
import com.aptisprep.mvc.entity.Certificate;
import com.aptisprep.mvc.entity.ScoringScaleRule;
import com.aptisprep.mvc.entity.ScoringScheme;
import com.aptisprep.mvc.entity.User;
import com.aptisprep.mvc.enums.RecordStatus;
import com.aptisprep.mvc.enums.Role;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.service.catalog.ScoringSchemeUsageService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScoringAdminServiceTests {
    @Mock CertificateRepository certificates;
    @Mock ScoringSchemeRepository schemes;
    @Mock ScoreBandRepository bands;
    @Mock UserRepository users;
    @Mock ScoringSchemeUsageService usage;
    @Mock ScoringScaleRuleRepository scaleRules;

    @Test
    void usedSchemeCannotBeEditedInPlaceButCanCreateNewVersion() {
        User admin = new User();
        admin.setId(1);
        admin.setRole(Role.ADMIN);
        Certificate certificate = mock(Certificate.class);
        when(certificate.getId()).thenReturn(10);
        when(certificate.getStatus()).thenReturn(RecordStatus.ACTIVE);
        ScoringScheme current = mock(ScoringScheme.class);
        when(current.getId()).thenReturn(20);
        when(current.getCertificate()).thenReturn(certificate);
        when(current.getVersion()).thenReturn("1");
        when(schemes.findByIdForUpdate(20)).thenReturn(Optional.of(current));
        when(certificates.findById(10)).thenReturn(Optional.of(certificate));
        when(users.findByEmailIgnoreCase("admin@aptis.local")).thenReturn(Optional.of(admin));
        when(usage.isUsed(20)).thenReturn(true);

        ScoringAdminService service =
                new com.aptisprep.mvc.service.scoring.impl.ScoringAdminServiceImpl(
                        certificates, schemes, bands, users, usage, scaleRules);
        ScoringSchemeDTO sameVersion = form("1");
        assertThrows(
                BusinessRuleException.class,
                () -> service.saveScheme("admin@aptis.local", 20, sameVersion));
        verify(schemes, never()).save(any(ScoringScheme.class));

        ScoringScheme replacement = mock(ScoringScheme.class);
        when(replacement.getId()).thenReturn(21);
        when(schemes.existsByCertificateIdAndVersionIgnoreCase(10, "2")).thenReturn(false);
        when(schemes.save(any(ScoringScheme.class))).thenReturn(replacement);
        assertEquals(21, service.saveScheme("admin@aptis.local", 20, form("2")));
        verify(schemes).save(any(ScoringScheme.class));
    }

    @Test
    void savesCustomScaleAndAdministratorDefinedBandAboveFifty() {
        User admin = new User();
        admin.setRole(Role.ADMIN);
        ScoringScheme scheme = new ScoringScheme();
        scheme.setCertificate(new Certificate());
        scheme.setStatus(RecordStatus.ACTIVE);
        when(users.findByEmailIgnoreCase("admin@aptis.local")).thenReturn(Optional.of(admin));
        when(schemes.findByIdForUpdate(20)).thenReturn(Optional.of(scheme));
        ScoringScaleRule rule = new ScoringScaleRule();
        when(scaleRules.findBySchemeIdAndSkillIgnoreCase(20, "SPEAKING"))
                .thenReturn(Optional.of(rule));

        ScoringAdminService service =
                new com.aptisprep.mvc.service.scoring.impl.ScoringAdminServiceImpl(
                        certificates, schemes, bands, users, usage, scaleRules);
        ScoringScaleRuleDTO scale = new ScoringScaleRuleDTO();
        scale.setRawMin(BigDecimal.ZERO);
        scale.setRawMax(BigDecimal.valueOf(50));
        scale.setScaleMin(BigDecimal.ONE);
        scale.setScaleMax(BigDecimal.valueOf(60));
        service.saveScaleRule("admin@aptis.local", 20, "SPEAKING", scale);
        verify(scaleRules).save(rule);
        assertEquals(new BigDecimal("1.00"), rule.getScaleMin());
        assertEquals(new BigDecimal("60.00"), rule.getScaleMax());

        ScoreBandDTO band = new ScoreBandDTO();
        band.setSkill("SPEAKING");
        band.setCefrLevel("C1");
        band.setMinScore(new BigDecimal("54.25"));
        band.setMaxScore(new BigDecimal("60.00"));
        service.saveBand("admin@aptis.local", 20, null, band);
        verify(bands)
                .save(
                        argThat(
                                saved ->
                                        saved.getSkill().equals("SPEAKING")
                                                && saved.getCefrLevel().equals("C1")
                                                && saved.getMinScore()
                                                        .equals(new BigDecimal("54.25"))
                                                && saved.getMaxScore()
                                                        .equals(new BigDecimal("60.00"))));

        band.setMaxScore(new BigDecimal("60.01"));
        assertThrows(
                BusinessRuleException.class,
                () -> service.saveBand("admin@aptis.local", 20, null, band));
    }

    private ScoringSchemeDTO form(String version) {
        ScoringSchemeDTO form = new ScoringSchemeDTO();
        form.setCertificateId(10);
        form.setName("Aptis scheme");
        form.setVersion(version);
        form.setStatus("ACTIVE");
        return form;
    }
}
