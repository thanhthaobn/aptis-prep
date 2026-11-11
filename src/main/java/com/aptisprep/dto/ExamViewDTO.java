package com.aptisprep.dto;

import java.util.List;

public class ExamViewDTO {
    private ExamSummaryDTO exam;
    private List<QuestionViewDTO> questions;

    public ExamViewDTO() {}

    public ExamViewDTO(ExamSummaryDTO exam, List<QuestionViewDTO> questions) {
        this.exam = exam;
        this.questions = questions;
    }

    public ExamViewDTO(ExamCatalogDTO.ExamSummaryDTO exam, List<QuestionViewDTO> questions) {
        if (exam instanceof ExamSummaryDTO) {
            this.exam = (ExamSummaryDTO) exam;
        } else if (exam != null) {
            this.exam = new ExamSummaryDTO(
                    exam.examId(),
                    exam.title(),
                    exam.examType(),
                    exam.skill(),
                    exam.targetLevel(),
                    exam.durationMinutes(),
                    exam.scoringSchemeId());
        }
        this.questions = questions;
    }

    public ExamSummaryDTO getExam() { return exam; }
    public void setExam(ExamSummaryDTO exam) { this.exam = exam; }
    public ExamSummaryDTO exam() { return exam; }

    public List<QuestionViewDTO> getQuestions() { return questions; }
    public void setQuestions(List<QuestionViewDTO> questions) { this.questions = questions; }
    public List<QuestionViewDTO> questions() { return questions; }
}
