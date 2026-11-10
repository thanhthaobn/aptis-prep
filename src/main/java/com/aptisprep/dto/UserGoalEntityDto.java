package com.aptisprep.dto;

import com.aptisprep.dto.form.GoalForm;
import com.aptisprep.entity.Certificate;
import com.aptisprep.entity.User;
import com.aptisprep.entity.UserGoal;
import com.aptisprep.mvc.common.enums.GoalStatus;

/** DTO boundary for the scalar fields of a user-goal entity. */
public class UserGoalEntityDto {
    private User user;
    private Certificate certificate;
    private java.time.LocalDate targetDate;

    public static UserGoalEntityDto fromForm(GoalForm form, User user, Certificate certificate) {
        if (form == null) return null;
        UserGoalEntityDto dto = new UserGoalEntityDto();
        dto.user = user;
        dto.certificate = certificate;
        dto.targetDate = form.getTargetDate();
        return dto;
    }

    public static UserGoal convertDtoToEntity(UserGoalEntityDto dto) {
        if (dto == null) return null;
        UserGoal entity = new UserGoal();
        entity.setUser(dto.user);
        entity.setCertificate(dto.certificate);
        entity.setTargetDate(dto.targetDate);
        entity.setStatus(GoalStatus.ACTIVE);
        return entity;
    }

    public static UserGoal convertDtoToEntity(GoalForm form, User user, Certificate certificate) {
        return convertDtoToEntity(fromForm(form, user, certificate));
    }

    public static UserGoalEntityDto convertEntityToDto(UserGoal entity) {
        if (entity == null) return null;
        UserGoalEntityDto dto = new UserGoalEntityDto();
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

    public java.time.LocalDate getTargetDate() {
        return targetDate;
    }
}
