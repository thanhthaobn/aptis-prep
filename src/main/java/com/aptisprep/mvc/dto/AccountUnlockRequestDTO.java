package com.aptisprep.mvc.dto;

import com.aptisprep.mvc.enums.UnlockRequestStatus;
import java.time.LocalDateTime;

/** View DTO for an account-unlock request and its associated user summary. */
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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UnlockRequestStatus getStatus() {
        return status;
    }

    public void setStatus(UnlockRequestStatus status) {
        this.status = status;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    public String getHandledByEmail() {
        return handledByEmail;
    }

    public void setHandledByEmail(String handledByEmail) {
        this.handledByEmail = handledByEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
    }
}
