package com.aptisprep.dto;

public class LessonStatsDTO extends LessonTeachingViewsDTO.LessonStatsDTO {
    public LessonStatsDTO() {
        super();
    }

    public LessonStatsDTO(
            long started,
            long completed,
            long audioListeners,
            long attempts,
            double averagePercent) {
        super(started, completed, audioListeners, attempts, averagePercent);
    }
}
