package com.ski.inventory.security;

import com.ski.inventory.service.PasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Argon2PasswordEncoderTest {

    @Mock
    private PasswordService passwordService;

    private Argon2PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new Argon2PasswordEncoder(passwordService);
    }

    @Test
    void encode_delegatesToPasswordService() {
        when(passwordService.hashPassword("mySecret")).thenReturn("$argon2id$v=19$...");

        String encoded = encoder.encode("mySecret");

        assertThat(encoded).isEqualTo("$argon2id$v=19$...");
        verify(passwordService).hashPassword("mySecret");
    }

    @Test
    void encode_charSequence_passedAsString() {
        CharSequence password = new StringBuilder("testPass");
        when(passwordService.hashPassword("testPass")).thenReturn("hashed");

        String result = encoder.encode(password);

        assertThat(result).isEqualTo("hashed");
        verify(passwordService).hashPassword("testPass");
    }

    @Test
    void matches_correctPassword_returnsTrue() {
        String hash = "$argon2id$v=19$m=65536,t=3,p=4$someSalt$someHash";
        when(passwordService.verifyPassword(hash, "correct")).thenReturn(true);

        boolean result = encoder.matches("correct", hash);

        assertThat(result).isTrue();
        verify(passwordService).verifyPassword(hash, "correct");
    }

    @Test
    void matches_wrongPassword_returnsFalse() {
        String hash = "$argon2id$v=19$m=65536,t=3,p=4$someSalt$someHash";
        when(passwordService.verifyPassword(hash, "wrong")).thenReturn(false);

        boolean result = encoder.matches("wrong", hash);

        assertThat(result).isFalse();
        verify(passwordService).verifyPassword(hash, "wrong");
    }

    @Test
    void matches_charSequenceRawPassword_delegatesCorrectly() {
        CharSequence rawPassword = new StringBuilder("myPass");
        String encodedPassword = "$argon2id$hash";
        when(passwordService.verifyPassword(encodedPassword, "myPass")).thenReturn(true);

        boolean result = encoder.matches(rawPassword, encodedPassword);

        assertThat(result).isTrue();
        verify(passwordService).verifyPassword(encodedPassword, "myPass");
    }
}
