package com.ski.inventory.security;

import com.ski.inventory.monitoring.ServerErrorRecordingFilter;
import com.ski.inventory.monitoring.ServerErrorRecorder;
import com.ski.inventory.service.JwtService;
import com.ski.inventory.service.PasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Mock
    private SwaggerCookieAuthenticationFilter swaggerCookieAuthenticationFilter;

    private ServerErrorRecordingFilter serverErrorRecordingFilter;
    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        ServerErrorRecorder recorder = new ServerErrorRecorder();
        serverErrorRecordingFilter = new ServerErrorRecordingFilter(recorder);
        securityConfig = new SecurityConfig(
                jwtAuthenticationFilter,
                swaggerCookieAuthenticationFilter,
                serverErrorRecordingFilter
        );
    }

    // ===================== CORS configuration =====================

    @Test
    void corsConfigurationSource_allowsLocalhost() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertThat(source).isNotNull();

        CorsConfiguration config = ((UrlBasedCorsConfigurationSource) source)
                .getCorsConfigurations().get("/**");
        assertThat(config).isNotNull();
        assertThat(config.getAllowedOriginPatterns()).contains("http://localhost:*");
        assertThat(config.getAllowedOriginPatterns()).contains("https://localhost:*");
    }

    @Test
    void corsConfigurationSource_allowsAllHttpMethods() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration config = ((UrlBasedCorsConfigurationSource) source)
                .getCorsConfigurations().get("/**");

        assertThat(config.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
    }

    @Test
    void corsConfigurationSource_allowsAllHeaders() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration config = ((UrlBasedCorsConfigurationSource) source)
                .getCorsConfigurations().get("/**");

        assertThat(config.getAllowedHeaders()).contains("*");
    }

    @Test
    void corsConfigurationSource_allowsCredentials() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration config = ((UrlBasedCorsConfigurationSource) source)
                .getCorsConfigurations().get("/**");

        assertThat(config.getAllowCredentials()).isTrue();
    }

    @Test
    void corsConfigurationSource_maxAgeIs3600() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration config = ((UrlBasedCorsConfigurationSource) source)
                .getCorsConfigurations().get("/**");

        assertThat(config.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    void corsConfigurationSource_registeredForAllPaths() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertThat(((UrlBasedCorsConfigurationSource) source).getCorsConfigurations())
                .containsKey("/**");
    }

    @Test
    void corsConfigurationSource_allowsHttpAndHttpsWildcards() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration config = ((UrlBasedCorsConfigurationSource) source)
                .getCorsConfigurations().get("/**");

        List<String> patterns = config.getAllowedOriginPatterns();
        assertThat(patterns).contains("http://*");
        assertThat(patterns).contains("https://*");
        assertThat(patterns).contains("http://*:*");
        assertThat(patterns).contains("https://*:*");
    }

    // ===================== AuthenticationManager bean =====================

    @Test
    void authenticationManager_delegatesToAuthenticationConfiguration() throws Exception {
        org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration config =
                mock(org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration.class);
        org.springframework.security.authentication.AuthenticationManager expected =
                mock(org.springframework.security.authentication.AuthenticationManager.class);
        when(config.getAuthenticationManager()).thenReturn(expected);

        org.springframework.security.authentication.AuthenticationManager result =
                securityConfig.authenticationManager(config);

        assertThat(result).isEqualTo(expected);
    }

    // ===================== PasswordEncoder bean =====================

    @Test
    void passwordEncoder_delegatesToArgon2PasswordEncoder() {
        PasswordService passwordService = mock(PasswordService.class);

        PasswordEncoder encoder = securityConfig.passwordEncoder(passwordService);

        assertThat(encoder).isInstanceOf(Argon2PasswordEncoder.class);
    }

    @Test
    void passwordEncoder_encode_delegatesToPasswordService() {
        PasswordService passwordService = mock(PasswordService.class);
        when(passwordService.hashPassword("myPass")).thenReturn("hashed");

        PasswordEncoder encoder = securityConfig.passwordEncoder(passwordService);
        String encoded = encoder.encode("myPass");

        assertThat(encoded).isEqualTo("hashed");
    }

    @Test
    void passwordEncoder_matches_delegatesToPasswordService() {
        PasswordService passwordService = mock(PasswordService.class);
        when(passwordService.verifyPassword("storedHash", "rawPass")).thenReturn(true);

        PasswordEncoder encoder = securityConfig.passwordEncoder(passwordService);
        boolean matches = encoder.matches("rawPass", "storedHash");

        assertThat(matches).isTrue();
    }

    @Test
    void passwordEncoder_matches_wrongPassword_returnsFalse() {
        PasswordService passwordService = mock(PasswordService.class);
        when(passwordService.verifyPassword("storedHash", "wrongPass")).thenReturn(false);

        PasswordEncoder encoder = securityConfig.passwordEncoder(passwordService);
        boolean matches = encoder.matches("wrongPass", "storedHash");

        assertThat(matches).isFalse();
    }
}
