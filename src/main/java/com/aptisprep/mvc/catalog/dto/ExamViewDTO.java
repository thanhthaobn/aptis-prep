package com.aptisprep.mvc.catalog.dto;

import java.util.List;

public record ExamViewDTO(ExamSummaryDTO exam, List<QuestionViewDTO> questions) {}
