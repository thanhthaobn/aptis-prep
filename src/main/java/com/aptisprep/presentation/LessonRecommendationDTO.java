package com.aptisprep.presentation;

public record LessonRecommendationDTO(
        Integer lessonId, String title, String skill, PathProgressDTO path, String reason) {}
