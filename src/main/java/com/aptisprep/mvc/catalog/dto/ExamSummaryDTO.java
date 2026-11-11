package com.aptisprep.mvc.catalog.dto;

public record ExamSummaryDTO(
        Integer examId,
        String title,
        String examType,
        String skill,
        String targetLevel,
        Integer durationMinutes,
        Integer scoringSchemeId) {}
