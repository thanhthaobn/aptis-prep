package com.aptisprep.mvc.service.exam.impl;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.Skill;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.exception.ResourceNotFoundException;
import com.aptisprep.mvc.repository.*;
import com.aptisprep.mvc.service.exam.ExamReadinessService;
import com.aptisprep.mvc.service.scoring.CertificateSchemeService;
import com.aptisprep.mvc.service.storage.StorageService;
import java.util.*;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/** Các kiểm tra sẵn sàng dùng chung cho catalog và transaction bắt đầu đề thi. */
@Service
public class ExamReadinessServiceImpl implements ExamReadinessService {
    private QuestionRepository questionRepository;
    private MockExamSectionRepository mockExamSectionRepository;
    private QuestionMediaRepository questionMediaRepository;
    private CertificateSchemeService schemes;
    private StorageService storage;

    public ExamReadinessServiceImpl(
            QuestionRepository questionRepository,
            MockExamSectionRepository mockExamSectionRepository,
            QuestionMediaRepository questionMediaRepository,
            CertificateSchemeService schemes,
            StorageService storage) {
        this.questionRepository = questionRepository;
        this.mockExamSectionRepository = mockExamSectionRepository;
        this.questionMediaRepository = questionMediaRepository;
        this.schemes = schemes;
        this.storage = storage;
    }

    @Override
    public void validate(MockExam exam, List<MockExamQuestion> links) {
        if (!"PUBLISHED".equals(exam.getStatus()) || exam.isDemoOnly())
            throw new BusinessRuleException("Đề chưa được công bố hoặc chỉ dành cho demo");
        validateForPublication(exam, links);
    }

    @Override
    public void validateForPublication(MockExam exam, List<MockExamQuestion> links) {
        if (exam.getDurationMinutes() == null || exam.getDurationMinutes() <= 0)
            throw new BusinessRuleException("Thời lượng đề thi phải lớn hơn 0");
        if (links.isEmpty()) throw new BusinessRuleException("Đề thi chưa có câu hỏi");
        Map<Integer, MockExamSection> sectionById = new HashMap<>();
        for (MockExamSection section :
                mockExamSectionRepository.findByMockExamIdOrderById(exam.getId())) {
            if (section.getDurationMinutes() <= 0)
                throw new BusinessRuleException("Thời lượng section phải lớn hơn 0");
            sectionById.put(section.getId(), section);
        }
        Set<Integer> uniqueQuestions = new HashSet<>();
        Set<Integer> usedSections = new HashSet<>();
        Set<String> skills = new HashSet<>();
        for (MockExamQuestion link : links) {
            if (!Objects.equals(link.getMockExamId(), exam.getId()))
                throw new BusinessRuleException("Câu hỏi không thuộc đề thi");
            if (!uniqueQuestions.add(link.getQuestionId()))
                throw new BusinessRuleException("Đề thi chứa câu hỏi bị lặp");
            Question question =
                    questionRepository
                            .findById(link.getQuestionId())
                            .orElseThrow(
                                    () ->
                                            new BusinessRuleException(
                                                    "Đề thi chứa câu hỏi không tồn tại"));
            if (!"PUBLISHED".equals(question.getStatus()))
                throw new BusinessRuleException("Đề thi chứa câu hỏi chưa được công bố");
            String skill = normalizeSkill(question.getSkill());
            if (!Skill.codes().contains(skill))
                throw new BusinessRuleException("Kỹ năng câu hỏi không hợp lệ");
            skills.add(skill);
            if (link.getMockExamSectionId() != null) {
                MockExamSection section = sectionById.get(link.getMockExamSectionId());
                if (section == null) throw new BusinessRuleException("Section không thuộc đề thi");
                if (!skill.equals(normalizeSkill(section.getSkill())))
                    throw new BusinessRuleException("Kỹ năng section không khớp với câu hỏi");
                usedSections.add(section.getId());
            } else if (!sectionById.isEmpty()) {
                throw new BusinessRuleException("Câu hỏi chưa được gán vào section của đề");
            }
            boolean hasAudio = false;
            for (QuestionMedia audio :
                    questionMediaRepository.findByQuestionIdOrderByPosition(question.getId())) {
                if ("AUDIO".equals(audio.getMediaType())) {
                    hasAudio = true;
                    validateAudio(audio.getMediaUrl());
                }
            }
            if ("LISTENING".equals(skill) && !hasAudio)
                throw new BusinessRuleException("Câu hỏi Listening chưa có file âm thanh");
        }
        if (usedSections.size() != sectionById.size())
            throw new BusinessRuleException("Đề thi có phần chưa được gán câu hỏi");
        if ("FULL_MOCK".equals(exam.getExamType())
                || ("PLACEMENT".equals(exam.getExamType())
                        && "FULL_MOCK".equals(exam.getSkill()))) {
            if (!skills.equals(new HashSet<>(Skill.codes())))
                throw new BusinessRuleException("Thi thử tổng hợp phải có đủ 5 kỹ năng Aptis");
        } else if (skills.size() != 1 || !skills.contains(normalizeSkill(exam.getSkill()))) {
            throw new BusinessRuleException("Câu hỏi phải thuộc đúng kỹ năng của đề");
        }
        if (exam.getScoringSchemeId() != null) {
            try {
                for (String skill : skills)
                    schemes.requireAvailableScheme(
                            exam.getCertificateId(), exam.getScoringSchemeId(), skill);
            } catch (ResourceNotFoundException exception) {
                throw new BusinessRuleException(exception.getMessage());
            }
        } else if (Set.of("CHECKPOINT", "PLACEMENT", "FULL_MOCK").contains(exam.getExamType())) {
            throw new BusinessRuleException("Loại đề này cần bộ chấm điểm để xác định CEFR");
        }
    }

    @Override
    public void validateAudio(String path) {
        if (path != null && !path.isBlank() && !path.startsWith("speech://")) {
            try {
                Resource resource = storage.load(path);
                if (resource.exists() && resource.isReadable()) return;
            } catch (RuntimeException ignored) {
                // Transcript không bao giờ thay thế được file audio đề thi bị thiếu.
            }
        }
        throw new BusinessRuleException(
                "Audio đề thi chưa sẵn sàng. Admin cần tải file âm thanh trước khi mở đề");
    }

    private String normalizeSkill(String value) {
        return Objects.toString(value, "")
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_");
    }
}
