package com.ski.inventory.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-minimum-32-characters-long-for-hs256";
    private static final long EXPIRATION_MS = 3600_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
    }

    @Test
    void generateToken_returnsNonEmptyToken() {
        String token = jwtService.generateToken("admin", "ADMIN");
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void extractUsername_returnsSubject() {
        String token = jwtService.generateToken("technician1", "TECHNICIAN");
        assertThat(jwtService.extractUsername(token)).isEqualTo("technician1");
    }

    @Test
    void extractRole_returnsRole() {
        String token = jwtService.generateToken("user", "ADMIN");
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = jwtService.generateToken("admin", "ADMIN");
        assertThat(jwtService.validateToken(token, "admin")).isTrue();
    }

    @Test
    void validateToken_wrongUsername_returnsFalse() {
        String token = jwtService.generateToken("admin", "ADMIN");
        assertThat(jwtService.validateToken(token, "other")).isFalse();
    }

    @Test
    void parseTokenSafe_validToken_returnsClaims() {
        String token = jwtService.generateToken("customer", "CUSTOMER");
        Optional<Claims> opt = jwtService.parseTokenSafe(token);
        assertThat(opt).isPresent();
        assertThat(opt.get().getSubject()).isEqualTo("customer");
        assertThat(opt.get().get("role", String.class)).isEqualTo("CUSTOMER");
    }

    @Test
    void parseTokenSafe_nullOrBlank_returnsEmpty() {
        assertThat(jwtService.parseTokenSafe(null)).isEmpty();
        assertThat(jwtService.parseTokenSafe("")).isEmpty();
        assertThat(jwtService.parseTokenSafe("   ")).isEmpty();
    }

    @Test
    void parseTokenSafe_invalidToken_returnsEmpty() {
        assertThat(jwtService.parseTokenSafe("invalid.token.here")).isEmpty();
        assertThat(jwtService.parseTokenSafe("not-a-jwt")).isEmpty();
    }

    @Test
    void createTokenWithCustomExpiry_usesGivenExpiry() {
        String token = jwtService.createTokenWithCustomExpiry(
                Map.of("role", "ADMIN"), "swagger", 60_000L);
        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("swagger");
    }

    @Test
    void generateSwaggerEntryToken_hasAdminRole() {
        String token = jwtService.generateSwaggerEntryToken();
        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("swagger");
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }
}
