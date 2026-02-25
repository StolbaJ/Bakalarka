package com.ski.inventory.service;

import com.ski.inventory.model.Customer;
import com.ski.inventory.model.Order;
import com.ski.inventory.model.User;
import com.ski.inventory.model.UserRole;
import com.ski.inventory.repository.CustomerRepository;
import com.ski.inventory.repository.OrderRepository;
import com.ski.inventory.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;

    public AuthenticationService(
            UserRepository userRepository,
            OrderRepository orderRepository,
            CustomerRepository customerRepository,
            PasswordService passwordService,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
    }
    
    /**
     * Autentizuje uživatele pomocí username a password
     */
    @Transactional(readOnly = true)
    public AuthResponse authenticate(String username, String password) {
        User user = userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        
        if (!passwordService.verifyPassword(user.getPasswordHash(), password)) {
            throw new BadCredentialsException("Invalid username or password");
        }
        
        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        
        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getFullName(),
                user.getEmail()
        );
    }
    
    private static final Pattern NON_DIGIT = Pattern.compile("\\D");

    /** Normalizuje telefon na pouze číslice pro porovnání. */
    private static String normalizePhone(String phone) {
        if (phone == null) return "";
        return NON_DIGIT.matcher(phone).replaceAll("").trim();
    }

    /**
     * Porovná dvě normalizovaná čísla – bere v úvění +420: např. 123456789 odpovídá 420123456789.
     */
    private static boolean phonesMatch(String normalizedInput, String normalizedStored) {
        if (normalizedInput.isEmpty() || normalizedStored.isEmpty()) return false;
        if (normalizedInput.equals(normalizedStored)) return true;
        if (normalizedStored.startsWith("420") && normalizedStored.length() == 12
                && normalizedInput.length() == 9 && ("420" + normalizedInput).equals(normalizedStored)) {
            return true;
        }
        if (normalizedInput.startsWith("420") && normalizedInput.length() == 12
                && normalizedStored.length() == 9 && ("420" + normalizedStored).equals(normalizedInput)) {
            return true;
        }
        return false;
    }

    /**
     * Autentizuje zákazníka pomocí čísla objednávky a telefonu.
     * JWT subject = customer id, aby zákazník viděl všechny své objednávky.
     */
    @Transactional(readOnly = true)
    public AuthResponse authenticateCustomer(String orderNumber, String phone) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new BadCredentialsException("Objednávka nebyla nalezena"));
        Customer customer = order.getCustomer();
        if (customer == null) {
            throw new BadCredentialsException("K objednávce není přiřazen zákazník");
        }
        String normalizedInput = normalizePhone(phone);
        String normalizedStored = normalizePhone(customer.getPhone());
        if (!phonesMatch(normalizedInput, normalizedStored)) {
            throw new BadCredentialsException("Telefonní číslo nesouhlasí s objednávkou");
        }

        String customerIdSubject = customer.getId().toString();
        String token = jwtService.generateToken(customerIdSubject, UserRole.CUSTOMER.name());

        return new AuthResponse(
                token,
                customer.getId(),
                customer.getCustomerNumber() != null ? customer.getCustomerNumber() : customerIdSubject,
                UserRole.CUSTOMER.name(),
                customer.getName(),
                customer.getEmail()
        );
    }
    
    /**
     * Obnoví JWT – na základě platného tokenu vydá nový.
     * Pro uživatele (ADMIN, TECHNICIAN) se načte uživatel z DB, pro zákazníka objednávka.
     */
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BadCredentialsException("Missing or invalid token");
        }
        String subject = jwtService.extractUsername(token);
        String role = jwtService.extractRole(token);
        if (subject == null || role == null) {
            throw new BadCredentialsException("Invalid token");
        }
        if (!jwtService.validateToken(token, subject)) {
            throw new BadCredentialsException("Token expired or invalid");
        }
        if (UserRole.CUSTOMER.name().equals(role)) {
            Long customerId = Long.parseLong(subject);
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new BadCredentialsException("Zákazník nenalezen"));
            String newToken = jwtService.generateToken(subject, role);
            return new AuthResponse(
                    newToken,
                    customer.getId(),
                    customer.getCustomerNumber() != null ? customer.getCustomerNumber() : subject,
                    role,
                    customer.getName(),
                    customer.getEmail()
            );
        }
        User user = userRepository.findByUsernameAndActiveTrue(subject)
                .orElseThrow(() -> new BadCredentialsException("User not found or inactive"));
        String newToken = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(
                newToken,
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                user.getFullName(),
                user.getEmail()
        );
    }
    
    /**
     * Registruje nového uživatele
     */
    @Transactional
    public User register(String username, String password, UserRole role, String fullName, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        String passwordHash = passwordService.hashPassword(password);
        
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setActive(true);
        
        return userRepository.save(user);
    }
    
    public record AuthResponse(
            String token,
            Long userId,
            String username,
            String role,
            String fullName,
            String email
    ) {}
}
