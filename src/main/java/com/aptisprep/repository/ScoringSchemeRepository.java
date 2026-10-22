package com.aptisprep.repository;

import com.aptisprep.entity.ScoringScheme;
import com.aptisprep.enums.RecordStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoringSchemeRepository extends JpaRepository<ScoringScheme, Integer> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from ScoringScheme s where s.id = :id")
    Optional<ScoringScheme> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);
    List<ScoringScheme> findByStatusOrderByName(RecordStatus status);
    List<ScoringScheme> findByCertificateIdAndStatusOrderByVersionDesc(Integer certificateId, RecordStatus status);
    List<ScoringScheme> findByCertificateIdOrderByVersionDesc(Integer certificateId);
    List<ScoringScheme> findAllByOrderByCertificateNameAscVersionDesc();
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<ScoringScheme> findByIdAndCertificateId(Integer id, Integer certificateId);
    boolean existsByCertificateIdAndVersionIgnoreCase(Integer certificateId, String version);
}
