package com.aptisprep.service.admin;

import com.aptisprep.dto.SchemeExamTypeDTO;
import java.util.List;

public interface ScoringSchemeUsageService {
    public boolean isUsed(Integer schemeId);

    public List<SchemeExamTypeDTO> findScoringSchemeExamTypes();
}
