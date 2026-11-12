package com.aptisprep.mvc.service.exam;

import com.aptisprep.mvc.support.contract.IntegrationModels.AttemptHandle;

/** Command contract for starting attempts from Placement and Learning Path flows. */
public interface ExamAttemptCommandService {
    AttemptHandle start(Integer userId, Integer examId, String requestKey);

    AttemptHandle startPlacement(Integer userId, Integer examId, String requestKey);

    AttemptHandle startCheckpoint(
            Integer userId, Integer examId, Integer pathId, String requestKey);

    AttemptHandle startCheckpointRetake(
            Integer userId, Integer examId, Integer pathId, String requestKey);
}
