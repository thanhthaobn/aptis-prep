package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.ScoringScheme;
import com.aptisprep.mvc.enums.RecordStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScoringSchemeRepository extends JpaRepository<ScoringScheme, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ScoringScheme s where s.id = :id")
    Optional<ScoringScheme> findByIdForUpdate(@Param("id") Integer id);

    List<ScoringScheme> findByStatusOrderByName(RecordStatus status);

    List<ScoringScheme> findByCertificateIdAndStatusOrderByVersionDesc(
            Integer certificateId, RecordStatus status);

    List<ScoringScheme> findByCertificateIdOrderByVersionDesc(Integer certificateId);

    List<ScoringScheme> findAllByOrderByCertificateNameAscVersionDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ScoringScheme> findByIdAndCertificateId(Integer id, Integer certificateId);

    boolean existsByCertificateIdAndVersionIgnoreCase(Integer certificateId, String version);
}
