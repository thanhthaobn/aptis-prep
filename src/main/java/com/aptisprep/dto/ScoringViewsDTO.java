package com.aptisprep.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ScoringViewsDTO {
    private ScoringViewsDTO() {}

    public static class CertificateView {
        private Integer id;
        private String name;
        private String code;
        private String description;
        private String status;
        private String statusLabel;

        public CertificateView() {}

        public CertificateView(
                Integer id,
                String name,
                String code,
                String description,
                String status,
                String statusLabel) {
            this.id = id;
            this.name = name;
            this.code = code;
            this.description = description;
            this.status = status;
            this.statusLabel = statusLabel;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer id() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String description() {
            return description;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String status() {
            return status;
        }

        public String getStatusLabel() {
            return statusLabel;
        }

        public void setStatusLabel(String statusLabel) {
            this.statusLabel = statusLabel;
        }

        public String statusLabel() {
            return statusLabel;
        }
    }

    public static class SchemeView {
        private Integer id;
        private Integer certificateId;
        private String certificateName;
        private String name;
        private String version;
        private String status;
        private String statusLabel;
        private String examType;
        private String scopeSkill;
        private LocalDateTime effectiveFrom;

        public SchemeView() {}

        public SchemeView(
                Integer id,
                Integer certificateId,
                String certificateName,
                String name,
                String version,
                String status,
                String statusLabel,
                String examType,
                String scopeSkill,
                LocalDateTime effectiveFrom) {
            this.id = id;
            this.certificateId = certificateId;
            this.certificateName = certificateName;
            this.name = name;
            this.version = version;
            this.status = status;
            this.statusLabel = statusLabel;
            this.examType = examType;
            this.scopeSkill = scopeSkill;
            this.effectiveFrom = effectiveFrom;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer id() {
            return id;
        }

        public Integer getCertificateId() {
            return certificateId;
        }

        public void setCertificateId(Integer certificateId) {
            this.certificateId = certificateId;
        }

        public Integer certificateId() {
            return certificateId;
        }

        public String getCertificateName() {
            return certificateName;
        }

        public void setCertificateName(String certificateName) {
            this.certificateName = certificateName;
        }

        public String certificateName() {
            return certificateName;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String version() {
            return version;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String status() {
            return status;
        }

        public String getStatusLabel() {
            return statusLabel;
        }

        public void setStatusLabel(String statusLabel) {
            this.statusLabel = statusLabel;
        }

        public String statusLabel() {
            return statusLabel;
        }

        public String getExamType() {
            return examType;
        }

        public void setExamType(String examType) {
            this.examType = examType;
        }

        public String examType() {
            return examType;
        }

        public String getScopeSkill() {
            return scopeSkill;
        }

        public void setScopeSkill(String scopeSkill) {
            this.scopeSkill = scopeSkill;
        }

        public String scopeSkill() {
            return scopeSkill;
        }

        public LocalDateTime getEffectiveFrom() {
            return effectiveFrom;
        }

        public void setEffectiveFrom(LocalDateTime effectiveFrom) {
            this.effectiveFrom = effectiveFrom;
        }

        public LocalDateTime effectiveFrom() {
            return effectiveFrom;
        }
    }

    public static class SchemeExamTypeView {
        private Integer schemeId;
        private String examType;

        public SchemeExamTypeView() {}

        public SchemeExamTypeView(Integer schemeId, String examType) {
            this.schemeId = schemeId;
            this.examType = examType;
        }

        public Integer getSchemeId() {
            return schemeId;
        }

        public void setSchemeId(Integer schemeId) {
            this.schemeId = schemeId;
        }

        public Integer schemeId() {
            return schemeId;
        }

        public String getExamType() {
            return examType;
        }

        public void setExamType(String examType) {
            this.examType = examType;
        }

        public String examType() {
            return examType;
        }
    }

    public static class BandView {
        private Integer id;
        private String skill;
        private String cefrLevel;
        private BigDecimal minScore;
        private BigDecimal maxScore;

        public BandView() {}

        public BandView(
                Integer id,
                String skill,
                String cefrLevel,
                BigDecimal minScore,
                BigDecimal maxScore) {
            this.id = id;
            this.skill = skill;
            this.cefrLevel = cefrLevel;
            this.minScore = minScore;
            this.maxScore = maxScore;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer id() {
            return id;
        }

        public String getSkill() {
            return skill;
        }

        public void setSkill(String skill) {
            this.skill = skill;
        }

        public String skill() {
            return skill;
        }

        public String getCefrLevel() {
            return cefrLevel;
        }

        public void setCefrLevel(String cefrLevel) {
            this.cefrLevel = cefrLevel;
        }

        public String cefrLevel() {
            return cefrLevel;
        }

        public BigDecimal getMinScore() {
            return minScore;
        }

        public void setMinScore(BigDecimal minScore) {
            this.minScore = minScore;
        }

        public BigDecimal minScore() {
            return minScore;
        }

        public BigDecimal getMaxScore() {
            return maxScore;
        }

        public void setMaxScore(BigDecimal maxScore) {
            this.maxScore = maxScore;
        }

        public BigDecimal maxScore() {
            return maxScore;
        }
    }
}
