package com.aptisprep.repository;

import com.aptisprep.entity.ScoringSchemeEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoringSchemeRepository extends JpaRepository<ScoringSchemeEntity, Integer> {
    List<ScoringSchemeEntity> findByStatusOrderByName(String status);
    List<ScoringSchemeEntity> findByCertificateIdAndStatusOrderByVersionDesc(Integer certificateId, String status);
}
