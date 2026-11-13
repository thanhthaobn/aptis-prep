package com.aptisprep.mvc.repository;

import com.aptisprep.mvc.entity.AccountUnlockRequest;
import com.aptisprep.mvc.enums.UnlockRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountUnlockRequestRepository
        extends JpaRepository<AccountUnlockRequest, Integer> {
    boolean existsByUserIdAndStatus(Integer userId, UnlockRequestStatus status);

    Optional<AccountUnlockRequest> findFirstByUser_EmailIgnoreCaseOrderByCreatedAtDesc(
            String email);

    List<AccountUnlockRequest> findAllByOrderByCreatedAtDesc();

    Page<AccountUnlockRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<AccountUnlockRequest> findByStatusOrderByCreatedAtDesc(UnlockRequestStatus status);

    Page<AccountUnlockRequest> findByStatusOrderByCreatedAtDesc(
            UnlockRequestStatus status, Pageable pageable);

    long countByStatus(UnlockRequestStatus status);
}
