package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import java.math.*;
import java.time.*;
import java.util.*;

public class ExamQuestionSnapshotDTO {
    private Integer id;
    private ExamAttemptSnapshot snapshot;
    private Integer sourceQuestionId;
    private Integer position;
    private String skill;
    private String questionType;
    private String content;
    private String instruction;
    private String readingPassage;
    private Integer sectionStartMinute;
    private Integer sectionEndMinute;
    private List<ExamItemSnapshot> items;
    private List<ExamMediaSnapshot> media;

    public ExamQuestionSnapshotDTO() {}

    public ExamQuestionSnapshotDTO(
            Integer id,
            ExamAttemptSnapshot snapshot,
            Integer sourceQuestionId,
            Integer position,
            String skill,
            String questionType,
            String content,
            String instruction,
            String readingPassage,
            Integer sectionStartMinute,
            Integer sectionEndMinute,
            List<ExamItemSnapshot> items,
            List<ExamMediaSnapshot> media) {
        this.id = id;
        this.snapshot = snapshot;
        this.sourceQuestionId = sourceQuestionId;
        this.position = position;
        this.skill = skill;
        this.questionType = questionType;
        this.content = content;
        this.instruction = instruction;
        this.readingPassage = readingPassage;
        this.sectionStartMinute = sectionStartMinute;
        this.sectionEndMinute = sectionEndMinute;
        this.items = items;
        this.media = media;
    }

    public static ExamQuestionSnapshotDTO convertEntityToDto(ExamQuestionSnapshot entity) {
        if (entity == null) return null;
        return new ExamQuestionSnapshotDTO(
                entity.getId(),
                entity.getSnapshot(),
                entity.getSourceQuestionId(),
                entity.getPosition(),
                entity.getSkill(),
                entity.getQuestionType(),
                entity.getContent(),
                entity.getInstruction(),
                entity.getReadingPassage(),
                entity.getSectionStartMinute(),
                entity.getSectionEndMinute(),
                entity.getItems(),
                entity.getMedia());
    }

    public static ExamQuestionSnapshot convertDtoToEntity(ExamQuestionSnapshotDTO dto) {
        if (dto == null) return null;
        return new ExamQuestionSnapshot(
                dto.getId(),
                dto.getSnapshot(),
                dto.getSourceQuestionId(),
                dto.getPosition(),
                dto.getSkill(),
                dto.getQuestionType(),
                dto.getContent(),
                dto.getInstruction(),
                dto.getReadingPassage(),
                dto.getSectionStartMinute(),
                dto.getSectionEndMinute(),
                dto.getItems(),
                dto.getMedia());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ExamAttemptSnapshot getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(ExamAttemptSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public Integer getSourceQuestionId() {
        return sourceQuestionId;
    }

    public void setSourceQuestionId(Integer sourceQuestionId) {
        this.sourceQuestionId = sourceQuestionId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
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

    public Integer getSectionStartMinute() {
        return sectionStartMinute;
    }

    public void setSectionStartMinute(Integer sectionStartMinute) {
        this.sectionStartMinute = sectionStartMinute;
    }

    public Integer getSectionEndMinute() {
        return sectionEndMinute;
    }

    public void setSectionEndMinute(Integer sectionEndMinute) {
        this.sectionEndMinute = sectionEndMinute;
    }

    public List<ExamItemSnapshot> getItems() {
        return items;
    }

    public void setItems(List<ExamItemSnapshot> items) {
        this.items = items;
    }

    public List<ExamMediaSnapshot> getMedia() {
        return media;
    }

    public void setMedia(List<ExamMediaSnapshot> media) {
        this.media = media;
    }
}











