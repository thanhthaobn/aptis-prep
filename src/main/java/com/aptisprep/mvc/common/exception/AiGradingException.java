package com.aptisprep.mvc.common.exception;

/**
 * Safe user-facing message only: never include provider response bodies or keys.
 */
public class AiGradingException extends BusinessRuleException {
    public AiGradingException(String message) {
        super(message);
    }
}
