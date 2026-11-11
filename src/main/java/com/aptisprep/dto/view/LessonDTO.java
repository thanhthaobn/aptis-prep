package com.aptisprep.dto.view;

import com.aptisprep.dto.form.LessonFormDTO;
import com.aptisprep.entity.Lesson;

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
            LessonMaterialDTO material,
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
        this.material = material;
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
    private LessonMaterialDTO material;
    private String videoPath;
    private String audioPath;
    private String audioName;
    private String imagePath;
    private String imageName;

    public static LessonDTO fromForm(LessonFormDTO form) {
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
        dto.material = form.copy();
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
        entity.setMaterial(LessonMaterialDTO.convertDtoToEntity(dto.material));
        return entity;
    }

    public static Lesson convertDtoToEntity(LessonFormDTO form) {
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
        dto.material =
                entity.getMaterial() == null
                        ? null
                        : LessonMaterialDTO.convertEntityToDto(entity.getMaterial());
        dto.videoPath = entity.getVideoPath();
        dto.audioPath = entity.getAudioPath();
        dto.audioName = entity.getAudioName();
        dto.imagePath = entity.getImagePath();
        dto.imageName = entity.getImageName();
        return dto;
    }

    public LessonFormDTO toForm() {
        LessonFormDTO form = new LessonFormDTO();
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

    private static void copyMaterial(LessonMaterialDTO source, LessonFormDTO target) {
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

    public LessonMaterialDTO getMaterial() {
        return material;
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

    public void setMaterial(LessonMaterialDTO material) {
        this.material = material;
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
}
