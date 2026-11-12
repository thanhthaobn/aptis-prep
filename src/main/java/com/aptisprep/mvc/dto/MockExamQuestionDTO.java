package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;
import com.aptisprep.mvc.dto.*;

public class MockExamQuestionDTO {
    private Integer id;
    private Integer mockExamId;
    private Integer mockExamSectionId;
    private Integer questionId;
    private int position;

    public MockExamQuestionDTO() {}

    public MockExamQuestionDTO(
            Integer id,
            Integer mockExamId,
            Integer mockExamSectionId,
            Integer questionId,
            int position) {
        this.id = id;
        this.mockExamId = mockExamId;
        this.mockExamSectionId = mockExamSectionId;
        this.questionId = questionId;
        this.position = position;
    }

    public static MockExamQuestionDTO convertEntityToDto(MockExamQuestion entity) {
        if (entity == null) return null;
        return new MockExamQuestionDTO(
                entity.getId(),
                entity.getMockExamId(),
                entity.getMockExamSectionId(),
                entity.getQuestionId(),
                entity.getPosition());
    }

    public static MockExamQuestion convertDtoToEntity(MockExamQuestionDTO dto) {
        if (dto == null) return null;
        return new MockExamQuestion(
                dto.getId(),
                dto.getMockExamId(),
                dto.getMockExamSectionId(),
                dto.getQuestionId(),
                dto.getPosition());
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

    public Integer getMockExamSectionId() {
        return mockExamSectionId;
    }

    public void setMockExamSectionId(Integer mockExamSectionId) {
        this.mockExamSectionId = mockExamSectionId;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}











