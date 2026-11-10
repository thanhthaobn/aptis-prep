package com.aptisprep.mvc.placement.service;

import com.aptisprep.mvc.placement.dto.PlacementDTO.PlacementExam;
import com.aptisprep.mvc.placement.dto.PlacementDTO.PlacementOption;
import com.aptisprep.service.contract.IntegrationModels.AttemptHandle;
import com.aptisprep.service.contract.IntegrationModels.AttemptResult;
import java.util.List;

public interface PlacementService {
    List<PlacementExam> available();

    List<PlacementOption> availableForStudent(String email);

    AttemptHandle start(String email);

    List<PlacementExam> available(String skill);

    List<PlacementOption> available(String email, String skill);

    AttemptHandle start(String email, String skill);

    AttemptHandle start(String email, String skill, Integer examId);

    AttemptResult result(String email, Integer attemptId);
}
