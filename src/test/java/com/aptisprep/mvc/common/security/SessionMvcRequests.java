package com.aptisprep.mvc.common.security;

import com.aptisprep.mvc.auth.entity.User;
import com.aptisprep.mvc.auth.repository.UserRepository;
import com.aptisprep.mvc.common.enums.Role;
import com.aptisprep.mvc.common.enums.UserStatus;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.support.WebApplicationContextUtils;

public final class SessionMvcRequests {
    private SessionMvcRequests() {}

    public static LoginUser user(String email) {
        return new LoginUser(email);
    }

    public record LoginUser(String email) {
        public RequestPostProcessor roles(String role) {
            return request -> {
                var context =
                        WebApplicationContextUtils.getRequiredWebApplicationContext(
                                request.getServletContext());
                var users = context.getBean(UserRepository.class);
                User user =
                        users.findByEmailIgnoreCase(email)
                                .orElseGet(() -> createUser(users, email, Role.valueOf(role)));
                if (user.getRole() != Role.valueOf(role)) {
                    throw new IllegalStateException("Unexpected test role for " + email);
                }
                LoginSession.store(request.getSession(), user);
                return request;
            };
        }
    }

    private static User createUser(UserRepository users, String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(email);
        user.setPasswordHash("test-hash");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return users.saveAndFlush(user);
    }

    public static RequestPostProcessor csrf() {
        return request -> {
            request.addParameter(
                    CsrfProtection.PARAMETER, CsrfProtection.token(request.getSession()));
            return request;
        };
    }
}
