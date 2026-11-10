package com.aptisprep.dto;

import com.aptisprep.dto.form.ProfileForm;
import com.aptisprep.dto.form.RegisterForm;
import com.aptisprep.entity.User;

/** DTO boundary for user fields that are safe for account forms. */
public class UserEntityDto {
    private String fullName;
    private String email;

    public static UserEntityDto fromForm(ProfileForm form) {
        if (form == null) return null;
        UserEntityDto dto = new UserEntityDto();
        dto.fullName = form.getFullName();
        return dto;
    }

    public static UserEntityDto fromForm(RegisterForm form) {
        if (form == null) return null;
        UserEntityDto dto = new UserEntityDto();
        dto.fullName = form.getFullName();
        dto.email = form.getEmail();
        return dto;
    }

    public static User convertDtoToEntity(UserEntityDto dto) {
        if (dto == null) return null;
        User entity = new User();
        entity.setFullName(dto.fullName);
        entity.setEmail(dto.email);
        return entity;
    }

    public static User convertDtoToEntity(ProfileForm form) {
        return convertDtoToEntity(fromForm(form));
    }

    public static User convertDtoToEntity(RegisterForm form) {
        return convertDtoToEntity(fromForm(form));
    }

    public static UserEntityDto convertEntityToDto(User entity) {
        if (entity == null) return null;
        UserEntityDto dto = new UserEntityDto();
        dto.fullName = entity.getFullName();
        dto.email = entity.getEmail();
        return dto;
    }

    public ProfileForm toProfileForm() {
        ProfileForm form = new ProfileForm();
        form.setFullName(fullName);
        return form;
    }

    public RegisterForm toRegisterForm() {
        RegisterForm form = new RegisterForm();
        form.setFullName(fullName);
        form.setEmail(email);
        return form;
    }

    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
}
