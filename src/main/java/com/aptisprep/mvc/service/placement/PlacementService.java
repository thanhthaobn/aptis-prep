package com.aptisprep.mvc.service.placement;

import com.aptisprep.mvc.support.contract.IntegrationModels.AttemptHandle;
import com.aptisprep.mvc.support.contract.IntegrationModels.AttemptResult;
import java.util.List;

public interface PlacementService {
    public default List<PlacementExamCatalogService.PlacementExam> available() {
        return available("FULL_MOCK");
    }

    public default List<PlacementOption> availableForStudent(String email) {
        return available(email, "FULL_MOCK");
    }

    public default AttemptHandle start(String email) {
        return start(email, "FULL_MOCK");
    }

    public List<PlacementExamCatalogService.PlacementExam> available(String skill);

    public List<PlacementOption> available(String email, String skill);

    public AttemptHandle start(String email, String skill);

    public AttemptHandle start(String email, String skill, Integer examId);

    public AttemptResult result(String email, Integer attemptId);

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







