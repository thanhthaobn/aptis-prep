package com.aptisprep.mvc.scoring.repository;

import com.aptisprep.mvc.common.enums.RecordStatus;
import com.aptisprep.mvc.scoring.entity.Certificate;
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
