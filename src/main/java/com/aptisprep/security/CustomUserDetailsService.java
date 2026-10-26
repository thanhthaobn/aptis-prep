package com.aptisprep.security;

import com.aptisprep.entity.User;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.repository.UserRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user =
                userRepository
                        .findByEmailIgnoreCase(email)
                        .orElseThrow(
                                () -> new UsernameNotFoundException("Không tìm thấy tài khoản"));

        boolean active = user.getStatus() == UserStatus.ACTIVE;
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                active,
                true,
                true,
                active,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }
}
