package com.aptisprep.mvc.catalog.service.impl;

import com.aptisprep.mvc.catalog.repository.MockExamRepository;
import com.aptisprep.mvc.catalog.service.ScoringSchemeUsageService;
import com.aptisprep.mvc.exam.service.ExamAttemptService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A referenced policy is immutable, including policies retained by old attempts. */
@Service
public class ScoringSchemeUsageServiceImpl implements ScoringSchemeUsageService {
    private final MockExamRepository mockExamRepository;
    private final ExamAttemptService examAttemptService;

    public ScoringSchemeUsageServiceImpl(
            MockExamRepository mockExamRepository, ExamAttemptService examAttemptService) {
        this.mockExamRepository = mockExamRepository;
        this.examAttemptService = examAttemptService;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsed(Integer schemeId) {
        return mockExamRepository.existsByScoringSchemeId(schemeId)
                || examAttemptService.hasAttemptsForScoringScheme(schemeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchemeExamTypeDTO> findScoringSchemeExamTypes() {
        return mockExamRepository.findScoringSchemeExamTypes().stream()
                .map(x -> new SchemeExamTypeDTO(x.getSchemeId(), x.getExamType()))
                .toList();
    }
}
