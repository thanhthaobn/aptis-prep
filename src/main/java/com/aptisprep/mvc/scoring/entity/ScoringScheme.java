package com.aptisprep.mvc.scoring.entity;

import com.aptisprep.mvc.common.enums.RecordStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "scoring_schemes",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_scoring_schemes_version",
                        columnNames = {"certificate_id", "version"}))
public class ScoringScheme {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certificate_id", nullable = false)
    private Certificate certificate;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 30)
    private String version;

    @Column(name = "exam_type", length = 30)
    private String examType;

    @Column(name = "scope_skill", length = 30)
    private String scopeSkill;

    public String getExamType() {
        return examType;
    }

    public void setExamType(String value) {
        examType = value;
    }

    public String getScopeSkill() {
        return scopeSkill;
    }

    public void setScopeSkill(String value) {
        scopeSkill = value;
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecordStatus status;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public RecordStatus getStatus() {
        return status;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setStatus(RecordStatus status) {
        this.status = status;
    }

    public void setEffectiveFrom(LocalDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }
}
