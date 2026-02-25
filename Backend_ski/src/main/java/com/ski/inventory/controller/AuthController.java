package com.ski.inventory.controller;

import com.ski.inventory.service.AuthenticationService;
import com.ski.inventory.service.UserService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
    
    public AuthController(AuthenticationService authenticationService, UserService userService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationService.AuthResponse> refresh(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }
        String token = authHeader.substring(7);
        try {
            AuthenticationService.AuthResponse response = authenticationService.refreshToken(token);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).build();
        } catch (JwtException e) {
            // Expirovaný nebo neplatný JWT (vyhazováno už při parsování v refreshToken)
            return ResponseEntity.status(401).build();
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthenticationService.AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthenticationService.AuthResponse response = authenticationService.authenticate(
                request.username(),
                request.password()
        );
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/login/customer")
    public ResponseEntity<AuthenticationService.AuthResponse> loginCustomer(@Valid @RequestBody CustomerLoginRequest request) {
        try {
            AuthenticationService.AuthResponse response = authenticationService.authenticateCustomer(
                    request.orderNumber(),
                    request.phone()
            );
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(null);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
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
    
    public record LoginRequest(String username, String password) {}
    public record CustomerLoginRequest(String orderNumber, String phone) {}
    public record ChangePasswordRequest(String currentPassword, String newPassword) {}
    public record UpdateProfileRequest(String fullName, String email) {}
}
