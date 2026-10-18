package com.aptisprep.repository;

import com.aptisprep.entity.CertificateEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<CertificateEntity, Integer> {
    List<CertificateEntity> findByStatusOrderByName(String status);
}
