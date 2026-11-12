package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.ArrayList;
import java.util.List;

public class MockExamSectionDTO {
    private Integer id;
    private Integer mockExamId;

    @NotBlank(message = "Kỹ năng section là bắt buộc")
    @Pattern(
            regexp = "GRAMMAR_VOCABULARY|READING|LISTENING|WRITING|SPEAKING",
            message = "Kỹ năng section không hợp lệ")
    private String skill;

    @Positive(message = "Thứ tự section phải lớn hơn 0")
    private Integer displayOrder;

    @NotNull(message = "Thời lượng section là bắt buộc")
    @Positive(message = "Thời lượng section phải lớn hơn 0")
    private Integer durationMinutes;

    private String status;

    // Dữ liệu nhập trên form, không phải cột của bảng mock_exam_sections.
    private String questionIds;
    private List<Integer> selectedQuestionIds = new ArrayList<>();
    private Integer questionCount;
    private String skillLabel;
    private String content;
    private String value;

    public MockExamSectionDTO() {}

    public MockExamSectionDTO(
            Integer id,
            Integer mockExamId,
            String skill,
            Integer displayOrder,
            Integer durationMinutes,
            String status,
            String questionIds) {
        this.id = id;
        this.mockExamId = mockExamId;
        this.skill = skill;
        this.displayOrder = displayOrder;
        this.durationMinutes = durationMinutes;
        this.status = status;
        this.questionIds = questionIds;
    }

    public MockExamSectionDTO(
            Integer id,
            Integer mockExamId,
            String skill,
            Integer displayOrder,
            Integer durationMinutes,
            String status,
            String questionIds,
            Integer questionCount,
            String skillLabel,
            String content,
            String value) {
        this(id, mockExamId, skill, displayOrder, durationMinutes, status, questionIds);
        this.questionCount = questionCount;
        this.skillLabel = skillLabel;
        this.content = content;
        this.value = value;
    }

    public static MockExamSectionDTO convertEntityToDto(MockExamSection entity) {
        if (entity == null) return null;
        return new MockExamSectionDTO(
                entity.getId(),
                entity.getMockExamId(),
                entity.getSkill(),
                entity.getDisplayOrder(),
                entity.getDurationMinutes(),
                entity.getStatus(),
                null);
    }

    public static MockExamSection convertDtoToEntity(MockExamSectionDTO dto) {
        if (dto == null) return null;
        return new MockExamSection(
                dto.getId(),
                dto.getMockExamId(),
                dto.getSkill(),
                dto.getDisplayOrder() == null ? 0 : dto.getDisplayOrder(),
                dto.getDurationMinutes() == null ? 0 : dto.getDurationMinutes(),
                dto.getStatus());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getMockExamId() {
        return mockExamId;
    }

    public void setMockExamId(Integer mockExamId) {
        this.mockExamId = mockExamId;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getQuestionIds() {
        return questionIds;
    }

    public void setQuestionIds(String questionIds) {
        this.questionIds = questionIds;
    }

    public List<Integer> getSelectedQuestionIds() {
        return selectedQuestionIds;
    }

    public void setSelectedQuestionIds(List<Integer> selectedQuestionIds) {
        this.selectedQuestionIds =
                selectedQuestionIds == null ? new ArrayList<>() : selectedQuestionIds;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public String getSkillLabel() {
        return skillLabel == null ? skill : skillLabel;
    }

    public void setSkillLabel(String skillLabel) {
        this.skillLabel = skillLabel;
    }

    public String getContent() {
        return content == null
                ? (questionCount == null ? null : questionCount + " câu hỏi")
                : content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getValue() {
        return value == null ? (durationMinutes == null ? null : durationMinutes + " phút") : value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTitle() {
        return getSkillLabel();
    }
}
