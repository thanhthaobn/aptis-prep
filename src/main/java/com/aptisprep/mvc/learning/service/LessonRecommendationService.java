package com.aptisprep.mvc.learning.service;

import com.aptisprep.mvc.learning.dto.LessonRecommendationDTO;
import com.aptisprep.mvc.learning.dto.PathProgressDTO;
import java.util.List;

public interface LessonRecommendationService {
    LessonRecommendationDTO recommend(Integer userId, List<PathProgressDTO> paths);

    record Recommendation(
            Integer lessonId, String title, String skill, PathProgressDTO path, String reason) {}
}
