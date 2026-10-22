package com.aptisprep.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

public class ScoringSchemeForm {
    @NotNull(message = "Phải chọn chứng chỉ")
    private Integer certificateId;
    @NotBlank(message = "Tên bộ chấm điểm không được để trống")
    @Size(max = 255, message = "Tên bộ chấm điểm tối đa 255 ký tự")
    private String name;
    @NotBlank(message = "Phiên bản không được để trống")
    @Size(max = 30, message = "Phiên bản tối đa 30 ký tự")
    private String version;
    @NotBlank(message = "Trạng thái không được để trống")
    private String status = "ACTIVE";
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime effectiveFrom;

    public Integer getCertificateId() {
        return certificateId;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setCertificateId(Integer value) {
        certificateId = value;
    }

    public void setName(String value) {
        name = value;
    }

    public void setVersion(String value) {
        version = value;
    }

    public void setStatus(String value) {
        status = value;
    }

    public void setEffectiveFrom(LocalDateTime value) {
        effectiveFrom = value;
    }
}
