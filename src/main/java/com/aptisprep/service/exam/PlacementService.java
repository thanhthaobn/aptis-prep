package com.aptisprep.service.exam;

import com.aptisprep.presentation.AttemptHandleDTO;
import com.aptisprep.presentation.AttemptResultDTO;
import com.aptisprep.presentation.PlacementDTO.PlacementExam;
import com.aptisprep.presentation.PlacementDTO.PlacementOption;
import java.util.List;

public interface PlacementService {
    List<PlacementExam> available();

    List<PlacementOption> availableForStudent(String email);

    AttemptHandleDTO start(String email);

    List<PlacementExam> available(String skill);

    List<PlacementOption> available(String email, String skill);

    AttemptHandleDTO start(String email, String skill);

    AttemptHandleDTO start(String email, String skill, Integer examId);

    AttemptResultDTO result(String email, Integer attemptId);
}
