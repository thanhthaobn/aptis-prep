package com.aptisprep.dto;

import com.aptisprep.entity.Certificate;
import com.aptisprep.entity.User;
import com.aptisprep.entity.UserGoal;
import com.aptisprep.enums.GoalStatus;
import com.aptisprep.presentation.form.GoalFormDTO;
import java.time.LocalDate;

public class UserGoalDTO {
    public UserGoalDTO() {}

    public UserGoalDTO(User user, Certificate certificate, LocalDate targetDate) {
        this.user = user;
        this.certificate = certificate;
        this.targetDate = targetDate;
    }

    private User user;
    private Certificate certificate;
    private LocalDate targetDate;

    public static UserGoalDTO fromForm(GoalFormDTO form, User user, Certificate certificate) {
        if (form == null) return null;
        UserGoalDTO dto = new UserGoalDTO();
        dto.user = user;
        dto.certificate = certificate;
        dto.targetDate = form.getTargetDate();
        return dto;
    }

    public static UserGoal convertDtoToEntity(UserGoalDTO dto) {
        if (dto == null) return null;
        UserGoal entity = new UserGoal();
        entity.setUser(dto.user);
        entity.setCertificate(dto.certificate);
        entity.setTargetDate(dto.targetDate);
        entity.setStatus(GoalStatus.ACTIVE);
        return entity;
    }

    public static UserGoal convertDtoToEntity(
            GoalFormDTO form, User user, Certificate certificate) {
        return convertDtoToEntity(fromForm(form, user, certificate));
    }

    public static UserGoalDTO convertEntityToDto(UserGoal entity) {
        if (entity == null) return null;
        UserGoalDTO dto = new UserGoalDTO();
        dto.user = entity.getUser();
        dto.certificate = entity.getCertificate();
        dto.targetDate = entity.getTargetDate();
        return dto;
    }

    public User getUser() {
        return user;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }
}
