package com.aptisprep.mvc.scoring.service;

import com.aptisprep.mvc.scoring.entity.Certificate;
import com.aptisprep.mvc.scoring.entity.ScoringScheme;
import java.util.List;
import java.util.Optional;

/** Owner 1 read-only contract for question/exam flows. */
public interface CertificateSchemeService {
    List<CertificateOption> activeCertificates();

    List<SchemeOption> activeSchemes(Integer certificateId);

    SchemeOption requireAvailableScheme(Integer certificateId, Integer schemeId, String skill);

    record CertificateOption(Integer id, String code, String name) {}

    record SchemeOption(
            Integer id,
            Integer certificateId,
            String version,
            String name,
            String status,
            List<String> skills) {}

    Optional<Certificate> findCertificateById(Integer id);

    Optional<Certificate> findById(Integer id);

    Optional<Certificate> findByCodeIgnoreCase(String code);

    List<Certificate> findAllActiveCertificates();

    Optional<ScoringScheme> findSchemeById(Integer id);

    List<ScoringScheme> findAllActiveSchemes();
}
