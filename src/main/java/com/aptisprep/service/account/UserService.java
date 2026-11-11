package com.aptisprep.service.account;

import com.aptisprep.dto.AuthResultDTO;
import com.aptisprep.dto.RegisterFormDTO;
import com.aptisprep.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

public interface UserService {
    public User getUserByEmailAndPassword(String email, String password);

    public User requireByEmail(String email);

    public User registerStudent(RegisterFormDTO form);

    public AuthResultDTO login(HttpServletRequest request, String email, String password);

    public User registerStudent(HttpServletRequest request, RegisterFormDTO form);

    public Optional<User> findById(Integer id);

    public Optional<User> findByIdForUpdate(Integer id);

    public Optional<User> findByEmailIgnoreCase(String email);

    public User save(User user);

    public Integer getUserIdByEmail(String email);

    public long countByRole(com.aptisprep.enums.Role role);

    public long countByRoleAndStatus(
            com.aptisprep.enums.Role role, com.aptisprep.enums.UserStatus status);

    public org.springframework.data.domain.Page<User> searchStudents(
            String pattern, String status, org.springframework.data.domain.Pageable pageable);
}
