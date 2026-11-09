package com.aptisprep.service.auth;

import com.aptisprep.dto.form.RegisterForm;
import com.aptisprep.entity.User;

public interface UserService {
    User getUserByEmailAndPassword(String email, String password);

    User registerStudent(RegisterForm form);
}
