package com.aptisprep.repository;

import com.aptisprep.entity.ScoringScheme;
import com.aptisprep.enums.RecordStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoringSchemeRepository extends JpaRepository<ScoringScheme, Integer> {
    List<ScoringScheme> findByStatusOrderByName(RecordStatus status);
    List<ScoringScheme> findByCertificateIdAndStatusOrderByVersionDesc(Integer certificateId, RecordStatus status);
    List<ScoringScheme> findByCertificateIdOrderByVersionDesc(Integer certificateId);
    List<ScoringScheme> findAllByOrderByCertificateNameAscVersionDesc();
    Optional<ScoringScheme> findByIdAndCertificateId(Integer id, Integer certificateId);
    boolean existsByCertificateIdAndVersionIgnoreCase(Integer certificateId, String version);
}
