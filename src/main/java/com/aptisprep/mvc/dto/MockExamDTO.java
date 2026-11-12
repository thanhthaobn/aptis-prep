package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.ArrayList;
import java.util.List;

public class MockExamDTO {
    public static final List<String> EXAM_TYPES = ExamType.codes();

    private Integer id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<@Valid MockExamSectionDTO> sections = new ArrayList<>();

    @NotBlank(message = "Tên đề thi là bắt buộc")
    @Size(max = 255, message = "Tên đề thi tối đa 255 ký tự")
    private String title;

    private String description;

    @Pattern(
            regexp = "PLACEMENT|PRACTICE|CHECKPOINT|FULL_MOCK",
            message = "Loại đề thi không hợp lệ")
    private String examType = "PRACTICE";

    @Pattern(regexp = "^$|A1|A2|B1|B2|C1|C2", message = "Cấp độ mục tiêu không hợp lệ")
    private String targetLevel = "B1";

    private String skill = "READING";
    private Integer durationMinutes;
    private Integer certificateId;
    private Integer scoringSchemeId;
    private String sectionSkill = "READING";
    private Integer sectionDurationMinutes = 35;
    private String questionIds;
    private String status = "DRAFT";
    private boolean demoOnly;
    // Chỉ dùng cho bộ lọc trên màn hình tạo đề, không lưu vào bảng mock_exams.
    private String questionLevelFilter = "";
    private String questionTypeFilter = "";
    private String skillLabel;
    private String kindLabel;
    private String durationLabel;
    private Integer sectionCount;
    private Integer questionCount;
    private boolean startable;
    private String readinessMessage;
    private List<String> readinessChecks = new ArrayList<>();

    public MockExamDTO() {}

    public MockExamDTO(
            Integer id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String title,
            String description,
            String examType,
            String targetLevel,
            String skill,
            Integer durationMinutes,
            Integer certificateId,
            Integer scoringSchemeId,
            String sectionSkill,
            Integer sectionDurationMinutes,
            String questionIds,
            String status,
            boolean demoOnly) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.title = title;
        this.description = description;
        this.examType = examType;
        this.targetLevel = targetLevel;
        this.skill = skill;
        this.durationMinutes = durationMinutes;
        this.certificateId = certificateId;
        this.scoringSchemeId = scoringSchemeId;
        this.sectionSkill = sectionSkill;
        this.sectionDurationMinutes = sectionDurationMinutes;
        this.questionIds = questionIds;
        this.status = status;
        this.demoOnly = demoOnly;
    }

    public MockExamDTO(
            Integer id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String title,
            String description,
            String examType,
            String targetLevel,
            String skill,
            Integer durationMinutes,
            Integer certificateId,
            Integer scoringSchemeId,
            String sectionSkill,
            Integer sectionDurationMinutes,
            String questionIds,
            String status,
            boolean demoOnly,
            List<MockExamSectionDTO> sections,
            String skillLabel,
            String kindLabel,
            String durationLabel,
            Integer sectionCount,
            Integer questionCount,
            boolean startable,
            String readinessMessage,
            List<String> readinessChecks) {
        this(
                id,
                createdAt,
                updatedAt,
                title,
                description,
                examType,
                targetLevel,
                skill,
                durationMinutes,
                certificateId,
                scoringSchemeId,
                sectionSkill,
                sectionDurationMinutes,
                questionIds,
                status,
                demoOnly);
        this.sections = sections == null ? new ArrayList<>() : sections;
        this.skillLabel = skillLabel;
        this.kindLabel = kindLabel;
        this.durationLabel = durationLabel;
        this.sectionCount = sectionCount;
        this.questionCount = questionCount;
        this.startable = startable;
        this.readinessMessage = readinessMessage;
        this.readinessChecks = readinessChecks == null ? new ArrayList<>() : readinessChecks;
    }

    public boolean isDemoOnly() {
        return demoOnly;
    }

    public void setDemoOnly(boolean value) {
        demoOnly = value;
    }

    public String getQuestionLevelFilter() {
        return questionLevelFilter;
    }

    public void setQuestionLevelFilter(String value) {
        questionLevelFilter = value == null ? "" : value;
    }

    public String getQuestionTypeFilter() {
        return questionTypeFilter;
    }

    public void setQuestionTypeFilter(String value) {
        questionTypeFilter = value == null ? "" : value;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String v) {
        title = v;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String v) {
        description = v;
    }

    public String getExamType() {
        return examType;
    }

    public void setExamType(String v) {
        examType = v;
    }

    public String getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(String v) {
        targetLevel = v;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String v) {
        skill = v;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer v) {
        durationMinutes = v;
    }

    public Integer getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(Integer v) {
        certificateId = v;
    }

    public Integer getScoringSchemeId() {
        return scoringSchemeId;
    }

    public void setScoringSchemeId(Integer v) {
        scoringSchemeId = v;
    }

    public String getSectionSkill() {
        return sectionSkill;
    }

    public void setSectionSkill(String v) {
        sectionSkill = v;
    }

    public Integer getSectionDurationMinutes() {
        return sectionDurationMinutes;
    }

    public void setSectionDurationMinutes(Integer v) {
        sectionDurationMinutes = v;
    }

    public String getQuestionIds() {
        return questionIds;
    }

    public void setQuestionIds(String v) {
        questionIds = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public List<MockExamSectionDTO> getSections() {
        return sections;
    }

    public void setSections(List<MockExamSectionDTO> value) {
        sections = value == null ? new ArrayList<>() : value;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer value) {
        this.id = value;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime value) {
        this.createdAt = value;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime value) {
        this.updatedAt = value;
    }

    public String getLevel() {
        return targetLevel;
    }

    public void setLevel(String level) {
        this.targetLevel = level;
    }

    public String getState() {
        return status;
    }

    public void setState(String state) {
        this.status = state;
    }

    public String getSummary() {
        return description;
    }

    public void setSummary(String summary) {
        this.description = summary;
    }

    public String getSkillLabel() {
        return skillLabel == null ? skill : skillLabel;
    }

    public void setSkillLabel(String skillLabel) {
        this.skillLabel = skillLabel;
    }

    public String getKindLabel() {
        return kindLabel == null ? examType : kindLabel;
    }

    public void setKindLabel(String kindLabel) {
        this.kindLabel = kindLabel;
    }

    public String getDurationLabel() {
        return durationLabel;
    }

    public void setDurationLabel(String durationLabel) {
        this.durationLabel = durationLabel;
    }

    public Integer getSectionCount() {
        return sectionCount;
    }

    public void setSectionCount(Integer sectionCount) {
        this.sectionCount = sectionCount;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public boolean isStartable() {
        return startable;
    }

    public void setStartable(boolean startable) {
        this.startable = startable;
    }

    public String getReadinessMessage() {
        return readinessMessage;
    }

    public void setReadinessMessage(String readinessMessage) {
        this.readinessMessage = readinessMessage;
    }

    public List<String> getReadinessChecks() {
        return readinessChecks;
    }

    public void setReadinessChecks(List<String> readinessChecks) {
        this.readinessChecks =
                readinessChecks == null ? new ArrayList<>() : new ArrayList<>(readinessChecks);
    }

    public String getSkillCode() {
        return "PLACEMENT".equals(examType) ? skill : getSkillLabel();
    }

    public String getKind() {
        return getKindLabel();
    }

    public String getSubtitle() {
        return description;
    }

    public String getDate() {
        return updatedAt == null ? "" : updatedAt.toLocalDate().toString();
    }

    public String getStatusLabel() {
        return switch (status) {
            case "PUBLISHED" -> "Đã công bố";
            case "ARCHIVED" -> "Đã ngừng công bố";
            default -> "Bản nháp";
        };
    }

    public static MockExamDTO convertEntityToDto(MockExam entity) {
        if (entity == null) return null;
        return new MockExamDTO(
                entity.getId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getExamType(),
                entity.getTargetLevel(),
                entity.getSkill(),
                entity.getDurationMinutes(),
                entity.getCertificateId(),
                entity.getScoringSchemeId(),
                null,
                null,
                null,
                entity.getStatus(),
                entity.isDemoOnly());
    }

    public static MockExam convertDtoToEntity(MockExamDTO dto) {
        if (dto == null) return null;
        return new MockExam(
                dto.getId(),
                dto.getCertificateId(),
                dto.getScoringSchemeId(),
                dto.getTitle(),
                dto.getDescription(),
                dto.getExamType(),
                dto.getTargetLevel(),
                dto.getSkill(),
                dto.getDurationMinutes(),
                dto.getStatus(),
                dto.isDemoOnly(),
                dto.getCreatedAt(),
                dto.getUpdatedAt());
    }
}
