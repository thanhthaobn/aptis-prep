package com.aptisprep.repository;

import com.aptisprep.entity.ScoringScheme;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoringSchemeRepository extends JpaRepository<ScoringScheme, Integer> {
    List<ScoringScheme> findAllByOrderByCertificateNameAscVersionDesc();
    List<ScoringScheme> findByCertificateIdOrderByVersionDesc(Integer certificateId);
    Optional<ScoringScheme> findByIdAndCertificateId(Integer id, Integer certificateId);
    boolean existsByCertificateIdAndVersionIgnoreCase(Integer certificateId, String version);
}
