package com.ski.inventory.service;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Service pro hashování hesel pomocí Argon2 (RFC 9106)
 * Používá Argon2id variantu, která je odolná proti side-channel útokům
 */
@Service
public class PasswordService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int DEFAULT_GENERATED_LENGTH = 12;
    
    private final Argon2 argon2;
    
    // Argon2 parametry podle OWASP doporučení
    // m = memory (64 MB), t = iterations (3), p = parallelism (4)
    private static final int MEMORY = 65536; // 64 MB
    private static final int ITERATIONS = 3;
    private static final int PARALLELISM = 4;
    
    public PasswordService() {
        // Použít Argon2id - nejbezpečnější varianta
        this.argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id, 32, 64);
    }
    
    /**
     * Hashuje heslo pomocí Argon2id
     * @param password Plaintext heslo
     * @return Argon2 hash string
     */
    public String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        char[] passwordChars = password.toCharArray();
        try {
            return argon2.hash(ITERATIONS, MEMORY, PARALLELISM, passwordChars);
        } finally {
            // Vymazat citlivá data z paměti
            argon2.wipeArray(passwordChars);
        }
    }
    
    /**
     * Ověří heslo proti Argon2 hashi
     * @param hash Argon2 hash string
     * @param password Plaintext heslo k ověření
     * @return true pokud heslo odpovídá hashi
     */
    public boolean verifyPassword(String hash, String password) {
        if (hash == null || password == null) {
            return false;
        }
        
        char[] passwordChars = password.toCharArray();
        try {
            return argon2.verify(hash, passwordChars);
        } catch (Exception e) {
            // V případě chyby (např. neplatný hash formát) vrátit false
            return false;
        } finally {
            argon2.wipeArray(passwordChars);
        }
    }

    /**
     * Vygeneruje náhodné bezpečné heslo.
     * @return plaintext heslo
     */
    public String generateRandomPassword() {
        return generateRandomPassword(DEFAULT_GENERATED_LENGTH);
    }

    /**
     * Vygeneruje náhodné bezpečné heslo dané délky.
     * @param length požadovaná délka hesla
     * @return plaintext heslo
     */
    public String generateRandomPassword(int length) {
        return IntStream.range(0, length)
                .mapToObj(i -> PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(PASSWORD_CHARS.length())))
                .map(Object::toString)
                .collect(Collectors.joining());
    }
}
