package com.aptisprep.dto.view;

public record LessonRecommendationDTO(
        Integer lessonId, String title, String skill, PathProgressDTO path, String reason) {}
