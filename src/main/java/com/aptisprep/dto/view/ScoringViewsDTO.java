package com.aptisprep.dto.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ScoringViewsDTO {
    private ScoringViewsDTO() {}

    public record CertificateView(
            Integer id,
            String name,
            String code,
            String description,
            String status,
            String statusLabel) {}

    public record SchemeView(
            Integer id,
            Integer certificateId,
            String certificateName,
            String name,
            String version,
            String status,
            String statusLabel,
            String examType,
            String scopeSkill,
            LocalDateTime effectiveFrom) {}

    public record SchemeExamTypeView(Integer schemeId, String examType) {}

    public record BandView(
            Integer id, String skill, String cefrLevel, BigDecimal minScore, BigDecimal maxScore) {}
}
