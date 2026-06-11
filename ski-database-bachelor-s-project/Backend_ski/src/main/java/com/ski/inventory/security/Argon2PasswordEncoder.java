package com.ski.inventory.security;

import com.ski.inventory.service.PasswordService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Adaptér pro Spring Security – deleguje hashování hesel na PasswordService (Argon2).
 * Zajišťuje kompatibilitu s rozhraním PasswordEncoder (delegace na Argon2).
 */
public class Argon2PasswordEncoder implements PasswordEncoder {

    private final PasswordService passwordService;

    public Argon2PasswordEncoder(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return passwordService.hashPassword(rawPassword.toString());
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return passwordService.verifyPassword(encodedPassword, rawPassword.toString());
    }
}
