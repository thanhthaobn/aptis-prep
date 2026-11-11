package com.aptisprep.dto;

import com.aptisprep.entity.Certificate;
import com.aptisprep.entity.User;
import com.aptisprep.entity.UserGoal;
import com.aptisprep.enums.GoalStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;

public class UserGoalDTO {
    private Integer id;
    private User user;
    private Certificate certificate;
    private Integer certificateId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @FutureOrPresent(message = "Ngày mục tiêu phải từ hôm nay trở đi")
    private LocalDate targetDate;

    private GoalStatus status = GoalStatus.ACTIVE;

    private Map<String, BigDecimal> targetScores = new LinkedHashMap<>();

    @Size(max = 10)
    @Pattern(regexp = "^(A1|A2|B1|B2|C1)?$", message = "Mục tiêu học tập chỉ hỗ trợ từ A1 đến C1")
    private String grammarVocabulary;

    @Size(max = 10)
    @Pattern(regexp = "^(A1|A2|B1|B2|C1)?$", message = "Mục tiêu học tập chỉ hỗ trợ từ A1 đến C1")
    private String reading;

    @Size(max = 10)
    @Pattern(regexp = "^(A1|A2|B1|B2|C1)?$", message = "Mục tiêu học tập chỉ hỗ trợ từ A1 đến C1")
    private String listening;

    @Size(max = 10)
    @Pattern(regexp = "^(A1|A2|B1|B2|C1)?$", message = "Mục tiêu học tập chỉ hỗ trợ từ A1 đến C1")
    private String writing;

    @Size(max = 10)
    @Pattern(regexp = "^(A1|A2|B1|B2|C1)?$", message = "Mục tiêu học tập chỉ hỗ trợ từ A1 đến C1")
    private String speaking;

    public UserGoalDTO() {}

    public UserGoalDTO(User user, Certificate certificate, LocalDate targetDate) {
        this.user = user;
        this.certificate = certificate;
        this.certificateId = certificate == null ? null : certificate.getId();
        this.targetDate = targetDate;
    }

    public static UserGoalDTO convertEntityToDto(UserGoal entity) {
        if (entity == null) return null;
        UserGoalDTO dto = new UserGoalDTO();
        dto.id = entity.getId();
        dto.user = entity.getUser();
        dto.certificate = entity.getCertificate();
        dto.certificateId =
                entity.getCertificate() == null ? null : entity.getCertificate().getId();
        dto.targetDate = entity.getTargetDate();
        dto.status = entity.getStatus();
        return dto;
    }

    public UserGoal convertDtoToEntity() {
        UserGoal entity = new UserGoal();
        updateEntityFromDto(entity);
        if (this.id != null) {
            entity.setId(this.id);
        }
        return entity;
    }

    public static UserGoal convertDtoToEntity(UserGoalDTO dto) {
        return dto == null ? null : dto.convertDtoToEntity();
    }

    public static UserGoal convertDtoToEntity(
            GoalFormDTO form, User user, Certificate certificate) {
        if (form == null) return null;
        UserGoal entity = new UserGoal();
        entity.setUser(user);
        entity.setCertificate(certificate);
        entity.setTargetDate(form.getTargetDate());
        entity.setStatus(GoalStatus.ACTIVE);
        return entity;
    }

    public void updateEntityFromDto(UserGoal entity) {
        if (entity == null) return;
        if (this.user != null) entity.setUser(this.user);
        if (this.certificate != null) entity.setCertificate(this.certificate);
        entity.setTargetDate(this.targetDate);
        if (this.status != null) entity.setStatus(this.status);
    }

    public UserGoalDTO toForm() {
        return this;
    }

    public static UserGoalDTO fromForm(GoalFormDTO form, User user, Certificate certificate) {
        if (form == null) return null;
        UserGoalDTO dto = new UserGoalDTO();
        dto.user = user;
        dto.certificate = certificate;
        dto.certificateId = certificate == null ? form.getCertificateId() : certificate.getId();
        dto.targetDate = form.getTargetDate();
        dto.targetScores = form.getTargetScores();
        dto.grammarVocabulary = form.getGrammarVocabulary();
        dto.reading = form.getReading();
        dto.listening = form.getListening();
        dto.writing = form.getWriting();
        dto.speaking = form.getSpeaking();
        return dto;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User user() {
        return user;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
        this.certificateId = certificate == null ? null : certificate.getId();
    }

    public Certificate certificate() {
        return certificate;
    }

    public Integer getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(Integer certificateId) {
        this.certificateId = certificateId;
    }

    public Integer certificateId() {
        return certificateId;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public LocalDate targetDate() {
        return targetDate;
    }

    public GoalStatus getStatus() {
        return status;
    }

    public void setStatus(GoalStatus status) {
        this.status = status;
    }

    public GoalStatus status() {
        return status;
    }

    public Map<String, BigDecimal> getTargetScores() {
        return targetScores;
    }

    public void setTargetScores(Map<String, BigDecimal> targetScores) {
        this.targetScores = targetScores == null ? new LinkedHashMap<>() : targetScores;
    }

    public Map<String, BigDecimal> targetScores() {
        return targetScores;
    }

    public String getGrammarVocabulary() {
        return grammarVocabulary;
    }

    public void setGrammarVocabulary(String grammarVocabulary) {
        this.grammarVocabulary = grammarVocabulary;
    }

    public String grammarVocabulary() {
        return grammarVocabulary;
    }

    public String getReading() {
        return reading;
    }

    public void setReading(String reading) {
        this.reading = reading;
    }

    public String reading() {
        return reading;
    }

    public String getListening() {
        return listening;
    }

    public void setListening(String listening) {
        this.listening = listening;
    }

    public String listening() {
        return listening;
    }

    public String getWriting() {
        return writing;
    }

    public void setWriting(String writing) {
        this.writing = writing;
    }

    public String writing() {
        return writing;
    }

    public String getSpeaking() {
        return speaking;
    }

    public void setSpeaking(String speaking) {
        this.speaking = speaking;
    }

    public String speaking() {
        return speaking;
    }
}
