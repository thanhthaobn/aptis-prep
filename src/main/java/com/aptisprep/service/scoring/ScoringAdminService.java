package com.aptisprep.service.scoring;

import com.aptisprep.dto.form.*;
import com.aptisprep.entity.*;
import com.aptisprep.enums.*;
import com.aptisprep.enums.CefrLevel;
import com.aptisprep.enums.Skill;
import com.aptisprep.exception.AccessDeniedException;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.*;
import com.aptisprep.service.contract.ScoringSchemeUsageContract;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScoringAdminService {
    private static final Set<String> SKILLS = Set.copyOf(Skill.codes());
    private static final Set<String> LEVELS = Set.copyOf(CefrLevel.codes());
    private final CertificateRepository certificates;
    private final ScoringSchemeRepository schemes;
    private final ScoreBandRepository bands;
    private final UserRepository users;
    private final ObjectProvider<ScoringSchemeUsageContract> usage;
    private final ScoringScaleRuleRepository scaleRules;

    public ScoringAdminService(
            CertificateRepository certificates,
            ScoringSchemeRepository schemes,
            ScoreBandRepository bands,
            UserRepository users,
            ObjectProvider<ScoringSchemeUsageContract> usage,
            ScoringScaleRuleRepository scaleRules) {
        this.certificates = certificates;
        this.schemes = schemes;
        this.bands = bands;
        this.users = users;
        this.usage = usage;
        this.scaleRules = scaleRules;
    }

    @Transactional(readOnly = true)
    public List<ScoringViews.CertificateView> certificateViews() {
        return certificates.findAllByOrderByNameAsc().stream().map(this::certificateView).toList();
    }

    @Transactional(readOnly = true)
    public List<ScoringViews.SchemeView> schemeViews() {
        return schemes.findAllByOrderByCertificateNameAscVersionDesc().stream()
                .map(this::schemeView)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScoringViews.CertificateView certificate(Integer id) {
        return certificateView(
                certificates
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy chứng chỉ")));
    }

    @Transactional(readOnly = true)
    public ScoringViews.SchemeView scheme(Integer id) {
        return schemeView(
                schemes.findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy bộ chấm điểm")));
    }

    @Transactional(readOnly = true)
    public List<ScoringViews.BandView> bandViews(Integer schemeId, String skill) {
        ScoringScheme scheme = requireScheme(schemeId);
        String normalized = normalizeSkill(skill);
        return bands.findBySchemeIdAndSkillOrderByMinScoreAsc(scheme.getId(), normalized).stream()
                .map(this::bandView)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScoringViews.BandView band(Integer schemeId, Integer bandId) {
        requireScheme(schemeId);
        return bandView(
                bands.findByIdAndSchemeId(bandId, schemeId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy khoảng điểm")));
    }

    @Transactional(readOnly = true)
    public List<ScoringViews.BandView> allBandViews(Integer schemeId) {
        requireScheme(schemeId);
        return bands.findBySchemeIdOrderBySkillAscMinScoreAsc(schemeId).stream()
                .map(this::bandView)
                .toList();
    }

    @Transactional
    public Integer createCertificate(String adminEmail, CertificateForm form) {
        User admin = requireAdmin(adminEmail);
        String code = normalizeCode(form.getCode());
        if (certificates.existsByCodeIgnoreCase(code))
            throw new BusinessRuleException("Mã chứng chỉ đã tồn tại");
        Certificate value = new Certificate();
        value.setName(required(form.getName(), "Tên chứng chỉ"));
        value.setCode(code);
        value.setDescription(blankToNull(form.getDescription()));
        value.setStatus(status(form.getStatus()));
        value.setCreatedBy(admin);
        try {
            return certificates.save(value).getId();
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessRuleException("Mã chứng chỉ đã tồn tại");
        }
    }

    @Transactional
    public void updateCertificate(String adminEmail, Integer id, CertificateForm form) {
        requireAdmin(adminEmail);
        Certificate value =
                certificates
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy chứng chỉ"));
        String code = normalizeCode(form.getCode());
        if (certificates.existsByCodeIgnoreCaseAndIdNot(code, id))
            throw new BusinessRuleException("Mã chứng chỉ đã tồn tại");
        value.setName(required(form.getName(), "Tên chứng chỉ"));
        value.setCode(code);
        value.setDescription(blankToNull(form.getDescription()));
        value.setStatus(status(form.getStatus()));
    }

    @Transactional
    public void disableCertificate(String adminEmail, Integer id) {
        requireAdmin(adminEmail);
        Certificate value =
                certificates
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy chứng chỉ"));
        value.setStatus(RecordStatus.INACTIVE);
    }

    @Transactional
    public Integer saveScheme(String adminEmail, Integer id, ScoringSchemeForm form) {
        requireAdmin(adminEmail);
        Certificate certificate =
                certificates
                        .findById(form.getCertificateId())
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy chứng chỉ"));
        if (certificate.getStatus() != RecordStatus.ACTIVE)
            throw new BusinessRuleException("Chứng chỉ đã ngừng sử dụng");
        String version = required(form.getVersion(), "Phiên bản");
        if (id == null) {
            ensureSchemeVersion(certificate.getId(), version, null);
            ScoringScheme value = new ScoringScheme();
            setScheme(value, certificate, form, version);
            return schemes.save(value).getId();
        }
        ScoringScheme current = requireSchemeForUpdate(id);
        if (!current.getCertificate().getId().equals(certificate.getId())
                || !current.getVersion().equalsIgnoreCase(version)) {
            ensureSchemeVersion(certificate.getId(), version, null);
            ScoringScheme replacement = new ScoringScheme();
            setScheme(replacement, certificate, form, version);
            replacement = schemes.save(replacement);
            for (ScoreBand old : bands.findBySchemeIdOrderBySkillAscMinScoreAsc(id)) {
                ScoreBand copy = new ScoreBand();
                copy.setScheme(replacement);
                copy.setSkill(old.getSkill());
                copy.setCefrLevel(old.getCefrLevel());
                copy.setMinScore(old.getMinScore());
                copy.setMaxScore(old.getMaxScore());
                bands.save(copy);
            }
            for (ScoringScaleRule old : scaleRules.findBySchemeId(id)) {
                ScoringScaleRule copy = new ScoringScaleRule();
                copy.setScheme(replacement);
                copy.setSkill(old.getSkill());
                copy.setRawMin(old.getRawMin());
                copy.setRawMax(old.getRawMax());
                copy.setScaleMin(old.getScaleMin());
                copy.setScaleMax(old.getScaleMax());
                scaleRules.save(copy);
            }
            return replacement.getId();
        }
        if (schemeIsUsed(current.getId())) {
            throw new BusinessRuleException(
                    "Bộ chấm điểm đã được sử dụng. Hãy tạo phiên bản mới để bảo toàn lịch sử");
        }
        current.setName(required(form.getName(), "Tên bộ chấm điểm"));
        current.setStatus(status(form.getStatus()));
        current.setEffectiveFrom(form.getEffectiveFrom());
        return current.getId();
    }

    @Transactional
    public void disableScheme(String adminEmail, Integer id) {
        requireAdmin(adminEmail);
        ScoringScheme value = requireScheme(id);
        value.setStatus(RecordStatus.INACTIVE);
    }

    @Transactional
    public void saveBand(String adminEmail, Integer schemeId, Integer id, ScoreBandForm form) {
        requireAdmin(adminEmail);
        ScoringScheme scheme = requireSchemeForUpdate(schemeId);
        String skill = normalizeSkill(form.getSkill());
        String level = normalizeLevel(form.getCefrLevel());
        if (scheme.getStatus() != RecordStatus.ACTIVE)
            throw new BusinessRuleException("Không thể sửa bộ chấm điểm đã ngừng sử dụng");
        if (schemeIsUsed(schemeId))
            throw new BusinessRuleException(
                    "Bộ chấm điểm đã được sử dụng. Hãy tạo phiên bản mới để bảo toàn lịch sử");
        BigDecimal min = decimal(form.getMinScore());
        BigDecimal max = decimal(form.getMaxScore());
        if (min == null || max == null || min.compareTo(max) > 0)
            throw new BusinessRuleException("Khoảng điểm phải có min ≤ max");
        if (min.signum() < 0 || max.compareTo(BigDecimal.valueOf(50)) > 0)
            throw new BusinessRuleException("Khoảng điểm CEFR phải nằm trong 0–50");
        scaleRules
                .findBySchemeIdAndSkillIgnoreCase(schemeId, skill)
                .ifPresent(
                        rule -> {
                            if (min.compareTo(rule.getScaleMin()) < 0
                                    || max.compareTo(rule.getScaleMax()) > 0)
                                throw new BusinessRuleException(
                                        "Khoảng điểm CEFR nằm ngoài thang điểm đã cấu hình");
                        });
        if (id == null
                && bands.existsBySchemeIdAndSkillAndCefrLevelIgnoreCase(schemeId, skill, level))
            throw new BusinessRuleException("CEFR này đã có khoảng điểm");
        if (id != null
                && bands.existsBySchemeIdAndSkillAndCefrLevelIgnoreCaseAndIdNot(
                        schemeId, skill, level, id))
            throw new BusinessRuleException("CEFR này đã có khoảng điểm");
        List<ScoreBand> overlaps =
                bands
                        .findBySchemeIdAndSkillAndMaxScoreGreaterThanEqualAndMinScoreLessThanEqual(
                                schemeId, skill, min, max)
                        .stream()
                        .filter(existing -> id == null || !existing.getId().equals(id))
                        .toList();
        if (!overlaps.isEmpty())
            throw new BusinessRuleException("Khoảng điểm bị chồng lấn, kể cả khi chung biên");
        ScoreBand value =
                id == null
                        ? new ScoreBand()
                        : bands.findByIdAndSchemeId(id, schemeId)
                                .orElseThrow(
                                        () ->
                                                new ResourceNotFoundException(
                                                        "Không tìm thấy khoảng điểm"));
        value.setScheme(scheme);
        value.setSkill(skill);
        value.setCefrLevel(level);
        value.setMinScore(min);
        value.setMaxScore(max);
        bands.save(value);
    }

    @Transactional
    public void deleteBand(String adminEmail, Integer schemeId, Integer id) {
        requireAdmin(adminEmail);
        requireSchemeForUpdate(schemeId);
        if (schemeIsUsed(schemeId))
            throw new BusinessRuleException(
                    "Bộ chấm điểm đã được sử dụng. Hãy tạo phiên bản mới để bảo toàn lịch sử");
        ScoreBand value =
                bands.findByIdAndSchemeId(id, schemeId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy khoảng điểm"));
        bands.delete(value);
    }

    private void setScheme(
            ScoringScheme value, Certificate certificate, ScoringSchemeForm form, String version) {
        value.setCertificate(certificate);
        value.setName(required(form.getName(), "Tên bộ chấm điểm"));
        value.setVersion(version);
        value.setStatus(status(form.getStatus()));
        value.setEffectiveFrom(form.getEffectiveFrom());
    }

    private void ensureSchemeVersion(Integer certificateId, String version, Integer ignored) {
        if (schemes.existsByCertificateIdAndVersionIgnoreCase(certificateId, version))
            throw new BusinessRuleException(
                    "Phiên bản đã tồn tại trong chứng chỉ. Hãy tăng version để bảo toàn lịch sử");
    }

    private User requireAdmin(String email) {
        User user =
                users.findByEmailIgnoreCase(email)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Không có quyền thực hiện thao tác"));
        if (user.getRole() != Role.ADMIN)
            throw new AccessDeniedException("Chỉ quản trị viên mới được ghi cấu hình chấm điểm");
        return user;
    }

    private ScoringScheme requireScheme(Integer id) {
        return schemes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ chấm điểm"));
    }

    private ScoringScheme requireSchemeForUpdate(Integer id) {
        return schemes.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ chấm điểm"));
    }

    private boolean schemeIsUsed(Integer schemeId) {
        ScoringSchemeUsageContract contract = usage.getIfAvailable();
        if (contract == null)
            throw new IllegalStateException("Thiếu dịch vụ bảo vệ lịch sử chấm điểm");
        return contract.isUsed(schemeId);
    }

    @Transactional(readOnly = true)
    public ScoringScaleRuleForm scaleRule(Integer schemeId, String skill) {
        requireScheme(schemeId);
        var form = new ScoringScaleRuleForm();
        scaleRules
                .findBySchemeIdAndSkillIgnoreCase(schemeId, normalizeSkill(skill))
                .ifPresent(
                        rule -> {
                            form.setRawMin(rule.getRawMin());
                            form.setRawMax(rule.getRawMax());
                            form.setScaleMin(rule.getScaleMin());
                            form.setScaleMax(rule.getScaleMax());
                        });
        return form;
    }

    @Transactional
    public void saveScaleRule(
            String adminEmail, Integer schemeId, String skill, ScoringScaleRuleForm form) {
        requireAdmin(adminEmail);
        ScoringScheme scheme = requireSchemeForUpdate(schemeId);
        if (scheme.getStatus() != RecordStatus.ACTIVE || schemeIsUsed(schemeId))
            throw new BusinessRuleException(
                    "Hãy tạo phiên bản mới trước khi thay quy tắc của bộ chấm điểm đã dùng");
        String normalized = normalizeSkill(skill);
        BigDecimal rawMin = decimal(form.getRawMin()), rawMax = decimal(form.getRawMax());
        BigDecimal scaleMin = decimal(form.getScaleMin()), scaleMax = decimal(form.getScaleMax());
        if (rawMin == null
                || rawMax == null
                || scaleMin == null
                || scaleMax == null
                || rawMin.signum() < 0
                || rawMax.compareTo(new BigDecimal("9999.99")) > 0
                || rawMin.compareTo(rawMax) >= 0
                || scaleMin.signum() < 0
                || scaleMin.compareTo(scaleMax) >= 0
                || scaleMax.compareTo(BigDecimal.valueOf(50)) > 0)
            throw new BusinessRuleException(
                    "Khoảng raw/scale không hợp lệ; scale phải nằm trong 0–50");
        if (bands.findBySchemeIdAndSkillOrderByMinScoreAsc(schemeId, normalized).stream()
                .anyMatch(
                        band ->
                                band.getMinScore().compareTo(scaleMin) < 0
                                        || band.getMaxScore().compareTo(scaleMax) > 0))
            throw new BusinessRuleException(
                    "Khoảng scale phải chứa các khoảng điểm CEFR đã cấu hình");
        ScoringScaleRule rule =
                scaleRules
                        .findBySchemeIdAndSkillIgnoreCase(schemeId, normalized)
                        .orElseGet(ScoringScaleRule::new);
        rule.setScheme(scheme);
        rule.setSkill(normalized);
        rule.setRawMin(rawMin);
        rule.setRawMax(rawMax);
        rule.setScaleMin(scaleMin);
        rule.setScaleMax(scaleMax);
        scaleRules.save(rule);
    }

    private ScoringViews.CertificateView certificateView(Certificate value) {
        return new ScoringViews.CertificateView(
                value.getId(),
                value.getName(),
                value.getCode(),
                value.getDescription(),
                value.getStatus().name(),
                statusLabel(value.getStatus()));
    }

    private ScoringViews.SchemeView schemeView(ScoringScheme value) {
        return new ScoringViews.SchemeView(
                value.getId(),
                value.getCertificate().getId(),
                value.getCertificate().getName(),
                value.getName(),
                value.getVersion(),
                value.getStatus().name(),
                statusLabel(value.getStatus()),
                value.getEffectiveFrom());
    }

    private ScoringViews.BandView bandView(ScoreBand value) {
        return new ScoringViews.BandView(
                value.getId(),
                value.getSkill(),
                value.getCefrLevel(),
                value.getMinScore(),
                value.getMaxScore());
    }

    private RecordStatus status(String value) {
        try {
            return RecordStatus.valueOf(required(value, "Trạng thái").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("Trạng thái không hợp lệ");
        }
    }

    private String normalizeCode(String value) {
        return required(value, "Mã chứng chỉ").trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeSkill(String value) {
        String normalized =
                required(value, "Kỹ năng")
                        .trim()
                        .toUpperCase(Locale.ROOT)
                        .replaceAll("[^A-Z0-9]+", "_");
        if (!SKILLS.contains(normalized)) throw new BusinessRuleException("Kỹ năng không hợp lệ");
        return normalized;
    }

    private String normalizeLevel(String value) {
        String normalized = required(value, "CEFR").trim().toUpperCase(Locale.ROOT);
        if (!LEVELS.contains(normalized)) throw new BusinessRuleException("CEFR không hợp lệ");
        return normalized;
    }

    private BigDecimal decimal(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String required(String value, String field) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) throw new BusinessRuleException(field + " không được để trống");
        return normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String statusLabel(RecordStatus value) {
        return value == RecordStatus.ACTIVE ? "Đang sử dụng" : "Ngừng sử dụng";
    }
}
