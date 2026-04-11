package com.ski.inventory.dto;

import com.ski.inventory.service.AuthenticationService;

/**
 * Odpověď po přihlášení / obnovení session – bez JWT v těle (token jen v HttpOnly cookie).
 */
public record AuthSessionResponse(
        Long userId,
        String username,
        String role,
        String fullName,
        String email
) {
    public static AuthSessionResponse from(AuthenticationService.AuthResponse r) {
        return new AuthSessionResponse(
                r.userId(),
                r.username(),
                r.role(),
                r.fullName(),
                r.email()
        );
    }
}
