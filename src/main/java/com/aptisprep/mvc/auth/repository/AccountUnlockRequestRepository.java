package com.aptisprep.mvc.auth.repository;

import com.aptisprep.mvc.auth.entity.AccountUnlockRequest;
import com.aptisprep.mvc.common.enums.UnlockRequestStatus;
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
