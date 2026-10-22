package com.aptisprep.repository;

import com.aptisprep.entity.Certificate;
import com.aptisprep.enums.RecordStatus;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, Integer> {
    List<Certificate> findByStatusOrderByName(RecordStatus status);

    List<Certificate> findByStatusOrderByNameAsc(RecordStatus status);

    List<Certificate> findAllByOrderByNameAsc();

    Optional<Certificate> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Integer id);
}
