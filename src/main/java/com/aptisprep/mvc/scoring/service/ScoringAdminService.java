package com.aptisprep.mvc.scoring.service;

import com.aptisprep.mvc.scoring.dto.*;
import java.util.List;

public interface ScoringAdminService {
    List<ScoringViewsDTO.CertificateView> certificateViews();

    List<ScoringViewsDTO.SchemeView> schemeViews();

    List<ScoringViewsDTO.SchemeExamTypeView> schemeExamTypes();

    ScoringViewsDTO.CertificateView certificate(Integer id);

    ScoringViewsDTO.SchemeView scheme(Integer id);

    List<ScoringViewsDTO.BandView> bandViews(Integer schemeId, String skill);

    ScoringViewsDTO.BandView band(Integer schemeId, Integer bandId);

    List<ScoringViewsDTO.BandView> allBandViews(Integer schemeId);

    Integer createCertificate(String adminEmail, CertificateFormDTO form);

    void updateCertificate(String adminEmail, Integer id, CertificateFormDTO form);

    void disableCertificate(String adminEmail, Integer id);

    Integer saveSchemeWithScores(String email, Integer id, ScoringSchemeFormDTO form);

    void activateScheme(String email, Integer id);

    void activateCertificate(String email, Integer id);

    void populateScoreConfiguration(Integer id, ScoringSchemeFormDTO form);

    Integer saveScheme(String adminEmail, Integer id, ScoringSchemeFormDTO form);

    void disableScheme(String adminEmail, Integer id);

    void saveBand(String adminEmail, Integer schemeId, Integer id, ScoreBandFormDTO form);

    void deleteBand(String adminEmail, Integer schemeId, Integer id);

    ScoringScaleRuleFormDTO scaleRule(Integer schemeId, String skill);

    void saveScaleRule(
            String adminEmail, Integer schemeId, String skill, ScoringScaleRuleFormDTO form);
}
