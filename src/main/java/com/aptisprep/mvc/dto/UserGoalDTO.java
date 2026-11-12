package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;

public class UserGoalDTO {
    private Integer id;
    private User user;
    private Certificate certificate;

    /**
     * Tên chứng chỉ đã được materialize trong service transaction để view không
     * phải truy cập trực tiếp vào một quan hệ JPA lazy.
     */
    private String certificateName;

    private GoalStatus status;
    private LocalDateTime createdAt;
    private Map<String, BigDecimal> targetScores = new LinkedHashMap<>();
    private Integer certificateId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @FutureOrPresent(message = "Ngày mục tiêu phải từ hôm nay trở đi")
    private LocalDate targetDate;

    @Size(max = 10, message = "Mục tiêu Grammar & Vocabulary tối đa 10 ký tự")
    @Pattern(regexp = "^(A1|A2|B1|B2|C1)?$", message = "Mục tiêu học tập chỉ hỗ trợ từ A1 đến C1")
    private String grammarVocabulary;

    @Size(max = 10, message = "Mục tiêu Reading tối đa 10 ký tự")
    @Pattern(regexp = "^(A1|A2|B1|B2|C1)?$", message = "Mục tiêu học tập chỉ hỗ trợ từ A1 đến C1")
    private String reading;

    @Size(max = 10, message = "Mục tiêu Listening tối đa 10 ký tự")
    @Pattern(regexp = "^(A1|A2|B1|B2|C1)?$", message = "Mục tiêu học tập chỉ hỗ trợ từ A1 đến C1")
    private String listening;

    @Size(max = 10, message = "Mục tiêu Writing tối đa 10 ký tự")
    @Pattern(regexp = "^(A1|A2|B1|B2|C1)?$", message = "Mục tiêu học tập chỉ hỗ trợ từ A1 đến C1")
    private String writing;

    @Size(max = 10, message = "Mục tiêu Speaking tối đa 10 ký tự")
    @Pattern(regexp = "^(A1|A2|B1|B2|C1)?$", message = "Mục tiêu học tập chỉ hỗ trợ từ A1 đến C1")
    private String speaking;

    private List<UserGoalSkill> skills = new ArrayList<>();

    public UserGoalDTO() {}

    public UserGoalDTO(
            Integer id,
            User user,
            Certificate certificate,
            GoalStatus status,
            LocalDateTime createdAt,
            Map<String, BigDecimal> targetScores,
            Integer certificateId,
            LocalDate targetDate,
            String grammarVocabulary,
            String reading,
            String listening,
            String writing,
            String speaking,
            List<UserGoalSkill> skills) {
        this.id = id;
        this.user = user;
        this.certificate = certificate;
        this.status = status;
        this.createdAt = createdAt;
        this.targetScores = targetScores == null ? new LinkedHashMap<>() : targetScores;
        this.certificateId = certificateId;
        this.targetDate = targetDate;
        this.grammarVocabulary = grammarVocabulary;
        this.reading = reading;
        this.listening = listening;
        this.writing = writing;
        this.speaking = speaking;
        this.skills = skills == null ? new ArrayList<>() : skills;
    }

    public static UserGoalDTO convertEntityToDto(UserGoal entity) {
        if (entity == null) return null;
        Certificate certificate = entity.getCertificate();
        UserGoalDTO dto =
                new UserGoalDTO(
                        entity.getId(),
                        entity.getUser(),
                        certificate,
                        entity.getStatus(),
                        entity.getCreatedAt(),
                        null,
                        certificate == null ? null : certificate.getId(),
                        entity.getTargetDate(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        entity.getSkills() == null
                                ? new ArrayList<>()
                                : new ArrayList<>(entity.getSkills()));
        dto.setCertificateName(certificate == null ? null : certificate.getName());
        return dto;
    }

    public String targetLevel(String skill) {
        return switch (skill) {
            case "GRAMMAR_VOCABULARY" -> grammarVocabulary;
            case "READING" -> reading;
            case "LISTENING" -> listening;
            case "WRITING" -> writing;
            case "SPEAKING" -> speaking;
            default -> null;
        };
    }

    public static UserGoal convertDtoToEntity(UserGoalDTO dto) {
        if (dto == null) return null;
        return new UserGoal(
                dto.getId(),
                dto.getUser(),
                dto.getCertificate(),
                dto.getTargetDate(),
                dto.getStatus(),
                dto.getCreatedAt(),
                dto.getSkills());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer value) {
        this.id = value;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User value) {
        this.user = value;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate value) {
        this.certificate = value;
    }

    public String getCertificateName() {
        return certificateName;
    }

    public void setCertificateName(String value) {
        this.certificateName = value;
    }

    public GoalStatus getStatus() {
        return status;
    }

    public void setStatus(GoalStatus value) {
        this.status = value;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime value) {
        this.createdAt = value;
    }

    public List<UserGoalSkill> getSkills() {
        return skills;
    }

    public void setSkills(List<UserGoalSkill> value) {
        this.skills = value == null ? new ArrayList<>() : value;
    }

    public Map<String, BigDecimal> getTargetScores() {
        return targetScores;
    }

    public void setTargetScores(Map<String, BigDecimal> value) {
        targetScores = value == null ? new LinkedHashMap<>() : value;
    }

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
