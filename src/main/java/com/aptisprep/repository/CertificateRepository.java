package com.aptisprep.repository;

import com.aptisprep.entity.Certificate;
import com.aptisprep.enums.RecordStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, Integer> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Integer id);
    List<Certificate> findAllByOrderByNameAsc();
    List<Certificate> findByStatusOrderByNameAsc(RecordStatus status);
    Optional<Certificate> findByCodeIgnoreCase(String code);
}
