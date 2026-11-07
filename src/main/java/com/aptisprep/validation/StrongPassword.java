package com.aptisprep.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {
    String message() default
            "Mật khẩu quá dễ đoán hoặc vượt giới hạn. " + AccountInput.PASSWORD_HINT;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
