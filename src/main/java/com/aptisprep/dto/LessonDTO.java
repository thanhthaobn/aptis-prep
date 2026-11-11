package com.aptisprep.dto;

import com.aptisprep.entity.Lesson;
import com.aptisprep.presentation.form.LessonEditorForm;
import jakarta.validation.constraints.*;

/** DTO boundary for a lesson entity; uploaded media paths stay service-owned. */
public class LessonDTO {
    public LessonDTO() {}

    public LessonDTO(
            Integer id,
            String title,
            String description,
            String skill,
            String cefrLevel,
            String transcript,
            Integer minWordCount,
            Integer maxWordCount,
            Integer displayOrder,
            String status,
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
            String sourceName,
            String videoPath,
            String audioPath,
            String audioName,
            String imagePath,
            String imageName) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.skill = skill;
        this.cefrLevel = cefrLevel;
        this.transcript = transcript;
        this.minWordCount = minWordCount;
        this.maxWordCount = maxWordCount;
        this.displayOrder = displayOrder;
        this.status = status;
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
        this.videoPath = videoPath;
        this.audioPath = audioPath;
        this.audioName = audioName;
        this.imagePath = imagePath;
        this.imageName = imageName;
    }

    private Integer id;
    private String title;
    private String description;
    private String skill;
    private String cefrLevel;
    private String transcript;
    private Integer minWordCount;
    private Integer maxWordCount;
    private Integer displayOrder;
    private String status;
    private String videoPath;
    private String audioPath;
    private String audioName;
    private String imagePath;
    private String imageName;

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

    public static LessonDTO fromForm(LessonEditorForm form) {
        if (form == null) return null;
        LessonDTO dto = new LessonDTO();
        dto.title = form.getTitle();
        dto.description = form.getDescription();
        dto.skill = form.getSkill();
        dto.cefrLevel = form.getCefrLevel();
        dto.transcript = form.getTranscript();
        dto.minWordCount = form.getMinWordCount();
        dto.maxWordCount = form.getMaxWordCount();
        dto.displayOrder = form.getDisplayOrder();
        dto.status = form.getStatus();
        copyMaterial(form, dto);
        return dto;
    }

    public static Lesson convertDtoToEntity(LessonDTO dto) {
        if (dto == null) return null;
        Lesson entity = new Lesson();
        entity.setTitle(trim(dto.title));
        entity.setDescription(trim(dto.description));
        entity.setSkill(dto.skill);
        entity.setCefrLevel(dto.cefrLevel);
        entity.setTranscript(trim(dto.transcript));
        entity.setMinWordCount(dto.minWordCount);
        entity.setMaxWordCount(dto.maxWordCount);
        entity.setDisplayOrder(dto.displayOrder);
        entity.setStatus(dto.status);
        copyMaterialToEntity(dto, entity);
        return entity;
    }

    public static Lesson convertDtoToEntity(LessonEditorForm form) {
        return convertDtoToEntity(fromForm(form));
    }

    public static LessonDTO convertEntityToDto(Lesson entity) {
        if (entity == null) return null;
        LessonDTO dto = new LessonDTO();
        dto.id = entity.getId();
        dto.title = entity.getTitle();
        dto.description = entity.getDescription();
        dto.skill = entity.getSkill();
        dto.cefrLevel = entity.getCefrLevel();
        dto.transcript = entity.getTranscript();
        dto.minWordCount = entity.getMinWordCount();
        dto.maxWordCount = entity.getMaxWordCount();
        dto.displayOrder = entity.getDisplayOrder();
        dto.status = entity.getStatus();
        copyMaterialFromEntity(entity, dto);
        dto.videoPath = entity.getVideoPath();
        dto.audioPath = entity.getAudioPath();
        dto.audioName = entity.getAudioName();
        dto.imagePath = entity.getImagePath();
        dto.imageName = entity.getImageName();
        return dto;
    }

    public LessonEditorForm toForm() {
        LessonEditorForm form = new LessonEditorForm();
        copyMaterial(this, form);
        form.setTitle(title);
        form.setDescription(description);
        form.setSkill(skill);
        form.setCefrLevel(cefrLevel);
        form.setTranscript(transcript);
        form.setMinWordCount(minWordCount);
        form.setMaxWordCount(maxWordCount);
        form.setDisplayOrder(displayOrder);
        form.setStatus(status);
        return form;
    }

    private static void copyMaterial(LessonDTO source, LessonDTO target) {
        target.setTranscriptVisibility(source.getTranscriptVisibility());
        target.setSampleVisibility(source.getSampleVisibility());
        target.setTopic(source.getTopic());
        target.setTaskType(source.getTaskType());
        target.setObjectives(source.getObjectives());
        target.setStrategy(source.getStrategy());
        target.setExampleText(source.getExampleText());
        target.setAnalysis(source.getAnalysis());
        target.setCommonMistakes(source.getCommonMistakes());
        target.setPracticePrompt(source.getPracticePrompt());
        target.setSampleResponse(source.getSampleResponse());
        target.setChecklist(source.getChecklist());
        target.setVideoUrl(source.getVideoUrl());
        target.setSourceUrl(source.getSourceUrl());
        target.setSourceName(source.getSourceName());
    }

    private static void copyMaterialToEntity(LessonDTO source, Lesson target) {
        target.setTranscriptVisibility(source.getTranscriptVisibility());
        target.setSampleVisibility(source.getSampleVisibility());
        target.setTopic(source.getTopic());
        target.setTaskType(source.getTaskType());
        target.setObjectives(source.getObjectives());
        target.setStrategy(source.getStrategy());
        target.setExampleText(source.getExampleText());
        target.setAnalysis(source.getAnalysis());
        target.setCommonMistakes(source.getCommonMistakes());
        target.setPracticePrompt(source.getPracticePrompt());
        target.setSampleResponse(source.getSampleResponse());
        target.setChecklist(source.getChecklist());
        target.setVideoUrl(source.getVideoUrl());
        target.setSourceUrl(source.getSourceUrl());
        target.setSourceName(source.getSourceName());
    }

    private static void copyMaterialFromEntity(Lesson source, LessonDTO target) {
        target.setTranscriptVisibility(source.getTranscriptVisibility());
        target.setSampleVisibility(source.getSampleVisibility());
        target.setTopic(source.getTopic());
        target.setTaskType(source.getTaskType());
        target.setObjectives(source.getObjectives());
        target.setStrategy(source.getStrategy());
        target.setExampleText(source.getExampleText());
        target.setAnalysis(source.getAnalysis());
        target.setCommonMistakes(source.getCommonMistakes());
        target.setPracticePrompt(source.getPracticePrompt());
        target.setSampleResponse(source.getSampleResponse());
        target.setChecklist(source.getChecklist());
        target.setVideoUrl(source.getVideoUrl());
        target.setSourceUrl(source.getSourceUrl());
        target.setSourceName(source.getSourceName());
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    public String getTitle() {
        return title;
    }

    public Integer getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getSkill() {
        return skill;
    }

    public String getCefrLevel() {
        return cefrLevel;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public String getStatus() {
        return status;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public String getAudioName() {
        return audioName;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getImageName() {
        return imageName;
    }

    public String getTranscript() {
        return transcript;
    }

    public Integer getMinWordCount() {
        return minWordCount;
    }

    public Integer getMaxWordCount() {
        return maxWordCount;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public void setCefrLevel(String cefrLevel) {
        this.cefrLevel = cefrLevel;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public void setMinWordCount(Integer minWordCount) {
        this.minWordCount = minWordCount;
    }

    public void setMaxWordCount(Integer maxWordCount) {
        this.maxWordCount = maxWordCount;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public void setAudioName(String audioName) {
        this.audioName = audioName;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getTranscriptVisibility() {
        return transcriptVisibility;
    }

    public void setTranscriptVisibility(String value) {
        transcriptVisibility = trim(value);
    }

    public String getSampleVisibility() {
        return sampleVisibility;
    }

    public void setSampleVisibility(String value) {
        sampleVisibility = trim(value);
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String value) {
        topic = trim(value);
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String value) {
        taskType = trim(value);
    }

    public String getObjectives() {
        return objectives;
    }

    public void setObjectives(String value) {
        objectives = trim(value);
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String value) {
        strategy = trim(value);
    }

    public String getExampleText() {
        return exampleText;
    }

    public void setExampleText(String value) {
        exampleText = trim(value);
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String value) {
        analysis = trim(value);
    }

    public String getCommonMistakes() {
        return commonMistakes;
    }

    public void setCommonMistakes(String value) {
        commonMistakes = trim(value);
    }

    public String getPracticePrompt() {
        return practicePrompt;
    }

    public void setPracticePrompt(String value) {
        practicePrompt = trim(value);
    }

    public String getSampleResponse() {
        return sampleResponse;
    }

    public void setSampleResponse(String value) {
        sampleResponse = trim(value);
    }

    public String getChecklist() {
        return checklist;
    }

    public void setChecklist(String value) {
        checklist = trim(value);
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String value) {
        videoUrl = trim(value);
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String value) {
        sourceUrl = trim(value);
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String value) {
        sourceName = trim(value);
    }
}
