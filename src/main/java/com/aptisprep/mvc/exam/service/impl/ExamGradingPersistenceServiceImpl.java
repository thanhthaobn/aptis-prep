package com.aptisprep.mvc.exam.service.impl;

import com.aptisprep.mvc.exam.entity.ExamAttemptEntity;
import com.aptisprep.mvc.exam.entity.ExamAttemptSnapshotEntity;
import com.aptisprep.mvc.exam.entity.UserAnswerDetailEntity;
import com.aptisprep.mvc.exam.entity.UserAnswerEntity;
import com.aptisprep.mvc.exam.repository.ExamAttemptRepository;
import com.aptisprep.mvc.exam.repository.ExamAttemptSnapshotRepository;
import com.aptisprep.mvc.exam.repository.UserAnswerDetailRepository;
import com.aptisprep.mvc.exam.repository.UserAnswerRepository;
import com.aptisprep.mvc.exam.service.ExamGradingPersistenceService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamGradingPersistenceServiceImpl implements ExamGradingPersistenceService {
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamAttemptSnapshotRepository examAttemptSnapshotRepository;
    private final UserAnswerRepository userAnswerRepository;
    private final UserAnswerDetailRepository userAnswerDetailRepository;

    public ExamGradingPersistenceServiceImpl(
            ExamAttemptRepository examAttemptRepository,
            ExamAttemptSnapshotRepository examAttemptSnapshotRepository,
            UserAnswerRepository userAnswerRepository,
            UserAnswerDetailRepository userAnswerDetailRepository) {
        this.examAttemptRepository = examAttemptRepository;
        this.examAttemptSnapshotRepository = examAttemptSnapshotRepository;
        this.userAnswerRepository = userAnswerRepository;
        this.userAnswerDetailRepository = userAnswerDetailRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExamAttemptEntity> findAttemptById(Integer attemptId) {
        return examAttemptRepository.findById(attemptId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExamAttemptEntity> findAttemptByIdAndUserId(Integer attemptId, Integer userId) {
        return examAttemptRepository.findByIdAndUserId(attemptId, userId);
    }

    @Override
    @Transactional
    public Optional<ExamAttemptEntity> findAttemptByIdAndUserIdForUpdate(
            Integer attemptId, Integer userId) {
        return examAttemptRepository.findByIdAndUserIdForUpdate(attemptId, userId);
    }

    @Override
    @Transactional
    public Optional<ExamAttemptEntity> findAttemptByIdForUpdate(Integer attemptId) {
        return examAttemptRepository.findByIdForUpdate(attemptId);
    }

    @Override
    @Transactional
    public ExamAttemptEntity saveAttempt(ExamAttemptEntity attempt) {
        return examAttemptRepository.save(attempt);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExamAttemptSnapshotEntity> findSnapshotByAttemptId(Integer attemptId) {
        return examAttemptSnapshotRepository.findByAttemptId(attemptId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAnswerEntity> findAnswersByAttemptId(Integer attemptId) {
        return userAnswerRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAnswerEntity> findAnswerByIdAndAttemptId(
            Integer answerId, Integer attemptId) {
        return userAnswerRepository.findByIdAndAttemptId(answerId, attemptId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAnswerEntity> findAnswerByAttemptIdAndQuestionId(
            Integer attemptId, Integer questionId) {
        return userAnswerRepository.findByAttemptIdAndQuestionId(attemptId, questionId);
    }

    @Override
    @Transactional
    public UserAnswerEntity saveAnswer(UserAnswerEntity answer) {
        return userAnswerRepository.save(answer);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAnswerDetailEntity> findAnswerDetail(
            Integer userAnswerId, Integer questionItemId) {
        return userAnswerDetailRepository.findByUserAnswerIdAndQuestionItemId(
                userAnswerId, questionItemId);
    }
}
