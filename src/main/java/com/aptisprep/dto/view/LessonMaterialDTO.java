package com.aptisprep.dto.view;

import com.aptisprep.entity.LessonMaterial;
import jakarta.validation.constraints.Size;

public class LessonMaterialDTO {
    public LessonMaterialDTO() {}

    public LessonMaterialDTO(
            String transcriptVisibility,
            String sampleVisibility,
            String topic,
            String taskType,
            String objectives,
            String strategy,
            String exampleText,
            String analysis,
            String commonMistakes,
            String practicePrompt,
            String sampleResponse,
            String checklist,
            String videoUrl,
            String sourceUrl,
            String sourceName) {
        this.transcriptVisibility = transcriptVisibility;
        this.sampleVisibility = sampleVisibility;
        this.topic = topic;
        this.taskType = taskType;
        this.objectives = objectives;
        this.strategy = strategy;
        this.exampleText = exampleText;
        this.analysis = analysis;
        this.commonMistakes = commonMistakes;
        this.practicePrompt = practicePrompt;
        this.sampleResponse = sampleResponse;
        this.checklist = checklist;
        this.videoUrl = videoUrl;
        this.sourceUrl = sourceUrl;
        this.sourceName = sourceName;
    }

    @Size(max = 30)
    private String transcriptVisibility;

    @Size(max = 30)
    private String sampleVisibility;

    @Size(max = 160)
    private String topic;

    @Size(max = 160)
    private String taskType;

    @Size(max = 4000)
    private String objectives;

    @Size(max = 10000)
    private String strategy;

    @Size(max = 10000)
    private String exampleText;

    @Size(max = 10000)
    private String analysis;

    @Size(max = 5000)
    private String commonMistakes;

    @Size(max = 5000)
    private String practicePrompt;

    @Size(max = 10000)
    private String sampleResponse;

    @Size(max = 5000)
    private String checklist;

    @Size(max = 500)
    private String videoUrl;

    @Size(max = 500)
    private String sourceUrl;

    @Size(max = 255)
    private String sourceName;

    public String getTranscriptVisibility() {
        return transcriptVisibility;
    }

    public void setTranscriptVisibility(String v) {
        transcriptVisibility = v;
    }

    public String getSampleVisibility() {
        return sampleVisibility;
    }

    public void setSampleVisibility(String v) {
        sampleVisibility = v;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String value) {
        topic = value == null ? null : value.trim();
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String value) {
        taskType = value == null ? null : value.trim();
    }

    public String getObjectives() {
        return objectives;
    }

    public void setObjectives(String value) {
        objectives = value == null ? null : value.trim();
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String value) {
        strategy = value == null ? null : value.trim();
    }

    public String getExampleText() {
        return exampleText;
    }

    public void setExampleText(String value) {
        exampleText = value == null ? null : value.trim();
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String value) {
        analysis = value == null ? null : value.trim();
    }

    public String getCommonMistakes() {
        return commonMistakes;
    }

    public void setCommonMistakes(String value) {
        commonMistakes = value == null ? null : value.trim();
    }

    public String getPracticePrompt() {
        return practicePrompt;
    }

    public void setPracticePrompt(String value) {
        practicePrompt = value == null ? null : value.trim();
    }

    public String getSampleResponse() {
        return sampleResponse;
    }

    public void setSampleResponse(String value) {
        sampleResponse = value == null ? null : value.trim();
    }

    public String getChecklist() {
        return checklist;
    }

    public void setChecklist(String value) {
        checklist = value == null ? null : value.trim();
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String value) {
        videoUrl = value == null ? null : value.trim();
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String value) {
        sourceUrl = value == null ? null : value.trim();
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String value) {
        sourceName = value == null ? null : value.trim();
    }

    public LessonMaterialDTO copy() {
        LessonMaterialDTO value = new LessonMaterialDTO();
        value.transcriptVisibility = this.transcriptVisibility;
        value.sampleVisibility = this.sampleVisibility;
        value.topic = this.topic;
        value.taskType = this.taskType;
        value.objectives = this.objectives;
        value.strategy = this.strategy;
        value.exampleText = this.exampleText;
        value.analysis = this.analysis;
        value.commonMistakes = this.commonMistakes;
        value.practicePrompt = this.practicePrompt;
        value.sampleResponse = this.sampleResponse;
        value.checklist = this.checklist;
        value.videoUrl = this.videoUrl;
        value.sourceUrl = this.sourceUrl;
        value.sourceName = this.sourceName;
        return value;
    }

    public static LessonMaterialDTO convertEntityToDto(LessonMaterial source) {
        if (source == null) return null;
        LessonMaterialDTO dto = new LessonMaterialDTO();
        dto.setTranscriptVisibility(source.getTranscriptVisibility());
        dto.setSampleVisibility(source.getSampleVisibility());
        dto.setTopic(source.getTopic());
        dto.setTaskType(source.getTaskType());
        dto.setObjectives(source.getObjectives());
        dto.setStrategy(source.getStrategy());
        dto.setExampleText(source.getExampleText());
        dto.setAnalysis(source.getAnalysis());
        dto.setCommonMistakes(source.getCommonMistakes());
        dto.setPracticePrompt(source.getPracticePrompt());
        dto.setSampleResponse(source.getSampleResponse());
        dto.setChecklist(source.getChecklist());
        dto.setVideoUrl(source.getVideoUrl());
        dto.setSourceUrl(source.getSourceUrl());
        dto.setSourceName(source.getSourceName());
        return dto;
    }

    public static LessonMaterial convertDtoToEntity(LessonMaterialDTO dto) {
        if (dto == null) return null;
        LessonMaterial entity = new LessonMaterial();
        entity.setTranscriptVisibility(dto.transcriptVisibility);
        entity.setSampleVisibility(dto.sampleVisibility);
        entity.setTopic(dto.topic);
        entity.setTaskType(dto.taskType);
        entity.setObjectives(dto.objectives);
        entity.setStrategy(dto.strategy);
        entity.setExampleText(dto.exampleText);
        entity.setAnalysis(dto.analysis);
        entity.setCommonMistakes(dto.commonMistakes);
        entity.setPracticePrompt(dto.practicePrompt);
        entity.setSampleResponse(dto.sampleResponse);
        entity.setChecklist(dto.checklist);
        entity.setVideoUrl(dto.videoUrl);
        entity.setSourceUrl(dto.sourceUrl);
        entity.setSourceName(dto.sourceName);
        return entity;
    }
}
