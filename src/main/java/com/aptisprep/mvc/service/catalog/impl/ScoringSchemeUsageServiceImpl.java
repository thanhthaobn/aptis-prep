package com.aptisprep.mvc.service.catalog.impl;

import com.aptisprep.mvc.repository.ExamAttemptRepository;
import com.aptisprep.mvc.repository.MockExamRepository;
import com.aptisprep.mvc.service.catalog.ScoringSchemeUsageService;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A referenced policy is immutable, including policies retained by old attempts. */
@Service
public class ScoringSchemeUsageServiceImpl implements ScoringSchemeUsageService {
    private MockExamRepository mockExamRepository;
    private ExamAttemptRepository examAttemptRepository;

    public ScoringSchemeUsageServiceImpl(
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

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Set<String>> examTypesByScheme() {
        Map<Integer, Set<String>> result = new HashMap<>();
        for (var row : mockExamRepository.findScoringSchemeExamTypes()) {
            result.computeIfAbsent(row.getSchemeId(), ignored -> new LinkedHashSet<>())
                    .add(row.getExamType());
        }
        return result;
    }
}
