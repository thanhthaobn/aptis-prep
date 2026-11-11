package com.aptisprep.service.account;

import com.aptisprep.dto.AccountUnlockRequestDTO;
import java.util.List;
import java.util.Optional;

public interface AccountUnlockRequestService {

    public AccountUnlockRequestDTO submit(String email, String password, String message);

    public Optional<AccountUnlockRequestDTO> findLatestByEmail(String email);

    public List<AccountUnlockRequestDTO> list(String statusFilter);

    public AccountUnlockRequestDTO approve(String adminEmail, Integer requestId, String adminNote);

    public AccountUnlockRequestDTO reject(String adminEmail, Integer requestId, String adminNote);

    public long countPending();
}
