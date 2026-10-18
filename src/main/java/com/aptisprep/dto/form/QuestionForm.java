package com.aptisprep.dto.form;

import java.util.ArrayList;
import java.util.List;

public class QuestionForm {
    private String content;
    private String instruction;
    private String questionType = "SINGLE_CHOICE";
    private String skill = "READING";
    private Integer partNo;
    private String cefrLevel = "B1";
    private String difficulty = "MEDIUM";
    private boolean allowOptionReuse = true;
    private Integer preparationSeconds;
    private String status = "DRAFT";
    private String itemContent;
    private Integer correctPosition;
    private Integer minWordCount;
    private Integer maxWordCount;
    private Integer responseSeconds;
    private List<OptionForm> options = new ArrayList<>();

    public QuestionForm() {
        options.add(new OptionForm("A")); options.add(new OptionForm("B")); options.add(new OptionForm("C"));
    }
    public String getContent() { return content; } public void setContent(String v) { content = v; }
    public String getInstruction() { return instruction; } public void setInstruction(String v) { instruction = v; }
    public String getQuestionType() { return questionType; } public void setQuestionType(String v) { questionType = v; }
    public String getSkill() { return skill; } public void setSkill(String v) { skill = v; }
    public Integer getPartNo() { return partNo; } public void setPartNo(Integer v) { partNo = v; }
    public String getCefrLevel() { return cefrLevel; } public void setCefrLevel(String v) { cefrLevel = v; }
    public String getDifficulty() { return difficulty; } public void setDifficulty(String v) { difficulty = v; }
    public boolean isAllowOptionReuse() { return allowOptionReuse; } public void setAllowOptionReuse(boolean v) { allowOptionReuse = v; }
    public Integer getPreparationSeconds() { return preparationSeconds; } public void setPreparationSeconds(Integer v) { preparationSeconds = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public String getItemContent() { return itemContent; } public void setItemContent(String v) { itemContent = v; }
    public Integer getCorrectPosition() { return correctPosition; } public void setCorrectPosition(Integer v) { correctPosition = v; }
    public Integer getMinWordCount() { return minWordCount; } public void setMinWordCount(Integer v) { minWordCount = v; }
    public Integer getMaxWordCount() { return maxWordCount; } public void setMaxWordCount(Integer v) { maxWordCount = v; }
    public Integer getResponseSeconds() { return responseSeconds; } public void setResponseSeconds(Integer v) { responseSeconds = v; }
    public List<OptionForm> getOptions() { return options; } public void setOptions(List<OptionForm> v) { options = v; }

    public static class OptionForm {
        private String code; private String content; private boolean correct;
        public OptionForm() {} public OptionForm(String code) { this.code = code; }
        public String getCode() { return code; } public void setCode(String v) { code = v; }
        public String getContent() { return content; } public void setContent(String v) { content = v; }
        public boolean isCorrect() { return correct; } public void setCorrect(boolean v) { correct = v; }
    }
}
