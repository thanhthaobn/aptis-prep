package com.aptisprep.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "questions")
public class QuestionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false, columnDefinition = "nvarchar(max)") private String content;
    @Column(columnDefinition = "nvarchar(max)") private String instruction;
    @Column(name = "question_type", nullable = false, length = 30) private String questionType;
    @Column(nullable = false, length = 30) private String skill;
    @Column(name = "part_no") private Integer partNo;
    @Column(name = "cefr_level", length = 10) private String cefrLevel;
    @Column(length = 30) private String difficulty;
    @Column(name = "allow_option_reuse", nullable = false) private boolean allowOptionReuse = true;
    @Column(name = "preparation_seconds") private Integer preparationSeconds;
    @Column(nullable = false, length = 30) private String status = "DRAFT";
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    public QuestionEntity() {}
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public Integer getId() { return id; }
    public String getContent() { return content; }
    public void setContent(String v) { content = v; }
    public String getInstruction() { return instruction; }
    public void setInstruction(String v) { instruction = v; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String v) { questionType = v; }
    public String getSkill() { return skill; }
    public void setSkill(String v) { skill = v; }
    public Integer getPartNo() { return partNo; }
    public void setPartNo(Integer v) { partNo = v; }
    public String getCefrLevel() { return cefrLevel; }
    public void setCefrLevel(String v) { cefrLevel = v; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String v) { difficulty = v; }
    public boolean isAllowOptionReuse() { return allowOptionReuse; }
    public void setAllowOptionReuse(boolean v) { allowOptionReuse = v; }
    public Integer getPreparationSeconds() { return preparationSeconds; }
    public void setPreparationSeconds(Integer v) { preparationSeconds = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { status = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
