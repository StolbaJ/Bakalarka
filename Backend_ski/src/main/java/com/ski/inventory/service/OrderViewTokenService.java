package com.ski.inventory.service;

import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Služba pro vytváření a ověřování tokenů v odkazech „zobraz objednávku“.
 * Token obsahuje orderId a normalizované telefonní číslo, platnost 90 dní.
 */
@Service
public class OrderViewTokenService {

    private static final String CLAIM_ORDER_ID = "oid";
    private static final String CLAIM_PHONE = "ph";
    private static final String CLAIM_PURPOSE = "p";
    private static final String PURPOSE_ORDER_VIEW = "order_view";
    private static final long EXPIRATION_MS = 90L * 24 * 60 * 60 * 1000;

    private static final Pattern NON_DIGIT = Pattern.compile("\\D");

    private final JwtService jwtService;

    public OrderViewTokenService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Normalizuje telefon na pouze číslice (pro konzistentní uložení do tokenu).
     */
    public static String normalizePhone(String phone) {
        if (phone == null) return "";
        return NON_DIGIT.matcher(phone).replaceAll("").trim();
    }

    /**
     * Vygeneruje token pro odkaz na zobrazení objednávky.
     * Subject = orderId (jako řetězec), claims = phone + purpose.
     */
    public String createToken(Long orderId, String customerPhone) {
        String phoneNorm = normalizePhone(customerPhone);
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_ORDER_ID, orderId);
        claims.put(CLAIM_PHONE, phoneNorm);
        claims.put(CLAIM_PURPOSE, PURPOSE_ORDER_VIEW);
        return jwtService.createTokenWithCustomExpiry(claims, String.valueOf(orderId), EXPIRATION_MS);
    }

    /**
     * Ověří token a vrátí orderId a normalizované telefonní číslo, pokud je token platný.
     */
    public Optional<OrderViewClaims> parseToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return jwtService.parseTokenSafe(token)
                .filter(claims -> PURPOSE_ORDER_VIEW.equals(claims.get(CLAIM_PURPOSE, String.class)))
                .filter(claims -> claims.get(CLAIM_ORDER_ID) != null && claims.get(CLAIM_PHONE) != null)
                .map(claims -> new OrderViewClaims(
                        ((Number) claims.get(CLAIM_ORDER_ID)).longValue(),
                        claims.get(CLAIM_PHONE, String.class)
                ));
    }

    public record OrderViewClaims(long orderId, String phoneNormalized) {}
}
