package com.aptisprep.service.admin.impl;

import com.aptisprep.dto.SchemeExamTypeDTO;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.service.admin.ScoringSchemeUsageService;
import com.aptisprep.service.exam.ExamAttemptService;
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
