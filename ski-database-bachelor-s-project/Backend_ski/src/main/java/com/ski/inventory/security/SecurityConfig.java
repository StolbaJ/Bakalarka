package com.ski.inventory.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ski.inventory.service.PasswordService;
import com.ski.inventory.monitoring.ServerErrorRecordingFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SwaggerCookieAuthenticationFilter swaggerCookieAuthenticationFilter;
    private final ServerErrorRecordingFilter serverErrorRecordingFilter;

    /** CSRF cookie čitelný JS (HttpOnly=false) – klient pošle hodnotu v hlavičce X-XSRF-TOKEN. */
    private static CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository r = CookieCsrfTokenRepository.withHttpOnlyFalse();
        r.setCookiePath("/");
        return r;
    }
    
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          SwaggerCookieAuthenticationFilter swaggerCookieAuthenticationFilter,
                          ServerErrorRecordingFilter serverErrorRecordingFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.swaggerCookieAuthenticationFilter = swaggerCookieAuthenticationFilter;
        this.serverErrorRecordingFilter = serverErrorRecordingFilter;
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName("_csrf");
        http
            .csrf(csrf -> csrf
                    .csrfTokenRepository(csrfTokenRepository())
                    .csrfTokenRequestHandler(csrfHandler)
                    .ignoringRequestMatchers(
                            "/api/auth/login",
                            "/api/auth/login/customer",
                            "/api/auth/refresh"
                    ))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/api/auth/change-password").hasAnyRole("ADMIN", "TECHNICIAN")
                .requestMatchers("/api/swagger-entry").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").hasRole("ADMIN")
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/technician/**").hasAnyRole("ADMIN", "TECHNICIAN")
                .requestMatchers("/api/customer/**").hasAnyRole("ADMIN", "CUSTOMER")
                .anyRequest().authenticated()
            )
            .addFilterBefore(serverErrorRecordingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(swaggerCookieAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://localhost:*",
            "https://127.0.0.1:*",
            "http://*",
            "http://*:*",
            "https://*",
            "https://*:*"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder(PasswordService passwordService) {
        return new Argon2PasswordEncoder(passwordService);
    }
}
