package com.aptisprep.service.account;

import com.aptisprep.dto.form.RegisterFormDTO;
import com.aptisprep.dto.view.AuthResultDTO;
import com.aptisprep.entity.User;
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

    long countByRole(com.aptisprep.enums.Role role);

    long countByRoleAndStatus(com.aptisprep.enums.Role role, com.aptisprep.enums.UserStatus status);

    org.springframework.data.domain.Page<User> searchStudents(
            String pattern, String status, org.springframework.data.domain.Pageable pageable);
}
