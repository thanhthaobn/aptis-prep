package com.aptisprep.mvc.catalog.dto;

import com.aptisprep.mvc.catalog.entity.QuestionEntity;

/** DTO boundary for the scalar fields of a question entity. */
public class QuestionEntityDTO {
    private String content;
    private String instruction;
    private String readingPassage;
    private String questionType;
    private String skill;
    private Integer partNo;
    private String cefrLevel;
    private String difficulty;
    private boolean allowOptionReuse;
    private Integer preparationSeconds;
    private String status;

    public static QuestionEntityDTO fromForm(QuestionFormDTO form) {
        if (form == null) return null;
        QuestionEntityDTO dto = new QuestionEntityDTO();
        dto.content = form.getContent();
        dto.instruction = form.getInstruction();
        dto.readingPassage = form.getReadingPassage();
        dto.questionType = form.getQuestionType();
        dto.skill = form.getSkill();
        dto.partNo = form.getPartNo();
        dto.cefrLevel = form.getCefrLevel();
        dto.difficulty = form.getDifficulty();
        dto.allowOptionReuse = form.isAllowOptionReuse();
        dto.preparationSeconds = form.getPreparationSeconds();
        dto.status = form.getStatus();
        return dto;
    }

    public static QuestionEntity convertDtoToEntity(QuestionEntityDTO dto) {
        if (dto == null) return null;
        QuestionEntity entity = new QuestionEntity();
        entity.setContent(trim(dto.content));
        entity.setInstruction(blankToNull(dto.instruction));
        entity.setReadingPassage(
                "READING".equals(dto.skill) ? blankToNull(dto.readingPassage) : null);
        entity.setQuestionType(dto.questionType);
        entity.setSkill(dto.skill);
        entity.setPartNo(dto.partNo);
        entity.setCefrLevel(dto.cefrLevel);
        entity.setDifficulty(dto.difficulty);
        entity.setAllowOptionReuse(dto.allowOptionReuse);
        entity.setPreparationSeconds(dto.preparationSeconds);
        entity.setStatus(dto.status);
        return entity;
    }

    public static QuestionEntity convertDtoToEntity(QuestionFormDTO form) {
        return convertDtoToEntity(fromForm(form));
    }

    public static QuestionEntityDTO convertEntityToDto(QuestionEntity entity) {
        if (entity == null) return null;
        QuestionEntityDTO dto = new QuestionEntityDTO();
        dto.content = entity.getContent();
        dto.instruction = entity.getInstruction();
        dto.readingPassage = entity.getReadingPassage();
        dto.questionType = entity.getQuestionType();
        dto.skill = entity.getSkill();
        dto.partNo = entity.getPartNo();
        dto.cefrLevel = entity.getCefrLevel();
        dto.difficulty = entity.getDifficulty();
        dto.allowOptionReuse = entity.isAllowOptionReuse();
        dto.preparationSeconds = entity.getPreparationSeconds();
        dto.status = entity.getStatus();
        return dto;
    }

    public QuestionFormDTO toForm() {
        QuestionFormDTO form = new QuestionFormDTO();
        form.setContent(content);
        form.setInstruction(instruction);
        form.setReadingPassage(readingPassage);
        form.setQuestionType(questionType);
        form.setSkill(skill);
        form.setPartNo(partNo);
        form.setCefrLevel(cefrLevel);
        form.setDifficulty(difficulty);
        form.setAllowOptionReuse(allowOptionReuse);
        form.setPreparationSeconds(preparationSeconds);
        form.setStatus(status);
        return form;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String blankToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }

    public String getContent() {
        return content;
    }

    public String getInstruction() {
        return instruction;
    }

    public String getReadingPassage() {
        return readingPassage;
    }

    public String getQuestionType() {
        return questionType;
    }

    public String getSkill() {
        return skill;
    }

    public Integer getPartNo() {
        return partNo;
    }

    public String getCefrLevel() {
        return cefrLevel;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public boolean isAllowOptionReuse() {
        return allowOptionReuse;
    }

    public Integer getPreparationSeconds() {
        return preparationSeconds;
    }

    public String getStatus() {
        return status;
    }
}
