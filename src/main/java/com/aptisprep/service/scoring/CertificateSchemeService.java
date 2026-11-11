package com.aptisprep.service.scoring;

import com.aptisprep.entity.Certificate;
import com.aptisprep.entity.ScoringScheme;
import java.util.List;
import java.util.Optional;

/** Owner 1 read-only contract for question/exam flows. */
public interface CertificateSchemeService {
    public List<CertificateOption> activeCertificates();

    public List<SchemeOption> activeSchemes(Integer certificateId);

    public SchemeOption requireAvailableScheme(
            Integer certificateId, Integer schemeId, String skill);

    public record CertificateOption(Integer id, String code, String name) {}

    public record SchemeOption(
            Integer id,
            Integer certificateId,
            String version,
            String name,
            String status,
            List<String> skills) {}

    public Optional<Certificate> findCertificateById(Integer id);

    public Optional<Certificate> findById(Integer id);

    public Optional<Certificate> findByCodeIgnoreCase(String code);

    public List<Certificate> findAllActiveCertificates();

    public Optional<ScoringScheme> findSchemeById(Integer id);

    public List<ScoringScheme> findAllActiveSchemes();
}
