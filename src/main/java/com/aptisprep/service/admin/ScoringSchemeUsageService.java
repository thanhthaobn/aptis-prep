package com.aptisprep.service.admin;

import com.aptisprep.presentation.SchemeExamTypeDTO;
import java.util.List;

public interface ScoringSchemeUsageService {
    boolean isUsed(Integer schemeId);

    List<SchemeExamTypeDTO> findScoringSchemeExamTypes();
}
