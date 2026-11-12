package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.AttemptOutbox;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptOutboxRepository extends JpaRepository<AttemptOutbox, Long> {
    Optional<AttemptOutbox> findByEventKey(String eventKey);

    List<AttemptOutbox> findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByIdAsc(
            Collection<String> statuses, LocalDateTime time);
}
