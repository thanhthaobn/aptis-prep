package com.aptisprep.mvc.service.catalog.impl;

import com.aptisprep.mvc.entity.MockExam;
import com.aptisprep.mvc.dto.MockExamDTO;
import com.aptisprep.mvc.exception.ResourceNotFoundException;
import com.aptisprep.mvc.repository.MockExamQuestionRepository;
import com.aptisprep.mvc.repository.MockExamRepository;
import com.aptisprep.mvc.repository.QuestionRepository;
import com.aptisprep.mvc.service.catalog.PublishedExamCatalogService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PublishedExamCatalogServiceImpl implements PublishedExamCatalogService {
    private MockExamRepository mockExamRepository;
    private MockExamQuestionRepository mockExamQuestionRepository;
    private QuestionRepository questionRepository;

    public PublishedExamCatalogServiceImpl(
            MockExamRepository mockExamRepository,
            MockExamQuestionRepository mockExamQuestionRepository,
            QuestionRepository questionRepository) {
        this.mockExamRepository = mockExamRepository;
        this.mockExamQuestionRepository = mockExamQuestionRepository;
        this.questionRepository = questionRepository;
    }

    @Override
    public MockExamDTO publishedExam(Integer userId, Integer examId) {
        MockExam exam = mockExamRepository.findById(examId)
                .filter(x -> "PUBLISHED".equals(x.getStatus()) && !x.isDemoOnly())
                .orElseThrow(() -> new ResourceNotFoundException("Đề thi chưa được công bố hoặc không tồn tại"));
        List<Integer> questionIds = mockExamQuestionRepository.findByMockExamIdOrderByPosition(examId).stream()
                .map(link -> questionRepository.findById(link.getQuestionId())
                        .filter(q -> "PUBLISHED".equals(q.getStatus()))
                        .map(q -> q.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Đề thi chứa câu hỏi chưa công bố")))
                .toList();
        if (questionIds.isEmpty()) throw new ResourceNotFoundException("Đề thi chưa có câu hỏi");
        return MockExamDTO.convertEntityToDto(exam);
    }

    @Override
    public MockExamDTO checkpoint(String skill, String targetLevel) {
        return mockExamRepository.findByStatusOrderByUpdatedAtDesc("PUBLISHED").stream()
                .filter(x -> !x.isDemoOnly())
                .filter(x -> "CHECKPOINT".equals(x.getExamType()) && skill.equals(x.getSkill()) && targetLevel.equals(x.getTargetLevel()))
                .filter(x -> !mockExamQuestionRepository.findByMockExamIdOrderByPosition(x.getId()).isEmpty())
                .findFirst().map(streamValue -> MockExamDTO.convertEntityToDto(streamValue))
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có đề checkpoint phù hợp"));
    }
}







