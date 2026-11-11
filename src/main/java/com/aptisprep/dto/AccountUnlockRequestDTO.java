package com.aptisprep.dto;

import com.aptisprep.entity.AccountUnlockRequest;
import com.aptisprep.enums.UnlockRequestStatus;
import java.time.LocalDateTime;

public class AccountUnlockRequestDTO {
    private Integer id;
    private Integer userId;
    private String userEmail;
    private String userFullName;
    private String userStatus;
    private String message;
    private UnlockRequestStatus status;
    private String adminNote;
    private String handledByEmail;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;

    public AccountUnlockRequestDTO() {}

    public AccountUnlockRequestDTO(
            Integer id,
            Integer userId,
            String userEmail,
            String userFullName,
            String userStatus,
            String message,
            UnlockRequestStatus status,
            String adminNote,
            String handledByEmail,
            LocalDateTime createdAt,
            LocalDateTime handledAt) {
        this.id = id;
        this.userId = userId;
        this.userEmail = userEmail;
        this.userFullName = userFullName;
        this.userStatus = userStatus;
        this.message = message;
        this.status = status;
        this.adminNote = adminNote;
        this.handledByEmail = handledByEmail;
        this.createdAt = createdAt;
        this.handledAt = handledAt;
    }

    public static AccountUnlockRequestDTO convertEntityToDto(AccountUnlockRequest entity) {
        if (entity == null) return null;
        AccountUnlockRequestDTO dto = new AccountUnlockRequestDTO();
        dto.id = entity.getId();
        if (entity.getUser() != null) {
            dto.userId = entity.getUser().getId();
            dto.userEmail = entity.getUser().getEmail();
            dto.userFullName = entity.getUser().getFullName();
            dto.userStatus =
                    entity.getUser().getStatus() == null
                            ? null
                            : entity.getUser().getStatus().name();
        }
        dto.message = entity.getMessage();
        dto.status = entity.getStatus();
        dto.adminNote = entity.getAdminNote();
        dto.handledByEmail =
                entity.getHandledBy() == null ? null : entity.getHandledBy().getEmail();
        dto.createdAt = entity.getCreatedAt();
        dto.handledAt = entity.getHandledAt();
        return dto;
    }

    public void updateEntityFromDto(AccountUnlockRequest entity) {
        if (entity == null) return;
        entity.setMessage(this.message);
        if (this.status != null) entity.setStatus(this.status);
        entity.setAdminNote(this.adminNote);
        entity.setHandledAt(this.handledAt);
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

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer userId() {
        return userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String userEmail() {
        return userEmail;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String userFullName() {
        return userFullName;
    }

    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }

    public String userStatus() {
        return userStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }

    public UnlockRequestStatus getStatus() {
        return status;
    }

    public void setStatus(UnlockRequestStatus status) {
        this.status = status;
    }

    public UnlockRequestStatus status() {
        return status;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    public String adminNote() {
        return adminNote;
    }

    public String getHandledByEmail() {
        return handledByEmail;
    }

    public void setHandledByEmail(String handledByEmail) {
        this.handledByEmail = handledByEmail;
    }

    public String handledByEmail() {
        return handledByEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
    }

    public LocalDateTime handledAt() {
        return handledAt;
    }
}
