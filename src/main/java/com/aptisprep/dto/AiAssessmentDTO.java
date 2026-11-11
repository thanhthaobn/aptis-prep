package com.aptisprep.dto;

import com.aptisprep.entity.AiAssessment;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public class AiAssessmentDTO {
    private Integer id;
    private String assessmentKey;
    private Integer answerId;
    private Integer answerVersion;
    private String state;
    private BigDecimal score;
    private String estimatedCefr;
    private String feedback;
    private String transcript;
    private String rubricVersion;
    private String policyVersion;
    private String provider;
    private String model;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public AiAssessmentDTO() {}

    public static class AssessmentRequest {
        private String assessmentKey;
        private Integer userId;
        private Integer attemptId;
        private Integer answerId;
        private Integer answerVersion;
        private String skill;
        private String taskType;
        private String taskContent;
        private String lockedContent;
        private String rubricVersion;
        private String policyVersion;
        private BigDecimal maxScore;
        private String transcript;
        private String sourceType;
        private Integer sourceId;

        public AssessmentRequest() {}

        public AssessmentRequest(
                String assessmentKey,
                Integer userId,
                Integer attemptId,
                Integer answerId,
                Integer answerVersion,
                String skill,
                String taskType,
                String taskContent,
                String lockedContent,
                String rubricVersion,
                String policyVersion,
                BigDecimal maxScore,
                String transcript,
                String sourceType,
                Integer sourceId) {
            this.assessmentKey = assessmentKey;
            this.userId = userId;
            this.attemptId = attemptId;
            this.answerId = answerId;
            this.answerVersion = answerVersion;
            this.skill = skill;
            this.taskType = taskType;
            this.taskContent = taskContent;
            this.lockedContent = lockedContent;
            this.rubricVersion = rubricVersion;
            this.policyVersion = policyVersion;
            this.maxScore = maxScore;
            this.transcript = transcript;
            this.sourceType = sourceType;
            this.sourceId = sourceId;
        }

        public AssessmentRequest(
                String assessmentKey,
                Integer userId,
                Integer attemptId,
                Integer answerId,
                Integer answerVersion,
                String skill,
                String taskType,
                String taskContent,
                String lockedContent,
                String rubricVersion,
                String policyVersion,
                BigDecimal maxScore,
                String transcript) {
            this(
                    assessmentKey,
                    userId,
                    attemptId,
                    answerId,
                    answerVersion,
                    skill,
                    taskType,
                    taskContent,
                    lockedContent,
                    rubricVersion,
                    policyVersion,
                    maxScore,
                    transcript,
                    "EXAM_ANSWER",
                    answerId);
        }

        public String getAssessmentKey() {
            return assessmentKey;
        }

        public void setAssessmentKey(String assessmentKey) {
            this.assessmentKey = assessmentKey;
        }

        public String assessmentKey() {
            return assessmentKey;
        }

        public Integer getUserId() {
            return userId;
        }

        public void setUserId(Integer userId) {
            this.userId = userId;
        }

        public Integer userId() {
            return userId;
        }

        public Integer getAttemptId() {
            return attemptId;
        }

        public void setAttemptId(Integer attemptId) {
            this.attemptId = attemptId;
        }

        public Integer attemptId() {
            return attemptId;
        }

        public Integer getAnswerId() {
            return answerId;
        }

        public void setAnswerId(Integer answerId) {
            this.answerId = answerId;
        }

        public Integer answerId() {
            return answerId;
        }

        public Integer getAnswerVersion() {
            return answerVersion;
        }

        public void setAnswerVersion(Integer answerVersion) {
            this.answerVersion = answerVersion;
        }

        public Integer answerVersion() {
            return answerVersion;
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

        public String getTaskType() {
            return taskType;
        }

        public void setTaskType(String taskType) {
            this.taskType = taskType;
        }

        public String taskType() {
            return taskType;
        }

        public String getTaskContent() {
            return taskContent;
        }

        public void setTaskContent(String taskContent) {
            this.taskContent = taskContent;
        }

        public String taskContent() {
            return taskContent;
        }

        public String getLockedContent() {
            return lockedContent;
        }

        public void setLockedContent(String lockedContent) {
            this.lockedContent = lockedContent;
        }

        public String lockedContent() {
            return lockedContent;
        }

        public String getRubricVersion() {
            return rubricVersion;
        }

        public void setRubricVersion(String rubricVersion) {
            this.rubricVersion = rubricVersion;
        }

        public String rubricVersion() {
            return rubricVersion;
        }

        public String getPolicyVersion() {
            return policyVersion;
        }

        public void setPolicyVersion(String policyVersion) {
            this.policyVersion = policyVersion;
        }

        public String policyVersion() {
            return policyVersion;
        }

        public BigDecimal getMaxScore() {
            return maxScore;
        }

        public void setMaxScore(BigDecimal maxScore) {
            this.maxScore = maxScore;
        }

        public BigDecimal maxScore() {
            return maxScore;
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

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        public String sourceType() {
            return sourceType;
        }

        public Integer getSourceId() {
            return sourceId;
        }

        public void setSourceId(Integer sourceId) {
            this.sourceId = sourceId;
        }

        public Integer sourceId() {
            return sourceId;
        }
    }

    public static class AssessmentView {
        private Integer assessmentId;
        private String assessmentKey;
        private Integer answerId;
        private Integer answerVersion;
        private String state;
        private BigDecimal score;
        private String estimatedCefr;
        private String feedback;
        private String transcript;
        private String rubricVersion;
        private String policyVersion;
        private String provider;
        private String model;
        private String errorMessage;
        private Instant createdAt;
        private Instant completedAt;

        public AssessmentView() {}

        public AssessmentView(
                Integer assessmentId,
                String assessmentKey,
                Integer answerId,
                Integer answerVersion,
                String state,
                BigDecimal score,
                String estimatedCefr,
                String feedback,
                String transcript,
                String rubricVersion,
                String policyVersion,
                String provider,
                String model,
                String errorMessage,
                Instant createdAt,
                Instant completedAt) {
            this.assessmentId = assessmentId;
            this.assessmentKey = assessmentKey;
            this.answerId = answerId;
            this.answerVersion = answerVersion;
            this.state = state;
            this.score = score;
            this.estimatedCefr = estimatedCefr;
            this.feedback = feedback;
            this.transcript = transcript;
            this.rubricVersion = rubricVersion;
            this.policyVersion = policyVersion;
            this.provider = provider;
            this.model = model;
            this.errorMessage = errorMessage;
            this.createdAt = createdAt;
            this.completedAt = completedAt;
        }

        public Integer getAssessmentId() {
            return assessmentId;
        }

        public void setAssessmentId(Integer assessmentId) {
            this.assessmentId = assessmentId;
        }

        public Integer assessmentId() {
            return assessmentId;
        }

        public String getAssessmentKey() {
            return assessmentKey;
        }

        public void setAssessmentKey(String assessmentKey) {
            this.assessmentKey = assessmentKey;
        }

        public String assessmentKey() {
            return assessmentKey;
        }

        public Integer getAnswerId() {
            return answerId;
        }

        public void setAnswerId(Integer answerId) {
            this.answerId = answerId;
        }

        public Integer answerId() {
            return answerId;
        }

        public Integer getAnswerVersion() {
            return answerVersion;
        }

        public void setAnswerVersion(Integer answerVersion) {
            this.answerVersion = answerVersion;
        }

        public Integer answerVersion() {
            return answerVersion;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String state() {
            return state;
        }

        public BigDecimal getScore() {
            return score;
        }

        public void setScore(BigDecimal score) {
            this.score = score;
        }

        public BigDecimal score() {
            return score;
        }

        public String getEstimatedCefr() {
            return estimatedCefr;
        }

        public void setEstimatedCefr(String estimatedCefr) {
            this.estimatedCefr = estimatedCefr;
        }

        public String estimatedCefr() {
            return estimatedCefr;
        }

        public String getFeedback() {
            return feedback;
        }

        public void setFeedback(String feedback) {
            this.feedback = feedback;
        }

        public String feedback() {
            return feedback;
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

        public String getRubricVersion() {
            return rubricVersion;
        }

        public void setRubricVersion(String rubricVersion) {
            this.rubricVersion = rubricVersion;
        }

        public String rubricVersion() {
            return rubricVersion;
        }

        public String getPolicyVersion() {
            return policyVersion;
        }

        public void setPolicyVersion(String policyVersion) {
            this.policyVersion = policyVersion;
        }

        public String policyVersion() {
            return policyVersion;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String provider() {
            return provider;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String model() {
            return model;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public String errorMessage() {
            return errorMessage;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        public Instant createdAt() {
            return createdAt;
        }

        public Instant getCompletedAt() {
            return completedAt;
        }

        public void setCompletedAt(Instant completedAt) {
            this.completedAt = completedAt;
        }

        public Instant completedAt() {
            return completedAt;
        }
    }

    public static AiAssessmentDTO convertEntityToDto(AiAssessment entity) {
        if (entity == null) return null;
        AiAssessmentDTO dto = new AiAssessmentDTO();
        dto.id = entity.getId();
        dto.assessmentKey = entity.getAssessmentKey();
        dto.answerId = entity.getAnswerId();
        dto.answerVersion = entity.getAnswerVersion();
        dto.state = entity.getState() == null ? null : entity.getState().name();
        dto.score = entity.getScore();
        dto.estimatedCefr = entity.getEstimatedCefr();
        dto.feedback = entity.getFeedback();
        dto.transcript = entity.getTranscript();
        dto.rubricVersion = entity.getRubricVersion();
        dto.policyVersion = entity.getPolicyVersion();
        dto.provider = entity.getProvider();
        dto.model = entity.getModel();
        dto.errorMessage = entity.getErrorMessage();
        dto.createdAt = entity.getCreatedAt();
        dto.completedAt = entity.getCompletedAt();
        return dto;
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

    public String getAssessmentKey() {
        return assessmentKey;
    }

    public void setAssessmentKey(String assessmentKey) {
        this.assessmentKey = assessmentKey;
    }

    public String assessmentKey() {
        return assessmentKey;
    }

    public Integer getAnswerId() {
        return answerId;
    }

    public void setAnswerId(Integer answerId) {
        this.answerId = answerId;
    }

    public Integer answerId() {
        return answerId;
    }

    public Integer getAnswerVersion() {
        return answerVersion;
    }

    public void setAnswerVersion(Integer answerVersion) {
        this.answerVersion = answerVersion;
    }

    public Integer answerVersion() {
        return answerVersion;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String state() {
        return state;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public BigDecimal score() {
        return score;
    }

    public String getEstimatedCefr() {
        return estimatedCefr;
    }

    public void setEstimatedCefr(String estimatedCefr) {
        this.estimatedCefr = estimatedCefr;
    }

    public String estimatedCefr() {
        return estimatedCefr;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String feedback() {
        return feedback;
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

    public String getRubricVersion() {
        return rubricVersion;
    }

    public void setRubricVersion(String rubricVersion) {
        this.rubricVersion = rubricVersion;
    }

    public String rubricVersion() {
        return rubricVersion;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(String policyVersion) {
        this.policyVersion = policyVersion;
    }

    public String policyVersion() {
        return policyVersion;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String provider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String model() {
        return model;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime completedAt() {
        return completedAt;
    }
}
