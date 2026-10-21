package com.aptisprep.repository;

import com.aptisprep.entity.AttemptOutboxEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptOutboxRepository extends JpaRepository<AttemptOutboxEntity, Long> {
    Optional<AttemptOutboxEntity> findByEventKey(String eventKey);
    List<AttemptOutboxEntity> findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(java.util.Collection<String> statuses, LocalDateTime time);
}
