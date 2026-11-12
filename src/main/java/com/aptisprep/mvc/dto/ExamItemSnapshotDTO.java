package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class ExamItemSnapshotDTO {
    private Integer id;
    private ExamQuestionSnapshot question;
    private Integer sourceItemId;
    private Integer position;
    private BigDecimal points;
    private String content;
    private Integer correctPosition;
    private Integer minWordCount;
    private Integer maxWordCount;
    private Integer responseSeconds;
    private List<ExamOptionSnapshot> options;

    public ExamItemSnapshotDTO() {}

    public ExamItemSnapshotDTO(
            Integer id,
            ExamQuestionSnapshot question,
            Integer sourceItemId,
            Integer position,
            BigDecimal points,
            String content,
            Integer correctPosition,
            Integer minWordCount,
            Integer maxWordCount,
            Integer responseSeconds,
            List<ExamOptionSnapshot> options) {
        this.id = id;
        this.question = question;
        this.sourceItemId = sourceItemId;
        this.position = position;
        this.points = points;
        this.content = content;
        this.correctPosition = correctPosition;
        this.minWordCount = minWordCount;
        this.maxWordCount = maxWordCount;
        this.responseSeconds = responseSeconds;
        this.options = options;
    }

    public static ExamItemSnapshotDTO convertEntityToDto(ExamItemSnapshot entity) {
        if (entity == null) return null;
        return new ExamItemSnapshotDTO(
                entity.getId(),
                entity.getQuestion(),
                entity.getSourceItemId(),
                entity.getPosition(),
                entity.getPoints(),
                entity.getContent(),
                entity.getCorrectPosition(),
                entity.getMinWordCount(),
                entity.getMaxWordCount(),
                entity.getResponseSeconds(),
                entity.getOptions());
    }

    public static ExamItemSnapshot convertDtoToEntity(ExamItemSnapshotDTO dto) {
        if (dto == null) return null;
        return new ExamItemSnapshot(
                dto.getId(),
                dto.getQuestion(),
                dto.getSourceItemId(),
                dto.getPosition(),
                dto.getPoints(),
                dto.getContent(),
                dto.getCorrectPosition(),
                dto.getMinWordCount(),
                dto.getMaxWordCount(),
                dto.getResponseSeconds(),
                dto.getOptions());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ExamQuestionSnapshot getQuestion() {
        return question;
    }

    public void setQuestion(ExamQuestionSnapshot question) {
        this.question = question;
    }

    public Integer getSourceItemId() {
        return sourceItemId;
    }

    public void setSourceItemId(Integer sourceItemId) {
        this.sourceItemId = sourceItemId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public BigDecimal getPoints() {
        return points;
    }

    public void setPoints(BigDecimal points) {
        this.points = points;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getCorrectPosition() {
        return correctPosition;
    }

    public void setCorrectPosition(Integer correctPosition) {
        this.correctPosition = correctPosition;
    }

    public Integer getMinWordCount() {
        return minWordCount;
    }

    public void setMinWordCount(Integer minWordCount) {
        this.minWordCount = minWordCount;
    }

    public Integer getMaxWordCount() {
        return maxWordCount;
    }

    public void setMaxWordCount(Integer maxWordCount) {
        this.maxWordCount = maxWordCount;
    }

    public Integer getResponseSeconds() {
        return responseSeconds;
    }

    public void setResponseSeconds(Integer responseSeconds) {
        this.responseSeconds = responseSeconds;
    }

    public List<ExamOptionSnapshot> getOptions() {
        return options;
    }

    public void setOptions(List<ExamOptionSnapshot> options) {
        this.options = options;
    }
}











