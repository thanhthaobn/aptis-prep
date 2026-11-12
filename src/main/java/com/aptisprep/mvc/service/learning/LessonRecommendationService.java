package com.aptisprep.mvc.service.learning;

import com.aptisprep.mvc.support.contract.IntegrationModels.PathProgress;
import java.util.List;

public interface LessonRecommendationService {
    record Recommendation(
            Integer lessonId, String title, String skill, PathProgress path, String reason) {}

    public Recommendation recommend(Integer userId, List<PathProgress> paths);
}
