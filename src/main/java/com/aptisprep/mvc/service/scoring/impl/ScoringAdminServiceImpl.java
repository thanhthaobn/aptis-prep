package com.aptisprep.mvc.service.scoring.impl;

import com.aptisprep.mvc.dto.*;
import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import com.aptisprep.mvc.enums.CefrLevel;
import com.aptisprep.mvc.enums.Skill;
import com.aptisprep.mvc.exception.AccessDeniedException;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.exception.ResourceNotFoundException;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.service.catalog.ScoringSchemeUsageService;
import com.aptisprep.mvc.support.scoring.ScoreBandCoverage;
import com.aptisprep.mvc.service.scoring.ScoringAdminService;
import com.aptisprep.mvc.support.scoring.ScoringViews;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScoringAdminServiceImpl implements ScoringAdminService {
    private ZoneId EFFECTIVE_TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private Set<String> SKILLS = Set.copyOf(Skill.codes());
    private Set<String> LEVELS = Set.copyOf(CefrLevel.codes());
    private CertificateRepository certificateRepository;
    private ScoringSchemeRepository scoringSchemeRepository;
    private ScoreBandRepository scoreBandRepository;
    private UserRepository userRepository;
    private ScoringSchemeUsageService usage;
    private ScoringScaleRuleRepository scoringScaleRuleRepository;

    public ScoringAdminServiceImpl(
            CertificateRepository certificateRepository,
            ScoringSchemeRepository scoringSchemeRepository,
            ScoreBandRepository scoreBandRepository,
            UserRepository userRepository,
            ScoringSchemeUsageService usage,
            ScoringScaleRuleRepository scoringScaleRuleRepository) {
        this.certificateRepository = certificateRepository;
        this.scoringSchemeRepository = scoringSchemeRepository;
        this.scoreBandRepository = scoreBandRepository;
        this.userRepository = userRepository;
        this.usage = usage;
        this.scoringScaleRuleRepository = scoringScaleRuleRepository;
    }

    @Transactional(readOnly = true)
    public List<ScoringViews.CertificateView> certificateViews() {
        return certificateRepository.findAllByOrderByNameAsc().stream()
                .filter(Certificate::isAptisGeneral)
                .map(streamValue -> certificateView(streamValue))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScoringViews.SchemeView> schemeViews() {
        return scoringSchemeRepository.findAllByOrderByCertificateNameAscVersionDesc().stream()
                .map(streamValue -> schemeView(streamValue))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SchemeCatalog filteredSchemes(Integer certificateId, String examType) {
        String selected = examType == null ? "" : examType.trim();
        Map<Integer, Set<String>> usageByScheme = usage.examTypesByScheme();
        List<ScoringViews.SchemeView> values =
                schemeViews().stream()
                        .filter(scheme -> scheme.certificateId().equals(generalCertificate().getId()))
                        .filter(
                                scheme ->
                                        certificateId == null
                                                || certificateId.equals(scheme.certificateId()))
                        .filter(
                                scheme ->
                                        selected.isBlank()
                                                || ("UNASSIGNED".equals(selected)
                                                        ? !usageByScheme.containsKey(scheme.id())
                                                        : selected.equals(scheme.examType())
                                                                || usageByScheme
                                                                        .getOrDefault(
                                                                                scheme.id(), Set.of())
                                                                        .contains(selected)))
                        .toList();
        return new SchemeCatalog(values, usageByScheme);
    }

    @Transactional(readOnly = true)
    public ScoringViews.CertificateView certificate(Integer id) {
        return certificateView(
                certificateRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy chứng chỉ")));
    }

    @Transactional(readOnly = true)
    public ScoringViews.SchemeView scheme(Integer id) {
        return schemeView(
                scoringSchemeRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Không tìm thấy bộ chấm điểm")));
    }

    @Transactional(readOnly = true)
    public List<ScoringViews.BandView> bandViews(Integer schemeId, String skill) {
        ScoringScheme scheme = requireScheme(schemeId);
        String normalized = normalizeSkill(skill);
        return scoreBandRepository
                .findBySchemeIdAndSkillOrderByMinScoreAsc(scheme.getId(), normalized)
                .stream()
                .map(streamValue -> bandView(streamValue))
                .toList();
    }

    @Transactional(readOnly = true)
    public ScoringViews.BandView band(Integer schemeId, Integer bandId) {
        requireScheme(schemeId);
        return bandView(
                scoreBandRepository
                        .findByIdAndSchemeId(bandId, schemeId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy khoảng điểm")));
    }

    @Transactional(readOnly = true)
    public List<ScoringViews.BandView> allBandViews(Integer schemeId) {
        requireScheme(schemeId);
        return scoreBandRepository.findBySchemeIdOrderBySkillAscMinScoreAsc(schemeId).stream()
                .map(streamValue -> bandView(streamValue))
                .toList();
    }

    @Transactional
    public Integer createCertificate(String adminEmail, CertificateDTO form) {
        User admin = requireAdmin(adminEmail);
        String code = normalizeCode(form.getCode());
        requireAptisGeneralCode(code);
        if (certificateRepository.existsByCodeIgnoreCase(code))
            throw new BusinessRuleException("Mã chứng chỉ đã tồn tại");
        Certificate value = CertificateDTO.convertDtoToEntity(form);
        value.setName(required(form.getName(), "Tên chứng chỉ"));
        value.setCode(code);
        value.setDescription(blankToNull(form.getDescription()));
        value.setStatus(status(form.getStatus()));
        value.setCreatedBy(admin);
        try {
            return certificateRepository.save(value).getId();
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessRuleException("Mã chứng chỉ đã tồn tại");
        }
    }

    @Transactional
    public void updateCertificate(String adminEmail, Integer id, CertificateDTO form) {
        requireAdmin(adminEmail);
        Certificate value =
                certificateRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy chứng chỉ"));
        String code = normalizeCode(form.getCode());
        if (!value.isAptisGeneral())
            throw new BusinessRuleException("Hệ thống chỉ phục vụ chứng chỉ Aptis ESOL General");
        requireAptisGeneralCode(code);
        if (certificateRepository.existsByCodeIgnoreCaseAndIdNot(code, id))
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
                certificateRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy chứng chỉ"));
        value.setStatus(RecordStatus.INACTIVE);
    }

    @Transactional
    public Integer saveSchemeWithScores(String email, Integer id, ScoringSchemeDTO form) {
        Integer savedId = saveScheme(email, id, form);
        if (!form.isConfigureScores() || blankToNull(form.getExamType()) == null) {
            if (requireScheme(savedId).getStatus() == RecordStatus.ACTIVE)
                requireReady(requireScheme(savedId));
            return savedId;
        }
        ScoringScheme scheme = requireSchemeForUpdate(savedId);
        List<String> skills =
                scheme.getScopeSkill() == null ? Skill.codes() : List.of(scheme.getScopeSkill());
        if (scheme.getStatus() == RecordStatus.INACTIVE) return savedId;
        for (String skill : skills) {
            ScoringScaleRuleDTO scale = form.getSkillScales().get(skill);
            if (!hasScale(scale)) {
                scale = new ScoringScaleRuleDTO();
                scale.setRawMin(form.getScale().getRawMin());
                scale.setRawMax(form.getScale().getRawMax());
                scale.setScaleMin(form.getScale().getScaleMin());
                scale.setScaleMax(form.getScale().getScaleMax());
            }
            boolean hasBands =
                    CefrLevel.scoringCodes().stream()
                            .map(level -> form.getRanges().get(skill + "_" + level))
                            .anyMatch(
                                    band ->
                                            band != null
                                                    && (band.getMinScore() != null
                                                            || band.getMaxScore() != null));
            boolean targetOnly =
                    scheme.getScopeSkill() != null
                            && (blankToNull(form.getTargetLevel()) != null
                                    || form.getPassingScore() != null);
            if (targetOnly && hasBands)
                throw new BusinessRuleException(
                        "Chọn ngưỡng đạt hoặc bảng CEFR đầy đủ, không dùng đồng thời");
            if (!hasScale(scale) && !hasBands && !targetOnly) continue;
            // Practice mode derives rawMax from the locked exam. The persisted 0–50 raw range is
            // retained only for compatibility with the existing schema and old admin pages.
            if (scale.getRawMin() == null) scale.setRawMin(BigDecimal.ZERO);
            if (scale.getRawMax() == null) scale.setRawMax(BigDecimal.valueOf(50));
            if (scale.getScaleMin() == null) scale.setScaleMin(BigDecimal.ZERO);
            if (scale.getScaleMax() == null) scale.setScaleMax(BigDecimal.valueOf(50));
            var previousBands =
                    scoreBandRepository.findBySchemeIdAndSkillOrderByMinScoreAsc(savedId, skill);
            scoreBandRepository.deleteAll(previousBands);
            scoreBandRepository.flush();
            saveScaleRule(email, savedId, skill, scale);
            if (targetOnly) {
                saveConfiguredBand(
                        email,
                        savedId,
                        skill,
                        form.getTargetLevel(),
                        form.getPassingScore(),
                        scale.getScaleMax());
            } else {
                for (String level : CefrLevel.scoringCodes()) {
                    if (CefrLevel.BELOW_A1.equals(level) && !supportsBelowA1(scheme, skill))
                        continue;
                    ScoreBandDTO band = form.getRanges().get(skill + "_" + level);
                    if (band != null && (band.getMinScore() != null || band.getMaxScore() != null))
                        saveConfiguredBand(
                                email,
                                savedId,
                                skill,
                                level,
                                band.getMinScore(),
                                band.getMaxScore());
                }
            }
        }
        if (scheme.getStatus() == RecordStatus.ACTIVE) requireReady(scheme);
        return savedId;
    }

    private boolean hasScale(ScoringScaleRuleDTO scale) {
        return scale != null
                && (scale.getRawMin() != null
                        || scale.getRawMax() != null
                        || scale.getScaleMin() != null
                        || scale.getScaleMax() != null);
    }

    @Transactional
    public void activateScheme(String email, Integer id) {
        requireAdmin(email);
        ScoringScheme scheme = requireSchemeForUpdate(id);
        requireReady(scheme);
        if (scheme.getStatus() != RecordStatus.ACTIVE || scheme.getEffectiveFrom() == null)
            scheme.setEffectiveFrom(LocalDateTime.now(EFFECTIVE_TIME_ZONE));
        scheme.setStatus(RecordStatus.ACTIVE);
    }

    @Transactional
    public void activateCertificate(String email, Integer id) {
        requireAdmin(email);
        Certificate certificate =
                certificateRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy chứng chỉ"));
        certificate.setStatus(RecordStatus.ACTIVE);
    }

    private void requireReady(ScoringScheme scheme) {
        if (scheme.getCertificate().getStatus() != RecordStatus.ACTIVE)
            throw new BusinessRuleException("Kích hoạt chứng chỉ trước khi dùng bộ chấm điểm");
        List<String> skills;
        if (scheme.getScopeSkill() != null) {
            skills = List.of(scheme.getScopeSkill());
        } else if (scheme.getExamType() == null) {
            skills =
                    scoringScaleRuleRepository.findBySchemeId(scheme.getId()).stream()
                            .map(streamValue -> streamValue.getSkill())
                            .distinct()
                            .toList();
            if (skills.isEmpty()) throw new BusinessRuleException("Chưa cấu hình thang điểm");
        } else {
            skills = Skill.codes();
        }
        for (String skill : skills) {
            ScoringScaleRule rule =
                    scoringScaleRuleRepository
                            .findBySchemeIdAndSkillIgnoreCase(scheme.getId(), skill)
                            .orElseThrow(
                                    () ->
                                            new BusinessRuleException(
                                                    "Chưa có thang điểm cho " + skill));
            if (rule.getRawMin().compareTo(rule.getRawMax()) >= 0
                    || rule.getScaleMin().compareTo(rule.getScaleMax()) >= 0)
                throw new BusinessRuleException("Thang điểm không hợp lệ cho " + skill);
            if (Set.of("WRITING", "SPEAKING").contains(skill)
                    && (rule.getRawMin().signum() != 0
                            || rule.getRawMax().compareTo(BigDecimal.valueOf(50)) != 0))
                throw new BusinessRuleException("Writing và Speaking cần thang điểm thô 0–50");
            if (scheme.getScopeSkill() == null
                    && scheme.getExamType() != null
                    && (rule.getScaleMin().signum() != 0
                            || rule.getScaleMax().compareTo(BigDecimal.valueOf(50)) != 0))
                throw new BusinessRuleException(
                        "Đề tổng hợp cần điểm quy đổi 0–50 cho từng kỹ năng");
            var bands =
                    scoreBandRepository.findBySchemeIdAndSkillOrderByMinScoreAsc(
                            scheme.getId(), skill);
            if (scheme.getScopeSkill() != null && bands.size() == 1) {
                if (bands.getFirst().getMinScore().compareTo(rule.getScaleMin()) < 0
                        || bands.getFirst().getMaxScore().compareTo(rule.getScaleMax()) != 0)
                    throw new BusinessRuleException("Ngưỡng đạt nằm ngoài thang điểm");
            } else {
                ScoreBandCoverage.requireComplete(rule, bands);
            }
        }
    }

    private void saveConfiguredBand(
            String email, Integer id, String skill, String level, BigDecimal min, BigDecimal max) {
        if (blankToNull(level) == null || min == null || max == null)
            throw new BusinessRuleException("Nhập đầy đủ trình độ và khoảng điểm cho " + skill);
        ScoreBandDTO band = new ScoreBandDTO();
        band.setSkill(skill);
        band.setCefrLevel(level);
        band.setMinScore(min);
        band.setMaxScore(max);
        saveBand(email, id, null, band);
    }

    @Transactional(readOnly = true)
    public void populateScoreConfiguration(Integer id, ScoringSchemeDTO form) {
        if (form.getExamType() == null) return;
        for (ScoringScaleRule rule : scoringScaleRuleRepository.findBySchemeId(id)) {
            var scale = form.getSkillScales().get(rule.getSkill());
            if (scale == null) continue;
            scale.setRawMin(rule.getRawMin());
            scale.setRawMax(rule.getRawMax());
            scale.setScaleMin(rule.getScaleMin());
            scale.setScaleMax(rule.getScaleMax());
        }
        var bands = scoreBandRepository.findBySchemeIdOrderBySkillAscMinScoreAsc(id);
        for (ScoreBand band : bands) {
            ScoreBandDTO field = form.getRanges().get(band.getSkill() + "_" + band.getCefrLevel());
            if (field != null) {
                field.setMinScore(band.getMinScore());
                field.setMaxScore(band.getMaxScore());
            }
        }
        if (form.getScopeSkill() != null) {
            var scoped =
                    bands.stream()
                            .filter(band -> band.getSkill().equals(form.getScopeSkill()))
                            .toList();
            if (scoped.size() == 1) {
                form.setTargetLevel(scoped.getFirst().getCefrLevel());
                form.setPassingScore(scoped.getFirst().getMinScore());
                var field =
                        form.getRanges()
                                .get(form.getScopeSkill() + "_" + scoped.getFirst().getCefrLevel());
                field.setMinScore(null);
                field.setMaxScore(null);
            }
        }
    }

    @Transactional
    public Integer saveScheme(String adminEmail, Integer id, ScoringSchemeDTO form) {
        requireAdmin(adminEmail);
        Certificate certificate =
                certificateRepository
                        .findByCodeIgnoreCase("APTIS_GENERAL")
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Chưa cấu hình chứng chỉ Aptis ESOL General"));
        form.setCertificateId(certificate.getId());
        if (status(form.getStatus()) == RecordStatus.ACTIVE
                && certificate.getStatus() != RecordStatus.ACTIVE)
            throw new BusinessRuleException("Cần kích hoạt chứng chỉ trước khi dùng bộ chấm điểm");
        String version = required(form.getVersion(), "Phiên bản");
        if (id == null) {
            ensureSchemeVersion(certificate.getId(), version, null);
            ScoringScheme value = ScoringSchemeDTO.convertDtoToEntity(form);
            setScheme(value, certificate, form, version);
            return scoringSchemeRepository.save(value).getId();
        }
        ScoringScheme current = requireSchemeForUpdate(id);
        if (!current.getCertificate().getId().equals(certificate.getId())
                || !current.getVersion().equalsIgnoreCase(version)) {
            ensureSchemeVersion(certificate.getId(), version, null);
            ScoringScheme replacement = ScoringSchemeDTO.convertDtoToEntity(form);
            setScheme(replacement, certificate, form, version);
            replacement = scoringSchemeRepository.save(replacement);
            copySchemeConfiguration(id, replacement);
            return replacement.getId();
        }
        if (schemeIsUsed(current.getId())) {
            throw new BusinessRuleException(
                    "Bộ chấm điểm đã được sử dụng. Hãy tạo phiên bản mới để bảo toàn lịch sử");
        }
        setScheme(current, certificate, form, version);
        return current.getId();
    }

    @Transactional
    public void disableScheme(String adminEmail, Integer id) {
        requireAdmin(adminEmail);
        ScoringScheme value = requireScheme(id);
        value.setStatus(RecordStatus.INACTIVE);
    }

    @Transactional
    public void saveBand(String adminEmail, Integer schemeId, Integer id, ScoreBandDTO form) {
        requireAdmin(adminEmail);
        ScoringScheme scheme = requireSchemeForUpdate(schemeId);
        String skill = normalizeSkill(form.getSkill());
        requireScopeSkill(scheme, skill);
        String level =
                CefrLevel.BELOW_A1.equals(form.getCefrLevel()) && supportsBelowA1(scheme, skill)
                        ? CefrLevel.BELOW_A1
                        : normalizeLevel(form.getCefrLevel());
        if (scheme.getCertificate().isAptisGeneral() && CefrLevel.C2.name().equals(level))
            throw new BusinessRuleException("Aptis ESOL General chỉ có mức tối đa C1");
        if (scheme.getStatus() == RecordStatus.INACTIVE)
            throw new BusinessRuleException("Không thể sửa bộ chấm điểm đã ngừng sử dụng");
        if (schemeIsUsed(schemeId))
            throw new BusinessRuleException(
                    "Bộ chấm điểm đã được sử dụng. Hãy tạo phiên bản mới để bảo toàn lịch sử");
        BigDecimal min = decimal(form.getMinScore());
        BigDecimal max = decimal(form.getMaxScore());
        if (min == null || max == null || min.compareTo(max) > 0)
            throw new BusinessRuleException("Khoảng điểm phải có min ≤ max");
        if (min.signum() < 0 || max.compareTo(new BigDecimal("999.99")) > 0)
            throw new BusinessRuleException("Khoảng điểm CEFR phải nằm trong 0–999.99");
        scoringScaleRuleRepository
                .findBySchemeIdAndSkillIgnoreCase(schemeId, skill)
                .ifPresent(
                        rule -> {
                            if (min.compareTo(rule.getScaleMin()) < 0
                                    || max.compareTo(rule.getScaleMax()) > 0)
                                throw new BusinessRuleException(
                                        "Khoảng điểm CEFR nằm ngoài thang điểm đã cấu hình");
                        });
        if (id == null
                && scoreBandRepository.existsBySchemeIdAndSkillAndCefrLevelIgnoreCase(
                        schemeId, skill, level))
            throw new BusinessRuleException("CEFR này đã có khoảng điểm");
        if (id != null
                && scoreBandRepository.existsBySchemeIdAndSkillAndCefrLevelIgnoreCaseAndIdNot(
                        schemeId, skill, level, id))
            throw new BusinessRuleException("CEFR này đã có khoảng điểm");
        List<ScoreBand> overlaps =
                scoreBandRepository
                        .findBySchemeIdAndSkillAndMaxScoreGreaterThanEqualAndMinScoreLessThanEqual(
                                schemeId, skill, min, max)
                        .stream()
                        .filter(existing -> id == null || !existing.getId().equals(id))
                        .toList();
        if (!overlaps.isEmpty())
            throw new BusinessRuleException("Khoảng điểm bị chồng lấn, kể cả khi chung biên");
        ScoreBand value =
                id == null
                        ? ScoreBandDTO.convertDtoToEntity(form)
                        : scoreBandRepository
                                .findByIdAndSchemeId(id, schemeId)
                                .orElseThrow(
                                        () ->
                                                new ResourceNotFoundException(
                                                        "Không tìm thấy khoảng điểm"));
        value.setScheme(scheme);
        value.setSkill(skill);
        value.setCefrLevel(level);
        value.setMinScore(min);
        value.setMaxScore(max);
        scoreBandRepository.save(value);
    }

    @Transactional
    public void deleteBand(String adminEmail, Integer schemeId, Integer id) {
        requireAdmin(adminEmail);
        requireSchemeForUpdate(schemeId);
        if (schemeIsUsed(schemeId))
            throw new BusinessRuleException(
                    "Bộ chấm điểm đã được sử dụng. Hãy tạo phiên bản mới để bảo toàn lịch sử");
        ScoreBand value =
                scoreBandRepository
                        .findByIdAndSchemeId(id, schemeId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Không tìm thấy khoảng điểm"));
        scoreBandRepository.delete(value);
    }

    private void setScheme(
            ScoringScheme value, Certificate certificate, ScoringSchemeDTO form, String version) {
        value.setCertificate(certificate);
        value.setName(required(form.getName(), "Tên bộ chấm điểm"));
        value.setVersion(version);
        String type = blankToNull(form.getExamType());
        String skill = blankToNull(form.getScopeSkill());
        if (type != null
                && !Set.of("PLACEMENT", "FULL_MOCK", "CHECKPOINT", "PRACTICE").contains(type))
            throw new BusinessRuleException("Loại đề không hợp lệ");
        boolean singleSkill = "CHECKPOINT".equals(type) || "PRACTICE".equals(type);
        if (singleSkill && (skill == null || !Skill.codes().contains(skill)))
            throw new BusinessRuleException("Chọn kỹ năng cho bộ chấm điểm");
        value.setExamType(type);
        value.setScopeSkill(singleSkill ? skill : null);
        RecordStatus nextStatus = status(form.getStatus());
        LocalDateTime now = LocalDateTime.now(EFFECTIVE_TIME_ZONE);
        if (nextStatus == RecordStatus.DRAFT) {
            value.setEffectiveFrom(null);
        } else if (nextStatus == RecordStatus.ACTIVE
                && (value.getStatus() != RecordStatus.ACTIVE
                        || value.getEffectiveFrom() == null
                        || value.getEffectiveFrom().isAfter(now))) {
            value.setEffectiveFrom(now);
        }
        value.setStatus(nextStatus);
    }

    private void ensureSchemeVersion(Integer certificateId, String version, Integer ignored) {
        if (scoringSchemeRepository.existsByCertificateIdAndVersionIgnoreCase(
                certificateId, version))
            throw new BusinessRuleException(
                    "Phiên bản đã tồn tại trong chứng chỉ. Hãy tăng version để bảo toàn lịch sử");
    }

    private User requireAdmin(String email) {
        User user =
                userRepository
                        .findByEmailIgnoreCase(email)
                        .orElseThrow(
                                () ->
                                        new AccessDeniedException(
                                                "Không có quyền thực hiện thao tác"));
        if (user.getRole() != Role.ADMIN)
            throw new AccessDeniedException("Chỉ quản trị viên mới được ghi cấu hình chấm điểm");
        return user;
    }

    private ScoringScheme requireScheme(Integer id) {
        return scoringSchemeRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ chấm điểm"));
    }

    private ScoringScheme requireSchemeForUpdate(Integer id) {
        return scoringSchemeRepository
                .findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ chấm điểm"));
    }

    private boolean schemeIsUsed(Integer schemeId) {
        return usage.isUsed(schemeId);
    }

    @Transactional(readOnly = true)
    public ScoringScaleRuleDTO scaleRule(Integer schemeId, String skill) {
        requireScheme(schemeId);
        ScoringScaleRuleDTO form = new ScoringScaleRuleDTO();
        scoringScaleRuleRepository
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
            String adminEmail, Integer schemeId, String skill, ScoringScaleRuleDTO form) {
        requireAdmin(adminEmail);
        ScoringScheme scheme = requireSchemeForUpdate(schemeId);
        if (scheme.getStatus() == RecordStatus.INACTIVE || schemeIsUsed(schemeId))
            throw new BusinessRuleException(
                    "Hãy tạo phiên bản mới trước khi thay quy tắc của bộ chấm điểm đã dùng");
        String normalized = normalizeSkill(skill);
        requireScopeSkill(scheme, normalized);
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
                || scaleMax.compareTo(new BigDecimal("999.99")) > 0)
            throw new BusinessRuleException(
                    "Khoảng raw/scale không hợp lệ; scale phải nằm trong 0–999.99");
        if (scoreBandRepository
                .findBySchemeIdAndSkillOrderByMinScoreAsc(schemeId, normalized)
                .stream()
                .anyMatch(
                        band ->
                                band.getMinScore().compareTo(scaleMin) < 0
                                        || band.getMaxScore().compareTo(scaleMax) > 0))
            throw new BusinessRuleException(
                    "Khoảng scale phải chứa các khoảng điểm CEFR đã cấu hình");
        ScoringScaleRule rule =
                scoringScaleRuleRepository
                        .findBySchemeIdAndSkillIgnoreCase(schemeId, normalized)
                        .orElseGet(() -> new ScoringScaleRule());
        rule.setScheme(scheme);
        rule.setSkill(normalized);
        rule.setRawMin(rawMin);
        rule.setRawMax(rawMax);
        rule.setScaleMin(scaleMin);
        rule.setScaleMax(scaleMax);
        scoringScaleRuleRepository.save(rule);
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

    private Certificate generalCertificate() {
        return certificateRepository.findByCodeIgnoreCase("APTIS_GENERAL")
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Chưa cấu hình chứng chỉ Aptis ESOL General"));
    }

    private void requireAptisGeneralCode(String code) {
        if (!"APTIS_GENERAL".equalsIgnoreCase(code))
            throw new BusinessRuleException(
                    "Hệ thống chỉ phục vụ chứng chỉ Aptis ESOL General");
    }

    private ScoringViews.SchemeView schemeView(ScoringScheme value) {
        LocalDateTime effectiveFrom = value.getEffectiveFrom();
        if (value.getStatus() == RecordStatus.DRAFT
                || (effectiveFrom != null
                        && effectiveFrom.isAfter(LocalDateTime.now(EFFECTIVE_TIME_ZONE)))) {
            effectiveFrom = null;
        }
        return new ScoringViews.SchemeView(
                value.getId(),
                value.getCertificate().getId(),
                value.getCertificate().getName(),
                value.getName(),
                value.getVersion(),
                value.getStatus().name(),
                statusLabel(value.getStatus()),
                value.getExamType(),
                value.getScopeSkill(),
                effectiveFrom);
    }

    private ScoringViews.BandView bandView(ScoreBand value) {
        return new ScoringViews.BandView(
                value.getId(),
                value.getSkill(),
                value.getCefrLevel(),
                value.getMinScore(),
                value.getMaxScore());
    }

    private void requireScopeSkill(ScoringScheme scheme, String skill) {
        if (scheme.getScopeSkill() != null && !scheme.getScopeSkill().equals(skill))
            throw new BusinessRuleException("Kỹ năng không thuộc phạm vi của bộ chấm điểm");
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

    private boolean supportsBelowA1(ScoringScheme scheme, String skill) {
        return "FULL_MOCK".equals(scheme.getExamType())
                && !Skill.GRAMMAR_VOCABULARY.name().equals(skill);
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
        return switch (value) {
            case ACTIVE -> "Đang sử dụng";
            case INACTIVE -> "Ngừng sử dụng";
            case DRAFT -> "Bản nháp";
        };
    }

    private void copySchemeConfiguration(Integer id, ScoringScheme replacement) {
        for (ScoreBand old : scoreBandRepository.findBySchemeIdOrderBySkillAscMinScoreAsc(id)) {
            ScoreBand copy = new ScoreBand();
            copy.setScheme(replacement);
            copy.setSkill(old.getSkill());
            copy.setCefrLevel(old.getCefrLevel());
            copy.setMinScore(old.getMinScore());
            copy.setMaxScore(old.getMaxScore());
            scoreBandRepository.save(copy);
        }
        for (ScoringScaleRule old : scoringScaleRuleRepository.findBySchemeId(id)) {
            ScoringScaleRule copy = new ScoringScaleRule();
            copy.setScheme(replacement);
            copy.setSkill(old.getSkill());
            copy.setRawMin(old.getRawMin());
            copy.setRawMax(old.getRawMax());
            copy.setScaleMin(old.getScaleMin());
            copy.setScaleMax(old.getScaleMax());
            scoringScaleRuleRepository.save(copy);
        }
    }
}







