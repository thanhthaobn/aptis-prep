package com.aptisprep.dto.view;

public record AdminHistoryItemDTO(
        String learnerName, String learnerEmail, AttemptResultDTO result) {}
