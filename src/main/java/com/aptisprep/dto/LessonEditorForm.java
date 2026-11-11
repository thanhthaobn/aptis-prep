package com.aptisprep.dto;

import java.util.List;

public class LessonEditorForm extends LessonDTO {
    public LessonEditorForm() {
        super();
    }

    public LessonEditorForm(List<LessonCheckDTO> checks) {
        super();
        setChecks(checks);
    }

    public static LessonEditorForm fromDto(LessonDTO dto) {
        if (dto == null) return null;
        LessonEditorForm f = new LessonEditorForm();
        f.setId(dto.getId());
        f.setTitle(dto.getTitle());
        f.setDescription(dto.getDescription());
        f.setSkill(dto.getSkill());
        f.setCefrLevel(dto.getCefrLevel());
        f.setTranscript(dto.getTranscript());
        f.setMinWordCount(dto.getMinWordCount());
        f.setMaxWordCount(dto.getMaxWordCount());
        f.setDisplayOrder(dto.getDisplayOrder());
        f.setStatus(dto.getStatus());
        f.setTranscriptVisibility(dto.getTranscriptVisibility());
        f.setSampleVisibility(dto.getSampleVisibility());
        f.setTopic(dto.getTopic());
        f.setTaskType(dto.getTaskType());
        f.setObjectives(dto.getObjectives());
        f.setStrategy(dto.getStrategy());
        f.setExampleText(dto.getExampleText());
        f.setAnalysis(dto.getAnalysis());
        f.setCommonMistakes(dto.getCommonMistakes());
        f.setPracticePrompt(dto.getPracticePrompt());
        f.setSampleResponse(dto.getSampleResponse());
        f.setChecklist(dto.getChecklist());
        f.setVideoUrl(dto.getVideoUrl());
        f.setSourceUrl(dto.getSourceUrl());
        f.setSourceName(dto.getSourceName());
        f.setVideoPath(dto.getVideoPath());
        f.setAudioPath(dto.getAudioPath());
        f.setAudioName(dto.getAudioName());
        f.setImagePath(dto.getImagePath());
        f.setImageName(dto.getImageName());
        f.setChecks(dto.getChecks());
        return f;
    }
}
