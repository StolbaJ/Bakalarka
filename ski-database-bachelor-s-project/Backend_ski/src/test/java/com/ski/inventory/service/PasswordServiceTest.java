package com.ski.inventory.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordServiceTest {

    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordService();
    }

    @Test
    void hashPassword_returnsArgon2idHash() {
        String hash = passwordService.hashPassword("testPassword123");

        assertThat(hash).isNotBlank();
        assertThat(hash).startsWith("$argon2id$");
    }

    @Test
    void hashPassword_samePassword_producesDifferentHashes() {
        String hash1 = passwordService.hashPassword("samePassword");
        String hash2 = passwordService.hashPassword("samePassword");

        // Argon2 uses random salt, so hashes should differ
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void hashPassword_nullPassword_throwsException() {
        assertThatThrownBy(() -> passwordService.hashPassword(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password cannot be null or empty");
    }

    @Test
    void hashPassword_emptyPassword_throwsException() {
        assertThatThrownBy(() -> passwordService.hashPassword(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password cannot be null or empty");
    }

    @Test
    void verifyPassword_correctPassword_returnsTrue() {
        String password = "correctPassword";
        String hash = passwordService.hashPassword(password);

        assertThat(passwordService.verifyPassword(hash, password)).isTrue();
    }

    @Test
    void verifyPassword_wrongPassword_returnsFalse() {
        String hash = passwordService.hashPassword("correctPassword");

        assertThat(passwordService.verifyPassword(hash, "wrongPassword")).isFalse();
    }

    @Test
    void verifyPassword_nullHash_returnsFalse() {
        assertThat(passwordService.verifyPassword(null, "password")).isFalse();
    }

    @Test
    void verifyPassword_nullPassword_returnsFalse() {
        String hash = passwordService.hashPassword("realPassword");

        assertThat(passwordService.verifyPassword(hash, null)).isFalse();
    }

    @Test
    void verifyPassword_invalidHashFormat_returnsFalse() {
        assertThat(passwordService.verifyPassword("not-a-valid-hash", "password")).isFalse();
    }

    @Test
    void generateRandomPassword_returnsDefaultLength() {
        String password = passwordService.generateRandomPassword();

        assertThat(password).isNotBlank();
        assertThat(password).hasSize(12);
    }

    @Test
    void generateRandomPassword_customLength_returnsCorrectLength() {
        String password = passwordService.generateRandomPassword(20);

        assertThat(password).hasSize(20);
    }

    @Test
    void generateRandomPassword_multipleInvocations_returnsDifferentPasswords() {
        String p1 = passwordService.generateRandomPassword();
        String p2 = passwordService.generateRandomPassword();
        String p3 = passwordService.generateRandomPassword();

        // Extremely unlikely to get all identical
        assertThat(java.util.Set.of(p1, p2, p3)).hasSizeGreaterThan(1);
    }

    @Test
    void generateRandomPassword_containsOnlyAllowedChars() {
        String allowed = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%";
        String password = passwordService.generateRandomPassword(50);

        for (char c : password.toCharArray()) {
            assertThat(allowed).contains(String.valueOf(c));
        }
    }
}
