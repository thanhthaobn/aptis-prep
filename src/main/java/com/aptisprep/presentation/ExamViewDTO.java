package com.aptisprep.presentation;

import java.util.List;

public record ExamViewDTO(ExamSummaryDTO exam, List<QuestionViewDTO> questions) {}
