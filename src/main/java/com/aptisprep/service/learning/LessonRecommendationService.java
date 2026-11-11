package com.aptisprep.service.learning;

import com.aptisprep.dto.LessonRecommendationDTO;
import com.aptisprep.dto.PathProgressDTO;
import java.util.List;

public interface LessonRecommendationService {
    public LessonRecommendationDTO recommend(Integer userId, List<PathProgressDTO> paths);

    public record Recommendation(
            Integer lessonId, String title, String skill, PathProgressDTO path, String reason) {}
}
