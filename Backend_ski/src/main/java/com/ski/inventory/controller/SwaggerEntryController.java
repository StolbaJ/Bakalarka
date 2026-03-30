package com.ski.inventory.controller;

import com.ski.inventory.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Vstup do Swagger UI pro přihlášené adminy.
 * Frontend volá tento endpoint s JWT v query; backend ověří roli ADMIN,
 * nastaví krátkodobou cookie a přesměruje na Swagger UI.
 */
@RestController
@RequestMapping("/api")
public class SwaggerEntryController {

    public static final String SWAGGER_ACCESS_COOKIE = "SWAGGER_ACCESS";
    private static final int COOKIE_MAX_AGE_SECONDS = 5 * 60; // 5 min

    private final JwtService jwtService;
    private final boolean cookieSecure;

    public SwaggerEntryController(
            JwtService jwtService,
            @Value("${app.swagger.cookie-secure:true}") boolean cookieSecure) {
        this.jwtService = jwtService;
        this.cookieSecure = cookieSecure;
    }

    @GetMapping("/swagger-entry")
    public void swaggerEntry(
            @RequestParam("token") String token,
            HttpServletResponse response
    ) throws IOException {
        if (token == null || token.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing token");
            return;
        }
        try {
            String role = jwtService.extractRole(token);
            String subject = jwtService.extractUsername(token);
            if (!"ADMIN".equals(role) || !jwtService.validateToken(token, subject)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required");
                return;
            }
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

        String swaggerToken = jwtService.generateSwaggerEntryToken();
        Cookie cookie = new Cookie(SWAGGER_ACCESS_COOKIE, swaggerToken);
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
        response.sendRedirect("/swagger-ui.html");
    }
}
