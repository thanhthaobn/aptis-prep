package com.aptisprep.dto;

import java.util.List;

public final class ExamCatalogDTO {
    private ExamCatalogDTO() {}

    public static class ExamSummaryDTO {
        private Integer examId;
        private String title;
        private String examType;
        private String skill;
        private String targetLevel;
        private Integer durationMinutes;
        private Integer scoringSchemeId;

        public ExamSummaryDTO() {}

        public ExamSummaryDTO(
                Integer examId,
                String title,
                String examType,
                String skill,
                String targetLevel,
                Integer durationMinutes,
                Integer scoringSchemeId) {
            this.examId = examId;
            this.title = title;
            this.examType = examType;
            this.skill = skill;
            this.targetLevel = targetLevel;
            this.durationMinutes = durationMinutes;
            this.scoringSchemeId = scoringSchemeId;
        }

        public Integer getExamId() {
            return examId;
        }

        public void setExamId(Integer examId) {
            this.examId = examId;
        }

        public Integer examId() {
            return examId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }

        public String getExamType() {
            return examType;
        }

        public void setExamType(String examType) {
            this.examType = examType;
        }

        public String examType() {
            return examType;
        }

        public String getSkill() {
            return skill;
        }

        public void setSkill(String skill) {
            this.skill = skill;
        }

        public String skill() {
            return skill;
        }

        public String getTargetLevel() {
            return targetLevel;
        }

        public void setTargetLevel(String targetLevel) {
            this.targetLevel = targetLevel;
        }

        public String targetLevel() {
            return targetLevel;
        }

        public Integer getDurationMinutes() {
            return durationMinutes;
        }

        public void setDurationMinutes(Integer durationMinutes) {
            this.durationMinutes = durationMinutes;
        }

        public Integer durationMinutes() {
            return durationMinutes;
        }

        public Integer getScoringSchemeId() {
            return scoringSchemeId;
        }

        public void setScoringSchemeId(Integer scoringSchemeId) {
            this.scoringSchemeId = scoringSchemeId;
        }

        public Integer scoringSchemeId() {
            return scoringSchemeId;
        }
    }

    public static class QuestionViewDTO extends com.aptisprep.dto.QuestionViewDTO {
        public QuestionViewDTO() {
            super();
        }

        public QuestionViewDTO(
                Integer questionId, String skill, String type, int position, String content) {
            super(questionId, skill, type, position, content);
        }
    }

    public static class ExamViewDTO extends com.aptisprep.dto.ExamViewDTO {
        public ExamViewDTO() {
            super();
        }

        public ExamViewDTO(ExamSummaryDTO exam, List<com.aptisprep.dto.QuestionViewDTO> questions) {
            super(exam, questions);
        }
    }

    public static class CertificateOptionDTO {
        private Integer id;
        private String name;

        public CertificateOptionDTO() {}

        public CertificateOptionDTO(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer id() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }
    }

    public static class SchemeOptionDTO {
        private Integer id;
        private String name;
        private String version;
        private String examType;
        private String scopeSkill;
        private CertificateOptionDTO certificate;

        public SchemeOptionDTO() {}

        public SchemeOptionDTO(
                Integer id,
                String name,
                String version,
                String examType,
                String scopeSkill,
                CertificateOptionDTO certificate) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.examType = examType;
            this.scopeSkill = scopeSkill;
            this.certificate = certificate;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer id() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String version() {
            return version;
        }

        public String getExamType() {
            return examType;
        }

        public void setExamType(String examType) {
            this.examType = examType;
        }

        public String examType() {
            return examType;
        }

        public String getScopeSkill() {
            return scopeSkill;
        }

        public void setScopeSkill(String scopeSkill) {
            this.scopeSkill = scopeSkill;
        }

        public String scopeSkill() {
            return scopeSkill;
        }

        public CertificateOptionDTO getCertificate() {
            return certificate;
        }

        public void setCertificate(CertificateOptionDTO certificate) {
            this.certificate = certificate;
        }

        public CertificateOptionDTO certificate() {
            return certificate;
        }
    }

    public static class PublishedQuestionOptionDTO {
        private Integer id;
        private String skill;
        private String content;

        public PublishedQuestionOptionDTO() {}

        public PublishedQuestionOptionDTO(Integer id, String skill, String content) {
            this.id = id;
            this.skill = skill;
            this.content = content;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer id() {
            return id;
        }

        public String getSkill() {
            return skill;
        }

        public void setSkill(String skill) {
            this.skill = skill;
        }

        public String skill() {
            return skill;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String content() {
            return content;
        }
    }

    public static class QuestionMediaDTO {
        private Integer id;
        private String mediaType;
        private String originalFilename;
        private String contentType;
        private Long fileSize;
        private int position;
        private String altText;
        private String mediaUrl;
        private String transcript;
        private Integer maxPlayCount;

        public QuestionMediaDTO() {}

        public QuestionMediaDTO(
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
            this.id = id;
            this.mediaType = mediaType;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.fileSize = fileSize;
            this.position = position;
            this.altText = altText;
            this.mediaUrl = mediaUrl;
            this.transcript = transcript;
            this.maxPlayCount = maxPlayCount;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer id() {
            return id;
        }

        public String getMediaType() {
            return mediaType;
        }

        public void setMediaType(String mediaType) {
            this.mediaType = mediaType;
        }

        public String mediaType() {
            return mediaType;
        }

        public String getOriginalFilename() {
            return originalFilename;
        }

        public void setOriginalFilename(String originalFilename) {
            this.originalFilename = originalFilename;
        }

        public String originalFilename() {
            return originalFilename;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public String contentType() {
            return contentType;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }

        public Long fileSize() {
            return fileSize;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public int position() {
            return position;
        }

        public String getAltText() {
            return altText;
        }

        public void setAltText(String altText) {
            this.altText = altText;
        }

        public String altText() {
            return altText;
        }

        public String getMediaUrl() {
            return mediaUrl;
        }

        public void setMediaUrl(String mediaUrl) {
            this.mediaUrl = mediaUrl;
        }

        public String mediaUrl() {
            return mediaUrl;
        }

        public String getTranscript() {
            return transcript;
        }

        public void setTranscript(String transcript) {
            this.transcript = transcript;
        }

        public String transcript() {
            return transcript;
        }

        public Integer getMaxPlayCount() {
            return maxPlayCount;
        }

        public void setMaxPlayCount(Integer maxPlayCount) {
            this.maxPlayCount = maxPlayCount;
        }

        public Integer maxPlayCount() {
            return maxPlayCount;
        }
    }
}
