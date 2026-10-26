package com.aptisprep.service.catalog;

import com.aptisprep.repository.ExamAttemptRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.service.contract.ScoringSchemeUsageContract;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A referenced policy is immutable, including policies retained by old attempts. */
@Service
public class ScoringSchemeUsageService implements ScoringSchemeUsageContract {
    private final MockExamRepository mockExamRepository;
    private final ExamAttemptRepository examAttemptRepository;

    public ScoringSchemeUsageService(
            MockExamRepository mockExamRepository, ExamAttemptRepository examAttemptRepository) {
        this.mockExamRepository = mockExamRepository;
        this.examAttemptRepository = examAttemptRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsed(Integer schemeId) {
        return mockExamRepository.existsByScoringSchemeId(schemeId)
                || examAttemptRepository.existsByScoringSchemeId(schemeId);
    }
}
