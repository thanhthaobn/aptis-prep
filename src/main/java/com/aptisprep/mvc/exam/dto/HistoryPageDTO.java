package com.aptisprep.mvc.exam.dto;

import java.util.List;

public record HistoryPageDTO(List<AttemptResultDTO> items, int page, int size, long totalItems) {}
