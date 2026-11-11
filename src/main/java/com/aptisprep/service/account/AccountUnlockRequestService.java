package com.aptisprep.service.account;

import com.aptisprep.dto.view.AccountUnlockRequestDTO;
import java.util.List;
import java.util.Optional;

public interface AccountUnlockRequestService {

    AccountUnlockRequestDTO submit(String email, String password, String message);

    Optional<AccountUnlockRequestDTO> findLatestByEmail(String email);

    List<AccountUnlockRequestDTO> list(String statusFilter);

    AccountUnlockRequestDTO approve(String adminEmail, Integer requestId, String adminNote);

    AccountUnlockRequestDTO reject(String adminEmail, Integer requestId, String adminNote);

    long countPending();
}
