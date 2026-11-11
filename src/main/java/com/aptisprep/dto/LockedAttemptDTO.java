package com.aptisprep.dto;

import java.time.Instant;
import java.util.List;

public class LockedAttemptDTO {
    private Integer attemptId;
    private Integer userId;
    private Integer examId;
    private String examType;
    private Integer answerVersion;
    private Instant submittedAt;
    private Integer scoringSchemeId;
    private String policyVersion;
    private String rubricVersion;
    private String gradingContext;
    private String gradingRequestKey;
    private List<LockedAnswerDTO> answers;

    public LockedAttemptDTO() {}

    public LockedAttemptDTO(
            Integer attemptId,
            Integer userId,
            Integer examId,
            String examType,
            Integer answerVersion,
            Instant submittedAt,
            Integer scoringSchemeId,
            String policyVersion,
            String rubricVersion,
            String gradingContext,
            String gradingRequestKey,
            List<LockedAnswerDTO> answers) {
        this.attemptId = attemptId;
        this.userId = userId;
        this.examId = examId;
        this.examType = examType;
        this.answerVersion = answerVersion;
        this.submittedAt = submittedAt;
        this.scoringSchemeId = scoringSchemeId;
        this.policyVersion = policyVersion;
        this.rubricVersion = rubricVersion;
        this.gradingContext = gradingContext;
        this.gradingRequestKey = gradingRequestKey;
        this.answers = answers;
    }

    public LockedAttemptDTO(
            Integer attemptId,
            Integer userId,
            Integer examId,
            String examType,
            Integer answerVersion,
            Instant submittedAt,
            Integer scoringSchemeId,
            String policyVersion,
            String gradingRequestKey,
            List<LockedAnswerDTO> answers) {
        this(
                attemptId,
                userId,
                examId,
                examType,
                answerVersion,
                submittedAt,
                scoringSchemeId,
                policyVersion,
                "aptis-rubric-v1",
                null,
                gradingRequestKey,
                answers);
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

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer userId() {
        return userId;
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

    public String getExamType() {
        return examType;
    }

    public void setExamType(String examType) {
        this.examType = examType;
    }

    public String examType() {
        return examType;
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

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant submittedAt() {
        return submittedAt;
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

    public String getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(String policyVersion) {
        this.policyVersion = policyVersion;
    }

    public String policyVersion() {
        return policyVersion;
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

    public String getGradingContext() {
        return gradingContext;
    }

    public void setGradingContext(String gradingContext) {
        this.gradingContext = gradingContext;
    }

    public String gradingContext() {
        return gradingContext;
    }

    public String getGradingRequestKey() {
        return gradingRequestKey;
    }

    public void setGradingRequestKey(String gradingRequestKey) {
        this.gradingRequestKey = gradingRequestKey;
    }

    public String gradingRequestKey() {
        return gradingRequestKey;
    }

    public List<LockedAnswerDTO> getAnswers() {
        return answers;
    }

    public void setAnswers(List<LockedAnswerDTO> answers) {
        this.answers = answers;
    }

    public List<LockedAnswerDTO> answers() {
        return answers;
    }
}
