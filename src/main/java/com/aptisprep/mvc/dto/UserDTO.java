package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.entity.*;
import com.aptisprep.mvc.enums.*;
import jakarta.validation.constraints.*;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.Locale;

public class UserDTO {
    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 255, message = "Họ tên tối đa 255 ký tự")
    @Pattern(
            regexp = "^\\p{Lu}[\\p{L}]+(?:[ -][\\p{L}]+)*$",
            message = "Họ tên phải bắt đầu bằng chữ hoa và chỉ chứa chữ cái")
    private String fullName;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email tối đa 255 ký tự")
    private String email;

    @Size(min = 8, max = 72, message = "Mật khẩu phải dài từ 8 đến 72 ký tự")
    @Pattern(
            regexp = "^(?=.*\\p{Lu})(?=.*\\d)(?=.*[^\\p{L}\\p{N}\\s])\\S+$",
            message = "Mật khẩu phải có chữ hoa, chữ số, ký tự đặc biệt và không có khoảng trắng")
    private String password;

    @Size(max = 72, message = "Mật khẩu xác nhận tối đa 72 ký tự")
    private String confirmPassword;

    private String currentPassword;
    private Integer id;
    private String passwordHash;
    private Role role;
    private String status;
    private String lockReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserDTO() {}

    public UserDTO(
            String fullName,
            String email,
            String password,
            String confirmPassword,
            String currentPassword,
            Integer id,
            String passwordHash,
            Role role,
            String status,
            String lockReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.currentPassword = currentPassword;
        this.id = id;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.lockReason = lockReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserDTO convertEntityToDto(User entity) {
        if (entity == null) return null;
        return new UserDTO(
                entity.getFullName(),
                entity.getEmail(),
                null,
                null,
                null,
                entity.getId(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getStatus() == null ? null : entity.getStatus().name(),
                entity.getLockReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static User convertDtoToEntity(UserDTO dto) {
        if (dto == null) return null;
        return new User(
                dto.getId(),
                dto.getFullName(),
                dto.getEmail(),
                dto.getPasswordHash(),
                dto.getRole(),
                dto.getStatus() == null ? null : UserStatus.valueOf(dto.getStatus()),
                dto.getLockReason(),
                dto.getCreatedAt(),
                dto.getUpdatedAt());
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName == null ? null : fullName.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLockReason() {
        return lockReason;
    }

    public void setLockReason(String lockReason) {
        this.lockReason = lockReason;
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
