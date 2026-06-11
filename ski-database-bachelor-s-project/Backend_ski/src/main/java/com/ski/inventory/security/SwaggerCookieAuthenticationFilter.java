package com.ski.inventory.security;

import com.ski.inventory.controller.SwaggerEntryController;
import com.ski.inventory.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Pro požadavky na Swagger UI a API docs: pokud je v requestu cookie SWAGGER_ACCESS
 * s platným tokenem, nastaví SecurityContext na ADMIN, aby hasRole("ADMIN") prošlo.
 */
@Component
public class SwaggerCookieAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public SwaggerCookieAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/swagger-ui") && !path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        // Pokud už je Bearer token, necháme to na JwtAuthenticationFilter
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String cookieToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (SwaggerEntryController.SWAGGER_ACCESS_COOKIE.equals(c.getName())) {
                    cookieToken = c.getValue();
                    break;
                }
            }
        }

        if (cookieToken != null) {
            try {
                if (Boolean.TRUE.equals(jwtService.validateToken(cookieToken, "swagger"))) {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            "swagger",
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) {
                // neplatný cookie token – nepřidáme auth
            }
        }

        filterChain.doFilter(request, response);
    }
}
