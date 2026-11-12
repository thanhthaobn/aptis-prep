package com.aptisprep.mvc.service.scoring;

import com.aptisprep.mvc.dto.CertificateDTO;
import com.aptisprep.mvc.dto.ScoringSchemeDTO;
import java.util.List;

/** Reads certificates and scoring schemes available to students. */
public interface CertificateSchemeService {
    public List<CertificateDTO> activeCertificates();

    public List<ScoringSchemeDTO> activeSchemes(Integer certificateId);

    public ScoringSchemeDTO requireAvailableScheme(
            Integer certificateId, Integer schemeId, String skill);
}
