package com.aptisprep.dto;

import com.aptisprep.entity.QuestionItemEntity;
import java.util.ArrayList;
import java.util.List;

public class QuestionItemDTO {
    private Integer id;
    private Integer questionId;
    private String content;
    private Integer correctPosition;
    private Integer position;
    private Integer minWordCount;
    private Integer maxWordCount;
    private Integer responseSeconds;
    private String taskContent;
    private String lockedContent;
    private Integer correctOptionId;

    private List<QuestionOptionDTO> options = new ArrayList<>();

    public QuestionItemDTO() {}

    public QuestionItemDTO(
            Integer id,
            Integer questionId,
            String content,
            Integer correctPosition,
            Integer position,
            Integer minWordCount,
            Integer maxWordCount,
            Integer responseSeconds) {
        this.id = id;
        this.questionId = questionId;
        this.content = content;
        this.correctPosition = correctPosition;
        this.position = position;
        this.minWordCount = minWordCount;
        this.maxWordCount = maxWordCount;
        this.responseSeconds = responseSeconds;
    }

    public static QuestionItemDTO convertEntityToDto(QuestionItemEntity entity) {
        if (entity == null) return null;
        return new QuestionItemDTO(
                entity.getId(),
                entity.getQuestionId(),
                entity.getContent(),
                entity.getCorrectPosition(),
                entity.getPosition(),
                entity.getMinWordCount(),
                entity.getMaxWordCount(),
                entity.getResponseSeconds());
    }

    public QuestionItemEntity convertDtoToEntity() {
        QuestionItemEntity entity =
                new QuestionItemEntity(
                        this.questionId,
                        this.content,
                        this.correctPosition,
                        this.position != null ? this.position : 0,
                        this.minWordCount,
                        this.maxWordCount,
                        this.responseSeconds);
        if (this.id != null) entity.setId(this.id);
        return entity;
    }

    public void updateEntityFromDto(QuestionItemEntity entity) {
        if (entity == null) return;
        entity.setContent(this.content);
        entity.setCorrectPosition(this.correctPosition);
        if (this.position != null) entity.setPosition(this.position);
        entity.setMinWordCount(this.minWordCount);
        entity.setMaxWordCount(this.maxWordCount);
        entity.setResponseSeconds(this.responseSeconds);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
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

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
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

    public String getTaskContent() {
        return taskContent;
    }

    public void setTaskContent(String taskContent) {
        this.taskContent = taskContent;
    }

    public String getLockedContent() {
        return lockedContent;
    }

    public void setLockedContent(String lockedContent) {
        this.lockedContent = lockedContent;
    }

    public Integer getCorrectOptionId() {
        return correctOptionId;
    }

    public void setCorrectOptionId(Integer correctOptionId) {
        this.correctOptionId = correctOptionId;
    }

    public List<QuestionOptionDTO> getOptions() {
        return options;
    }

    public void setOptions(List<QuestionOptionDTO> options) {
        this.options = options;
    }

    public int position() {
        return position != null ? position : 0;
    }

    public String content() {
        return getContent();
    }

    public Integer responseSeconds() {
        return getResponseSeconds();
    }

    public Integer minWordCount() {
        return getMinWordCount();
    }

    public Integer maxWordCount() {
        return getMaxWordCount();
    }

    public List<QuestionOptionDTO> options() {
        return getOptions();
    }
}
