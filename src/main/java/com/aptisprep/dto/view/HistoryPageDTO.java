package com.aptisprep.dto.view;

import java.util.List;

public record HistoryPageDTO(List<AttemptResultDTO> items, int page, int size, long totalItems) {}
