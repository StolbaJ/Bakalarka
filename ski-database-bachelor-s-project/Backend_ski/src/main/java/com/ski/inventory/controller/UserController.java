package com.ski.inventory.controller;

import com.ski.inventory.model.User;
import com.ski.inventory.model.UserAuditLog;
import com.ski.inventory.model.UserRole;
import com.ski.inventory.repository.UserAuditLogRepository;
import com.ski.inventory.repository.UserRepository;
import com.ski.inventory.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserAuditLogRepository auditLogRepository;
    private final boolean showPasswordOnReset;

    public UserController(UserService userService, UserRepository userRepository,
                          UserAuditLogRepository auditLogRepository,
                          @Value("${app.user-management.show-generated-password-on-reset:false}") boolean showPasswordOnReset) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.showPasswordOnReset = showPasswordOnReset;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserResponse> response = users.stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getRole().name(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getPhone(),
                        user.getActive(),
                        user.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(
                request.username(),
                request.password(),
                UserRole.valueOf(request.role()),
                request.fullName(),
                request.email(),
                request.generatePassword() != null && request.generatePassword()
        );

        UserResponse response = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getActive(),
                user.getCreatedAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        User user = userService.updateRole(id, UserRole.valueOf(request.role()));
        return ResponseEntity.ok(toResponse(user));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@PathVariable Long id) {
        String newPassword = userService.resetPassword(id);
        return ResponseEntity.ok(new ResetPasswordResponse(showPasswordOnReset ? newPassword : null));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long id) {
        User user = userService.deactivateUser(id);
        return ResponseEntity.ok(toResponse(user));
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<UserResponse> reactivateUser(@PathVariable Long id) {
        User user = userService.reactivateUser(id);
        return ResponseEntity.ok(toResponse(user));
    }

    @GetMapping("/audit-log")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Long userId) {
        Page<UserAuditLog> logs = userId != null
                ? auditLogRepository.findByTargetUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                : auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));

        Page<AuditLogResponse> response = logs.map(log -> new AuditLogResponse(
                log.getId(),
                log.getAction().name(),
                log.getTargetUserId(),
                log.getTargetUsername(),
                log.getPerformedBy(),
                log.getDetails(),
                log.getCreatedAt()
        ));
        return ResponseEntity.ok(response);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getActive(),
                user.getCreatedAt()
        );
    }

    public record UserResponse(
            Long id,
            String username,
            String role,
            String fullName,
            String email,
            String phone,
            Boolean active,
            java.time.LocalDateTime createdAt
    ) {}

    public record CreateUserRequest(
            @NotBlank @Size(min = 1, max = 64) String username,
            @Size(min = 6, max = 128) String password,
            @NotBlank @Pattern(regexp = "ADMIN|TECHNICIAN") String role,
            @Size(max = 200) String fullName,
            @Email @Size(max = 255) String email,
            Boolean generatePassword
    ) {}

    public record UpdateRoleRequest(
            @NotBlank @Pattern(regexp = "ADMIN|TECHNICIAN|CUSTOMER") String role
    ) {}

    public record ResetPasswordResponse(String newPassword) {}

    public record AuditLogResponse(
            Long id,
            String action,
            Long targetUserId,
            String targetUsername,
            String performedBy,
            String details,
            java.time.LocalDateTime createdAt
    ) {}
}
