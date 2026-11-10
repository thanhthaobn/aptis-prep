package com.aptisprep.mvc.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PasswordFormDTO {
    private String currentPassword;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới")
    @Size(min = 8, max = 72, message = "Mật khẩu phải có từ 8 đến 72 ký tự")
    @Pattern(
            regexp = "^(?=.*\\p{Lu})(?=.*\\d)(?=.*[^\\p{L}\\p{N}\\s])\\S+$",
            message = "Mật khẩu phải có chữ hoa, chữ số, ký tự đặc biệt và không chứa khoảng trắng")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    @Size(max = 72, message = "Mật khẩu xác nhận tối đa 72 ký tự")
    private String confirmPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String v) {
        currentPassword = v;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String v) {
        password = v;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String v) {
        confirmPassword = v;
    }
}

