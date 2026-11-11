package com.aptisprep.dto;

public class ExamSummaryDTO extends ExamCatalogDTO.ExamSummaryDTO {
    public ExamSummaryDTO() {
        super();
    }

    public ExamSummaryDTO(
            Integer examId,
            String title,
            String examType,
            String skill,
            String targetLevel,
            Integer durationMinutes,
            Integer scoringSchemeId) {
        super(examId, title, examType, skill, targetLevel, durationMinutes, scoringSchemeId);
    }
}
