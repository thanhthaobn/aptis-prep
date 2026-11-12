package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.UserMediaActivity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserMediaActivityRepository
        extends JpaRepository<UserMediaActivity, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select value from UserMediaActivity value where value.attemptId = :attemptId and value.mediaId = :mediaId")
    Optional<UserMediaActivity> findByAttemptIdAndMediaIdForUpdate(
            @Param("attemptId") Integer attemptId, @Param("mediaId") Integer mediaId);
}







