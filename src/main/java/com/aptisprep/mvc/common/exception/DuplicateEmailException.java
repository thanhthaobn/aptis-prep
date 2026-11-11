package com.aptisprep.mvc.common.exception;

public class DuplicateEmailException extends BusinessRuleException {
    public DuplicateEmailException() {
        super(
                "Email đã được sử dụng. Nếu đây là tài khoản của bạn, hãy đăng nhập để xem trạng"
                        + " thái hoặc gửi yêu cầu mở khóa.");
    }
}
