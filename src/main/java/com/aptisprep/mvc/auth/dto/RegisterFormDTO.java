package com.aptisprep.mvc.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterFormDTO {

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

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, max = 72, message = "Mật khẩu phải có từ 8 đến 72 ký tự")
    @Pattern(
            regexp = "^(?=.*\\p{Lu})(?=.*\\d)(?=.*[^\\p{L}\\p{N}\\s])\\S+$",
            message = "Mật khẩu phải có chữ hoa, chữ số, ký tự đặc biệt và không chứa khoảng trắng")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    @Size(max = 72, message = "Mật khẩu xác nhận tối đa 72 ký tự")
    private String confirmPassword;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName == null ? null : fullName.strip();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.strip().toLowerCase(java.util.Locale.ROOT);
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
}

