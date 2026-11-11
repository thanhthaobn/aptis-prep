package com.aptisprep.dto;

import com.aptisprep.entity.MockExamSectionEntity;

public class ExamSectionDTO {
    private Integer id;
    private Integer mockExamId;
    private String skill;
    private Integer displayOrder;
    private Integer durationMinutes;
    private String status = "ACTIVE";
    private int questionCount = 0;

    public ExamSectionDTO() {}

    public ExamSectionDTO(String skill, int durationMinutes) {
        this.skill = skill;
        this.durationMinutes = durationMinutes;
    }

    public ExamSectionDTO(
            Integer id,
            Integer mockExamId,
            String skill,
            Integer displayOrder,
            Integer durationMinutes,
            String status) {
        this.id = id;
        this.mockExamId = mockExamId;
        this.skill = skill;
        this.displayOrder = displayOrder;
        this.durationMinutes = durationMinutes;
        this.status = status;
    }

    public static ExamSectionDTO convertEntityToDto(MockExamSectionEntity entity) {
        if (entity == null) return null;
        return new ExamSectionDTO(
                entity.getId(),
                entity.getMockExamId(),
                entity.getSkill(),
                entity.getDisplayOrder(),
                entity.getDurationMinutes(),
                entity.getStatus());
    }

    public MockExamSectionEntity convertDtoToEntity() {
        MockExamSectionEntity entity =
                new MockExamSectionEntity(
                        this.mockExamId,
                        this.skill,
                        this.displayOrder != null ? this.displayOrder : 0,
                        this.durationMinutes != null ? this.durationMinutes : 0,
                        this.status != null ? this.status : "ACTIVE");
        if (this.id != null) entity.setId(this.id);
        return entity;
    }

    public void updateEntityFromDto(MockExamSectionEntity entity) {
        if (entity == null) return;
        entity.setSkill(this.skill);
        if (this.displayOrder != null) entity.setDisplayOrder(this.displayOrder);
        if (this.durationMinutes != null) entity.setDurationMinutes(this.durationMinutes);
        if (this.status != null) entity.setStatus(this.status);
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

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }

    public String skill() {
        return getSkill();
    }

    public int durationMinutes() {
        return durationMinutes != null ? durationMinutes : 0;
    }
}
