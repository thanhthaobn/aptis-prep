package com.aptisprep.dto;

import com.aptisprep.entity.Lesson;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public class LessonDTO {
    private Integer id;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    @Size(max = 4000, message = "Mô tả tối đa 4000 ký tự")
    private String description;

    @NotBlank(message = "Kỹ năng không được để trống")
    private String skill;

    @NotBlank(message = "Trình độ CEFR không được để trống")
    private String cefrLevel;

    @Size(max = 10000, message = "Transcript tối đa 10000 ký tự")
    private String transcript;

    @Min(value = 0, message = "Số từ tối thiểu không được âm")
    private Integer minWordCount;

    @Min(value = 0, message = "Số từ tối đa không được âm")
    private Integer maxWordCount;

    @NotNull(message = "Thứ tự hiển thị không được để trống")
    private Integer displayOrder = 0;

    @NotBlank(message = "Trạng thái không được để trống")
    private String status = "DRAFT";

    private String transcriptVisibility = "ALWAYS";
    private String sampleVisibility = "ALWAYS";
    private String topic;
    private String taskType;
    private String objectives;
    private String strategy;
    private String exampleText;
    private String analysis;
    private String commonMistakes;
    private String practicePrompt;
    private String sampleResponse;
    private String checklist;
    private String videoUrl;
    private String sourceUrl;
    private String sourceName;
    private String videoPath;
    private String audioPath;
    private String audioName;
    private String imagePath;
    private String imageName;

    // Form fields for file uploads and deletes
    private MultipartFile videoFile;
    private MultipartFile audioFile;
    private MultipartFile imageFile;
    private boolean removeVideo;
    private boolean removeAudio;
    private boolean removeImage;

    // Aggregated checks (merges LessonEditorForm)
    private List<@Valid LessonCheckDTO> checks = new ArrayList<>();

    // Aggregated stats (merges LessonStatsDTO)
    private long started;
    private long completed;
    private long audioListeners;
    private long attempts;
    private double averagePercent;

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
        dto.transcriptVisibility = entity.getTranscriptVisibility();
        dto.sampleVisibility = entity.getSampleVisibility();
        dto.topic = entity.getTopic();
        dto.taskType = entity.getTaskType();
        dto.objectives = entity.getObjectives();
        dto.strategy = entity.getStrategy();
        dto.exampleText = entity.getExampleText();
        dto.analysis = entity.getAnalysis();
        dto.commonMistakes = entity.getCommonMistakes();
        dto.practicePrompt = entity.getPracticePrompt();
        dto.sampleResponse = entity.getSampleResponse();
        dto.checklist = entity.getChecklist();
        dto.videoUrl = entity.getVideoUrl();
        dto.sourceUrl = entity.getSourceUrl();
        dto.sourceName = entity.getSourceName();
        dto.videoPath = entity.getVideoPath();
        dto.audioPath = entity.getAudioPath();
        dto.audioName = entity.getAudioName();
        dto.imagePath = entity.getImagePath();
        dto.imageName = entity.getImageName();
        return dto;
    }

    public Lesson convertDtoToEntity() {
        Lesson entity = new Lesson();
        updateEntityFromDto(entity);
        if (this.id != null) {
            entity.setId(this.id);
        }
        return entity;
    }

    public static Lesson convertDtoToEntity(LessonDTO dto) {
        return dto == null ? null : dto.convertDtoToEntity();
    }

    public void updateEntityFromDto(Lesson entity) {
        if (entity == null) return;
        entity.setTitle(trim(this.title));
        entity.setDescription(blankToNull(this.description));
        entity.setSkill(trim(this.skill));
        entity.setCefrLevel(trim(this.cefrLevel));
        entity.setTranscript(this.transcript);
        entity.setMinWordCount(this.minWordCount);
        entity.setMaxWordCount(this.maxWordCount);
        if (this.displayOrder != null) entity.setDisplayOrder(this.displayOrder);
        if (this.status != null) entity.setStatus(this.status);
        entity.setTranscriptVisibility(this.transcriptVisibility);
        entity.setSampleVisibility(this.sampleVisibility);
        entity.setTopic(this.topic);
        entity.setTaskType(this.taskType);
        entity.setObjectives(this.objectives);
        entity.setStrategy(this.strategy);
        entity.setExampleText(this.exampleText);
        entity.setAnalysis(this.analysis);
        entity.setCommonMistakes(this.commonMistakes);
        entity.setPracticePrompt(this.practicePrompt);
        entity.setSampleResponse(this.sampleResponse);
        entity.setChecklist(this.checklist);
        entity.setVideoUrl(this.videoUrl);
        entity.setSourceUrl(this.sourceUrl);
        entity.setSourceName(this.sourceName);
        if (this.videoPath != null) entity.setVideoPath(this.videoPath);
        if (this.audioPath != null) entity.setAudioPath(this.audioPath);
        if (this.audioName != null) entity.setAudioName(this.audioName);
        if (this.imagePath != null) entity.setImagePath(this.imagePath);
        if (this.imageName != null) entity.setImageName(this.imageName);
    }

    public LessonEditorForm toForm() {
        return LessonEditorForm.fromDto(this);
    }

    public static LessonDTO fromForm(LessonDTO form) {
        return form;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String blankToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isBlank() ? null : trimmed;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer id() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String description() {
        return description;
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

    public String getCefrLevel() {
        return cefrLevel;
    }

    public void setCefrLevel(String cefrLevel) {
        this.cefrLevel = cefrLevel;
    }

    public String cefrLevel() {
        return cefrLevel;
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

    public Integer getMinWordCount() {
        return minWordCount;
    }

    public void setMinWordCount(Integer minWordCount) {
        this.minWordCount = minWordCount;
    }

    public Integer minWordCount() {
        return minWordCount;
    }

    public Integer getMaxWordCount() {
        return maxWordCount;
    }

    public void setMaxWordCount(Integer maxWordCount) {
        this.maxWordCount = maxWordCount;
    }

    public Integer maxWordCount() {
        return maxWordCount;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Integer displayOrder() {
        return displayOrder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String status() {
        return status;
    }

    public String getTranscriptVisibility() {
        return transcriptVisibility;
    }

    public void setTranscriptVisibility(String transcriptVisibility) {
        this.transcriptVisibility = transcriptVisibility;
    }

    public String transcriptVisibility() {
        return transcriptVisibility;
    }

    public String getSampleVisibility() {
        return sampleVisibility;
    }

    public void setSampleVisibility(String sampleVisibility) {
        this.sampleVisibility = sampleVisibility;
    }

    public String sampleVisibility() {
        return sampleVisibility;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String topic() {
        return topic;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String taskType() {
        return taskType;
    }

    public String getObjectives() {
        return objectives;
    }

    public void setObjectives(String objectives) {
        this.objectives = objectives;
    }

    public String objectives() {
        return objectives;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String strategy() {
        return strategy;
    }

    public String getExampleText() {
        return exampleText;
    }

    public void setExampleText(String exampleText) {
        this.exampleText = exampleText;
    }

    public String exampleText() {
        return exampleText;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    public String analysis() {
        return analysis;
    }

    public String getCommonMistakes() {
        return commonMistakes;
    }

    public void setCommonMistakes(String commonMistakes) {
        this.commonMistakes = commonMistakes;
    }

    public String commonMistakes() {
        return commonMistakes;
    }

    public String getPracticePrompt() {
        return practicePrompt;
    }

    public void setPracticePrompt(String practicePrompt) {
        this.practicePrompt = practicePrompt;
    }

    public String practicePrompt() {
        return practicePrompt;
    }

    public String getSampleResponse() {
        return sampleResponse;
    }

    public void setSampleResponse(String sampleResponse) {
        this.sampleResponse = sampleResponse;
    }

    public String sampleResponse() {
        return sampleResponse;
    }

    public String getChecklist() {
        return checklist;
    }

    public void setChecklist(String checklist) {
        this.checklist = checklist;
    }

    public String checklist() {
        return checklist;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String videoUrl() {
        return videoUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String sourceUrl() {
        return sourceUrl;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String sourceName() {
        return sourceName;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public String videoPath() {
        return videoPath;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public String audioPath() {
        return audioPath;
    }

    public String getAudioName() {
        return audioName;
    }

    public void setAudioName(String audioName) {
        this.audioName = audioName;
    }

    public String audioName() {
        return audioName;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String imagePath() {
        return imagePath;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String imageName() {
        return imageName;
    }

    public MultipartFile getVideoFile() {
        return videoFile;
    }

    public void setVideoFile(MultipartFile videoFile) {
        this.videoFile = videoFile;
    }

    public MultipartFile getAudioFile() {
        return audioFile;
    }

    public void setAudioFile(MultipartFile audioFile) {
        this.audioFile = audioFile;
    }

    public MultipartFile getImageFile() {
        return imageFile;
    }

    public void setImageFile(MultipartFile imageFile) {
        this.imageFile = imageFile;
    }

    public boolean isRemoveVideo() {
        return removeVideo;
    }

    public void setRemoveVideo(boolean removeVideo) {
        this.removeVideo = removeVideo;
    }

    public boolean isRemoveAudio() {
        return removeAudio;
    }

    public void setRemoveAudio(boolean removeAudio) {
        this.removeAudio = removeAudio;
    }

    public boolean isRemoveImage() {
        return removeImage;
    }

    public void setRemoveImage(boolean removeImage) {
        this.removeImage = removeImage;
    }

    public List<LessonCheckDTO> getChecks() {
        return checks;
    }

    public void setChecks(List<LessonCheckDTO> checks) {
        this.checks = checks == null ? new ArrayList<>() : checks;
    }

    public List<LessonCheckDTO> checks() {
        return checks;
    }

    public long getStarted() {
        return started;
    }

    public void setStarted(long started) {
        this.started = started;
    }

    public long started() {
        return started;
    }

    public long getCompleted() {
        return completed;
    }

    public void setCompleted(long completed) {
        this.completed = completed;
    }

    public long completed() {
        return completed;
    }

    public long getAudioListeners() {
        return audioListeners;
    }

    public void setAudioListeners(long audioListeners) {
        this.audioListeners = audioListeners;
    }

    public long audioListeners() {
        return audioListeners;
    }

    public long getAttempts() {
        return attempts;
    }

    public void setAttempts(long attempts) {
        this.attempts = attempts;
    }

    public long attempts() {
        return attempts;
    }

    public double getAveragePercent() {
        return averagePercent;
    }

    public void setAveragePercent(double averagePercent) {
        this.averagePercent = averagePercent;
    }

    public double averagePercent() {
        return averagePercent;
    }
}
