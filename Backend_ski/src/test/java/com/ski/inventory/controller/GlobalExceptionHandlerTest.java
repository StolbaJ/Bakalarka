package com.ski.inventory.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBadCredentials_returns401WithErrorBody() {
        BadCredentialsException ex = new BadCredentialsException("Invalid credentials");

        ResponseEntity<Map<String, String>> response = handler.handleBadCredentials(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("error");
        assertThat(response.getBody().get("error")).isEqualTo("Invalid username or password");
    }

    @Test
    void handleBadCredentials_differentMessage_alwaysReturns401() {
        BadCredentialsException ex = new BadCredentialsException("Token expired");

        ResponseEntity<Map<String, String>> response = handler.handleBadCredentials(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // Error message in body is always generic to not leak internal info
        assertThat(response.getBody()).containsEntry("error", "Invalid username or password");
    }

    @Test
    void handleBadCredentials_bodyHasExactlyOneKey() {
        BadCredentialsException ex = new BadCredentialsException("test");

        ResponseEntity<Map<String, String>> response = handler.handleBadCredentials(ex);

        assertThat(response.getBody()).hasSize(1);
    }
}
