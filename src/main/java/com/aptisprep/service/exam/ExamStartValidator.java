package com.aptisprep.service.exam;

import com.aptisprep.entity.*;
import com.aptisprep.enums.Skill;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.*;
import com.aptisprep.service.contract.CertificateSchemeContract;
import com.aptisprep.service.storage.StorageService;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.*;

/** Shared readiness checks for the catalog and the actual start transaction. */
@Component
public class ExamStartValidator {
    private final QuestionRepository questionRepository;
    private final MockExamSectionRepository mockExamSectionRepository;
    private final QuestionMediaRepository questionMediaRepository;
    private final CertificateSchemeContract schemes;
    private final StorageService storage;

    public ExamStartValidator(
            QuestionRepository questionRepository,
            MockExamSectionRepository mockExamSectionRepository,
            QuestionMediaRepository questionMediaRepository,
            CertificateSchemeContract schemes,
            StorageService storage) {
        this.questionRepository = questionRepository;
        this.mockExamSectionRepository = mockExamSectionRepository;
        this.questionMediaRepository = questionMediaRepository;
        this.schemes = schemes;
        this.storage = storage;
    }

    public void validate(MockExamEntity exam, List<MockExamQuestionEntity> links) {
        if (!"PUBLISHED".equals(exam.getStatus()) || exam.isDemoOnly())
            throw new BusinessRuleException("Đề chưa được công bố hoặc chỉ dành cho demo");
        validateForPublication(exam, links);
    }

    public void validateForPublication(MockExamEntity exam, List<MockExamQuestionEntity> links) {
        if (exam.getDurationMinutes() == null || exam.getDurationMinutes() <= 0)
            throw new BusinessRuleException("Thời lượng đề thi phải lớn hơn 0");
        if (links.isEmpty()) throw new BusinessRuleException("Đề thi chưa có câu hỏi");
        Map<Integer, MockExamSectionEntity> sectionById = new HashMap<>();
        for (MockExamSectionEntity section :
                mockExamSectionRepository.findByMockExamIdOrderById(exam.getId())) {
            if (section.getDurationMinutes() <= 0)
                throw new BusinessRuleException("Thời lượng section phải lớn hơn 0");
            sectionById.put(section.getId(), section);
        }
        Set<Integer> uniqueQuestions = new HashSet<>();
        Set<Integer> usedSections = new HashSet<>();
        Set<String> skills = new HashSet<>();
        for (MockExamQuestionEntity link : links) {
            if (!Objects.equals(link.getMockExamId(), exam.getId()))
                throw new BusinessRuleException("Câu hỏi không thuộc đề thi");
            if (!uniqueQuestions.add(link.getQuestionId()))
                throw new BusinessRuleException("Đề thi chứa câu hỏi bị lặp");
            QuestionEntity question =
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
                MockExamSectionEntity section = sectionById.get(link.getMockExamSectionId());
                if (section == null) throw new BusinessRuleException("Section không thuộc đề thi");
                if (!skill.equals(normalizeSkill(section.getSkill())))
                    throw new BusinessRuleException("Kỹ năng section không khớp với câu hỏi");
                usedSections.add(section.getId());
            } else if (!sectionById.isEmpty()) {
                throw new BusinessRuleException("Câu hỏi chưa được gán vào section của đề");
            }
            boolean hasAudio = false;
            for (QuestionMediaEntity audio :
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

    void validateAudio(String path) {
        if (path != null && !path.isBlank() && !path.startsWith("speech://")) {
            try {
                Resource resource = storage.load(path);
                if (resource.exists() && resource.isReadable()) return;
            } catch (RuntimeException ignored) {
                // A transcript is never a substitute for a missing exam audio file.
            }
        }
        throw new BusinessRuleException(
                "Audio đề thi chưa sẵn sàng. Admin cần tải file âm thanh trước khi mở đề");
    }

    static String normalizeSkill(String value) {
        return Objects.toString(value, "")
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_");
    }
}
