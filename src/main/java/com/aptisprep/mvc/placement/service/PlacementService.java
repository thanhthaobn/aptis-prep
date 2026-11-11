package com.aptisprep.mvc.placement.service;

import com.aptisprep.mvc.exam.dto.AttemptHandleDTO;
import com.aptisprep.mvc.exam.dto.AttemptResultDTO;
import com.aptisprep.mvc.placement.dto.PlacementDTO.PlacementExam;
import com.aptisprep.mvc.placement.dto.PlacementDTO.PlacementOption;
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
