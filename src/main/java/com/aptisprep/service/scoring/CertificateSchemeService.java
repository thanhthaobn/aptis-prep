package com.aptisprep.service.scoring;

import com.aptisprep.entity.*;
import com.aptisprep.enums.RecordStatus;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.*;
import com.aptisprep.service.contract.CertificateSchemeContract;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CertificateSchemeService implements CertificateSchemeContract {
    private static final Set<String> SKILLS = Set.of("GRAMMAR_VOCABULARY", "READING", "LISTENING", "WRITING", "SPEAKING");
    private final CertificateRepository certificates;
    private final ScoringSchemeRepository schemes;
    private final ScoreBandRepository bands;

    public CertificateSchemeService(CertificateRepository certificates, ScoringSchemeRepository schemes,
            ScoreBandRepository bands) {
        this.certificates = certificates; this.schemes = schemes; this.bands = bands;
    }

    @Override @Transactional(readOnly = true)
    public List<CertificateOption> activeCertificates() {
        return certificates.findByStatusOrderByNameAsc(RecordStatus.ACTIVE).stream()
                .map(value -> new CertificateOption(value.getId(), value.getCode(), value.getName())).toList();
    }

    @Override @Transactional(readOnly = true)
    public List<SchemeOption> activeSchemes(Integer certificateId) {
        if (certificateId == null || certificates.findById(certificateId)
                .filter(value -> value.getStatus() == RecordStatus.ACTIVE).isEmpty()) {
            return List.of();
        }
        return schemes.findByCertificateIdOrderByVersionDesc(certificateId).stream()
                .filter(value -> value.getStatus() == RecordStatus.ACTIVE)
                .map(this::option).toList();
    }

    @Override @Transactional(readOnly = true)
    public SchemeOption requireAvailableScheme(Integer certificateId, Integer schemeId, String skill) {
        String normalizedSkill = skill == null ? "" : skill.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
        if (!SKILLS.contains(normalizedSkill)) throw new BusinessRuleException("Kỹ năng không hợp lệ");
        ScoringScheme scheme = schemes.findByIdAndCertificateId(schemeId, certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Bộ chấm điểm không thuộc chứng chỉ đã chọn"));
        if (scheme.getStatus() != RecordStatus.ACTIVE || scheme.getCertificate().getStatus() != RecordStatus.ACTIVE) {
            throw new BusinessRuleException("Bộ chấm điểm hoặc chứng chỉ đã ngừng sử dụng");
        }
        if (bands.findBySchemeIdAndSkillOrderByMinScoreAsc(scheme.getId(), normalizedSkill).isEmpty()) {
            throw new BusinessRuleException("Bộ chấm điểm chưa cấu hình kỹ năng này");
        }
        return option(scheme);
    }

    private SchemeOption option(ScoringScheme scheme) {
        List<String> skills = bands.findBySchemeIdOrderBySkillAscMinScoreAsc(scheme.getId()).stream()
                .map(ScoreBand::getSkill).distinct().toList();
        return new SchemeOption(scheme.getId(), scheme.getCertificate().getId(), scheme.getVersion(), scheme.getName(), scheme.getStatus().name(), skills);
    }
}
