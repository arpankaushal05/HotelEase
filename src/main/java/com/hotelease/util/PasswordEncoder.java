package com.hotelease.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordEncoder {

    private static final int DEFAULT_ROUNDS = 10;

    private PasswordEncoder() {
    }

    public static String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(DEFAULT_ROUNDS));
    }

    public static boolean matches(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        return BCrypt.checkpw(rawPassword, hashedPassword);
    }
}
