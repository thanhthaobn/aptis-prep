package com.aptisprep.dto;

import com.aptisprep.entity.User;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class UserDTO {
    private Integer id;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 255, message = "Họ tên không được vượt quá 255 ký tự")
    @Pattern(
            regexp = "\\p{Lu}\\p{M}*(?: *\\p{L}\\p{M}*)*",
            message =
                    "Họ tên phải bắt đầu bằng chữ in hoa và chỉ được chứa chữ cái, dấu tiếng Việt và khoảng trắng.")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String email;

    @Size(min = 8, max = 72, message = "Mật khẩu phải có từ 8 đến 72 ký tự")
    private String password;

    private String confirmPassword;
    private String currentPassword;
    private Role role = Role.STUDENT;
    private UserStatus status = UserStatus.ACTIVE;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserDTO() {}

    public UserDTO(Integer id, String fullName, String email, Role role, UserStatus status) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.status = status;
    }

    public UserDTO(String fullName, String email) {
        this.fullName = fullName;
        this.email = email;
    }

    public static UserDTO convertEntityToDto(User entity) {
        if (entity == null) return null;
        UserDTO dto = new UserDTO();
        dto.id = entity.getId();
        dto.fullName = entity.getFullName();
        dto.email = entity.getEmail();
        dto.role = entity.getRole();
        dto.status = entity.getStatus();
        dto.createdAt = entity.getCreatedAt();
        dto.updatedAt = entity.getUpdatedAt();
        return dto;
    }

    public User convertDtoToEntity() {
        User entity = new User();
        updateEntityFromDto(entity);
        if (this.id != null) {
            entity.setId(this.id);
        }
        return entity;
    }

    public static User convertDtoToEntity(UserDTO dto) {
        return dto == null ? null : dto.convertDtoToEntity();
    }

    public static User convertDtoToEntity(RegisterFormDTO form) {
        if (form == null) return null;
        User entity = new User();
        entity.setFullName(trim(form.getFullName()));
        entity.setEmail(trim(form.getEmail()));
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            entity.setPasswordHash(form.getPassword());
        }
        return entity;
    }

    public void updateEntityFromDto(User entity) {
        if (entity == null) return;
        entity.setFullName(trim(this.fullName));
        entity.setEmail(trim(this.email));
        if (this.password != null && !this.password.isBlank()) {
            entity.setPasswordHash(this.password);
        }
        if (this.role != null) entity.setRole(this.role);
        if (this.status != null) entity.setStatus(this.status);
    }

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
        dto.password = form.getPassword();
        dto.confirmPassword = form.getConfirmPassword();
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
        form.setPassword(password);
        form.setConfirmPassword(confirmPassword);
        return form;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName == null ? null : fullName.strip();
    }

    public String fullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.strip().toLowerCase(java.util.Locale.ROOT);
    }

    public String email() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String password() {
        return password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String confirmPassword() {
        return confirmPassword;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String currentPassword() {
        return currentPassword;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Role role() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public UserStatus status() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
