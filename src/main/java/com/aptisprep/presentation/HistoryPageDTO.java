package com.aptisprep.presentation;

import java.util.List;

public record HistoryPageDTO(List<AttemptResultDTO> items, int page, int size, long totalItems) {}
