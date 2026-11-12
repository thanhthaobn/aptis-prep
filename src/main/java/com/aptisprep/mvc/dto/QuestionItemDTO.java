package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.QuestionItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class QuestionItemDTO {
    private Integer id;
    private Integer questionId;

    @NotBlank(message = "Nội dung ý hỏi là bắt buộc")
    @Size(max = 10000, message = "Nội dung ý hỏi tối đa 10.000 ký tự")
    private String content;

    private Integer correctPosition;
    private int position;

    @Positive(message = "Điểm câu hỏi phải lớn hơn 0")
    private BigDecimal points = BigDecimal.ONE;

    @PositiveOrZero(message = "Số từ tối thiểu không được âm")
    private Integer minWordCount;

    @PositiveOrZero(message = "Số từ tối đa không được âm")
    private Integer maxWordCount;

    @Positive(message = "Thời gian trả lời phải lớn hơn 0")
    private Integer responseSeconds;

    private List<@Valid QuestionOptionDTO> options = new ArrayList<>();

    public QuestionItemDTO(
            Integer id,
            Integer questionId,
            String content,
            Integer correctPosition,
            int position,
            BigDecimal points,
            Integer minWordCount,
            Integer maxWordCount,
            Integer responseSeconds,
            List<QuestionOptionDTO> options) {
        this.id = id;
        this.questionId = questionId;
        this.content = content;
        this.correctPosition = correctPosition;
        this.position = position;
        this.points = points == null ? BigDecimal.ONE : points;
        this.minWordCount = minWordCount;
        this.maxWordCount = maxWordCount;
        this.responseSeconds = responseSeconds;
        this.options = options == null ? new ArrayList<>() : options;
    }

    public QuestionItemDTO() {
        options.add(new QuestionOptionDTO(null, null, null, "A", false, 1));
        options.add(new QuestionOptionDTO(null, null, null, "B", false, 2));
        options.add(new QuestionOptionDTO(null, null, null, "C", false, 3));
    }

    public static QuestionItemDTO convertEntityToDto(QuestionItem entity) {
        if (entity == null) return null;
        return new QuestionItemDTO(
                entity.getId(),
                entity.getQuestionId(),
                entity.getContent(),
                entity.getCorrectPosition(),
                entity.getPosition(),
                entity.getPoints(),
                entity.getMinWordCount(),
                entity.getMaxWordCount(),
                entity.getResponseSeconds(),
                new ArrayList<>());
    }

    public static QuestionItem convertDtoToEntity(QuestionItemDTO dto) {
        if (dto == null) return null;
        return new QuestionItem(
                dto.getId(),
                dto.getQuestionId(),
                dto.getContent(),
                dto.getCorrectPosition(),
                dto.getPosition(),
                dto.getPoints(),
                dto.getMinWordCount(),
                dto.getMaxWordCount(),
                dto.getResponseSeconds());
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

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public BigDecimal getPoints() {
        return points;
    }

    public void setPoints(BigDecimal points) {
        this.points = points;
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

    public List<QuestionOptionDTO> getOptions() {
        return options;
    }

    public void setOptions(List<QuestionOptionDTO> options) {
        this.options = options;
    }
}
