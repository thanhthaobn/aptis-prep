package com.aptisprep.dto.form;

import com.aptisprep.validation.AccountInput;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProfileForm {
    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 255, message = "Họ và tên tối đa 255 ký tự")
    @Pattern(
            regexp = AccountInput.NAME_PATTERN,
            message = "Họ tên cần có chữ, chỉ dùng chữ, dấu cách, dấu chấm, nháy hoặc gạch nối")
    private String fullName;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = AccountInput.name(fullName);
    }
}
