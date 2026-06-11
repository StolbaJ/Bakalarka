package com.ski.inventory.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * HttpOnly cookie s JWT pro SPA – token není v JSON ani v localStorage.
 */
@Service
public class AuthCookieService {

    private final String cookieName;
    private final long maxAgeSeconds;
    private final boolean secure;
    private final String sameSite;
    private static final String COOKIE_PATH = "/api";

    public AuthCookieService(
            @Value("${app.auth.cookie-name:access_token}") String cookieName,
            @Value("${jwt.expiration:86400000}") long jwtExpirationMs,
            @Value("${app.auth.cookie-secure:true}") boolean secure,
            @Value("${app.auth.cookie-same-site:Lax}") String sameSite
    ) {
        this.cookieName = cookieName;
        this.maxAgeSeconds = Math.max(1L, jwtExpirationMs / 1000L);
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void addAuthCookie(HttpServletResponse response, String jwt) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildAuthCookie(jwt).toString());
    }

    public ResponseCookie buildAuthCookie(String jwt) {
        return ResponseCookie.from(cookieName, jwt)
                .path(COOKIE_PATH)
                .httpOnly(true)
                .secure(secure)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .sameSite(sameSite)
                .build();
    }

    public void clearAuthCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildClearCookie().toString());
    }

    public ResponseCookie buildClearCookie() {
        return ResponseCookie.from(cookieName, "")
                .path(COOKIE_PATH)
                .httpOnly(true)
                .secure(secure)
                .maxAge(Duration.ZERO)
                .sameSite(sameSite)
                .build();
    }
}
