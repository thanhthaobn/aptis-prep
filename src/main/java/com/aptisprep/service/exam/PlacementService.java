package com.aptisprep.service.exam;

import com.aptisprep.dto.AttemptHandleDTO;
import com.aptisprep.dto.AttemptResultDTO;
import com.aptisprep.dto.PlacementDTO.PlacementExam;
import com.aptisprep.dto.PlacementDTO.PlacementOption;
import java.util.List;

public interface PlacementService {
    public List<PlacementExam> available();

    public List<PlacementOption> availableForStudent(String email);

    public AttemptHandleDTO start(String email);

    public List<PlacementExam> available(String skill);

    public List<PlacementOption> available(String email, String skill);

    public AttemptHandleDTO start(String email, String skill);

    public AttemptHandleDTO start(String email, String skill, Integer examId);

    public AttemptResultDTO result(String email, Integer attemptId);
}
