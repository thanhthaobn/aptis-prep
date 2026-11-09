package com.aptisprep.service.placement;

import com.aptisprep.service.contract.IntegrationModels.AttemptHandle;
import com.aptisprep.service.contract.IntegrationModels.AttemptResult;
import java.util.List;

public interface PlacementService {
    List<PlacementExamCatalogService.PlacementExam> available(String skill);
    List<PlacementOption> available(String email, String skill);
    AttemptHandle start(String email, String skill);
    AttemptHandle start(String email, String skill, Integer examId);
    AttemptResult result(String email, Integer attemptId);

    record PlacementOption(
            Integer id,
            String title,
            String subtitle,
            int durationMinutes,
            List<PlacementExamCatalogService.Section> sections,
            boolean resumable) {
        public String displayTitle() {
            return title == null ? null : title.replaceFirst("^Aptis Placement 01\\s*[–-]\\s*", "");
        }
    }
}
