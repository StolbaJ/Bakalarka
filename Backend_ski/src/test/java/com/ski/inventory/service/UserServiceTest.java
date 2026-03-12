package com.ski.inventory.service;

import com.ski.inventory.model.User;
import com.ski.inventory.model.UserAuditLog;
import com.ski.inventory.model.UserRole;
import com.ski.inventory.repository.UserAuditLogRepository;
import com.ski.inventory.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserAuditLogRepository auditLogRepository;
    @Mock
    private PasswordService passwordService;
    @Mock
    private EmailService emailService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, auditLogRepository, passwordService, emailService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ===================== createUser =====================

    @Test
    void createUser_withProvidedPassword_savesUser() {
        when(userRepository.existsByUsername("techuser")).thenReturn(false);
        when(passwordService.hashPassword("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(5L);
            return u;
        });
        when(auditLogRepository.save(any(UserAuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.createUser("techuser", "password123", UserRole.TECHNICIAN,
                "Tech User", "tech@test.cz", false);

        assertThat(result.getUsername()).isEqualTo("techuser");
        assertThat(result.getRole()).isEqualTo(UserRole.TECHNICIAN);
        assertThat(result.getActive()).isTrue();
        verify(passwordService).hashPassword("password123");
        verify(emailService, never()).sendNewUserCredentials(any(), any(), any(), any());
    }

    @Test
    void createUser_withGeneratedPassword_sendsEmail() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordService.generateRandomPassword()).thenReturn("Rand0mPass!");
        when(passwordService.hashPassword("Rand0mPass!")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(auditLogRepository.save(any(UserAuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.createUser("newuser", null, UserRole.ADMIN,
                "New User", "newuser@test.cz", true);

        assertThat(result.getUsername()).isEqualTo("newuser");
        verify(emailService).sendNewUserCredentials("newuser@test.cz", "newuser", "Rand0mPass!", "New User");
    }

    @Test
    void createUser_usernameExists_throwsException() {
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThatThrownBy(() ->
                userService.createUser("existing", "pass123", UserRole.TECHNICIAN, "X", "x@x.cz", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("již existuje");
    }

    @Test
    void createUser_generatePasswordWithoutEmail_throwsException() {
        when(userRepository.existsByUsername("user")).thenReturn(false);

        assertThatThrownBy(() ->
                userService.createUser("user", null, UserRole.TECHNICIAN, "X", null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("E-mail");
    }

    @Test
    void createUser_tooShortPassword_throwsException() {
        when(userRepository.existsByUsername("user")).thenReturn(false);

        assertThatThrownBy(() ->
                userService.createUser("user", "abc", UserRole.TECHNICIAN, "X", null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alespoň 6 znaků");
    }

    // ===================== updateRole =====================

    @Test
    void updateRole_existingUser_updatesRole() {
        User user = makeUser(1L, "admin", UserRole.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateRole(1L, UserRole.TECHNICIAN);

        assertThat(result.getRole()).isEqualTo(UserRole.TECHNICIAN);
        verify(userRepository).save(user);
    }

    @Test
    void updateRole_userNotFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateRole(99L, UserRole.ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nenalezen");
    }

    // ===================== resetPassword =====================

    @Test
    void resetPassword_userWithEmail_resetsAndSendsEmail() {
        User user = makeUser(1L, "tech", UserRole.TECHNICIAN);
        user.setEmail("tech@test.cz");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordService.generateRandomPassword()).thenReturn("NewPass123!");
        when(passwordService.hashPassword("NewPass123!")).thenReturn("newHash");
        when(userRepository.save(user)).thenReturn(user);
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String newPassword = userService.resetPassword(1L);

        assertThat(newPassword).isEqualTo("NewPass123!");
        verify(emailService).sendPasswordReset("tech@test.cz", "tech", "NewPass123!", user.getFullName());
        assertThat(user.getPasswordHash()).isEqualTo("newHash");
    }

    @Test
    void resetPassword_userWithoutEmail_throwsException() {
        User user = makeUser(1L, "tech", UserRole.TECHNICIAN);
        user.setEmail(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.resetPassword(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("e-mail");
    }

    @Test
    void resetPassword_userNotFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.resetPassword(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===================== deactivateUser =====================

    @Test
    void deactivateUser_otherUser_deactivates() {
        mockAuthenticatedUser("adminUser");

        User target = makeUser(2L, "techUser", UserRole.TECHNICIAN);
        target.setActive(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.deactivateUser(2L);

        assertThat(result.getActive()).isFalse();
    }

    @Test
    void deactivateUser_ownAccount_throwsException() {
        mockAuthenticatedUser("adminUser");

        User self = makeUser(1L, "adminUser", UserRole.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> userService.deactivateUser(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vlastní");
    }

    @Test
    void deactivateUser_notFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deactivateUser(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===================== reactivateUser =====================

    @Test
    void reactivateUser_existingUser_reactivates() {
        User user = makeUser(1L, "tech", UserRole.TECHNICIAN);
        user.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.reactivateUser(1L);

        assertThat(result.getActive()).isTrue();
    }

    @Test
    void reactivateUser_notFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.reactivateUser(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===================== changeOwnPassword =====================

    @Test
    void changeOwnPassword_correctCurrentPassword_changesPassword() {
        mockAuthenticatedUser("admin");

        User user = makeUser(1L, "admin", UserRole.ADMIN);
        user.setPasswordHash("currentHash");
        when(userRepository.findByUsernameAndActiveTrue("admin")).thenReturn(Optional.of(user));
        when(passwordService.verifyPassword("currentHash", "oldPass")).thenReturn(true);
        when(passwordService.hashPassword("newSecurePass")).thenReturn("newHash");
        when(userRepository.save(user)).thenReturn(user);
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.changeOwnPassword("oldPass", "newSecurePass");

        assertThat(user.getPasswordHash()).isEqualTo("newHash");
        verify(userRepository).save(user);
    }

    @Test
    void changeOwnPassword_wrongCurrentPassword_throwsBadCredentials() {
        mockAuthenticatedUser("admin");

        User user = makeUser(1L, "admin", UserRole.ADMIN);
        user.setPasswordHash("currentHash");
        when(userRepository.findByUsernameAndActiveTrue("admin")).thenReturn(Optional.of(user));
        when(passwordService.verifyPassword("currentHash", "wrongOld")).thenReturn(false);

        assertThatThrownBy(() -> userService.changeOwnPassword("wrongOld", "newSecurePass"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("správné");
    }

    @Test
    void changeOwnPassword_newPasswordTooShort_throwsException() {
        mockAuthenticatedUser("admin");

        User user = makeUser(1L, "admin", UserRole.ADMIN);
        user.setPasswordHash("currentHash");
        when(userRepository.findByUsernameAndActiveTrue("admin")).thenReturn(Optional.of(user));
        when(passwordService.verifyPassword("currentHash", "oldPass")).thenReturn(true);

        assertThatThrownBy(() -> userService.changeOwnPassword("oldPass", "abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alespoň 6 znaků");
    }

    @Test
    void changeOwnPassword_notLoggedIn_throwsBadCredentials() {
        // SecurityContext is empty → getCurrentUsername returns "system"
        assertThatThrownBy(() -> userService.changeOwnPassword("old", "newPass"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("přihlášen");
    }

    // ===================== updateOwnProfile =====================

    @Test
    void updateOwnProfile_updatesNameAndEmail() {
        mockAuthenticatedUser("tech");

        User user = makeUser(1L, "tech", UserRole.TECHNICIAN);
        when(userRepository.findByUsernameAndActiveTrue("tech")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.updateOwnProfile("New Name", "new@email.cz");

        assertThat(result.getFullName()).isEqualTo("New Name");
        assertThat(result.getEmail()).isEqualTo("new@email.cz");
    }

    @Test
    void updateOwnProfile_notLoggedIn_throwsBadCredentials() {
        assertThatThrownBy(() -> userService.updateOwnProfile("Name", "email@test.cz"))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ===================== Helpers =====================

    private User makeUser(Long id, String username, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        user.setActive(true);
        user.setFullName("Full Name");
        return user;
    }

    private void mockAuthenticatedUser(String username) {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(username);
        when(auth.getName()).thenReturn(username);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }
}
