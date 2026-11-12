package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.Question;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class QuestionDTO {
    private Integer id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @NotBlank(message = "Nội dung câu hỏi là bắt buộc")
    @Size(max = 10000, message = "Nội dung câu hỏi tối đa 10.000 ký tự")
    private String content;

    private String instruction;

    @Size(max = 50000, message = "Đoạn văn Reading tối đa 50.000 ký tự")
    private String readingPassage;

    @NotBlank(message = "Dạng câu hỏi là bắt buộc")
    @Pattern(
            regexp = "SINGLE_CHOICE|MATCHING|ORDERING|WRITING_TEXT|SPEAKING_AUDIO",
            message = "Dạng câu hỏi không hợp lệ")
    private String questionType = "SINGLE_CHOICE";

    @NotBlank(message = "Kỹ năng là bắt buộc")
    @Pattern(
            regexp = "GRAMMAR_VOCABULARY|READING|LISTENING|WRITING|SPEAKING",
            message = "Kỹ năng không hợp lệ")
    private String skill = "READING";

    private Integer partNo;

    @NotBlank(message = "Cấp độ là bắt buộc")
    @Pattern(regexp = "A1|A2|B1|B2|C1", message = "Cấp độ không hợp lệ")
    private String cefrLevel = "B1";

    @NotBlank(message = "Độ khó là bắt buộc")
    @Pattern(regexp = "EASY|MEDIUM|HARD", message = "Độ khó không hợp lệ")
    private String difficulty = "MEDIUM";

    private boolean allowOptionReuse = true;

    @PositiveOrZero(message = "Thời gian chuẩn bị không được âm")
    private Integer preparationSeconds;

    @NotBlank(message = "Trạng thái là bắt buộc")
    @Pattern(regexp = "DRAFT|PUBLISHED|ARCHIVED", message = "Trạng thái không hợp lệ")
    private String status = "DRAFT";

    private List<@Valid QuestionItemDTO> items = new ArrayList<>();

    public QuestionDTO(
            Integer id,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String content,
            String instruction,
            String readingPassage,
            String questionType,
            String skill,
            Integer partNo,
            String cefrLevel,
            String difficulty,
            boolean allowOptionReuse,
            Integer preparationSeconds,
            String status,
            List<QuestionItemDTO> items) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.content = content;
        this.instruction = instruction;
        this.readingPassage = readingPassage;
        this.questionType = questionType;
        this.skill = skill;
        this.partNo = partNo;
        this.cefrLevel = cefrLevel;
        this.difficulty = difficulty;
        this.allowOptionReuse = allowOptionReuse;
        this.preparationSeconds = preparationSeconds;
        this.status = status;
        this.items = items == null ? new ArrayList<>() : items;
    }

    public QuestionDTO() {}

    public static QuestionDTO convertEntityToDto(Question entity) {
        if (entity == null) return null;
        return new QuestionDTO(
                entity.getId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getContent(),
                entity.getInstruction(),
                entity.getReadingPassage(),
                entity.getQuestionType(),
                entity.getSkill(),
                entity.getPartNo(),
                entity.getCefrLevel(),
                entity.getDifficulty(),
                entity.isAllowOptionReuse(),
                entity.getPreparationSeconds(),
                entity.getStatus(),
                new ArrayList<>());
    }

    public static Question convertDtoToEntity(QuestionDTO dto) {
        if (dto == null) return null;
        return new Question(
                dto.getId(),
                dto.getContent(),
                dto.getInstruction(),
                dto.getReadingPassage(),
                dto.getQuestionType(),
                dto.getSkill(),
                dto.getPartNo(),
                dto.getCefrLevel(),
                dto.getDifficulty(),
                dto.isAllowOptionReuse(),
                dto.getPreparationSeconds(),
                dto.getStatus(),
                dto.getCreatedAt(),
                dto.getUpdatedAt());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public String getReadingPassage() {
        return readingPassage;
    }

    public void setReadingPassage(String readingPassage) {
        this.readingPassage = readingPassage;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public Integer getPartNo() {
        return partNo;
    }

    public void setPartNo(Integer partNo) {
        this.partNo = partNo;
    }

    public String getCefrLevel() {
        return cefrLevel;
    }

    public void setCefrLevel(String cefrLevel) {
        this.cefrLevel = cefrLevel;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public boolean isAllowOptionReuse() {
        return allowOptionReuse;
    }

    public void setAllowOptionReuse(boolean allowOptionReuse) {
        this.allowOptionReuse = allowOptionReuse;
    }

    public Integer getPreparationSeconds() {
        return preparationSeconds;
    }

    public void setPreparationSeconds(Integer preparationSeconds) {
        this.preparationSeconds = preparationSeconds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<QuestionItemDTO> getItems() {
        return items;
    }

    public void setItems(List<QuestionItemDTO> items) {
        this.items = items;
    }

    public String getTitle() {
        return content;
    }

    public String getSubtitle() {
        return instruction == null || instruction.isBlank() ? content : instruction;
    }

    public String getLevel() {
        return cefrLevel;
    }

    public String getKind() {
        return questionType;
    }

    public String getDate() {
        return updatedAt == null ? "" : updatedAt.toLocalDate().toString();
    }

    public String getStatusLabel() {
        return switch (status) {
            case "PUBLISHED" -> "Đã công bố";
            case "ARCHIVED" -> "Đã lưu trữ";
            default -> "Bản nháp";
        };
    }
}
