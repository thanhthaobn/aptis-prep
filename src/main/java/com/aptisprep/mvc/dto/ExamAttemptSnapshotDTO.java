package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class ExamAttemptSnapshotDTO {
    private Integer id;
    private Integer attemptId;
    private Integer examId;
    private Integer certificateId;
    private Integer scoringSchemeId;
    private String examType;
    private String skill;
    private String targetLevel;
    private String title;
    private Integer durationMinutes;
    private Integer answerVersion;
    private String policyVersion;
    private LocalDateTime createdAt;
    private Long sectionAdvanceMillis;
    private List<ExamQuestionSnapshot> questions;

    public ExamAttemptSnapshotDTO() {}

    public ExamAttemptSnapshotDTO(
            Integer id,
            Integer attemptId,
            Integer examId,
            Integer certificateId,
            Integer scoringSchemeId,
            String examType,
            String skill,
            String targetLevel,
            String title,
            Integer durationMinutes,
            Integer answerVersion,
            String policyVersion,
            LocalDateTime createdAt,
            Long sectionAdvanceMillis,
            List<ExamQuestionSnapshot> questions) {
        this.id = id;
        this.attemptId = attemptId;
        this.examId = examId;
        this.certificateId = certificateId;
        this.scoringSchemeId = scoringSchemeId;
        this.examType = examType;
        this.skill = skill;
        this.targetLevel = targetLevel;
        this.title = title;
        this.durationMinutes = durationMinutes;
        this.answerVersion = answerVersion;
        this.policyVersion = policyVersion;
        this.createdAt = createdAt;
        this.sectionAdvanceMillis = sectionAdvanceMillis;
        this.questions = questions;
    }


    public static ExamAttemptSnapshotDTO convertEntityToDto(ExamAttemptSnapshot entity) {
        if (entity == null) return null;
        return new ExamAttemptSnapshotDTO(
                entity.getId(),
                entity.getAttemptId(),
                entity.getExamId(),
                entity.getCertificateId(),
                entity.getScoringSchemeId(),
                entity.getExamType(),
                entity.getSkill(),
                entity.getTargetLevel(),
                entity.getTitle(),
                entity.getDurationMinutes(),
                entity.getAnswerVersion(),
                entity.getPolicyVersion(),
                entity.getCreatedAt(),
                entity.getSectionAdvanceMillis(),
                entity.getQuestions());
    }

    public static ExamAttemptSnapshot convertDtoToEntity(ExamAttemptSnapshotDTO dto) {
        if (dto == null) return null;
        return new ExamAttemptSnapshot(
                dto.getId(),
                dto.getAttemptId(),
                dto.getExamId(),
                dto.getCertificateId(),
                dto.getScoringSchemeId(),
                dto.getExamType(),
                dto.getSkill(),
                dto.getTargetLevel(),
                dto.getTitle(),
                dto.getDurationMinutes(),
                dto.getAnswerVersion(),
                dto.getPolicyVersion(),
                dto.getSectionAdvanceMillis(),
                dto.getQuestions(),
                dto.getCreatedAt());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public Integer getExamId() {
        return examId;
    }

    public void setExamId(Integer examId) {
        this.examId = examId;
    }

    public Integer getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(Integer certificateId) {
        this.certificateId = certificateId;
    }

    public Integer getScoringSchemeId() {
        return scoringSchemeId;
    }

    public void setScoringSchemeId(Integer scoringSchemeId) {
        this.scoringSchemeId = scoringSchemeId;
    }

    public String getExamType() {
        return examType;
    }

    public void setExamType(String examType) {
        this.examType = examType;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(String targetLevel) {
        this.targetLevel = targetLevel;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getAnswerVersion() {
        return answerVersion;
    }

    public void setAnswerVersion(Integer answerVersion) {
        this.answerVersion = answerVersion;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(String policyVersion) {
        this.policyVersion = policyVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getSectionAdvanceMillis() {
        return sectionAdvanceMillis;
    }

    public void setSectionAdvanceMillis(Long sectionAdvanceMillis) {
        this.sectionAdvanceMillis = sectionAdvanceMillis;
    }

    public List<ExamQuestionSnapshot> getQuestions() {
        return questions;
    }

    public void setQuestions(List<ExamQuestionSnapshot> questions) {
        this.questions = questions;
    }
}











