package com.aptisprep.dto;

import com.aptisprep.entity.MockExamEntity;
import com.aptisprep.entity.MockExamQuestionEntity;
import com.aptisprep.entity.MockExamSectionEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ExamDTO {
    private Integer id;
    private Integer certificateId;
    private String certificateName;
    private Integer scoringSchemeId;
    private String scoringSchemeName;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    @Size(max = 4000, message = "Mô tả tối đa 4000 ký tự")
    private String description;

    @NotBlank(message = "Phải chọn loại bài thi")
    private String examType = "PRACTICE";

    private String targetLevel = "B1";
    private String skill;

    @NotNull(message = "Thời lượng không được để trống")
    private Integer durationMinutes = 30;

    private String status = "DRAFT";
    private Boolean demoOnly = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<ExamSectionDTO> sections = new ArrayList<>();
    private List<QuestionDTO> questions = new ArrayList<>();
    private List<Integer> selectedQuestionIds = new ArrayList<>();

    public ExamDTO() {}

    public ExamDTO(Integer id, Integer certificateId, Integer scoringSchemeId, String title, String description, String examType, String targetLevel, String skill, Integer durationMinutes, String status, Boolean demoOnly, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.certificateId = certificateId;
        this.scoringSchemeId = scoringSchemeId;
        this.title = title;
        this.description = description;
        this.examType = examType;
        this.targetLevel = targetLevel;
        this.skill = skill;
        this.durationMinutes = durationMinutes;
        this.status = status;
        this.demoOnly = demoOnly;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ExamDTO convertEntityToDto(MockExamEntity entity) {
        if (entity == null) return null;
        return new ExamDTO(
                entity.getId(),
                entity.getCertificateId(),
                entity.getScoringSchemeId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getExamType(),
                entity.getTargetLevel(),
                entity.getSkill(),
                entity.getDurationMinutes(),
                entity.getStatus(),
                entity.isDemoOnly(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static ExamDTO convertEntityToDto(
            MockExamEntity entity,
            List<MockExamSectionEntity> sectionEntities,
            List<QuestionDTO> questionDtos) {
        ExamDTO dto = convertEntityToDto(entity);
        if (dto == null) return null;
        if (sectionEntities != null) {
            for (MockExamSectionEntity se : sectionEntities) {
                dto.getSections().add(ExamSectionDTO.convertEntityToDto(se));
            }
        }
        if (questionDtos != null) {
            dto.setQuestions(questionDtos);
            for (QuestionDTO q : questionDtos) {
                if (q.getId() != null) dto.getSelectedQuestionIds().add(q.getId());
            }
        }
        return dto;
    }

    public MockExamEntity convertDtoToEntity() {
        MockExamEntity entity = new MockExamEntity();
        updateEntityFromDto(entity);
        if (this.id != null) entity.setId(this.id);
        return entity;
    }

    public void updateEntityFromDto(MockExamEntity entity) {
        if (entity == null) return;
        entity.setCertificateId(this.certificateId);
        entity.setScoringSchemeId(this.scoringSchemeId);
        entity.setTitle(this.title);
        entity.setDescription(this.description);
        entity.setExamType(this.examType);
        entity.setTargetLevel(this.targetLevel);
        entity.setSkill(this.skill);
        entity.setDurationMinutes(this.durationMinutes);
        if (this.status != null) entity.setStatus(this.status);
        entity.setDemoOnly(this.demoOnly != null && this.demoOnly);
        entity.setUpdatedAt(LocalDateTime.now());
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCertificateId() { return certificateId; }
    public void setCertificateId(Integer certificateId) { this.certificateId = certificateId; }
    public String getCertificateName() { return certificateName; }
    public void setCertificateName(String certificateName) { this.certificateName = certificateName; }
    public Integer getScoringSchemeId() { return scoringSchemeId; }
    public void setScoringSchemeId(Integer scoringSchemeId) { this.scoringSchemeId = scoringSchemeId; }
    public String getScoringSchemeName() { return scoringSchemeName; }
    public void setScoringSchemeName(String scoringSchemeName) { this.scoringSchemeName = scoringSchemeName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getExamType() { return examType; }
    public void setExamType(String examType) { this.examType = examType; }
    public String getTargetLevel() { return targetLevel; }
    public void setTargetLevel(String targetLevel) { this.targetLevel = targetLevel; }
    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getDemoOnly() { return demoOnly; }
    public void setDemoOnly(Boolean demoOnly) { this.demoOnly = demoOnly; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<ExamSectionDTO> getSections() { return sections; }
    public void setSections(List<ExamSectionDTO> sections) { this.sections = sections; }
    public List<QuestionDTO> getQuestions() { return questions; }
    public void setQuestions(List<QuestionDTO> questions) { this.questions = questions; }
    public List<Integer> getSelectedQuestionIds() { return selectedQuestionIds; }
    public void setSelectedQuestionIds(List<Integer> selectedQuestionIds) { this.selectedQuestionIds = selectedQuestionIds; }

    public Integer examId() { return getId(); }
    public String title() { return getTitle(); }
    public String examType() { return getExamType(); }
    public String skill() { return getSkill(); }
    public String targetLevel() { return getTargetLevel(); }
    public Integer durationMinutes() { return getDurationMinutes(); }
    public Integer scoringSchemeId() { return getScoringSchemeId(); }
    public List<QuestionDTO> questions() { return getQuestions(); }
}
