package com.aptisprep.mvc.exam.dto;

public record AdminHistoryItemDTO(
        String learnerName, String learnerEmail, AttemptResultDTO result) {}
