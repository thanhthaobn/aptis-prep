package com.aptisprep.service.contract;

import java.util.List;

/** Owner 1 read-only contract for question/exam flows. */
public interface CertificateSchemeContract {
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
}
