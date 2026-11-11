package com.aptisprep.dto.view;

import com.aptisprep.enums.UnlockRequestStatus;
import java.time.LocalDateTime;

public record AccountUnlockRequestDTO(
        Integer id,
        Integer userId,
        String userEmail,
        String userFullName,
        String userStatus,
        String message,
        UnlockRequestStatus status,
        String adminNote,
        String handledByEmail,
        LocalDateTime createdAt,
        LocalDateTime handledAt) {}
