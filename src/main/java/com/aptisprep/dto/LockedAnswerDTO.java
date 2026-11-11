package com.aptisprep.dto;

import java.util.List;

public class LockedAnswerDTO {
    private Integer answerId;
    private Integer questionId;
    private String skill;
    private String questionType;
    private String taskContent;
    private String lockedContent;
    private String transcript;
    private List<LockedItemDTO> items;

    public LockedAnswerDTO() {}

    public LockedAnswerDTO(
            Integer answerId,
            Integer questionId,
            String skill,
            String questionType,
            String taskContent,
            String lockedContent,
            String transcript,
            List<LockedItemDTO> items) {
        this.answerId = answerId;
        this.questionId = questionId;
        this.skill = skill;
        this.questionType = questionType;
        this.taskContent = taskContent;
        this.lockedContent = lockedContent;
        this.transcript = transcript;
        this.items = items;
    }

    public LockedAnswerDTO(
            Integer answerId,
            Integer questionId,
            String skill,
            String questionType,
            String taskContent,
            String lockedContent,
            String transcript) {
        this(
                answerId,
                questionId,
                skill,
                questionType,
                taskContent,
                lockedContent,
                transcript,
                List.of());
    }

    public Integer getAnswerId() {
        return answerId;
    }

    public void setAnswerId(Integer answerId) {
        this.answerId = answerId;
    }

    public Integer answerId() {
        return answerId;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public Integer questionId() {
        return questionId;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String skill() {
        return skill;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String questionType() {
        return questionType;
    }

    public String getTaskContent() {
        return taskContent;
    }

    public void setTaskContent(String taskContent) {
        this.taskContent = taskContent;
    }

    public String taskContent() {
        return taskContent;
    }

    public String getLockedContent() {
        return lockedContent;
    }

    public void setLockedContent(String lockedContent) {
        this.lockedContent = lockedContent;
    }

    public String lockedContent() {
        return lockedContent;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public String transcript() {
        return transcript;
    }

    public List<LockedItemDTO> getItems() {
        return items;
    }

    public void setItems(List<LockedItemDTO> items) {
        this.items = items;
    }

    public List<LockedItemDTO> items() {
        return items;
    }
}
