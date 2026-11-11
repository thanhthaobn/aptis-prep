package com.aptisprep.dto;

import com.aptisprep.entity.QuestionEntity;
import com.aptisprep.entity.QuestionItemEntity;
import com.aptisprep.entity.QuestionMediaEntity;
import com.aptisprep.entity.QuestionOptionEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public class QuestionDTO {
    private Integer id;

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    @Size(max = 4000, message = "Nội dung câu hỏi tối đa 4000 ký tự")
    private String content;

    @Size(max = 2000, message = "Hướng dẫn tối đa 2000 ký tự")
    private String instruction;

    private String readingPassage;

    @NotBlank(message = "Phải chọn loại câu hỏi")
    private String questionType;

    @NotBlank(message = "Phải chọn kỹ năng")
    private String skill;

    private Integer partNo = 1;
    private String cefrLevel = "B1";
    private String difficulty = "MEDIUM";
    private Boolean allowOptionReuse = false;
    private Integer preparationSeconds = 0;
    private String status = "DRAFT";
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<QuestionItemDTO> items = new ArrayList<>();
    private List<QuestionMediaDTO> media = new ArrayList<>();
    private MultipartFile upload;

    public QuestionDTO() {}

    public QuestionDTO(
            Integer id,
            String content,
            String instruction,
            String readingPassage,
            String questionType,
            String skill,
            Integer partNo,
            String cefrLevel,
            String difficulty,
            Boolean allowOptionReuse,
            Integer preparationSeconds,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.content = content;
        this.instruction = instruction;
        this.readingPassage = readingPassage;
        this.questionType = questionType;
        this.skill = skill;
        this.partNo = partNo;
        this.cefrLevel = cefrLevel;
        this.difficulty = difficulty;
        this.allowOptionReuse = allowOptionReuse;
        this.preparationSeconds = preparationSeconds;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static QuestionDTO convertEntityToDto(QuestionEntity entity) {
        if (entity == null) return null;
        return new QuestionDTO(
                entity.getId(),
                entity.getContent(),
                entity.getInstruction(),
                entity.getReadingPassage(),
                entity.getQuestionType(),
                entity.getSkill(),
                entity.getPartNo(),
                entity.getCefrLevel(),
                entity.getDifficulty(),
                entity.isAllowOptionReuse(),
                entity.getPreparationSeconds(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static QuestionDTO convertEntityToDto(
            QuestionEntity entity,
            List<QuestionItemEntity> itemEntities,
            Map<Integer, List<QuestionOptionEntity>> optionsByItemId,
            List<QuestionMediaEntity> mediaEntities) {
        QuestionDTO dto = convertEntityToDto(entity);
        if (dto == null) return null;

        if (itemEntities != null) {
            for (QuestionItemEntity ie : itemEntities) {
                QuestionItemDTO idto = QuestionItemDTO.convertEntityToDto(ie);
                if (optionsByItemId != null && optionsByItemId.containsKey(ie.getId())) {
                    List<QuestionOptionEntity> oes = optionsByItemId.get(ie.getId());
                    if (oes != null) {
                        for (QuestionOptionEntity oe : oes) {
                            idto.getOptions().add(QuestionOptionDTO.convertEntityToDto(oe));
                        }
                    }
                }
                dto.getItems().add(idto);
            }
        }
        if (mediaEntities != null) {
            for (QuestionMediaEntity me : mediaEntities) {
                dto.getMedia().add(QuestionMediaDTO.convertEntityToDto(me));
            }
        }
        return dto;
    }

    public QuestionEntity convertDtoToEntity() {
        QuestionEntity entity = new QuestionEntity();
        updateEntityFromDto(entity);
        if (this.id != null) entity.setId(this.id);
        return entity;
    }

    public void updateEntityFromDto(QuestionEntity entity) {
        if (entity == null) return;
        entity.setContent(this.content);
        entity.setInstruction(this.instruction);
        entity.setReadingPassage(this.readingPassage);
        entity.setQuestionType(this.questionType);
        entity.setSkill(this.skill);
        entity.setPartNo(this.partNo != null ? this.partNo : 1);
        entity.setCefrLevel(this.cefrLevel != null ? this.cefrLevel : "B1");
        entity.setDifficulty(this.difficulty != null ? this.difficulty : "MEDIUM");
        entity.setAllowOptionReuse(this.allowOptionReuse != null && this.allowOptionReuse);
        entity.setPreparationSeconds(this.preparationSeconds != null ? this.preparationSeconds : 0);
        if (this.status != null) entity.setStatus(this.status);
        entity.setUpdatedAt(LocalDateTime.now());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public String getReadingPassage() {
        return readingPassage;
    }

    public void setReadingPassage(String readingPassage) {
        this.readingPassage = readingPassage;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public Integer getPartNo() {
        return partNo;
    }

    public void setPartNo(Integer partNo) {
        this.partNo = partNo;
    }

    public String getCefrLevel() {
        return cefrLevel;
    }

    public void setCefrLevel(String cefrLevel) {
        this.cefrLevel = cefrLevel;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Boolean getAllowOptionReuse() {
        return allowOptionReuse;
    }

    public void setAllowOptionReuse(Boolean allowOptionReuse) {
        this.allowOptionReuse = allowOptionReuse;
    }

    public Integer getPreparationSeconds() {
        return preparationSeconds;
    }

    public void setPreparationSeconds(Integer preparationSeconds) {
        this.preparationSeconds = preparationSeconds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public List<QuestionItemDTO> getItems() {
        return items;
    }

    public void setItems(List<QuestionItemDTO> items) {
        this.items = items;
    }

    public List<QuestionMediaDTO> getMedia() {
        return media;
    }

    public void setMedia(List<QuestionMediaDTO> media) {
        this.media = media;
    }

    public MultipartFile getUpload() {
        return upload;
    }

    public void setUpload(MultipartFile upload) {
        this.upload = upload;
    }

    public Integer id() {
        return getId();
    }

    public String skill() {
        return getSkill();
    }

    public String type() {
        return getQuestionType();
    }

    public String content() {
        return getContent();
    }

    public String difficulty() {
        return getDifficulty();
    }
}
