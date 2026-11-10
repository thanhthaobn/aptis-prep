package com.aptisprep.dto;

import com.aptisprep.dto.form.LessonForm;
import com.aptisprep.entity.Lesson;
import com.aptisprep.entity.LessonMaterial;

/** DTO boundary for a lesson entity; uploaded media paths stay service-owned. */
public class LessonEntityDto {
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
    private LessonMaterial material;
    private String videoPath;
    private String audioPath;
    private String audioName;
    private String imagePath;
    private String imageName;

    public static LessonEntityDto fromForm(LessonForm form) {
        if (form == null) return null;
        LessonEntityDto dto = new LessonEntityDto();
        dto.title = form.getTitle();
        dto.description = form.getDescription();
        dto.skill = form.getSkill();
        dto.cefrLevel = form.getCefrLevel();
        dto.transcript = form.getTranscript();
        dto.minWordCount = form.getMinWordCount();
        dto.maxWordCount = form.getMaxWordCount();
        dto.displayOrder = form.getDisplayOrder();
        dto.status = form.getStatus();
        dto.material = form.copy();
        return dto;
    }

    public static Lesson convertDtoToEntity(LessonEntityDto dto) {
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
        entity.setMaterial(dto.material == null ? new LessonMaterial() : dto.material.copy());
        return entity;
    }

    public static Lesson convertDtoToEntity(LessonForm form) {
        return convertDtoToEntity(fromForm(form));
    }

    public static LessonEntityDto convertEntityToDto(Lesson entity) {
        if (entity == null) return null;
        LessonEntityDto dto = new LessonEntityDto();
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
        dto.material = entity.getMaterial() == null ? null : entity.getMaterial().copy();
        dto.videoPath = entity.getVideoPath();
        dto.audioPath = entity.getAudioPath();
        dto.audioName = entity.getAudioName();
        dto.imagePath = entity.getImagePath();
        dto.imageName = entity.getImageName();
        return dto;
    }

    public LessonForm toForm() {
        LessonForm form = new LessonForm();
        if (material != null) copyMaterial(material, form);
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

    private static void copyMaterial(LessonMaterial source, LessonForm target) {
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

    public String getTitle() { return title; }
    public Integer getId() { return id; }
    public String getDescription() { return description; }
    public String getSkill() { return skill; }
    public String getCefrLevel() { return cefrLevel; }
    public Integer getDisplayOrder() { return displayOrder; }
    public String getStatus() { return status; }
    public LessonMaterial getMaterial() { return material; }
    public String getVideoPath() { return videoPath; }
    public String getAudioPath() { return audioPath; }
    public String getAudioName() { return audioName; }
    public String getImagePath() { return imagePath; }
    public String getImageName() { return imageName; }
    public String getTranscript() { return transcript; }
    public Integer getMinWordCount() { return minWordCount; }
    public Integer getMaxWordCount() { return maxWordCount; }
}
