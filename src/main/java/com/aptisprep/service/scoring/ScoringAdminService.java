package com.aptisprep.service.scoring;

import com.aptisprep.dto.form.CertificateForm;
import com.aptisprep.dto.form.ScoreBandForm;
import com.aptisprep.dto.form.ScoringScaleRuleForm;
import com.aptisprep.dto.form.ScoringSchemeForm;
import java.util.List;

public interface ScoringAdminService {
    List<ScoringViews.CertificateView> certificateViews();

    List<ScoringViews.SchemeView> schemeViews();

    List<ScoringViews.SchemeExamTypeView> schemeExamTypes();

    ScoringViews.CertificateView certificate(Integer id);

    ScoringViews.SchemeView scheme(Integer id);

    List<ScoringViews.BandView> bandViews(Integer schemeId, String skill);

    ScoringViews.BandView band(Integer schemeId, Integer bandId);

    List<ScoringViews.BandView> allBandViews(Integer schemeId);

    Integer createCertificate(String adminEmail, CertificateForm form);

    void updateCertificate(String adminEmail, Integer id, CertificateForm form);

    void disableCertificate(String adminEmail, Integer id);

    Integer saveSchemeWithScores(String email, Integer id, ScoringSchemeForm form);

    void activateScheme(String email, Integer id);

    void activateCertificate(String email, Integer id);

    void populateScoreConfiguration(Integer id, ScoringSchemeForm form);

    Integer saveScheme(String adminEmail, Integer id, ScoringSchemeForm form);

    void disableScheme(String adminEmail, Integer id);

    void saveBand(String adminEmail, Integer schemeId, Integer id, ScoreBandForm form);

    void deleteBand(String adminEmail, Integer schemeId, Integer id);

    ScoringScaleRuleForm scaleRule(Integer schemeId, String skill);

    void saveScaleRule(
            String adminEmail, Integer schemeId, String skill, ScoringScaleRuleForm form);
}
