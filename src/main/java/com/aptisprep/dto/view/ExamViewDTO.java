package com.aptisprep.dto.view;

import java.util.List;

public record ExamViewDTO(ExamSummaryDTO exam, List<QuestionViewDTO> questions) {}
