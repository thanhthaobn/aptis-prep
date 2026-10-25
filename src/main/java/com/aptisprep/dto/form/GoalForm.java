package com.aptisprep.dto.form;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;

public class GoalForm {
    private Map<String, BigDecimal> targetScores = new LinkedHashMap<>();

    public Map<String, BigDecimal> getTargetScores() {
        return targetScores;
    }

    public void setTargetScores(Map<String, BigDecimal> value) {
        targetScores = value == null ? new LinkedHashMap<>() : value;
    }

    private Integer certificateId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @FutureOrPresent(message = "Ngày mục tiêu phải từ hôm nay trở đi")
    private LocalDate targetDate;

    @Size(max = 10)
    @Pattern(regexp = "^(A1|A2|B1|B2|C1|C2)?$", message = "CEFR không hợp lệ")
    private String grammarVocabulary;

    @Size(max = 10)
    @Pattern(regexp = "^(A1|A2|B1|B2|C1|C2)?$", message = "CEFR không hợp lệ")
    private String reading;

    @Size(max = 10)
    @Pattern(regexp = "^(A1|A2|B1|B2|C1|C2)?$", message = "CEFR không hợp lệ")
    private String listening;

    @Size(max = 10)
    @Pattern(regexp = "^(A1|A2|B1|B2|C1|C2)?$", message = "CEFR không hợp lệ")
    private String writing;

    @Size(max = 10)
    @Pattern(regexp = "^(A1|A2|B1|B2|C1|C2)?$", message = "CEFR không hợp lệ")
    private String speaking;

    public Integer getCertificateId() {
        return certificateId;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public String getGrammarVocabulary() {
        return grammarVocabulary;
    }

    public String getReading() {
        return reading;
    }

    public String getListening() {
        return listening;
    }

    public String getWriting() {
        return writing;
    }

    public String getSpeaking() {
        return speaking;
    }

    public void setCertificateId(Integer value) {
        certificateId = value;
    }

    public void setTargetDate(LocalDate value) {
        targetDate = value;
    }

    public void setGrammarVocabulary(String value) {
        grammarVocabulary = value;
    }

    public void setReading(String value) {
        reading = value;
    }

    public void setListening(String value) {
        listening = value;
    }

    public void setWriting(String value) {
        writing = value;
    }

    public void setSpeaking(String value) {
        speaking = value;
    }
}
