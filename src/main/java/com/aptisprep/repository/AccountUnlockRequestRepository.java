package com.aptisprep.repository;

import com.aptisprep.entity.AccountUnlockRequest;
import com.aptisprep.enums.UnlockRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountUnlockRequestRepository
        extends JpaRepository<AccountUnlockRequest, Integer> {

    boolean existsByUserIdAndStatus(Integer userId, UnlockRequestStatus status);

    Optional<AccountUnlockRequest> findFirstByUserIdOrderByCreatedAtDesc(Integer userId);

    Optional<AccountUnlockRequest> findFirstByUser_EmailIgnoreCaseOrderByCreatedAtDesc(
            String email);

    List<AccountUnlockRequest> findByStatusOrderByCreatedAtDesc(UnlockRequestStatus status);

    List<AccountUnlockRequest> findAllByOrderByCreatedAtDesc();

    long countByStatus(UnlockRequestStatus status);
}
