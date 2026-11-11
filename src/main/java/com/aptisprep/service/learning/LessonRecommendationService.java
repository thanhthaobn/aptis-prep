package com.aptisprep.service.learning;

import com.aptisprep.presentation.LessonRecommendationDTO;
import com.aptisprep.presentation.PathProgressDTO;
import java.util.List;

public interface LessonRecommendationService {
    LessonRecommendationDTO recommend(Integer userId, List<PathProgressDTO> paths);

    record Recommendation(
            Integer lessonId, String title, String skill, PathProgressDTO path, String reason) {}
}
