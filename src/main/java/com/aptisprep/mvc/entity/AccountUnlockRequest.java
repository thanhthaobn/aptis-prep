package com.aptisprep.mvc.entity;

import com.aptisprep.mvc.enums.UnlockRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_unlock_requests")
public class AccountUnlockRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UnlockRequestStatus status;

    @Column(name = "admin_note", length = 1000)
    private String adminNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    private User handledBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    public Integer getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getMessage() {
        return message;
    }

    public UnlockRequestStatus getStatus() {
        return status;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public User getHandledBy() {
        return handledBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setStatus(UnlockRequestStatus status) {
        this.status = status;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    public void setHandledBy(User handledBy) {
        this.handledBy = handledBy;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
    }
}
