package com.aptisprep.service.contract;

import com.aptisprep.service.contract.IntegrationModels.PathProgress;
import java.util.List;

/** Owner 2. No AI dependency; progress math follows REAL_PROGRESS_CONTRACT.md. */
public interface LearningPathContract {
    /** Repeated calls with unchanged inputs must not create duplicate active paths. */
    void ensurePaths(Integer userId);
    List<PathProgress> summaries(Integer userId);
    PathProgress summary(Integer userId, Integer pathId);
    void completeLesson(Integer userId, Integer lessonId);
    /** Revalidates mandatory lessons and calls the shared attempt engine. */
    Integer startCheckpoint(Integer userId, Integer pathId, String requestKey);
    /** Idempotent processing of a completed checkpoint; calls owner 1 for Profile writes. */
    void applyCheckpoint(Integer userId, Integer attemptId);
}
