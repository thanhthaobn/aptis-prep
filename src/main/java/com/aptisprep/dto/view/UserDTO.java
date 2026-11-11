package com.aptisprep.dto.view;

import com.aptisprep.dto.form.ProfileFormDTO;
import com.aptisprep.dto.form.RegisterFormDTO;
import com.aptisprep.entity.User;

/** DTO boundary for user fields that are safe for account forms. */
public class UserDTO {
    public UserDTO() {}

    public UserDTO(String fullName, String email) {
        this.fullName = fullName;
        this.email = email;
    }

    private String fullName;
    private String email;

    public static UserDTO fromForm(ProfileFormDTO form) {
        if (form == null) return null;
        UserDTO dto = new UserDTO();
        dto.fullName = form.getFullName();
        return dto;
    }

    public static UserDTO fromForm(RegisterFormDTO form) {
        if (form == null) return null;
        UserDTO dto = new UserDTO();
        dto.fullName = form.getFullName();
        dto.email = form.getEmail();
        return dto;
    }

    public static User convertDtoToEntity(UserDTO dto) {
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

    public static UserDTO convertEntityToDto(User entity) {
        if (entity == null) return null;
        UserDTO dto = new UserDTO();
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

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
