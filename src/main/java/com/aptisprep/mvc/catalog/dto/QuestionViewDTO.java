package com.aptisprep.mvc.catalog.dto;

/** Safe learner view: deliberately contains no answers or internal grading context. */
public record QuestionViewDTO(
        Integer questionId, String skill, String type, int position, String content) {}
