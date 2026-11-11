package com.aptisprep.mvc.auth.dto;

public record AuthResultDTO(
        boolean success,
        boolean locked,
        String redirectUrl,
        String errorMessage,
        String lockReason) {

    public static AuthResultDTO success(String redirectUrl) {
        return new AuthResultDTO(true, false, redirectUrl, null, null);
    }

    public static AuthResultDTO failed(String errorMessage) {
        return new AuthResultDTO(false, false, null, errorMessage, null);
    }

    public static AuthResultDTO locked(String lockReason) {
        return new AuthResultDTO(false, true, null, null, lockReason);
    }
}
