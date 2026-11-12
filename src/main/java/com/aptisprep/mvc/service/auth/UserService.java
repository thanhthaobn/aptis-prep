package com.aptisprep.mvc.service.auth;

import com.aptisprep.mvc.dto.*;
import com.aptisprep.mvc.entity.User;

public interface UserService {
    public UserDTO authenticate(String email, String password);

    public UserDTO findAuthenticatedById(Integer userId);

    public User getUserByEmailAndPassword(String email, String password);

    public User registerStudent(UserDTO form);
}
