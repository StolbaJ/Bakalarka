package com.ski.inventory.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OrderViewTokenServiceTest {

    private static final String SECRET = "order-view-secret-key-minimum-32-characters-long";
    private static final long EXPIRATION_MS = 3600_000L;

    private JwtService jwtService;
    private OrderViewTokenService orderViewTokenService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
        orderViewTokenService = new OrderViewTokenService(jwtService);
    }

    @Test
    void normalizePhone_removesNonDigits() {
        assertThat(OrderViewTokenService.normalizePhone("+420 123 456 789")).isEqualTo("420123456789");
        assertThat(OrderViewTokenService.normalizePhone("123-456-789")).isEqualTo("123456789");
        assertThat(OrderViewTokenService.normalizePhone("(603) 123 456")).isEqualTo("603123456");
    }

    @Test
    void normalizePhone_null_returnsEmpty() {
        assertThat(OrderViewTokenService.normalizePhone(null)).isEqualTo("");
    }

    @Test
    void normalizePhone_emptyString_returnsEmpty() {
        assertThat(OrderViewTokenService.normalizePhone("")).isEqualTo("");
    }

    @Test
    void normalizePhone_pureDigits_unchanged() {
        assertThat(OrderViewTokenService.normalizePhone("123456789")).isEqualTo("123456789");
    }

    @Test
    void createToken_returnsValidJwt() {
        String token = orderViewTokenService.createToken(42L, "+420 123 456 789");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void createToken_subjectIsOrderId() {
        String token = orderViewTokenService.createToken(99L, "123456789");

        String subject = jwtService.extractUsername(token);
        assertThat(subject).isEqualTo("99");
    }

    @Test
    void parseToken_validToken_returnsOrderViewClaims() {
        String token = orderViewTokenService.createToken(7L, "+420 777 888 999");

        Optional<OrderViewTokenService.OrderViewClaims> result = orderViewTokenService.parseToken(token);

        assertThat(result).isPresent();
        assertThat(result.get().orderId()).isEqualTo(7L);
        assertThat(result.get().phoneNormalized()).isEqualTo("420777888999");
    }

    @Test
    void parseToken_null_returnsEmpty() {
        assertThat(orderViewTokenService.parseToken(null)).isEmpty();
    }

    @Test
    void parseToken_blank_returnsEmpty() {
        assertThat(orderViewTokenService.parseToken("  ")).isEmpty();
    }

    @Test
    void parseToken_invalidToken_returnsEmpty() {
        assertThat(orderViewTokenService.parseToken("not.a.jwt")).isEmpty();
    }

    @Test
    void parseToken_wrongPurposeToken_returnsEmpty() {
        // Token created directly as a regular user JWT (purpose claim missing)
        String wrongToken = jwtService.generateToken("42", "CUSTOMER");

        Optional<OrderViewTokenService.OrderViewClaims> result = orderViewTokenService.parseToken(wrongToken);

        assertThat(result).isEmpty();
    }

    @Test
    void createAndParseToken_roundTrip_phoneMatchesNormalized() {
        String phone = "+420-123-456-789";
        long orderId = 123L;

        String token = orderViewTokenService.createToken(orderId, phone);
        Optional<OrderViewTokenService.OrderViewClaims> claims = orderViewTokenService.parseToken(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().orderId()).isEqualTo(orderId);
        assertThat(claims.get().phoneNormalized()).isEqualTo("420123456789");
    }
}
