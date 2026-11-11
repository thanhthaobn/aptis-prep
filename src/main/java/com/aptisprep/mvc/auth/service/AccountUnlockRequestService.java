package com.aptisprep.mvc.auth.service;

import com.aptisprep.mvc.auth.dto.AccountUnlockRequestDTO;
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
