package com.ski.inventory.service;

import com.ski.inventory.model.User;
import com.ski.inventory.model.UserAuditAction;
import com.ski.inventory.model.UserAuditLog;
import com.ski.inventory.model.UserRole;
import com.ski.inventory.repository.UserAuditLogRepository;
import com.ski.inventory.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserAuditLogRepository auditLogRepository;
    private final PasswordService passwordService;
    private final EmailService emailService;

    public UserService(UserRepository userRepository,
                       UserAuditLogRepository auditLogRepository,
                       PasswordService passwordService,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordService = passwordService;
        this.emailService = emailService;
    }

    /**
     * Vytvoří nového uživatele. Pokud generatePassword=true, vygeneruje heslo a pošle na email.
     * @param generatePassword true = vygenerovat heslo a poslat emailem (vyžaduje email)
     * @return vytvořený uživatel
     */
    @Transactional
    public User createUser(String username, String password, UserRole role,
                           String fullName, String email, boolean generatePassword) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Uživatelské jméno již existuje");
        }

        String plainPassword;
        if (generatePassword) {
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("E-mail je povinný pro odeslání vygenerovaného hesla");
            }
            plainPassword = passwordService.generateRandomPassword();
        } else {
            if (password == null || password.isBlank() || password.length() < 6) {
                throw new IllegalArgumentException("Heslo musí mít alespoň 6 znaků");
            }
            plainPassword = password;
        }

        String passwordHash = passwordService.hashPassword(plainPassword);
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setActive(true);

        user = userRepository.save(user);

        if (generatePassword && email != null) {
            emailService.sendNewUserCredentials(email, username, plainPassword, fullName);
        }

        logAudit(UserAuditAction.CREATE, user.getId(), user.getUsername(),
                generatePassword ? "Heslo vygenerováno a odesláno na email" : "Účet vytvořen");

        return user;
    }

    /**
     * Změní roli uživatele.
     */
    @Transactional
    public User updateRole(Long userId, UserRole newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Uživatel nenalezen"));

        UserRole oldRole = user.getRole();
        user.setRole(newRole);
        user = userRepository.save(user);

        logAudit(UserAuditAction.UPDATE_ROLE, user.getId(), user.getUsername(),
                "Změna role: " + oldRole + " -> " + newRole);

        return user;
    }

    /**
     * Resetuje heslo uživatele - vygeneruje nové a pošle na email.
     * @return nové heslo (plaintext) - pouze pro případné zobrazení adminovi
     */
    @Transactional
    public String resetPassword(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Uživatel nenalezen"));

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("Uživatel nemá nastavený e-mail, nelze resetovat heslo");
        }

        String newPassword = passwordService.generateRandomPassword();
        user.setPasswordHash(passwordService.hashPassword(newPassword));
        userRepository.save(user);

        emailService.sendPasswordReset(user.getEmail(), user.getUsername(), newPassword, user.getFullName());

        logAudit(UserAuditAction.RESET_PASSWORD, user.getId(), user.getUsername(),
                "Heslo resetováno a odesláno na " + user.getEmail());

        return newPassword;
    }

    /**
     * Deaktivuje uživatelský účet.
     * Nelze deaktivovat vlastní účet.
     */
    @Transactional
    public User deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Uživatel nenalezen"));

        String currentUser = getCurrentUsername();
        if (user.getUsername().equals(currentUser)) {
            throw new IllegalArgumentException("Nelze deaktivovat vlastní účet");
        }

        user.setActive(false);
        user = userRepository.save(user);

        logAudit(UserAuditAction.DEACTIVATE, user.getId(), user.getUsername(), "Účet deaktivován");

        return user;
    }

    /**
     * Reaktivuje uživatelský účet.
     */
    @Transactional
    public User reactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Uživatel nenalezen"));

        user.setActive(true);
        user = userRepository.save(user);

        logAudit(UserAuditAction.REACTIVATE, user.getId(), user.getUsername(), "Účet reaktivován");

        return user;
    }

    /**
     * Změní heslo přihlášeného uživatele (ADMIN/TECHNICIAN).
     * Vyžaduje platné současné heslo.
     */
    @Transactional
    public void changeOwnPassword(String currentPassword, String newPassword) {
        String username = getCurrentUsername();
        if ("system".equals(username)) {
            throw new BadCredentialsException("Uživatel není přihlášen");
        }

        User user = userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new BadCredentialsException("Uživatel nenalezen"));

        if (!passwordService.verifyPassword(user.getPasswordHash(), currentPassword)) {
            throw new BadCredentialsException("Aktuální heslo není správné");
        }

        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 6) {
            throw new IllegalArgumentException("Nové heslo musí mít alespoň 6 znaků");
        }

        user.setPasswordHash(passwordService.hashPassword(newPassword));
        userRepository.save(user);

        logAudit(UserAuditAction.CHANGE_OWN_PASSWORD, user.getId(), user.getUsername(),
                "Uživatel změnil své heslo");
    }

    /**
     * Aktualizuje profil přihlášeného uživatele (jméno, e-mail).
     * Pouze pro uživatele s rolí ADMIN nebo TECHNICIAN (zákazníci mají údaje v objednávce).
     */
    @Transactional
    public User updateOwnProfile(String fullName, String email) {
        String username = getCurrentUsername();
        if ("system".equals(username)) {
            throw new BadCredentialsException("Uživatel není přihlášen");
        }

        User user = userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new BadCredentialsException("Uživatel nenalezen"));

        if (fullName != null) {
            user.setFullName(fullName.trim().isEmpty() ? null : fullName.trim());
        }
        if (email != null) {
            user.setEmail(email.trim().isEmpty() ? null : email.trim());
        }
        return userRepository.save(user);
    }

    private void logAudit(UserAuditAction action, Long targetUserId, String targetUsername, String details) {
        String performedBy = getCurrentUsername();
        UserAuditLog log = new UserAuditLog();
        log.setAction(action);
        log.setTargetUserId(targetUserId);
        log.setTargetUsername(targetUsername);
        log.setPerformedBy(performedBy);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "system";
    }
}
