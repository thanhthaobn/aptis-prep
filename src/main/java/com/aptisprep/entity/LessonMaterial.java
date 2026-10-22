package com.aptisprep.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

/**
 * Structured teaching content, separate from the formal exam engine.
 */
@Embeddable
public class LessonMaterial {
    @Size(max = 30)
    @Column(name = "transcript_visibility", length = 30)
    private String transcriptVisibility;
    @Size(max = 30)
    @Column(name = "sample_visibility", length = 30)
    private String sampleVisibility;

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

    @Size(max = 160)
    @Column(name = "topic", length = 160)
    private String topic;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String value) {
        topic = value == null ? null : value.trim();
    }

    @Size(max = 160)
    @Column(name = "task_type", length = 160)
    private String taskType;

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String value) {
        taskType = value == null ? null : value.trim();
    }

    @Size(max = 4000)
    @Column(name = "objectives", columnDefinition = "nvarchar(max)")
    private String objectives;

    public String getObjectives() {
        return objectives;
    }

    public void setObjectives(String value) {
        objectives = value == null ? null : value.trim();
    }

    @Size(max = 10000)
    @Column(name = "strategy", columnDefinition = "nvarchar(max)")
    private String strategy;

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String value) {
        strategy = value == null ? null : value.trim();
    }

    @Size(max = 10000)
    @Column(name = "example_text", columnDefinition = "nvarchar(max)")
    private String exampleText;

    public String getExampleText() {
        return exampleText;
    }

    public void setExampleText(String value) {
        exampleText = value == null ? null : value.trim();
    }

    @Size(max = 10000)
    @Column(name = "analysis", columnDefinition = "nvarchar(max)")
    private String analysis;

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String value) {
        analysis = value == null ? null : value.trim();
    }

    @Size(max = 5000)
    @Column(name = "common_mistakes", columnDefinition = "nvarchar(max)")
    private String commonMistakes;

    public String getCommonMistakes() {
        return commonMistakes;
    }

    public void setCommonMistakes(String value) {
        commonMistakes = value == null ? null : value.trim();
    }

    @Size(max = 5000)
    @Column(name = "practice_prompt", columnDefinition = "nvarchar(max)")
    private String practicePrompt;

    public String getPracticePrompt() {
        return practicePrompt;
    }

    public void setPracticePrompt(String value) {
        practicePrompt = value == null ? null : value.trim();
    }

    @Size(max = 10000)
    @Column(name = "sample_response", columnDefinition = "nvarchar(max)")
    private String sampleResponse;

    public String getSampleResponse() {
        return sampleResponse;
    }

    public void setSampleResponse(String value) {
        sampleResponse = value == null ? null : value.trim();
    }

    @Size(max = 5000)
    @Column(name = "checklist", columnDefinition = "nvarchar(max)")
    private String checklist;

    public String getChecklist() {
        return checklist;
    }

    public void setChecklist(String value) {
        checklist = value == null ? null : value.trim();
    }

    @Size(max = 500)
    @Column(name = "video_url", length = 500)
    private String videoUrl;

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String value) {
        videoUrl = value == null ? null : value.trim();
    }

    @Size(max = 500)
    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String value) {
        sourceUrl = value == null ? null : value.trim();
    }

    @Size(max = 255)
    @Column(name = "source_name", length = 255)
    private String sourceName;

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String value) {
        sourceName = value == null ? null : value.trim();
    }

    public LessonMaterial copy() {
        var value = new LessonMaterial();
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
}
