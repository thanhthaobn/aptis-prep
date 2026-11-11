package com.aptisprep.presentation.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public class QuestionFormDTO {
    public QuestionFormDTO(
            String content,
            String instruction,
            String readingPassage,
            String questionType,
            String skill,
            Integer partNo,
            String cefrLevel,
            String difficulty,
            boolean allowOptionReuse,
            Integer preparationSeconds,
            String status,
            String itemContent,
            Integer correctPosition,
            Integer minWordCount,
            Integer maxWordCount,
            Integer responseSeconds,
            List<OptionForm> options,
            List<ItemForm> items) {
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
        this.itemContent = itemContent;
        this.correctPosition = correctPosition;
        this.minWordCount = minWordCount;
        this.maxWordCount = maxWordCount;
        this.responseSeconds = responseSeconds;
        this.options = options;
        this.items = items;
    }

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    @Size(max = 4000)
    private String content;

    @Size(max = 4000)
    private String instruction;

    @Size(max = 12000)
    private String readingPassage;

    @NotBlank
    @Size(max = 40)
    private String questionType = "SINGLE_CHOICE";

    @NotBlank
    @Size(max = 30)
    private String skill = "READING";

    @Positive private Integer partNo;

    @Size(max = 20)
    private String cefrLevel = "B1";

    @Size(max = 20)
    private String difficulty = "MEDIUM";

    private boolean allowOptionReuse = true;

    @Min(0)
    private Integer preparationSeconds;

    @NotBlank
    @Size(max = 30)
    private String status = "DRAFT";

    @Size(max = 4000)
    private String itemContent;

    @Positive private Integer correctPosition;

    @Min(0)
    private Integer minWordCount;

    @Min(0)
    private Integer maxWordCount;

    @Min(0)
    private Integer responseSeconds;

    private List<@Valid OptionForm> options = new ArrayList<>();
    private List<@Valid ItemForm> items = new ArrayList<>();

    public QuestionFormDTO() {
        items.add(new ItemForm());
    }

    public String getContent() {
        return content;
    }

    public void setContent(String v) {
        content = v;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String v) {
        instruction = v;
    }

    public String getReadingPassage() {
        return readingPassage;
    }

    public void setReadingPassage(String v) {
        readingPassage = v;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String v) {
        questionType = v;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String v) {
        skill = v;
    }

    public Integer getPartNo() {
        return partNo;
    }

    public void setPartNo(Integer v) {
        partNo = v;
    }

    public String getCefrLevel() {
        return cefrLevel;
    }

    public void setCefrLevel(String v) {
        cefrLevel = v;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String v) {
        difficulty = v;
    }

    public boolean isAllowOptionReuse() {
        return allowOptionReuse;
    }

    public void setAllowOptionReuse(boolean v) {
        allowOptionReuse = v;
    }

    public Integer getPreparationSeconds() {
        return preparationSeconds;
    }

    public void setPreparationSeconds(Integer v) {
        preparationSeconds = v;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String v) {
        status = v;
    }

    public String getItemContent() {
        return first().getContent();
    }

    public void setItemContent(String v) {
        first().setContent(v);
        itemContent = v;
    }

    public Integer getCorrectPosition() {
        return first().getCorrectPosition();
    }

    public void setCorrectPosition(Integer v) {
        first().setCorrectPosition(v);
        correctPosition = v;
    }

    public Integer getMinWordCount() {
        return first().getMinWordCount();
    }

    public void setMinWordCount(Integer v) {
        first().setMinWordCount(v);
        minWordCount = v;
    }

    public Integer getMaxWordCount() {
        return first().getMaxWordCount();
    }

    public void setMaxWordCount(Integer v) {
        first().setMaxWordCount(v);
        maxWordCount = v;
    }

    public Integer getResponseSeconds() {
        return first().getResponseSeconds();
    }

    public void setResponseSeconds(Integer v) {
        first().setResponseSeconds(v);
        responseSeconds = v;
    }

    public List<OptionForm> getOptions() {
        return first().getOptions();
    }

    public void setOptions(List<OptionForm> v) {
        first().setOptions(v);
        options = v;
    }

    public List<ItemForm> getItems() {
        return items;
    }

    public void setItems(List<ItemForm> value) {
        items = value == null || value.isEmpty() ? new ArrayList<>(List.of(new ItemForm())) : value;
    }

    private ItemForm first() {
        if (items == null || items.isEmpty()) items = new ArrayList<>(List.of(new ItemForm()));
        return items.get(0);
    }

    public static class ItemForm {
        @Size(max = 4000)
        private String content;

        @Positive private Integer correctPosition;

        @Min(0)
        private Integer minWordCount;

        @Min(0)
        private Integer maxWordCount;

        @Min(0)
        private Integer responseSeconds;

        private List<@Valid OptionForm> options = new ArrayList<>();

        public ItemForm() {
            options.add(new OptionForm("A"));
            options.add(new OptionForm("B"));
            options.add(new OptionForm("C"));
        }

        public String getContent() {
            return content;
        }

        public void setContent(String v) {
            content = v;
        }

        public Integer getCorrectPosition() {
            return correctPosition;
        }

        public void setCorrectPosition(Integer v) {
            correctPosition = v;
        }

        public Integer getMinWordCount() {
            return minWordCount;
        }

        public void setMinWordCount(Integer v) {
            minWordCount = v;
        }

        public Integer getMaxWordCount() {
            return maxWordCount;
        }

        public void setMaxWordCount(Integer v) {
            maxWordCount = v;
        }

        public Integer getResponseSeconds() {
            return responseSeconds;
        }

        public void setResponseSeconds(Integer v) {
            responseSeconds = v;
        }

        public List<OptionForm> getOptions() {
            return options;
        }

        public void setOptions(List<OptionForm> v) {
            options = v == null ? new ArrayList<>() : v;
        }
    }

    public static class OptionForm {
        @Size(max = 20)
        private String code;

        @Size(max = 4000)
        private String content;

        private boolean correct;

        public OptionForm() {}

        public OptionForm(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String v) {
            code = v;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String v) {
            content = v;
        }

        public boolean isCorrect() {
            return correct;
        }

        public void setCorrect(boolean v) {
            correct = v;
        }
    }
}
