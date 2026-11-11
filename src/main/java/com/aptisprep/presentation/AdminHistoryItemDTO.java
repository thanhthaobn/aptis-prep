package com.aptisprep.presentation;

public record AdminHistoryItemDTO(
        String learnerName, String learnerEmail, AttemptResultDTO result) {}
