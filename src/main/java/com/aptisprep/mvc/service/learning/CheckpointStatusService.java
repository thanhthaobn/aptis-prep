package com.aptisprep.mvc.service.learning;

import com.aptisprep.mvc.entity.LearningPath;
import com.aptisprep.mvc.support.contract.IntegrationModels.AttemptResult;
import java.util.List;

/** Đọc trạng thái checkpoint theo lộ trình và lịch sử attempt. */
public interface CheckpointStatusService {
    public String state(LearningPath path);

    public List<AttemptResult> history(LearningPath path);

    public boolean unfinished(AttemptResult result);

    public boolean terminal(AttemptResult result);

    public boolean notBeforePath(AttemptResult result, LearningPath path);
}
