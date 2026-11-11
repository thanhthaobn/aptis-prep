package com.aptisprep.dto.view;

public record ExamSummaryDTO(
        Integer examId,
        String title,
        String examType,
        String skill,
        String targetLevel,
        Integer durationMinutes,
        Integer scoringSchemeId) {}
