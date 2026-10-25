package com.aptisprep.service.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.aptisprep.dto.form.ScoringSchemeForm;
import com.aptisprep.entity.Certificate;
import com.aptisprep.entity.ScoringScheme;
import com.aptisprep.entity.User;
import com.aptisprep.enums.RecordStatus;
import com.aptisprep.enums.Role;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.repository.*;
import com.aptisprep.service.contract.ScoringSchemeUsageContract;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class ScoringAdminServiceTests {
    @Mock CertificateRepository certificates;
    @Mock ScoringSchemeRepository schemes;
    @Mock ScoreBandRepository bands;
    @Mock UserRepository users;
    @Mock ObjectProvider<ScoringSchemeUsageContract> usageProvider;
    @Mock ScoringSchemeUsageContract usage;
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
        when(usageProvider.getIfAvailable()).thenReturn(usage);
        when(usage.isUsed(20)).thenReturn(true);

        ScoringAdminService service =
                new ScoringAdminService(
                        certificates, schemes, bands, users, usageProvider, scaleRules);
        ScoringSchemeForm sameVersion = form("1");
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

    private ScoringSchemeForm form(String version) {
        ScoringSchemeForm form = new ScoringSchemeForm();
        form.setCertificateId(10);
        form.setName("Aptis scheme");
        form.setVersion(version);
        form.setStatus("ACTIVE");
        return form;
    }
}
