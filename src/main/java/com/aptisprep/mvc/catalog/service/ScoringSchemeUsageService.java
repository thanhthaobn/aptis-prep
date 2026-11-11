package com.aptisprep.mvc.catalog.service;

import java.util.List;

public interface ScoringSchemeUsageService {
    boolean isUsed(Integer schemeId);

    List<SchemeExamTypeDTO> findScoringSchemeExamTypes();

    record SchemeExamTypeDTO(Integer schemeId, String examType) {}
}
