package com.aptisprep.mvc.learning.dto;

public record LessonRecommendationDTO(
        Integer lessonId, String title, String skill, PathProgressDTO path, String reason) {}
