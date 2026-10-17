package com.aptisprep.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CertificateForm {
    @NotBlank(message = "Tên chứng chỉ không được để trống") @Size(max = 255, message = "Tên chứng chỉ tối đa 255 ký tự") private String name;
    @NotBlank(message = "Mã chứng chỉ không được để trống") @Size(max = 50, message = "Mã chứng chỉ tối đa 50 ký tự") private String code;
    @Size(max = 4000, message = "Mô tả quá dài") private String description;
    @NotBlank(message = "Trạng thái không được để trống") private String status = "ACTIVE";
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public void setName(String value) { name = value; }
    public void setCode(String value) { code = value; }
    public void setDescription(String value) { description = value; }
    public void setStatus(String value) { status = value; }
}
