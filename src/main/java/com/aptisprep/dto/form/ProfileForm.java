package com.aptisprep.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProfileForm {
    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 255, message = "Họ và tên tối đa 255 ký tự")
    @Pattern(
            regexp = "\\p{Lu}\\p{M}*(?: *\\p{L}\\p{M}*)*",
            message =
                    "Họ tên phải bắt đầu bằng chữ in hoa và chỉ được chứa chữ cái, dấu tiếng Việt và khoảng trắng.")
    private String fullName;


    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName == null ? null : fullName.strip();
    }
}
