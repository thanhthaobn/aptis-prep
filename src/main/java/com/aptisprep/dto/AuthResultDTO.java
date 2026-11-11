package com.aptisprep.dto;

public class AuthResultDTO {
    private boolean success;
    private boolean locked;
    private boolean inactive;
    private String redirectUrl;
    private String errorMessage;
    private String lockReason;
    private String removalReason;
    private String email;

    public AuthResultDTO() {}

    public AuthResultDTO(
            boolean success,
            boolean locked,
            boolean inactive,
            String redirectUrl,
            String errorMessage,
            String lockReason,
            String removalReason,
            String email) {
        this.success = success;
        this.locked = locked;
        this.inactive = inactive;
        this.redirectUrl = redirectUrl;
        this.errorMessage = errorMessage;
        this.lockReason = lockReason;
        this.removalReason = removalReason;
        this.email = email;
    }

    public static AuthResultDTO success(String redirectUrl) {
        return new AuthResultDTO(true, false, false, redirectUrl, null, null, null, null);
    }

    public static AuthResultDTO failed(String errorMessage) {
        return new AuthResultDTO(false, false, false, null, errorMessage, null, null, null);
    }

    public static AuthResultDTO locked(String lockReason) {
        return new AuthResultDTO(false, true, false, null, null, lockReason, null, null);
    }

    public static AuthResultDTO locked(String email, String lockReason) {
        return new AuthResultDTO(false, true, false, null, null, lockReason, null, email);
    }

    public static AuthResultDTO inactive(String email, String removalReason) {
        return new AuthResultDTO(false, false, true, null, null, null, removalReason, email);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean success() {
        return success;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean locked() {
        return locked;
    }

    public boolean isInactive() {
        return inactive;
    }

    public void setInactive(boolean inactive) {
        this.inactive = inactive;
    }

    public boolean inactive() {
        return inactive;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String redirectUrl() {
        return redirectUrl;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public String getLockReason() {
        return lockReason;
    }

    public void setLockReason(String lockReason) {
        this.lockReason = lockReason;
    }

    public String lockReason() {
        return lockReason;
    }

    public String getRemovalReason() {
        return removalReason;
    }

    public void setRemovalReason(String removalReason) {
        this.removalReason = removalReason;
    }

    public String removalReason() {
        return removalReason;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String email() {
        return email;
    }
}
