package com.aptisprep.service.scoring;

import com.aptisprep.entity.*;
import com.aptisprep.enums.RecordStatus;
import com.aptisprep.enums.Skill;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.*;
import com.aptisprep.service.contract.CertificateSchemeContract;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CertificateSchemeService implements CertificateSchemeContract {
    private static final Set<String> SKILLS = Set.copyOf(Skill.codes());
    private final CertificateRepository certificateRepository;
    private final ScoringSchemeRepository scoringSchemeRepository;
    private final ScoreBandRepository scoreBandRepository;
    private final ScoringScaleRuleRepository scoringScaleRuleRepository;

    public CertificateSchemeService(
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
    public List<CertificateOption> activeCertificates() {
        return certificateRepository.findByStatusOrderByNameAsc(RecordStatus.ACTIVE).stream()
                .map(
                        value ->
                                new CertificateOption(
                                        value.getId(), value.getCode(), value.getName()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchemeOption> activeSchemes(Integer certificateId) {
        if (certificateId == null
                || certificateRepository
                        .findById(certificateId)
                        .filter(value -> value.getStatus() == RecordStatus.ACTIVE)
                        .isEmpty()) {
            return List.of();
        }
        return scoringSchemeRepository.findByCertificateIdOrderByVersionDesc(certificateId).stream()
                .filter(value -> value.getStatus() == RecordStatus.ACTIVE)
                .map(this::option)
                .toList();
    }

    @Override
    @Transactional(
            readOnly = true,
            noRollbackFor = {BusinessRuleException.class, ResourceNotFoundException.class})
    public SchemeOption requireAvailableScheme(
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
        ScoreBandCoverage.requireComplete(
                rule,
                scoreBandRepository.findBySchemeIdAndSkillOrderByMinScoreAsc(
                        scheme.getId(), normalizedSkill));
        return option(scheme);
    }

    private SchemeOption option(ScoringScheme scheme) {
        List<String> skills =
                scoreBandRepository
                        .findBySchemeIdOrderBySkillAscMinScoreAsc(scheme.getId())
                        .stream()
                        .map(ScoreBand::getSkill)
                        .distinct()
                        .toList();
        return new SchemeOption(
                scheme.getId(),
                scheme.getCertificate().getId(),
                scheme.getVersion(),
                scheme.getName(),
                scheme.getStatus().name(),
                skills);
    }
}
