package com.aptisprep.mvc.service.account;

import com.aptisprep.mvc.dto.AccountUnlockRequestDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

public interface AccountUnlockRequestService {
    AccountUnlockRequestDTO submit(String email, String password, String message);

    Optional<AccountUnlockRequestDTO> findLatestByEmail(String email);

    List<AccountUnlockRequestDTO> list(String status);

    Page<AccountUnlockRequestDTO> page(String status, int page, int size);

    AccountUnlockRequestDTO approve(String adminEmail, Integer requestId, String adminNote);

    AccountUnlockRequestDTO reject(String adminEmail, Integer requestId, String adminNote);

    long countPending();
}
