package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.entity.Lesson;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.ArrayList;
import java.util.List;

public class LessonDTO {
    private Integer id;
    private String videoPath;
    private String audioPath;
    private String audioName;
    private String imagePath;
    private String imageName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Size(max = 30, message = "Cấu hình hiển thị transcript tối đa 30 ký tự")
    private String transcriptVisibility;

    @Size(max = 30, message = "Cấu hình hiển thị bài mẫu tối đa 30 ký tự")
    private String sampleVisibility;

    @Size(max = 160, message = "Chủ đề tối đa 160 ký tự")
    private String topic;

    @Size(max = 160, message = "Loại nhiệm vụ tối đa 160 ký tự")
    private String taskType;

    @Size(max = 4000, message = "Mục tiêu tối đa 4000 ký tự")
    private String objectives;

    @Size(max = 10000, message = "Chiến lược tối đa 10000 ký tự")
    private String strategy;

    @Size(max = 10000, message = "Ví dụ tối đa 10000 ký tự")
    private String exampleText;

    @Size(max = 10000, message = "Phân tích tối đa 10000 ký tự")
    private String analysis;

    @Size(max = 5000, message = "Lỗi thường gặp tối đa 5000 ký tự")
    private String commonMistakes;

    @Size(max = 5000, message = "Đề bài luyện tập tối đa 5000 ký tự")
    private String practicePrompt;

    @Size(max = 10000, message = "Bài mẫu tối đa 10000 ký tự")
    private String sampleResponse;

    @Size(max = 5000, message = "Checklist tối đa 5000 ký tự")
    private String checklist;

    @Size(max = 500, message = "Đường dẫn video tối đa 500 ký tự")
    private String videoUrl;

    @Size(max = 500, message = "Đường dẫn nguồn tối đa 500 ký tự")
    private String sourceUrl;

    @Size(max = 255, message = "Tên nguồn tối đa 255 ký tự")
    private String sourceName;

    private List<@Valid LessonCheckDTO> checks = new ArrayList<>();

    public LessonDTO() {}

    @NotBlank(message = "Tiêu đề bài học không được để trống")
    @Size(max = 255, message = "Tiêu đề bài học tối đa 255 ký tự")
    private String title;

    @Size(max = 10000, message = "Mô tả bài học tối đa 10000 ký tự")
    private String description;

    @NotBlank(message = "Kỹ năng không được để trống")
    private String skill;

    @NotBlank(message = "Cấp độ không được để trống")
    private String cefrLevel;

    private Integer displayOrder;

    @NotBlank(message = "Trạng thái không được để trống")
    private String status;

    @Size(max = 10000, message = "Transcript tối đa 10000 ký tự")
    private String transcript;

    @Min(value = 1, message = "Số từ tối thiểu phải lớn hơn 0")
    @Max(value = 5000, message = "Số từ tối thiểu tối đa 5000")
    private Integer minWordCount;

    @Min(value = 1, message = "Số từ tối đa phải lớn hơn 0")
    @Max(value = 5000, message = "Số từ tối đa không vượt quá 5000")
    private Integer maxWordCount;

    public LessonDTO(
            Integer id,
            String title,
            String description,
            String skill,
            String cefrLevel,
            String videoPath,
            String audioPath,
            String audioName,
            String imagePath,
            String imageName,
            String transcript,
            Integer minWordCount,
            Integer maxWordCount,
            Integer displayOrder,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
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
            List<LessonCheckDTO> checks) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.skill = skill;
        this.cefrLevel = cefrLevel;
        this.videoPath = videoPath;
        this.audioPath = audioPath;
        this.audioName = audioName;
        this.imagePath = imagePath;
        this.imageName = imageName;
        this.transcript = transcript;
        this.minWordCount = minWordCount;
        this.maxWordCount = maxWordCount;
        this.displayOrder = displayOrder;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
        this.checks = checks == null ? new ArrayList<>() : new ArrayList<>(checks);
    }

    public static LessonDTO convertEntityToDto(Lesson entity) {
        if (entity == null) return null;
        return new LessonDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getSkill(),
                entity.getCefrLevel(),
                entity.getVideoPath(),
                entity.getAudioPath(),
                entity.getAudioName(),
                entity.getImagePath(),
                entity.getImageName(),
                entity.getTranscript(),
                entity.getMinWordCount(),
                entity.getMaxWordCount(),
                entity.getDisplayOrder(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getTranscriptVisibility(),
                entity.getSampleVisibility(),
                entity.getTopic(),
                entity.getTaskType(),
                entity.getObjectives(),
                entity.getStrategy(),
                entity.getExampleText(),
                entity.getAnalysis(),
                entity.getCommonMistakes(),
                entity.getPracticePrompt(),
                entity.getSampleResponse(),
                entity.getChecklist(),
                entity.getVideoUrl(),
                entity.getSourceUrl(),
                entity.getSourceName(),
                null);
    }

    public static Lesson convertDtoToEntity(LessonDTO dto) {
        if (dto == null) return null;
        return new Lesson(
                dto.getId(),
                dto.getTitle(),
                dto.getDescription(),
                dto.getSkill(),
                dto.getCefrLevel(),
                dto.getVideoPath(),
                dto.getAudioPath(),
                dto.getAudioName(),
                dto.getImagePath(),
                dto.getImageName(),
                dto.getTranscript(),
                dto.getMinWordCount(),
                dto.getMaxWordCount(),
                dto.getDisplayOrder(),
                dto.getStatus(),
                dto.getCreatedAt(),
                dto.getUpdatedAt(),
                dto.getTranscriptVisibility(),
                dto.getSampleVisibility(),
                dto.getTopic(),
                dto.getTaskType(),
                dto.getObjectives(),
                dto.getStrategy(),
                dto.getExampleText(),
                dto.getAnalysis(),
                dto.getCommonMistakes(),
                dto.getPracticePrompt(),
                dto.getSampleResponse(),
                dto.getChecklist(),
                dto.getVideoUrl(),
                dto.getSourceUrl(),
                dto.getSourceName());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public String getAudioName() {
        return audioName;
    }

    public void setAudioName(String audioName) {
        this.audioName = audioName;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
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

    public String getTranscriptVisibility() {
        return transcriptVisibility;
    }

    public void setTranscriptVisibility(String transcriptVisibility) {
        this.transcriptVisibility = transcriptVisibility;
    }

    public String getSampleVisibility() {
        return sampleVisibility;
    }

    public void setSampleVisibility(String sampleVisibility) {
        this.sampleVisibility = sampleVisibility;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getObjectives() {
        return objectives;
    }

    public void setObjectives(String objectives) {
        this.objectives = objectives;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getExampleText() {
        return exampleText;
    }

    public void setExampleText(String exampleText) {
        this.exampleText = exampleText;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    public String getCommonMistakes() {
        return commonMistakes;
    }

    public void setCommonMistakes(String commonMistakes) {
        this.commonMistakes = commonMistakes;
    }

    public String getPracticePrompt() {
        return practicePrompt;
    }

    public void setPracticePrompt(String practicePrompt) {
        this.practicePrompt = practicePrompt;
    }

    public String getSampleResponse() {
        return sampleResponse;
    }

    public void setSampleResponse(String sampleResponse) {
        this.sampleResponse = sampleResponse;
    }

    public String getChecklist() {
        return checklist;
    }

    public void setChecklist(String checklist) {
        this.checklist = checklist;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public List<LessonCheckDTO> getChecks() {
        return checks;
    }

    public void setChecks(List<LessonCheckDTO> checks) {
        this.checks = checks;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String getCefrLevel() {
        return cefrLevel;
    }

    public void setCefrLevel(String cefrLevel) {
        this.cefrLevel = cefrLevel;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusLabel() {
        if ("PUBLISHED".equals(status)) return "Đã công bố";
        if ("ARCHIVED".equals(status)) return "Đã lưu trữ";
        return "Bản nháp";
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
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
}
