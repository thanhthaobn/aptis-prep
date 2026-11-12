package com.aptisprep.mvc.service.scoring;

import com.aptisprep.mvc.dto.CertificateDTO;
import com.aptisprep.mvc.dto.ScoreBandDTO;
import com.aptisprep.mvc.dto.ScoringScaleRuleDTO;
import com.aptisprep.mvc.dto.ScoringSchemeDTO;
import com.aptisprep.mvc.support.scoring.ScoringViews;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ScoringAdminService {
    public record SchemeCatalog(
            List<ScoringViews.SchemeView> schemes, Map<Integer, Set<String>> examTypesByScheme) {}

    public SchemeCatalog filteredSchemes(Integer certificateId, String examType);

    public List<ScoringViews.CertificateView> certificateViews();

    public List<ScoringViews.SchemeView> schemeViews();

    public ScoringViews.CertificateView certificate(Integer id);

    public ScoringViews.SchemeView scheme(Integer id);

    public List<ScoringViews.BandView> bandViews(Integer schemeId, String skill);

    public ScoringViews.BandView band(Integer schemeId, Integer bandId);

    public List<ScoringViews.BandView> allBandViews(Integer schemeId);

    public Integer createCertificate(String adminEmail, CertificateDTO form);

    public void updateCertificate(String adminEmail, Integer id, CertificateDTO form);

    public void disableCertificate(String adminEmail, Integer id);

    public Integer saveSchemeWithScores(String email, Integer id, ScoringSchemeDTO form);

    public void activateScheme(String email, Integer id);

    public void activateCertificate(String email, Integer id);

    public void populateScoreConfiguration(Integer id, ScoringSchemeDTO form);

    public Integer saveScheme(String adminEmail, Integer id, ScoringSchemeDTO form);

    public void disableScheme(String adminEmail, Integer id);

    public void saveBand(String adminEmail, Integer schemeId, Integer id, ScoreBandDTO form);

    public void deleteBand(String adminEmail, Integer schemeId, Integer id);

    public ScoringScaleRuleDTO scaleRule(Integer schemeId, String skill);

    public void saveScaleRule(
            String adminEmail, Integer schemeId, String skill, ScoringScaleRuleDTO form);
}







