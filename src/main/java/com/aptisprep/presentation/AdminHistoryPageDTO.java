package com.aptisprep.presentation;

import java.util.List;

public record AdminHistoryPageDTO(
        List<AdminHistoryItemDTO> items, int page, int size, long totalItems) {}
