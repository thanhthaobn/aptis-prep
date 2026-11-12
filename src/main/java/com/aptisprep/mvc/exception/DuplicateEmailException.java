package com.aptisprep.mvc.exception;

public class DuplicateEmailException extends BusinessRuleException {
    public DuplicateEmailException() {
        super("Email đã được sử dụng. Vui lòng đăng nhập hoặc dùng email khác.");
    }
}







