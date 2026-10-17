package com.aptisprep.repository;

import com.aptisprep.entity.ScoringScheme;
import com.aptisprep.enums.RecordStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoringSchemeRepository extends JpaRepository<ScoringScheme, Integer> {
    boolean existsByCertificateIdAndVersionIgnoreCase(Integer certificateId, String version);
    boolean existsByCertificateIdAndVersionIgnoreCaseAndIdNot(Integer certificateId, String version, Integer id);
    List<ScoringScheme> findAllByOrderByCertificateNameAscVersionDesc();
    List<ScoringScheme> findByStatusOrderByCertificateNameAscVersionDesc(RecordStatus status);
    List<ScoringScheme> findByCertificateIdOrderByVersionDesc(Integer certificateId);
    Optional<ScoringScheme> findByIdAndCertificateId(Integer id, Integer certificateId);
}
