package com.aptisprep.dto.form;

import com.aptisprep.validation.StrongPassword;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordForm {
    private String currentPassword;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới")
    @StrongPassword
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
