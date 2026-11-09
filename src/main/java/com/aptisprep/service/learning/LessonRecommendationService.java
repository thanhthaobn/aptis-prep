package com.aptisprep.service.learning;

import com.aptisprep.service.contract.IntegrationModels.PathProgress;
import java.util.List;

public interface LessonRecommendationService {
    record Recommendation(
            Integer lessonId, String title, String skill, PathProgress path, String reason) {}

    Recommendation recommend(Integer userId, List<PathProgress> paths);
}
