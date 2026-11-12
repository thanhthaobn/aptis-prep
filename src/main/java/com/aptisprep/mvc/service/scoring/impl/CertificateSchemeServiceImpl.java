package com.aptisprep.mvc.service.scoring.impl;

import com.aptisprep.mvc.service.scoring.*;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.RecordStatus;
import com.aptisprep.mvc.enums.Skill;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.exception.ResourceNotFoundException;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.service.scoring.CertificateSchemeService;
import com.aptisprep.mvc.dto.CertificateDTO;
import com.aptisprep.mvc.dto.ScoringSchemeDTO;
import com.aptisprep.mvc.support.scoring.ScoreBandCoverage;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CertificateSchemeServiceImpl implements CertificateSchemeService {
    private Set<String> SKILLS = Set.copyOf(Skill.codes());
    private CertificateRepository certificateRepository;
    private ScoringSchemeRepository scoringSchemeRepository;
    private ScoreBandRepository scoreBandRepository;
    private ScoringScaleRuleRepository scoringScaleRuleRepository;

    public CertificateSchemeServiceImpl(
            CertificateRepository certificateRepository,
            ScoringSchemeRepository scoringSchemeRepository,
            ScoreBandRepository scoreBandRepository,
            ScoringScaleRuleRepository scoringScaleRuleRepository) {
        this.certificateRepository = certificateRepository;
        this.scoringSchemeRepository = scoringSchemeRepository;
        this.scoreBandRepository = scoreBandRepository;
        this.scoringScaleRuleRepository = scoringScaleRuleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CertificateDTO> activeCertificates() {
        return certificateRepository.findByStatusOrderByNameAsc(RecordStatus.ACTIVE).stream()
                .map(value -> CertificateDTO.convertEntityToDto(value))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoringSchemeDTO> activeSchemes(Integer certificateId) {
        if (certificateId == null
                || certificateRepository
                        .findById(certificateId)
                        .filter(value -> value.getStatus() == RecordStatus.ACTIVE)
                        .isEmpty()) {
            return List.of();
        }
        return scoringSchemeRepository.findByCertificateIdOrderByVersionDesc(certificateId).stream()
                .filter(value -> value.getStatus() == RecordStatus.ACTIVE)
                .map(streamValue -> toDto(streamValue))
                .toList();
    }

    @Override
    @Transactional(
            readOnly = true,
            noRollbackFor = {BusinessRuleException.class, ResourceNotFoundException.class})
    public ScoringSchemeDTO requireAvailableScheme(
            Integer certificateId, Integer schemeId, String skill) {
        String normalizedSkill =
                skill == null
                        ? ""
                        : skill.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        if (!SKILLS.contains(normalizedSkill))
            throw new BusinessRuleException("Kỹ năng không hợp lệ");
        ScoringScheme scheme =
                scoringSchemeRepository
                        .findByIdAndCertificateId(schemeId, certificateId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Bộ chấm điểm không thuộc chứng chỉ đã chọn"));
        if (scheme.getStatus() != RecordStatus.ACTIVE
                || scheme.getCertificate().getStatus() != RecordStatus.ACTIVE) {
            throw new BusinessRuleException("Bộ chấm điểm hoặc chứng chỉ đã ngừng sử dụng");
        }
        ScoringScaleRule rule =
                scoringScaleRuleRepository
                        .findBySchemeIdAndSkillIgnoreCase(schemeId, normalizedSkill)
                        .orElseThrow(
                                () ->
                                        new BusinessRuleException(
                                                "Bộ chấm điểm chưa cấu hình quy đổi raw-to-scale"
                                                        + " cho kỹ năng này"));
        var bands =
                scoreBandRepository.findBySchemeIdAndSkillOrderByMinScoreAsc(
                        scheme.getId(), normalizedSkill);
        if (!normalizedSkill.equals(scheme.getScopeSkill()) || bands.size() != 1) {
            ScoreBandCoverage.requireComplete(rule, bands);
        } else if (bands.getFirst().getMinScore().compareTo(rule.getScaleMin()) < 0
                || bands.getFirst().getMaxScore().compareTo(rule.getScaleMax()) != 0) {
            throw new BusinessRuleException("Ngưỡng đạt nằm ngoài thang điểm");
        }
        return toDto(scheme);
    }

    private ScoringSchemeDTO toDto(ScoringScheme scheme) {
        ScoringSchemeDTO result = ScoringSchemeDTO.convertEntityToDto(scheme);
        result.setSkills(
                scoreBandRepository.findBySchemeIdOrderBySkillAscMinScoreAsc(scheme.getId()).stream()
                        .map(value -> value.getSkill())
                        .distinct()
                        .toList());
        return result;
    }
}







