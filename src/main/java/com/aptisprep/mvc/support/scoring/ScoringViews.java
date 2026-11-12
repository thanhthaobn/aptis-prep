package com.aptisprep.mvc.support.scoring;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ScoringViews {
    private ScoringViews() {}

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

    public record BandView(
            Integer id, String skill, String cefrLevel, BigDecimal minScore, BigDecimal maxScore) {}
}
