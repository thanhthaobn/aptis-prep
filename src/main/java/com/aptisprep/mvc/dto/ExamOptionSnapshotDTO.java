package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class ExamOptionSnapshotDTO {
    private Integer id;
    private ExamItemSnapshot item;
    private Integer sourceOptionId;
    private String optionCode;
    private Integer position;
    private String content;
    private Boolean correct;

    public ExamOptionSnapshotDTO() {}

    public ExamOptionSnapshotDTO(
            Integer id,
            ExamItemSnapshot item,
            Integer sourceOptionId,
            String optionCode,
            Integer position,
            String content,
            Boolean correct) {
        this.id = id;
        this.item = item;
        this.sourceOptionId = sourceOptionId;
        this.optionCode = optionCode;
        this.position = position;
        this.content = content;
        this.correct = correct;
    }

    public static ExamOptionSnapshotDTO convertEntityToDto(ExamOptionSnapshot entity) {
        if (entity == null) return null;
        return new ExamOptionSnapshotDTO(
                entity.getId(),
                entity.getItem(),
                entity.getSourceOptionId(),
                entity.getOptionCode(),
                entity.getPosition(),
                entity.getContent(),
                entity.getCorrect());
    }

    public static ExamOptionSnapshot convertDtoToEntity(ExamOptionSnapshotDTO dto) {
        if (dto == null) return null;
        return new ExamOptionSnapshot(
                dto.getId(),
                dto.getItem(),
                dto.getSourceOptionId(),
                dto.getOptionCode(),
                dto.getPosition(),
                dto.getContent(),
                dto.getCorrect());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ExamItemSnapshot getItem() {
        return item;
    }

    public void setItem(ExamItemSnapshot item) {
        this.item = item;
    }

    public Integer getSourceOptionId() {
        return sourceOptionId;
    }

    public void setSourceOptionId(Integer sourceOptionId) {
        this.sourceOptionId = sourceOptionId;
    }

    public String getOptionCode() {
        return optionCode;
    }

    public void setOptionCode(String optionCode) {
        this.optionCode = optionCode;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }
}
