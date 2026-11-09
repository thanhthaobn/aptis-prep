package com.aptisprep.service.auth;

import com.aptisprep.dto.form.RegisterForm;
import com.aptisprep.entity.User;
import java.util.Optional;

public interface UserService {
    Optional<User> authenticate(String email, String password);

    User registerStudent(RegisterForm form);
}
