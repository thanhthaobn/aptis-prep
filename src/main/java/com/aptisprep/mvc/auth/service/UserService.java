package com.aptisprep.mvc.auth.service;

import com.aptisprep.mvc.auth.dto.AuthResultDTO;
import com.aptisprep.mvc.auth.dto.RegisterFormDTO;
import com.aptisprep.mvc.auth.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

public interface UserService {
    User getUserByEmailAndPassword(String email, String password);

    User requireByEmail(String email);

    User registerStudent(RegisterFormDTO form);

    AuthResultDTO login(HttpServletRequest request, String email, String password);

    User registerStudent(HttpServletRequest request, RegisterFormDTO form);

    Optional<User> findById(Integer id);

    Optional<User> findByIdForUpdate(Integer id);

    Optional<User> findByEmailIgnoreCase(String email);

    User save(User user);

    Integer getUserIdByEmail(String email);
}
