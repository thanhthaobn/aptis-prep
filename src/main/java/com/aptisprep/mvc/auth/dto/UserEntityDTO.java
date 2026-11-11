package com.aptisprep.mvc.auth.dto;

import com.aptisprep.mvc.account.dto.ProfileFormDTO;
import com.aptisprep.mvc.auth.entity.User;

/** DTO boundary for user fields that are safe for account forms. */
public class UserEntityDTO {
    private String fullName;
    private String email;

    public static UserEntityDTO fromForm(ProfileFormDTO form) {
        if (form == null) return null;
        UserEntityDTO dto = new UserEntityDTO();
        dto.fullName = form.getFullName();
        return dto;
    }

    public static UserEntityDTO fromForm(RegisterFormDTO form) {
        if (form == null) return null;
        UserEntityDTO dto = new UserEntityDTO();
        dto.fullName = form.getFullName();
        dto.email = form.getEmail();
        return dto;
    }

    public static User convertDtoToEntity(UserEntityDTO dto) {
        if (dto == null) return null;
        User entity = new User();
        entity.setFullName(dto.fullName);
        entity.setEmail(dto.email);
        return entity;
    }

    public static User convertDtoToEntity(ProfileFormDTO form) {
        return convertDtoToEntity(fromForm(form));
    }

    public static User convertDtoToEntity(RegisterFormDTO form) {
        return convertDtoToEntity(fromForm(form));
    }

    public static UserEntityDTO convertEntityToDto(User entity) {
        if (entity == null) return null;
        UserEntityDTO dto = new UserEntityDTO();
        dto.fullName = entity.getFullName();
        dto.email = entity.getEmail();
        return dto;
    }

    public ProfileFormDTO toProfileForm() {
        ProfileFormDTO form = new ProfileFormDTO();
        form.setFullName(fullName);
        return form;
    }

    public RegisterFormDTO toRegisterForm() {
        RegisterFormDTO form = new RegisterFormDTO();
        form.setFullName(fullName);
        form.setEmail(email);
        return form;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }
}
