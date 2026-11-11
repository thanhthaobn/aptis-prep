package com.aptisprep.mvc.catalog.dto;

import java.util.List;

public final class ExamCatalogDTO {
    private ExamCatalogDTO() {}

    public record ExamSummaryDTO(
            Integer examId,
            String title,
            String examType,
            String skill,
            String targetLevel,
            Integer durationMinutes,
            Integer scoringSchemeId) {}

    public record QuestionViewDTO(
            Integer questionId, String skill, String type, int position, String content) {}

    public record ExamViewDTO(ExamSummaryDTO exam, List<QuestionViewDTO> questions) {}

    public record CertificateOptionDTO(Integer id, String name) {
        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public record SchemeOptionDTO(
            Integer id,
            String name,
            String version,
            String examType,
            String scopeSkill,
            CertificateOptionDTO certificate) {
        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getExamType() {
            return examType;
        }

        public String getScopeSkill() {
            return scopeSkill;
        }

        public CertificateOptionDTO getCertificate() {
            return certificate;
        }
    }

    public record PublishedQuestionOptionDTO(Integer id, String skill, String content) {
        public Integer getId() {
            return id;
        }

        public String getSkill() {
            return skill;
        }

        public String getContent() {
            return content;
        }
    }

    public record QuestionMediaDTO(
            Integer id,
            String mediaType,
            String originalFilename,
            String contentType,
            Long fileSize,
            int position,
            String altText,
            String mediaUrl,
            String transcript,
            Integer maxPlayCount) {
        public Integer getId() {
            return id;
        }

        public String getMediaType() {
            return mediaType;
        }

        public String getOriginalFilename() {
            return originalFilename;
        }

        public String getContentType() {
            return contentType;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public int getPosition() {
            return position;
        }

        public String getAltText() {
            return altText;
        }

        public String getMediaUrl() {
            return mediaUrl;
        }

        public String getTranscript() {
            return transcript;
        }

        public Integer getMaxPlayCount() {
            return maxPlayCount;
        }
    }
}
