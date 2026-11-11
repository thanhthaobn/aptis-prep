package com.aptisprep.repository;

import com.aptisprep.entity.UserMediaActivityEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserMediaActivityRepository
        extends JpaRepository<UserMediaActivityEntity, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select value from UserMediaActivityEntity value where value.attemptId = :attemptId and value.mediaId = :mediaId")
    Optional<UserMediaActivityEntity> findByAttemptIdAndMediaIdForUpdate(
            @Param("attemptId") Integer attemptId, @Param("mediaId") Integer mediaId);
}
