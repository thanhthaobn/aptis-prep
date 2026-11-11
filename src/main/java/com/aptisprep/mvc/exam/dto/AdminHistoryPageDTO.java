package com.aptisprep.mvc.exam.dto;

import java.util.List;

public record AdminHistoryPageDTO(
        List<AdminHistoryItemDTO> items, int page, int size, long totalItems) {}
