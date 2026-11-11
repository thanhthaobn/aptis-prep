package com.aptisprep.mvc.account.dto;

import com.aptisprep.mvc.account.entity.UserGoal;
import com.aptisprep.mvc.auth.entity.User;
import com.aptisprep.mvc.common.enums.GoalStatus;
import com.aptisprep.mvc.scoring.entity.Certificate;
import java.time.LocalDate;

public class UserGoalEntityDTO {
    private User user;
    private Certificate certificate;
    private LocalDate targetDate;

    public static UserGoalEntityDTO fromForm(GoalFormDTO form, User user, Certificate certificate) {
        if (form == null) return null;
        UserGoalEntityDTO dto = new UserGoalEntityDTO();
        dto.user = user;
        dto.certificate = certificate;
        dto.targetDate = form.getTargetDate();
        return dto;
    }

    public static UserGoal convertDtoToEntity(UserGoalEntityDTO dto) {
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

    public static UserGoalEntityDTO convertEntityToDto(UserGoal entity) {
        if (entity == null) return null;
        UserGoalEntityDTO dto = new UserGoalEntityDTO();
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
}
