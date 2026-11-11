package com.aptisprep.service.grading.impl;

import com.aptisprep.entity.ExamSkillResultEntity;
import com.aptisprep.repository.ExamSkillResultRepository;
import com.aptisprep.service.grading.GradingResultQueryService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradingResultQueryServiceImpl implements GradingResultQueryService {
    private final ExamSkillResultRepository examSkillResultRepository;

    public GradingResultQueryServiceImpl(ExamSkillResultRepository examSkillResultRepository) {
        this.examSkillResultRepository = examSkillResultRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamSkillResultEntity> findSkillResultsByAttemptId(Integer attemptId) {
        return examSkillResultRepository.findByAttemptIdOrderBySkillAsc(attemptId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByAttemptId(Integer attemptId) {
        return examSkillResultRepository.existsByAttemptId(attemptId);
    }
}
