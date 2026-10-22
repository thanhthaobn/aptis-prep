package com.aptisprep.service.catalog;

import com.aptisprep.repository.ExamAttemptRepository;
import com.aptisprep.repository.MockExamRepository;
import com.aptisprep.service.contract.ScoringSchemeUsageContract;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A referenced policy is immutable, including policies retained by old attempts. */
@Service
public class ScoringSchemeUsageService implements ScoringSchemeUsageContract {
    private final MockExamRepository exams;
    private final ExamAttemptRepository attempts;

    public ScoringSchemeUsageService(MockExamRepository exams, ExamAttemptRepository attempts) {
        this.exams = exams;
        this.attempts = attempts;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsed(Integer schemeId) {
        return exams.existsByScoringSchemeId(schemeId) || attempts.existsByScoringSchemeId(schemeId);
    }
}
