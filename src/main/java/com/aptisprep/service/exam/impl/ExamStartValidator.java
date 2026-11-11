package com.aptisprep.service.exam.impl;

import com.aptisprep.entity.*;
import com.aptisprep.enums.Skill;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.service.admin.ExamCatalogService;
import com.aptisprep.service.scoring.CertificateSchemeService;
import com.aptisprep.service.storage.StorageService;
import java.util.*;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/** Shared readiness checks for the catalog and the actual start transaction. */
@Component
public class ExamStartValidator {
    private final ExamCatalogService examCatalogService;
    private final CertificateSchemeService schemes;
    private final StorageService storage;

    public ExamStartValidator(
            ExamCatalogService examCatalogService,
            CertificateSchemeService schemes,
            StorageService storage) {
        this.examCatalogService = examCatalogService;
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
        for (MockExamSectionEntity section : examCatalogService.findExamSections(exam.getId())) {
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
                    examCatalogService
                            .findQuestionById(link.getQuestionId())
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
                    examCatalogService.findQuestionMedia(question.getId())) {
                if ("AUDIO".equals(audio.getMediaType())) {
                    hasAudio = true;
                    if ("LISTENING".equals(skill))
                        validateAudio(audio.getMediaUrl(), audio.getTranscript());
                    else validateAudio(audio.getMediaUrl());
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
        if (isReadableAudio(path)) return;
        throw new BusinessRuleException(
                "Audio đề thi chưa sẵn sàng. Admin cần tải file âm thanh trước khi mở đề");
    }

    void validateAudio(String path, String transcript) {
        if (canUseBrowserSpeech(path, transcript)) return;
        validateAudio(path);
    }

    boolean canUseBrowserSpeech(String path, String transcript) {
        if (transcript == null || transcript.isBlank()) return false;
        if (path == null || path.isBlank() || path.startsWith("speech://")) return true;
        return !isReadableAudio(path);
    }

    private boolean isReadableAudio(String path) {
        if (path == null || path.isBlank() || path.startsWith("speech://")) return false;
        try {
            Resource resource = storage.load(path);
            return resource.exists() && resource.isReadable();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static String normalizeSkill(String value) {
        return Objects.toString(value, "")
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_");
    }
}
