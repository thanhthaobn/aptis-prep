package com.aptisprep.dto.view;

public record AuthResultDTO(
        boolean success,
        boolean locked,
        boolean inactive,
        String redirectUrl,
        String errorMessage,
        String lockReason,
        String removalReason,
        String email) {

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
}
