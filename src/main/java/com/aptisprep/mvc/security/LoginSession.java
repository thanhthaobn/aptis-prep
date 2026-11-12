package com.aptisprep.mvc.security;

import com.aptisprep.mvc.dto.UserDTO;
import com.aptisprep.mvc.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public record LoginSession(int userId, String credentialVersion) {
    private static final String ATTRIBUTE = LoginSession.class.getName();

    public static void start(HttpServletRequest request, User user) {
        end(request);
        store(request.getSession(), user);
    }

    public static void start(HttpServletRequest request, UserDTO account) {
        end(request);
        request.getSession().setAttribute(
                ATTRIBUTE,
                new LoginSession(
                        account.getId(), credentialVersion(account.getPasswordHash())));
    }

    static void store(HttpSession session, User user) {
        session.setAttribute(ATTRIBUTE, new LoginSession(user.getId(), versionOf(user)));
    }

    public static LoginSession current(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        try {
            return session == null ? null : (LoginSession) session.getAttribute(ATTRIBUTE);
        } catch (IllegalStateException exception) {
            // A simultaneous logout can invalidate the session.
            return null;
        }
    }

    public static void end(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return;
        try {
            session.invalidate();
        } catch (IllegalStateException exception) {
            // Another request already ended the session.
        }
    }

    boolean matchesPassword(User user) {
        return credentialVersion.equals(versionOf(user));
    }

    private static String versionOf(User user) {
        return credentialVersion(user.getPasswordHash());
    }

    public static String credentialVersion(String passwordHash) {
        try {
            byte[] hash = passwordHash.getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(hash));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}







