package com.ski.inventory.controller;

import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;

/**
 * Veřejné endpointy (bez přihlášení).
 * Nápověda k výchozím přihlašovacím údajům jen v dev – na prod se nezobrazuje.
 */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final Environment environment;

    public PublicController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/credentials-hint")
    public ResponseEntity<Map<String, Object>> getCredentialsHint() {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (isProd) {
            return ResponseEntity.ok(Map.of("showHint", false));
        }
        // Dev: admin + technik (loginy pod sebou na FE)
        return ResponseEntity.ok(Map.of(
                "showHint", true,
                "logins", java.util.List.of(
                        Map.of("label", "Admin", "username", "admin", "password", "admin123"),
                        Map.of("label", "Technik", "username", "technician", "password", "tech123")
                )
        ));
    }
}
