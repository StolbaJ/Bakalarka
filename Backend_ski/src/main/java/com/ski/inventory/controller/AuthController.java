package com.ski.inventory.controller;

import com.ski.inventory.dto.AuthSessionResponse;
import com.ski.inventory.security.AuthCookieService;
import com.ski.inventory.service.AuthenticationService;
import com.ski.inventory.service.UserService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final AuthCookieService authCookieService;
    
    public AuthController(
            AuthenticationService authenticationService,
            UserService userService,
            AuthCookieService authCookieService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.authCookieService = authCookieService;
    }

    /**
     * Prázdný endpoint – první GET s credentials nastaví CSRF cookie (XSRF-TOKEN) pro následné POST/PATCH/DELETE.
     */
    @GetMapping("/csrf-ping")
    public ResponseEntity<Void> csrfPing() {
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<AuthSessionResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String token = resolveAccessToken(request);
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        try {
            AuthenticationService.AuthResponse auth = authenticationService.refreshToken(token);
            authCookieService.addAuthCookie(response, auth.token());
            return ResponseEntity.ok(AuthSessionResponse.from(auth));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).build();
        } catch (JwtException e) {
            return ResponseEntity.status(401).build();
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthSessionResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthenticationService.AuthResponse auth = authenticationService.authenticate(
                request.username(),
                request.password()
        );
        authCookieService.addAuthCookie(response, auth.token());
        return ResponseEntity.ok(AuthSessionResponse.from(auth));
    }
    
    @PostMapping("/login/customer")
    public ResponseEntity<AuthSessionResponse> loginCustomer(@Valid @RequestBody CustomerLoginRequest request, HttpServletResponse response) {
        try {
            AuthenticationService.AuthResponse auth = authenticationService.authenticateCustomer(
                    request.orderNumber(),
                    request.phone()
            );
            authCookieService.addAuthCookie(response, auth.token());
            return ResponseEntity.ok(AuthSessionResponse.from(auth));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        authCookieService.clearAuthCookie(response);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        try {
            userService.changeOwnPassword(request.currentPassword(), request.newPassword());
            return ResponseEntity.ok().build();
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/profile")
    public ResponseEntity<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        if (isCustomer()) {
            return ResponseEntity.status(403).build();
        }
        try {
            userService.updateOwnProfile(request.fullName(), request.email());
            return ResponseEntity.noContent().build();
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).build();
        }
    }

    private boolean isCustomer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_CUSTOMER"::equals);
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        String name = authCookieService.getCookieName();
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
    
    public record LoginRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(max = 128) String password
    ) {}

    public record CustomerLoginRequest(
            @NotBlank @Size(max = 64) String orderNumber,
            @NotBlank @Size(max = 32) String phone
    ) {}

    public record ChangePasswordRequest(
            @NotBlank @Size(max = 128) String currentPassword,
            @NotBlank @Size(min = 6, max = 128) String newPassword
    ) {}

    public record UpdateProfileRequest(
            @Size(max = 200) String fullName,
            @Email @Size(max = 255) String email
    ) {}
}
