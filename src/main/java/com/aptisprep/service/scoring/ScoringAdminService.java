package com.aptisprep.service.scoring;

import com.aptisprep.dto.CertificateDTO;
import com.aptisprep.dto.ScoreBandDTO;
import com.aptisprep.dto.ScoringScaleRuleDTO;
import com.aptisprep.dto.ScoringSchemeFormDTO;
import com.aptisprep.dto.ScoringViewsDTO;
import java.util.List;
import org.springframework.ui.Model;

public interface ScoringAdminService {

    public List<ScoringViewsDTO.CertificateView> getAllCertificateViews();

    public List<ScoringViewsDTO.CertificateView> certificateViews();

    public List<ScoringViewsDTO.SchemeView> getAllSchemeViews();

    public List<ScoringViewsDTO.SchemeView> schemeViews();

    public List<ScoringViewsDTO.SchemeExamTypeView> getAllSchemeExamTypes();

    public List<ScoringViewsDTO.SchemeExamTypeView> schemeExamTypes();

    public ScoringViewsDTO.CertificateView getCertificateById(Integer id);

    public ScoringViewsDTO.CertificateView certificate(Integer id);

    public ScoringViewsDTO.SchemeView getScoringSchemeById(Integer id);

    public ScoringViewsDTO.SchemeView scheme(Integer id);

    public List<ScoringViewsDTO.BandView> getScoreBandViews(Integer schemeId, String skill);

    public List<ScoringViewsDTO.BandView> bandViews(Integer schemeId, String skill);

    public ScoringViewsDTO.BandView getScoreBandById(Integer schemeId, Integer bandId);

    public ScoringViewsDTO.BandView band(Integer schemeId, Integer bandId);

    public List<ScoringViewsDTO.BandView> getAllScoreBandViews(Integer schemeId);

    public List<ScoringViewsDTO.BandView> allBandViews(Integer schemeId);

    public Integer createCertificate(String adminEmail, CertificateDTO form);

    public void updateCertificate(String adminEmail, Integer id, CertificateDTO form);

    public void disableCertificate(String adminEmail, Integer id);

    public Integer saveSchemeWithScores(String email, Integer id, ScoringSchemeFormDTO form);

    public void activateScheme(String email, Integer id);

    public void activateCertificate(String email, Integer id);

    public void populateScoreConfiguration(Integer id, ScoringSchemeFormDTO form);

    public Integer saveScheme(String adminEmail, Integer id, ScoringSchemeFormDTO form);

    public void disableScheme(String adminEmail, Integer id);

    public void saveBand(String adminEmail, Integer schemeId, Integer id, ScoreBandDTO form);

    public void deleteBand(String adminEmail, Integer schemeId, Integer id);

    public ScoringScaleRuleDTO getScaleRule(Integer schemeId, String skill);

    public ScoringScaleRuleDTO scaleRule(Integer schemeId, String skill);

    public void saveScaleRule(
            String adminEmail, Integer schemeId, String skill, ScoringScaleRuleDTO form);

    public void populateScoringSchemeModel(
            int id, String skill, Integer bandId, boolean readOnly, Model model);
}
